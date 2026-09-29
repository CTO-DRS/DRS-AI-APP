// DRS AI — host smoke test: exercises generation, stop sequences, prefix-cache
// reuse and embeddings against a tiny GGUF (stories260K).
#include "drs_core.h"

#include <cassert>
#include <cstdio>
#include <string>
#include <vector>

using drs::Engine;
using drs::GenParams;
using drs::RunParams;
using drs::RunStats;

static int failures = 0;

#define CHECK(cond, msg) do { \
    if (cond) { printf("  PASS: %s\n", msg); } \
    else { printf("  FAIL: %s\n", msg); ++failures; } \
} while (0)

int main(int argc, char** argv) {
    if (argc < 2) {
        fprintf(stderr, "usage: %s <model.gguf>\n", argv[0]);
        return 2;
    }
    printf("drs-core smoke test — model: %s\n", argv[1]);
    printf("library: %s\n", Engine::library_version());

    Engine engine;
    GenParams gp;
    gp.n_ctx = 512;
    gp.n_threads = 2;
    gp.n_batch = 128;

    std::string err = engine.load(argv[1], gp);
    CHECK(err.empty(), "model load");
    if (!err.empty()) return 1;

    // 1) tokenization
    auto toks = engine.tokenize("Hello world", false, true);
    CHECK(!toks.empty(), "tokenize non-empty");
    CHECK(engine.token_count("Hello world") == (int32_t) toks.size(), "token_count matches tokenize");

    // 2) generation with streaming sink
    std::string acc;
    RunParams rp;
    rp.max_tokens = 48;
    rp.temperature = 0.1f; // near-greedy for determinism
    rp.top_k = 1;
    rp.seed = 42;
    drs::RunStats stats;
    err = engine.generate("Once upon a time", {}, rp, [&](const std::string& piece, int32_t) {
        acc += piece;
        return true;
    }, stats);
    CHECK(err.empty(), "generate");
    CHECK(!acc.empty(), "generated text non-empty");
    CHECK(stats.tokens > 0, "stats.tokens > 0");
    CHECK(stats.milliseconds > 0, "stats.ms > 0");
    CHECK(stats.prefix_len == 0, "first call is full re-encode (prefix=0)");
    printf("  gen[%d tok / %ld ms]: %s\n", stats.tokens, (long) stats.milliseconds, acc.substr(0, 60).c_str());

    // 3) prefix-cache: same prompt prefix + suffix → must reuse
    std::string acc2;
    drs::RunStats stats2;
    err = engine.generate("Once upon a time and then", {}, rp, [&](const std::string& piece, int32_t) {
        acc2 += piece;
        return true;
    }, stats2);
    CHECK(err.empty(), "generate #2");
    CHECK(stats2.prefix_len > 0, "prefix cache REUSED on extended prompt");
    CHECK(stats2.ctx_used > stats.ctx_used, "ctx_used grows with session");
    printf("  cache: prefix=%d tokens reused\n", stats2.prefix_len);

    // 4) full-contrast prompt → full re-encode
    drs::RunStats stats3;
    err = engine.generate("The dog ran fast", {}, rp, [&](const std::string&, int32_t) { return true; }, stats3);
    CHECK(err.empty(), "generate #3 (different prompt)");
    CHECK(stats3.prefix_len == 0, "unrelated prompt triggers full re-encode");

    // 5) stop sequences with holdback
    std::string acc4;
    drs::RunStats stats4;
    err = engine.generate("Once upon a time", {"day"}, rp, [&](const std::string& piece, int32_t) {
        acc4 += piece;
        return true;
    }, stats4);
    CHECK(err.empty(), "generate with stop sequence");
    CHECK(stats4.stopped_by_stop_seq, "stop sequence fired");
    CHECK(acc4.find("day") == std::string::npos, "stop word withheld from output");

    // 6) sink-driven stop
    int count = 0;
    drs::RunStats stats5;
    err = engine.generate("Once upon a time", {}, rp, [&](const std::string&, int32_t) {
        return ++count < 5;
    }, stats5);
    CHECK(err.empty(), "generate with sink stop");
    CHECK(count == 5, "sink stop respected (5 pieces)");

    // 7) context guard: oversized prompt rejected honestly
    std::string huge(20000, 'a');
    drs::RunStats stats6;
    err = engine.generate(huge, {}, rp, [&](const std::string&, int32_t) { return true; }, stats6);
    CHECK(!err.empty(), "oversized prompt rejected with clear error");
    engine.reset_session();

    // 8) embeddings (non-embedding model may fail gracefully — both outcomes OK)
    std::vector<float> vec;
    std::string eerr;
    bool ok = engine.embed("test", vec, eerr);
    printf("  embed on generative model: %s (%s)\n", ok ? "ok" : "rejected", eerr.c_str());

    // cleanup + reload sanity
    engine.unload();
    err = engine.load(argv[1], gp);
    CHECK(err.empty(), "reload after unload");

    printf("\n%s (%d failures)\n", failures == 0 ? "SMOKE TEST PASSED" : "SMOKE TEST FAILED", failures);
    return failures == 0 ? 0 : 1;
}
