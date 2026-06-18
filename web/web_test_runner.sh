#!/bin/bash

set -euo pipefail
# Get the absolute path to the runfiles directory
RUNFILES_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

./$1 --config $2 \
  --root-dir "${RUNFILES_DIR}" \
  --dir 'plugins/messageoftheday/web/_bazel_ts_out_tests' \
  --test-files 'plugins/messageoftheday/web/_bazel_ts_out_tests/*_test.js' \
  --ts-config="plugins/messageoftheday/web/tsconfig.json"
