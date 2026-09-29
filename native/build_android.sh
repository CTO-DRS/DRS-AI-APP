#!/usr/bin/env bash
# DRS AI — Native build for arm64-v8a (llama.cpp b6000 + Vulkan GPU backend)
# Reconstructed for v1.3.0: adds GGML_VULKAN and fixes missing libc++_shared.so.
set -euo pipefail

REPO=/home/z/my-project/DRS-AI-APP-src
SDK=/tmp/my-project/android-sdk
NDK=$SDK/ndk/27.2.12479018
CMAKE=$SDK/cmake/3.31.1/bin/cmake
NINJA=$SDK/cmake/3.31.1/bin/ninja
GLSLC_DIR=/home/z/my-project/tools/glslc-local/usr
LLAMA=$REPO/native/third_party/llama.cpp
WHISPER=$REPO/native/third_party/whisper.cpp
JNI=$REPO/native
OUT=$REPO/app/src/main/jniLibs/arm64-v8a
BUILD=$REPO/native/build-android

export ANDROID_NDK_HOME=$NDK
export PATH="$GLSLC_DIR/bin:$SDK/cmake/3.31.1/bin:$PATH"
export LD_LIBRARY_PATH="$GLSLC_DIR/lib/x86_64-linux-gnu:${LD_LIBRARY_PATH:-}"
export VULKAN_SDK=$GLSLC_DIR

STRIP=$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip
CLANGXX=$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android26-clang++

VKLIB=$NDK/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/lib/aarch64-linux-android/35/libvulkan.so
VKINC=/home/z/my-project/tools/Vulkan-Headers/include

cmd="${1:-all}"
mkdir -p "$OUT" "$BUILD"

configure() {
  echo "== CMake configure (Vulkan ON) =="
  "$CMAKE" -S "$LLAMA" -B "$BUILD" -G Ninja \
    -DCMAKE_MAKE_PROGRAM=$NINJA \
    -DCMAKE_TOOLCHAIN_FILE=$NDK/build/cmake/android.toolchain.cmake \
    -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-26 \
    -DCMAKE_BUILD_TYPE=Release \
    -DBUILD_SHARED_LIBS=ON \
    -DGGML_VULKAN=ON \
    -DGGML_OPENMP=OFF \
    -DGGML_BACKEND_DL=ON \
    -DLLAMA_CURL=OFF \
    -DLLAMA_BUILD_TESTS=OFF -DLLAMA_BUILD_EXAMPLES=OFF -DLLAMA_BUILD_SERVER=OFF \
    -DVulkan_LIBRARY=$VKLIB \
    -DVulkan_INCLUDE_DIR=$VKINC \
    -DVulkan_GLSLC_EXECUTABLE=$GLSLC_DIR/bin/glslc
  echo "== configure done =="
}

build_target() { "$CMAKE" --build "$BUILD" --target "$1" -- -j1 && echo "== target $1 done =="; }

install_llama() {
  for f in libggml.so libggml-base.so libggml-cpu.so libggml-vulkan.so libllama.so libmtmd.so; do
    src=$(find "$BUILD" -name "$f" | head -1)
    [ -z "$src" ] && { echo "MISSING $f"; exit 1; }
    "$STRIP" --strip-unneeded "$src"
    cp "$src" "$OUT/$f"
    echo "installed $f ($(du -h "$OUT/$f" | cut -f1))"
  done
  # CRITICAL FIX: ship libc++_shared.so (JNI libs need it; was missing in <= v1.2.1)
  cp "$NDK/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/lib/aarch64-linux-android/libc++_shared.so" "$OUT/"
  "$STRIP" --strip-unneeded "$OUT/libc++_shared.so" || true
  echo "installed libc++_shared.so (critical fix)"
}

build_jni() {
  LL=$REPO/native/build-android
  INC="-I$JNI -I$LLAMA/include -I$LLAMA/ggml/include"
  LIBS="-L$OUT -lllama -lggml -lggml-base -llog -landroid"

  echo "== libdrs_core_jni.so =="
  $CLANGXX -shared -fPIC -O3 -std=c++17 -Wl,--no-undefined $INC \
    "$JNI/drs_core.cpp" "$JNI/drs_jni.cpp" $LIBS \
    -o "$OUT/libdrs_core_jni.so"
  "$STRIP" --strip-unneeded "$OUT/libdrs_core_jni.so"

  echo "== libdrs_vision_jni.so =="
  $CLANGXX -shared -fPIC -O3 -std=c++17 -Wl,--no-undefined $INC -I$LLAMA/tools/mtmd \
    "$JNI/drs_vision_jni.cpp" $LIBS -lmtmd \
    -o "$OUT/libdrs_vision_jni.so"
  "$STRIP" --strip-unneeded "$OUT/libdrs_vision_jni.so"

  echo "== libdrs_whisper_jni.so (whisper v1.7.4 static-in) =="
  $CLANGXX -shared -fPIC -O3 -std=c++17 -Wl,--no-undefined $INC -I$WHISPER/include \
    "$JNI/drs_whisper_jni.cpp" "$WHISPER/src/whisper.cpp" \
    -L$OUT -lggml -lggml-base -lggml-cpu -llog -landroid \
    -o "$OUT/libdrs_whisper_jni.so"
  "$STRIP" --strip-unneeded "$OUT/libdrs_whisper_jni.so"
  echo "== JNI done =="
}

case "$cmd" in
  configure) configure ;;
  ggml-base) build_target ggml-base ;;
  ggml-cpu)  build_target ggml-cpu ;;
  ggml)      build_target ggml ;;
  llama)     build_target llama ;;
  mtmd)      build_target mtmd ;;
  install)   install_llama ;;
  jni)       build_jni ;;
  all)
    configure
    for t in ggml-base ggml-cpu ggml llama mtmd; do build_target $t; done
    install_llama
    build_jni
    echo "=== NATIVE BUILD COMPLETE (Vulkan) ==="
    ls -lh "$OUT"
    ;;
  *) echo "usage: $0 {configure|ggml-base|ggml-cpu|ggml|llama|mtmd|install|jni|all}"; exit 1 ;;
esac
