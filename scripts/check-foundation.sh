#!/usr/bin/env bash
# ตรวจ foundation เท่านั้น ไม่รัน application หรือ Docker
set -euo pipefail
script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)"
python3 "$script_dir/check_foundation.py"
python3 -B "$script_dir/test_foundation_tools.py"
