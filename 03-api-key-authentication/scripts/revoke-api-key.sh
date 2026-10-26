#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
[[ $# -eq 1 && "$1" =~ ^[a-zA-Z0-9_-]+$ ]] || { echo 'Usage: revoke-api-key.sh local-label' >&2; exit 2; }
material="${CH03_MATERIAL_DIR:-$PWD/certs/generated}"
uri="${OPENSEARCH_URI:-https://localhost:${OPENSEARCH_PORT:-9203}}"
[[ "$uri" == https://* ]] || exit 2
# Stored ID file has a final newline; strip it before URL encoding.
id="$(tr -d '\r\n' < "$material/secrets/$1.id" | jq -sRr '@uri')"
curl -fsS --max-time 15 --config "$material/secrets/admin.curl" -X DELETE \
    "$uri/_plugins/_security/api/apitokens/$id" > /dev/null
printf 'Revoked key for local label %s.\n' "$1"
