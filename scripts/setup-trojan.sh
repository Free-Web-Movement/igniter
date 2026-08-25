#!/usr/bin/env bash
# setup-trojan.sh — clone/pull trojan-rs, build libtrojan.so for all Android
# ABIs, and copy the native libraries + JNI Kotlin files into igniter.
#
# Rebuild is skipped when the remote HEAD has not changed since the last build.
# Force a rebuild by removing .build/trojan-rs-stamp.
#
# Prerequisites:
#   - rustup + cargo-ndk (cargo install cargo-ndk)
#   - ANDROID_NDK_HOME  (or auto-detected from local.properties / $ANDROID_HOME)
#
# Usage (called by Gradle or manually):
#   ./scripts/setup-trojan.sh [project_root]

set -euo pipefail

# ── paths ────────────────────────────────────────────────────────────────────
PROJECT_ROOT="${1:-$(cd "$(dirname "$0")/.." && pwd)}"
BUILD_DIR="$PROJECT_ROOT/.build/trojan-rs"
STAMP_FILE="$BUILD_DIR/.stamp"
REPO_URL="https://github.com/Free-Web-Movement/trojan-rs.git"

LIBS_DIR="$PROJECT_ROOT/app/libs"
JNI_SRC="$BUILD_DIR/android/JNIHelper.kt"
JNI_DST="$PROJECT_ROOT/app/src/main/java/io/github/freewebmovement/igniter/JNIHelper.kt"

ABIS=(arm64-v8a armeabi-v7a x86_64 x86)
RUST_TARGETS=(aarch64-linux-android armv7-linux-androideabi x86_64-linux-android i686-linux-android)

# ── NDK auto-detect ──────────────────────────────────────────────────────────
if [ -z "${ANDROID_NDK_HOME:-}" ]; then
    # Try local.properties
    if [ -f "$PROJECT_ROOT/local.properties" ]; then
        sdk_dir=$(grep '^sdk.dir' "$PROJECT_ROOT/local.properties" 2>/dev/null | cut -d= -f2 | tr -d ' ')
        if [ -n "$sdk_dir" ]; then
            ndk_candidates=("$sdk_dir/ndk/"*/)
            if [ ${#ndk_candidates[@]} -gt 0 ]; then
                # Pick highest version
                ANDROID_NDK_HOME=$(ls -d "$sdk_dir/ndk/"*/ 2>/dev/null | sort -V | tail -1 | sed 's:/$::')
            fi
        fi
    fi
fi
if [ -z "${ANDROID_NDK_HOME:-}" ] && [ -n "${ANDROID_HOME:-}" ]; then
    ANDROID_NDK_HOME=$(ls -d "$ANDROID_HOME/ndk/"*/ 2>/dev/null | sort -V | tail -1 | sed 's:/$::' || true)
fi
if [ -z "${ANDROID_NDK_HOME:-}" ]; then
    echo "error: ANDROID_NDK_HOME not set and cannot be auto-detected" >&2
    exit 1
fi
export ANDROID_NDK_HOME
echo "using NDK: $ANDROID_NDK_HOME"

# ── cargo-ndk check ──────────────────────────────────────────────────────────
command -v cargo-ndk >/dev/null 2>&1 || {
    echo "error: cargo-ndk not installed. Run: cargo install cargo-ndk" >&2
    exit 1
}

# ── clone / pull trojan-rs ──────────────────────────────────────────────────
if [ ! -d "$BUILD_DIR/.git" ]; then
    echo ">>> cloning trojan-rs -> $BUILD_DIR"
    git clone --recurse-submodules "$REPO_URL" "$BUILD_DIR"
fi

# ── check if rebuild needed (before network fetch) ──────────────────────────
LOCAL_SHA=$(git -C "$BUILD_DIR" rev-parse HEAD)
if [ -f "$STAMP_FILE" ] && [ "$(cat "$STAMP_FILE")" = "$LOCAL_SHA" ]; then
    echo ">>> trojan-rs unchanged ($LOCAL_SHA), skipping build"
    if [ -f "$JNI_SRC" ]; then
        cp "$JNI_SRC" "$JNI_DST"
    fi
    exit 0
fi

# ── fetch remote updates (with timeout) ─────────────────────────────────────
echo ">>> fetching remote updates..."
if timeout 30 git -C "$BUILD_DIR" fetch --quiet origin main 2>/dev/null; then
    REMOTE_SHA=$(git -C "$BUILD_DIR" rev-parse origin/main)
    if [ "$REMOTE_SHA" != "$LOCAL_SHA" ]; then
        echo ">>> pulling new changes..."
        git -C "$BUILD_DIR" checkout --quiet main 2>/dev/null || true
        git -C "$BUILD_DIR" pull --quiet --recurse-submodules origin main 2>/dev/null || true
    fi
else
    echo ">>> network unavailable, using local copy ($LOCAL_SHA)"
fi

REMOTE_SHA=$(git -C "$BUILD_DIR" rev-parse HEAD)
echo ">>> trojan-rs version: $REMOTE_SHA, building..."

# ── build libtrojan.so for each ABI ──────────────────────────────────────────
for i in "${!ABIS[@]}"; do
    abi="${ABIS[$i]}"
    target="${RUST_TARGETS[$i]}"
    echo "=== building $abi ($target) ==="
    rustup target add "$target" >/dev/null 2>&1 || true
    cargo ndk -t "$abi" -o "$LIBS_DIR" build --release --lib 2>&1
done

# ── write stamp ──────────────────────────────────────────────────────────────
echo "$(git -C "$BUILD_DIR" rev-parse HEAD)" > "$STAMP_FILE"

# ── copy JNI Kotlin binding ─────────────────────────────────────────────────
if [ -f "$JNI_SRC" ]; then
    cp "$JNI_SRC" "$JNI_DST"
    echo ">>> JNIHelper.kt updated from trojan-rs"
fi

echo ">>> done. .so files:"
find "$LIBS_DIR" -name "libtrojan.so" -exec ls -lh {} \;
