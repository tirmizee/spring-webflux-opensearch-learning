#!/usr/bin/env bash
# Token response never goes to stdout; refuse local overwrite and use unique server names.
set -euo pipefail
umask 077
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
[[ $# -ge 2 && $# -le 3 ]] || { echo 'Usage: create-api-key.sh label specification.json [duration-seconds]' >&2; exit 2; }
label="$1"; specification="$2"; duration="${3:-3600}"
[[ "$label" =~ ^[a-zA-Z0-9_-]+$ && "$duration" =~ ^[0-9]+$ ]] || exit 2
[[ "$duration" -ge 1 && "$duration" -le 86400 ]] || exit 2
material="${CH03_MATERIAL_DIR:-$PWD/certs/generated}"
uri="${OPENSEARCH_URI:-https://localhost:${OPENSEARCH_PORT:-9203}}"
[[ "$uri" == https://* ]] || exit 2
[[ ! -e "$material/secrets/$label.token" && ! -e "$material/secrets/$label.id" ]] || { echo 'Local label exists; choose a new label' >&2; exit 2; }
work="$(mktemp -d)"
trap 'rm -rf -- "$work"' EXIT
name="ch03_${label}_$(openssl rand -hex 8)"
jq --arg name "$name" --argjson duration "$duration" '. + {name:$name,duration_seconds:$duration}' "$specification" > "$work/request.json"
curl -fsS --max-time 15 --config "$material/secrets/admin.curl" -H 'Content-Type: application/json' \
    -X POST --data-binary "@$work/request.json" "$uri/_plugins/_security/api/apitokens" -o "$work/response.json"
jq -er '.id | select(type=="string" and length>0)' "$work/response.json" > "$material/secrets/$label.id"
jq -er '.token | select(type=="string" and test("^os_[A-Za-z0-9_-]+$"))' "$work/response.json" > "$material/secrets/$label.token"
# A protected curl config keeps the credential out of argv. Never print this file.
printf 'header = "Authorization: ApiKey %s"\n' "$(cat "$material/secrets/$label.token")" > "$material/secrets/$label.curl"
if [[ "$label" == monitor ]]; then
    cp "$material/secrets/$label.curl" "$material/runtime/monitor.curl"
    chmod 644 "$material/runtime/monitor.curl"
fi
printf 'Created scoped key for local label %s; token stored in protected file.\n' "$label"
