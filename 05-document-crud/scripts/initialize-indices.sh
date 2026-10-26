#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
OPENSEARCH_URI=${OPENSEARCH_URI:-http://localhost:9205}
LEARNING_INDEX_NAME=${LEARNING_INDEX_NAME:-ch05-products}
if [[ ! "$LEARNING_INDEX_NAME" =~ ^ch05-[a-z0-9][a-z0-9_-]{0,50}$ ]]; then
    echo 'FAIL: expected a Chapter 05 index name' >&2
    exit 1
fi
for tool in curl jq; do
    command -v "$tool" >/dev/null || { echo "Missing prerequisite: $tool" >&2; exit 1; }
done
result=$(mktemp)
trap 'rm -f "$result"' EXIT
status=$(curl -sS --max-time 10 -o "$result" -w '%{http_code}' "$OPENSEARCH_URI/$LEARNING_INDEX_NAME")
if [[ "$status" == 200 ]]; then
    echo "Index $LEARNING_INDEX_NAME already exists; inspect its mapping/settings before reusing it. No changes made."
    exit 0
fi
if [[ "$status" != 404 ]]; then
    echo "FAIL: index preflight HTTP $status" >&2
    exit 1
fi
status=$(curl -sS --max-time 15 -o "$result" -w '%{http_code}' -X PUT "$OPENSEARCH_URI/$LEARNING_INDEX_NAME" \
    -H 'Content-Type: application/json' --data-binary @src/main/resources/opensearch/product-index.json)
if [[ "$status" != 200 ]] || ! jq -e '.acknowledged == true' "$result" >/dev/null; then
    echo "FAIL: explicit index creation HTTP $status" >&2
    cat "$result" >&2
    exit 1
fi
echo "Created $LEARNING_INDEX_NAME; no documents seeded"
