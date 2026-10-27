#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
APP_URL=${APP_URL:-http://localhost:8085}
OPENSEARCH_URI=${OPENSEARCH_URI:-http://localhost:9205}
LEARNING_INDEX_NAME=${LEARNING_INDEX_NAME:-ch05-products}
export OPENSEARCH_URI LEARNING_INDEX_NAME
if [[ ! "$LEARNING_INDEX_NAME" =~ ^ch05-[a-z0-9][a-z0-9_-]{0,50}$ ]]; then
    echo 'FAIL: index name must belong to Chapter 05' >&2
    exit 1
fi
for tool in curl jq; do
    command -v "$tool" >/dev/null || { echo "Missing prerequisite: $tool" >&2; exit 1; }
done
result=$(mktemp)
created=false
native="$OPENSEARCH_URI/$LEARNING_INDEX_NAME"
api="$APP_URL/api/spring-data/products"
cleanup() {
    if [[ "$created" == true ]]; then
        curl -sS --max-time 10 -X DELETE "$native" >/dev/null || true
    fi
    rm -f "$result"
}
trap cleanup EXIT
request() {
    local method=$1 url=$2 expected=$3 assertion=$4
    shift 4
    local status
    status=$(curl -sS --max-time 15 -o "$result" -w '%{http_code}' -X "$method" "$url" "$@")
    if [[ "$status" != "$expected" ]]; then
        echo "FAIL $method $url: HTTP $status expected $expected" >&2
        cat "$result" >&2
        exit 1
    fi
    if [[ "$assertion" != '-' ]]; then
        jq -e "$assertion" "$result" >/dev/null || { echo "FAIL payload: $method $url" >&2; exit 1; }
    fi
    echo "PASS $method $url HTTP $status"
}
request GET "$OPENSEARCH_URI/" 200 '.version.distribution == "opensearch" and .version.number == "3.9.0"'
status=$(curl -sS --max-time 10 -o "$result" -w '%{http_code}' "$native")
if [[ "$status" != 404 ]]; then
    echo "FAIL: target index must be absent (HTTP $status); existing data left unchanged" >&2
    exit 1
fi
request GET "$api/p-001" 503 '.code == "INDEX_NOT_READY"'
request PUT "$api/p-001" 503 '.code == "INDEX_NOT_READY"' -H 'Content-Type: application/json' --data-binary @fixtures/product.json
bash scripts/initialize-indices.sh
created=true
# Disabling periodic refresh makes visibility assertions deterministic.
request PUT "$native/_settings" 200 '.acknowledged == true' -H 'Content-Type: application/json' -d '{"index.refresh_interval":"-1"}'
request GET "$api/missing" 404 '.code == "PRODUCT_NOT_FOUND"'
request DELETE "$api/missing" 404 '.code == "PRODUCT_NOT_FOUND"'
request POST "$api/p-001" 201 '.id == "p-001" and .name == "Mechanical Keyboard" and .priceSatang == 129900 and .createdAt == "2026-01-01T00:00:00Z"' -H 'Content-Type: application/json' --data-binary @fixtures/product.json
request POST "$api/p-001" 409 '.code == "PRODUCT_ALREADY_EXISTS"' -H 'Content-Type: application/json' --data-binary @fixtures/product-replacement.json
request GET "$api/p-001" 200 '.name == "Mechanical Keyboard" and .stock == 10'
request GET "$native/_doc/p-001" 200 '._source._class == "com.example.learning.opensearch.documentcrud.domain.ProductDocument" and (._source | has("id") | not)'
request POST "$native/_search" 200 '.hits.total.value == 0 and .hits.total.relation == "eq"' -H 'Content-Type: application/json' -d '{"query":{"match_all":{}},"size":10}'
request POST "$native/_refresh" 200 '._shards.failed == 0'
request POST "$native/_search" 200 '.hits.total.value == 1 and .hits.hits[0]._id == "p-001" and .hits.hits[0]._source.name == "Mechanical Keyboard"' -H 'Content-Type: application/json' -d '{"query":{"match_all":{}},"size":10}'
request PUT "$api/p-001" 200 '.name == "Wireless Keyboard" and .category == "accessories" and .priceSatang == 99000 and .stock == 5 and .available == false' -H 'Content-Type: application/json' --data-binary @fixtures/product-replacement.json
request GET "$api/p-001" 200 '.name == "Wireless Keyboard" and .createdAt == "2026-01-02T00:00:00Z"'
request POST "$native/_search" 200 '.hits.total.value == 1 and .hits.hits[0]._source.name == "Mechanical Keyboard"' -H 'Content-Type: application/json' -d '{"query":{"match_all":{}},"size":10}'
request POST "$native/_refresh" 200 '._shards.failed == 0'
request POST "$native/_search" 200 '.hits.total.value == 1 and .hits.hits[0]._source.name == "Wireless Keyboard"' -H 'Content-Type: application/json' -d '{"query":{"match_all":{}},"size":10}'
request PUT "$api/p-new" 200 '.id == "p-new"' -H 'Content-Type: application/json' --data-binary @fixtures/product.json
request GET "$api/p-new" 200 '.name == "Mechanical Keyboard"'
request DELETE "$api/p-new" 204 '-'
request PUT "$api/p-001" 400 '.code == "INVALID_REQUEST"' -H 'Content-Type: application/json' -d '{"name":"partial"}'
request PUT "$api/p-001" 400 '.code == "INVALID_REQUEST"' -H 'Content-Type: application/json' -d '{"name":"Keyboard","category":"electronics","stock":-1,"priceSatang":100,"available":true,"createdAt":"2026-01-01T00:00:00Z"}'
request PUT "$api/p-001" 400 '.code == "INVALID_REQUEST"' -H 'Content-Type: application/json' -d '{"name":"Keyboard","category":"electronics","stock":1,"priceSatang":100,"available":true,"createdAt":"2026-01-01T00:00:00Z","unknown":1}'
request PUT "$api/p-001" 400 '.code == "INVALID_REQUEST"' -H 'Content-Type: application/json' -d '{"name":"Keyboard","category":"electronics","stock":1.5,"priceSatang":100,"available":true,"createdAt":"2026-01-01T00:00:00Z"}'
request GET "$api/bad.id" 400 '.code == "INVALID_PRODUCT_ID"'
request DELETE "$api/p-001" 204 '-'
request GET "$api/p-001" 404 '.code == "PRODUCT_NOT_FOUND"'
request DELETE "$api/p-001" 404 '.code == "PRODUCT_NOT_FOUND"'
request POST "$native/_search" 200 '.hits.total.value == 1 and .hits.hits[0]._id == "p-001"' -H 'Content-Type: application/json' -d '{"query":{"match_all":{}},"size":10}'
request POST "$native/_refresh" 200 '._shards.failed == 0'
request POST "$native/_search" 200 '.hits.total.value == 0 and .hits.total.relation == "eq"' -H 'Content-Type: application/json' -d '{"query":{"match_all":{}},"size":10}'
request DELETE "$native" 200 '.acknowledged == true'
created=false
request GET "$api/p-001" 503 '.code == "INDEX_NOT_READY"'
echo 'PASS Chapter 05 smoke; owned index removed, app and Compose left running'
