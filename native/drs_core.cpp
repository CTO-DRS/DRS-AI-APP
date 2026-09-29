// DRS AI — core implementation over llama.cpp b6000
#include "drs_core.h"

#include "llama.h"
#include "ggml.h"
#include "ggml-backend.h"

#include <algorithm>
#include <chrono>
#include <cmath>
#include <cstring>
#include <mutex>

namespace drs {

static const char* kTag = "drs-core";

struct Engine::Impl {
    std::mutex           mtx;
    llama_model*         model        = nullptr;
    llama_context*       ctx          = nullptr;
    const llama_vocab*   vocab        = nullptr;
    GenParams            gp;
    int32_t              embed_dim    = 0;
    std::vector<int32_t> cached;          // tokens currently in KV cache (seq 0)
    // cache statistics
    uint64_t             prefix_hits  = 0;
    uint64_t             full_reencodes = 0;
};

static void log_callback(enum ggml_log_level level, const char* text, void* /*user*/) {
    if (level >= GGML_LOG_LEVEL_ERROR) {
        fprintf(stderr, "%s: %s", kTag, text);
    }
}

void init_backends(const std::string& dir) {
    static std::once_flag backend_once;
    std::call_once(backend_once, [&dir] {
        // Dynamic backend loading (GGML_BACKEND_DL=ON): registers CPU and,
        // when the device supports Vulkan 1.1+, the Vulkan GPU backend.
        // Failure to load a backend is non-fatal — the CPU path remains.
        ggml_backend_load_all_from_path(dir.c_str());
        llama_backend_init();
    });
}

Engine::Engine() : impl_(new Impl) {
    llama_log_set(log_callback, nullptr);
}

Engine::~Engine() {
    unload();
    delete impl_;
}

const char* Engine::library_version() {
    return ggml_version();
}

bool Engine::loaded() const { return impl_->model != nullptr && impl_->ctx != nullptr; }

std::string Engine::load(const std::string& model_path, const GenParams& params) {
    std::lock_guard<std::mutex> lock(impl_->mtx);
    unload_locked();

    init_backends(params.backend_dir.empty() ? "." : params.backend_dir);

    llama_model_params mp = llama_model_default_params();
    mp.use_mmap  = params.use_mmap;
    mp.use_mlock = false;
    mp.n_gpu_layers = params.n_gpu_layers > 0 ? params.n_gpu_layers : 0; // 0 = CPU; v1.3: Vulkan GPU offload

    impl_->model = llama_model_load_from_file(model_path.c_str(), mp);
    if (!impl_->model) {
        return "Failed to load model file: " + model_path;
    }

    llama_context_params cp = llama_context_default_params();
    cp.n_ctx            = (uint32_t) std::max(256, params.n_ctx);
    cp.n_batch          = (uint32_t) std::max(64,  params.n_batch);
    cp.n_ubatch         = (uint32_t) std::max(64,  params.n_ubatch);
    cp.n_threads        = std::max(1, params.n_threads);
    cp.n_threads_batch  = std::max(1, params.n_threads);
    cp.embeddings       = params.embed_mode;
    cp.flash_attn       = params.flash_attn;
    if (params.embed_mode) {
        cp.pooling_type = LLAMA_POOLING_TYPE_MEAN;
    }

    impl_->ctx = llama_init_from_model(impl_->model, cp);
    if (!impl_->ctx) {
        llama_model_free(impl_->model);
        impl_->model = nullptr;
        return "Failed to create llama_context (ctx=" + std::to_string(params.n_ctx) + ")";
    }

    impl_->vocab     = llama_model_get_vocab(impl_->model);
    impl_->gp        = params;
    impl_->embed_dim = params.embed_mode ? llama_n_embd(impl_->model) : 0;
    impl_->cached.clear();
    impl_->prefix_hits = 0;
    impl_->full_reencodes = 0;
    return "";
}

void Engine::unload() {
    if (!impl_) return;
    std::lock_guard<std::mutex> lock(impl_->mtx);
    unload_locked();
}

void Engine::unload_locked() {
    if (impl_->ctx)        { llama_free(impl_->ctx);           impl_->ctx = nullptr; }
    if (impl_->model)      { llama_model_free(impl_->model);   impl_->model = nullptr; }
    impl_->vocab = nullptr;
    impl_->cached.clear();
}

std::vector<int32_t> Engine::tokenize(const std::string& text, bool add_special, bool parse_special) const {
    if (!impl_->vocab) return {};
    const llama_vocab* v = impl_->vocab;
    // two-pass sizing (first pass with the real buffer; grows if needed)
    int n = text.size() + 16;
    std::vector<int32_t> tokens(n);
    int n_tokens = llama_tokenize(v, text.c_str(), (int32_t) text.size(), tokens.data(), n, add_special, parse_special);
    if (n_tokens < 0) {
        n = -n_tokens;
        tokens.resize(n);
        n_tokens = llama_tokenize(v, text.c_str(), (int32_t) text.size(), tokens.data(), n, add_special, parse_special);
        if (n_tokens < 0) return {};
    }
    tokens.resize(n_tokens);
    return tokens;
}

std::string Engine::token_piece(int32_t token) const {
    if (!impl_->vocab) return "";
    char buf[256];
    // special=0: do not render special tokens as text
    int n = llama_token_to_piece(impl_->vocab, token, buf, sizeof(buf), 0, 0);
    if (n < 0) return "";
    return std::string(buf, (size_t) n);
}

int32_t Engine::token_count(const std::string& text) const {
    return (int32_t) tokenize(text, false, true).size();
}

int32_t Engine::vocab_size() const { return impl_->vocab ? llama_vocab_n_tokens(impl_->vocab) : 0; }
int32_t Engine::n_ctx() const      { return impl_->ctx ? llama_n_ctx(impl_->ctx) : 0; }
size_t  Engine::cached_tokens() const { return impl_->cached.size(); }
uint64_t Engine::cache_prefix_hits() const { return impl_->prefix_hits; }
uint64_t Engine::cache_full_reencodes() const { return impl_->full_reencodes; }

void Engine::reset_session() {
    std::lock_guard<std::mutex> lock(impl_->mtx);
    if (impl_->ctx) {
        llama_memory_t mem = llama_get_memory(impl_->ctx);
        llama_memory_seq_rm(mem, 0, 0, -1);
    }
    impl_->cached.clear();
}

// ---- embeddings ------------------------------------------------------------

bool Engine::embed(const std::string& text, std::vector<float>& out, std::string& err) {
    std::lock_guard<std::mutex> lock(impl_->mtx);
    if (!loaded()) { err = "no model loaded"; return false; }
    if (!impl_->gp.embed_mode) { err = "engine not in embed mode"; return false; }

    auto tokens = tokenize(text, true, true);
    if (tokens.empty()) { err = "empty tokenization"; return false; }
    if ((int32_t) tokens.size() > llama_n_ctx(impl_->ctx)) {
        tokens.resize(llama_n_ctx(impl_->ctx));
    }

    llama_batch batch = llama_batch_init((uint32_t) tokens.size(), 0, 1);
    for (size_t i = 0; i < tokens.size(); ++i) {
        batch.token[i]   = tokens[i];
        batch.pos[i]     = (llama_pos) i;
        batch.n_seq_id[i] = 1;
        batch.seq_id[i][0] = 0;
        batch.logits[i]  = 0;
    }
    batch.n_tokens = (int32_t) tokens.size();

    bool ok = llama_decode(impl_->ctx, batch) == 0;
    llama_batch_free(batch);
    if (!ok) {
        llama_memory_t mem = llama_get_memory(impl_->ctx);
        llama_memory_seq_rm(mem, 0, 0, -1);
        impl_->cached.clear();
        err = "embed decode failed";
        return false;
    }

    float* emb = llama_get_embeddings_seq(impl_->ctx, 0);
    if (!emb) { err = "no embeddings returned (model may not support embeddings)"; return false; }

    int dim = llama_n_embd(impl_->model);
    out.assign(emb, emb + dim);

    // L2 normalize
    double norm = 0.0;
    for (float f : out) norm += (double) f * f;
    norm = std::sqrt(norm);
    if (norm > 1e-12) for (auto& f : out) f = (float) (f / norm);

    // keep memory bounded between embed calls
    llama_memory_t mem = llama_get_memory(impl_->ctx);
    llama_memory_seq_rm(mem, 0, 0, -1);
    impl_->cached.clear();
    return true;
}

int Engine::embed_dim() const { return impl_->embed_dim; }

// ---- generation ------------------------------------------------------------

static void batch_add(llama_batch& b, int32_t token, llama_pos pos, bool logits) {
    b.token[b.n_tokens]   = (llama_token) token;
    b.pos[b.n_tokens]     = pos;
    b.n_seq_id[b.n_tokens] = 1;
    b.seq_id[b.n_tokens][0] = 0;
    b.logits[b.n_tokens]  = logits ? 1 : 0;
    b.n_tokens++;
}

std::string Engine::generate(const std::string& prompt,
                             const std::vector<std::string>& stop_sequences,
                             const RunParams& params,
                             const TokenSink& sink,
                             RunStats& stats) {
    std::lock_guard<std::mutex> lock(impl_->mtx);
    if (!loaded()) return std::string("no model loaded");

    const auto t0 = std::chrono::steady_clock::now();

    auto prompt_tokens = tokenize(prompt, false, true);
    if (prompt_tokens.empty()) return std::string("empty prompt after tokenization");

    const int32_t ctx_limit = (int32_t) llama_n_ctx(impl_->ctx);
    const int32_t max_total = ctx_limit - 4;
    if ((int32_t) prompt_tokens.size() >= max_total) {
        return std::string("prompt exceeds context window (" + std::to_string(prompt_tokens.size()) + " >= " + std::to_string(max_total) + ")");
    }

    // ---- prefix cache reuse ------------------------------------------------
    llama_memory_t mem = llama_get_memory(impl_->ctx);
    size_t common = 0;
    const size_t cache_n = impl_->cached.size();
    const size_t max_common = std::min(cache_n, prompt_tokens.size());
    while (common < max_common && impl_->cached[common] == prompt_tokens[common]) ++common;

    bool cache_hit = false;
    if (common > 0) {
        // identical prompt (regenerate case): force at least the final token re-eval
        if (common == prompt_tokens.size()) common -= 1;
        llama_memory_seq_rm(mem, 0, (llama_pos) common, -1);
        cache_hit = common > 0;
        impl_->prefix_hits += cache_hit ? 1 : 0;
    } else {
        llama_memory_seq_rm(mem, 0, 0, -1);
        impl_->full_reencodes += 1;
    }
    impl_->cached.resize(common);

    // ---- prefill ------------------------------------------------------------
    int32_t n_past = (int32_t) common;
    const int32_t n_batch = std::max(64, impl_->gp.n_batch);
    size_t i = common;
    while (i < prompt_tokens.size()) {
        size_t take = std::min((size_t) n_batch, prompt_tokens.size() - i);
        llama_batch batch = llama_batch_init((uint32_t) take, 0, 1);
        size_t produced = 0;
        for (size_t k = 0; k < take; ++k) {
            const bool want_logits = (i + k + 1 == prompt_tokens.size());
            batch_add(batch, prompt_tokens[i + k], n_past + (llama_pos) produced, want_logits);
            if (want_logits) break; // stop prefill batch at token that must produce logits
            produced++;
        }
        // include the logits token itself in the count
        int32_t decoded = (int32_t) produced + 1;
        batch.n_tokens = decoded;
        if (llama_decode(impl_->ctx, batch) != 0) {
            llama_batch_free(batch);
            llama_memory_seq_rm(mem, 0, 0, -1);
            impl_->cached.clear();
            return std::string("prefill decode failed (context may be exhausted)");
        }
        llama_batch_free(batch);
        n_past += decoded;
        i += decoded;
    }
    impl_->cached.assign(prompt_tokens.begin(), prompt_tokens.end());

    // ---- sampling chain -------------------------------------------------------
    llama_sampler_chain_params sparams = llama_sampler_chain_default_params();
    sparams.no_perf = true;
    llama_sampler* chain = llama_sampler_chain_init(sparams);
    const bool greedy = params.temperature <= 0.0f;
    if (params.repeat_penalty != 1.0f) {
        llama_sampler_chain_add(chain, llama_sampler_init_penalties(64, params.repeat_penalty, 0.0f, 0.0f));
    }
    if (greedy) {
        llama_sampler_chain_add(chain, llama_sampler_init_greedy());
    } else {
        if (params.top_k > 0) llama_sampler_chain_add(chain, llama_sampler_init_top_k(params.top_k));
        llama_sampler_chain_add(chain, llama_sampler_init_top_p(params.top_p, 1));
        llama_sampler_chain_add(chain, llama_sampler_init_min_p(params.min_p, 1));
        llama_sampler_chain_add(chain, llama_sampler_init_temp(params.temperature));
        const uint32_t seed = params.seed < 0
            ? (uint32_t) (std::chrono::steady_clock::now().time_since_epoch().count() & 0xFFFFFFFFu)
            : (uint32_t) params.seed;
        llama_sampler_chain_add(chain, llama_sampler_init_dist(seed));
    }

    // ---- stop sequences state ---------------------------------------------------
    size_t max_stop = 0;
    for (const auto& s : stop_sequences) max_stop = std::max(max_stop, s.size());

    std::string full_text;
    std::string pending;               // withheld tail (holdback)
    int32_t n_generated = 0;
    bool stopped_by_stop = false;
    std::string error;

    auto flush_pending = [&](bool flush_all) {
        if (flush_all) {
            full_text += pending;
            pending.clear();
        } else if (pending.size() > max_stop) {
            const size_t emit = pending.size() - max_stop;
            full_text.append(pending, 0, emit);
            pending.erase(0, emit);
        }
    };

    auto check_stop = [&]() -> bool {
        if (stop_sequences.empty()) return false;
        const std::string hay = full_text + pending;
        for (const auto& s : stop_sequences) {
            if (s.empty()) continue;
            size_t pos = hay.rfind(s);
            if (pos != std::string::npos) {
                // cut at first occurrence within hay
                size_t first = hay.find(s);
                const std::string kept = hay.substr(0, first);
                const std::string over = hay.substr(first + s.size());
                full_text = kept;
                pending = over; // text after the stop marker (already emitted pieces) is dropped
                return true;
            }
        }
        return false;
    };

    llama_batch batch = llama_batch_init(1, 0, 1);
    bool logits_ready = true; // prefill left logits on the last prompt token

    while (n_generated < params.max_tokens) {
        if (!logits_ready) { error = "internal error: no logits"; break; }

        // sample from the most recent logits (-1 = last output token)
        llama_token next = llama_sampler_sample(chain, impl_->ctx, -1);

        if (llama_vocab_is_eog(impl_->vocab, next)) {
            flush_pending(true);
            break;
        }

        const std::string piece = token_piece(next);
        n_generated++;
        if (!piece.empty()) {
            pending += piece;
        }
        bool user_stop = false;
        if (!piece.empty() && sink) {
            // stream only what is safe to show (keeps stop-seq holdback)
            std::string safe = pending.substr(0, pending.size() > max_stop ? pending.size() - max_stop : 0);
            if (!safe.empty()) {
                if (!sink(safe, next)) user_stop = true;
            }
        }

        // feed token into KV cache
        batch.n_tokens = 0;
        batch_add(batch, next, n_past, true);
        if (llama_decode(impl_->ctx, batch) != 0) {
            error = "decode failed (context exhausted)";
            flush_pending(true);
            break;
        }
        n_past++;
        impl_->cached.push_back(next);

        if (check_stop()) { stopped_by_stop = true; flush_pending(true); break; }
        if (user_stop)    { flush_pending(true); break; }
        flush_pending(false);

        if ((int32_t) impl_->cached.size() >= max_total) {
            // honest guard: cache is full — end the turn cleanly
            flush_pending(true);
            break;
        }
    }
    llama_batch_free(batch);
    llama_sampler_free(chain);

    if (sink && !pending.empty()) {
        // deliver any withheld tail at end of turn
        if (!sink(pending, 0)) { /* ignore sink result at final flush */ }
        pending.clear();
    }

    const auto t1 = std::chrono::steady_clock::now();
    stats.tokens = n_generated;
    stats.milliseconds = std::chrono::duration_cast<std::chrono::milliseconds>(t1 - t0).count();
    stats.prefix_len = cache_hit ? (int32_t) common : 0;
    stats.stopped_by_stop_seq = stopped_by_stop;
    stats.ctx_used = (int32_t) impl_->cached.size();

    if (!error.empty()) {
        llama_memory_seq_rm(mem, 0, 0, -1);
        impl_->cached.clear();
        return error;
    }
    return "";
}

} // namespace drs
