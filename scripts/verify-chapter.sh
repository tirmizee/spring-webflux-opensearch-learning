#!/usr/bin/env bash
# Convenience command; ไม่รับ arbitrary Maven flags หรือ paths
set -euo pipefail
if [[ $# -ne 1 || ! "$1" =~ ^(0[1-9]|1[0-9]|20)-[a-z0-9]+(-[a-z0-9]+)*$ ]]; then
    echo 'Usage: bash scripts/verify-chapter.sh NN-topic-name (01–20)' >&2
    exit 2
fi
script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)"
repo_root="$(cd -- "$script_dir/.." && pwd -P)"
chapter_dir="$repo_root/$1"
if [[ -L "$chapter_dir" || ! -d "$chapter_dir" || ! -f "$chapter_dir/pom.xml" || ! -f "$chapter_dir/mvnw" || -L "$chapter_dir/mvnw" ]]; then
    echo "Chapter is missing or incomplete: $1" >&2
    exit 2
fi
cd -- "$chapter_dir"
# Bash รองรับ wrapper ที่ยังไม่มี executable bit ใน working tree
exec bash ./mvnw clean verify
