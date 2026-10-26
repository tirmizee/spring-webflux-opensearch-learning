#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
[[ $# -eq 0 ]] || exit 2
material="${CH03_MATERIAL_DIR:-$PWD/certs/generated}"
uri="${OPENSEARCH_URI:-https://localhost:${OPENSEARCH_PORT:-9203}}"
[[ "$uri" == https://* ]] || exit 2
curl -fsS --max-time 15 --config "$material/secrets/admin.curl" "$uri/_plugins/_security/api/apitokens" \
    | jq '[.[] | select(.name | startswith("ch03_")) | {id,name,expires_at,revoked_at}]'
