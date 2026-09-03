#!/usr/bin/env bash
# Builds the cdylib/staticlib and runs capi/test_decirational.c against both,
# proving the C ABI actually works end to end (not just "the Rust compiles").
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"

echo "== cargo build --release (lib + cdylib + staticlib + decirational binary) =="
cargo build --release

echo
echo "== dynamic link (libdecirational.so) =="
gcc -Wall -Wextra -o /tmp/decirational_capi_test_dynamic capi/test_decirational.c \
    -Icapi -Ltarget/release -ldecirational
LD_LIBRARY_PATH=target/release /tmp/decirational_capi_test_dynamic

echo
echo "== static link (libdecirational.a) =="
gcc -Wall -Wextra -o /tmp/decirational_capi_test_static capi/test_decirational.c \
    -Icapi target/release/libdecirational.a -lpthread -ldl -lm
/tmp/decirational_capi_test_static
