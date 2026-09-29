// DRS AI — portable core over llama.cpp (b6000)
// Design notes:
//  * One Engine = one model + one context. Chat and embedder use separate engines.
//  * Incremental KV-cache: prompts that extend the previous prompt reuse the cached
//    prefix (llama_memory_seq_rm at the divergence point). Falls back to full
//    re-encode whenever the prefix check fails. Cache is honest, self-healing.
//  * Generation is stoppable via TokenSink (return false) and stop-sequences with
//    holdback (a tail up to the longest stop-sequence length is withheld until safe).
#pragma once

#include <cstdint>
#include <functional>
#include <string>
#include <vector>

namespace drs {

struct GenParams {
    int      n_ctx          = 2048;
    int      n_threads      = 2;
    int      n_batch        = 256;
    int      n_ubatch       = 256;
    bool     embed_mode     = false; // pooling=MEAN + embeddings on
    bool     use_mmap       = true;
    bool     flash_attn     = false;
    int      n_gpu_layers   = 0;     // 0 = CPU only; >0 offloads N layers (Vulkan)
    std::string backend_dir;         // directory holding libggml-*.so (dlopen registry)
};

// Load dynamic backends (CPU, Vulkan...) from dir + llama_backend_init().
// Safe to call from any module; the work happens exactly once per process.
void init_backends(const std::string& dir);

struct RunParams {
    int      max_tokens     = 512;
    float    temperature    = 0.7f;
    int      top_k          = 40;
    float    top_p          = 0.95f;
    float    min_p          = 0.05f;
    float    repeat_penalty = 1.1f;
    long     seed           = -1;    // -1 = random
};

using TokenSink = std::function<bool(const std::string& piece, int32_t token_id)>;

struct RunStats {
    int  tokens              = 0;   // generated tokens
    long milliseconds        = 0;   // wall time of the whole call
    int  prefix_len          = 0;   // KV prefix reused this call (0 = full re-encode)
    bool stopped_by_stop_seq = false;
    int  ctx_used            = 0;   // positions in KV cache after the call
};

class Engine {
public:
    Engine();
    ~Engine();
    Engine(const Engine&) = delete;
    Engine& operator=(const Engine&) = delete;

    // Returns empty string on success, error text otherwise.
    std::string load(const std::string& model_path, const GenParams& params);
    void        unload();
    bool        loaded() const;

private:
    void unload_locked();

public:

    std::vector<int32_t> tokenize(const std::string& text, bool add_special, bool parse_special) const;
    std::string          token_piece(int32_t token) const;
    int32_t              token_count(const std::string& text) const;
    int32_t              vocab_size() const;
    int32_t              n_ctx() const;

    // Streaming generation. sink returns false to stop. Throws nothing; on failure
    // returns a non-empty error string and the session state is reset.
    std::string generate(const std::string& prompt,
                         const std::vector<std::string>& stop_sequences,
                         const RunParams& params,
                         const TokenSink& sink,
                         RunStats& stats);

    // Embeddings (requires load with embed_mode=true). Returns false + err.
    bool             embed(const std::string& text, std::vector<float>& out, std::string& err);
    int              embed_dim() const;

    void             reset_session();
    size_t           cached_tokens() const;
    uint64_t         cache_prefix_hits() const;
    uint64_t         cache_full_reencodes() const;

    static const char* library_version();

private:
    struct Impl;
    Impl* impl_;
};

} // namespace drs
