#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
APP_URL=${APP_URL:-http://localhost:8084}
OPENSEARCH_URI=${OPENSEARCH_URI:-http://localhost:9204}
LEARNING_INDEX_NAME=${LEARNING_INDEX_NAME:-ch04-products}
if [[ ! "$LEARNING_INDEX_NAME" =~ ^ch04-[a-z0-9][a-z0-9_-]{0,50}$ ]]; then
    echo 'FAIL: index name must belong to the Chapter 04 namespace' >&2
    exit 1
fi
for tool in curl jq; do
    command -v "$tool" >/dev/null || { echo "Missing prerequisite: $tool" >&2; exit 1; }
done
result=$(mktemp)
created=false
owned_index=
cleanup() {
    if [[ "$created" == true ]]; then
        curl -sS --max-time 10 -X DELETE "$OPENSEARCH_URI/$owned_index" >/dev/null || true
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
index="$APP_URL/api/spring-data/indices/products"
sample="$APP_URL/api/spring-data/mapping-examples/products"
native="$OPENSEARCH_URI/$LEARNING_INDEX_NAME"
request GET "$OPENSEARCH_URI/" 200 '.version.distribution == "opensearch" and .version.number == "3.9.0"'
status=$(curl -sS --max-time 10 -o "$result" -w '%{http_code}' "$native")
if [[ "$status" != 404 ]]; then
    echo "FAIL: target index must be absent (HTTP $status); inspect existing data before choosing to delete it" >&2
    exit 1
fi
request GET "$index/mapping" 404 '.code == "INDEX_NOT_FOUND"'
request GET "$index/settings" 404 '.code == "INDEX_NOT_FOUND"'
request PATCH "$index/settings" 404 '.code == "INDEX_NOT_FOUND"' -H 'Content-Type: application/json' -d '{"numberOfReplicas":0,"refreshInterval":"2s"}'
request PUT "$sample/p-001" 409 '.code == "INDEX_NOT_READY"' -H 'Content-Type: application/json' --data-binary @fixtures/product.json
request PUT "$native/_doc/no-auto" 404 '.error.type == "index_not_found_exception"' -H 'Content-Type: application/json' -d '{"stock":1}'
request POST "$index" 201 '.acknowledged == true'
owned_index=$(jq -er '.indexName | select(test("^ch04-[a-z0-9][a-z0-9_-]{0,50}$"))' "$result")
created=true
jq -e --arg name "$LEARNING_INDEX_NAME" '.indexName == $name' "$result" >/dev/null
request POST "$index" 409 '.code == "INDEX_ALREADY_EXISTS"'
request GET "$index/mapping" 200 '.mapping.dynamic == "strict" and .mapping.properties.name.fields.raw.type == "keyword" and .mapping.properties.stock.type == "integer" and .mapping.properties._class.type == "keyword"'
request GET "$index/settings" 200 '.numberOfShards == 1 and .numberOfReplicas == 0 and .refreshInterval == "1s" and .coerce == false'
request GET "$sample/missing" 404 '.code == "PRODUCT_NOT_FOUND"'
request PUT "$sample/p-001" 400 '.code == "INVALID_REQUEST"' -H 'Content-Type: application/json' -d '{"name":" "}'
request PUT "$sample/p-001" 200 '.id == "p-001" and .priceSatang == 129900 and .stock == 10 and .createdAt == "2026-01-01T00:00:00Z"' -H 'Content-Type: application/json' --data-binary @fixtures/product.json
request GET "$sample/p-001" 200 '.id == "p-001" and .name == "Mechanical Keyboard" and .available == true'
request GET "$native/_doc/p-001" 200 '._id == "p-001" and ._source.priceSatang == 129900 and (._source | has("id") | not) and ._source._class == "com.example.learning.opensearch.mappings.domain.ProductDocument"'
request PATCH "$index/settings" 200 '.numberOfReplicas == 1 and .refreshInterval == "2s"' -H 'Content-Type: application/json' -d '{"numberOfReplicas":1,"refreshInterval":"2s"}'
request GET "$index/settings" 200 '.numberOfShards == 1 and .numberOfReplicas == 1 and .refreshInterval == "2s"'
request PATCH "$index/settings" 400 '.code == "INVALID_REQUEST"' -H 'Content-Type: application/json' -d '{"numberOfReplicas":0,"refreshInterval":"1s","numberOfShards":2}'
request PATCH "$index/settings" 400 '.code == "INVALID_REQUEST"' -H 'Content-Type: application/json' -d '{"numberOfReplicas":2,"refreshInterval":"-1"}'
request PATCH "$index/settings" 200 '.numberOfReplicas == 0 and .refreshInterval == "1s"' -H 'Content-Type: application/json' -d '{"numberOfReplicas":0,"refreshInterval":"1s"}'
for value in '"10"' '"abc"'; do
    request PUT "$native/_doc/bad" 400 '.error.type == "mapper_parsing_exception"' -H 'Content-Type: application/json' -d "{\"stock\":$value}"
done
request PUT "$native/_doc/bad" 400 '.error.type == "strict_dynamic_mapping_exception"' -H 'Content-Type: application/json' -d '{"unknownField":"unexpected"}'
request GET "$native/_doc/bad" 404 '.found == false'
request PUT "$native/_mapping" 400 '.error.type == "illegal_argument_exception"' -H 'Content-Type: application/json' -d '{"properties":{"stock":{"type":"keyword"}}}'
request PUT "$native/_settings" 400 '.error.type == "illegal_argument_exception"' -H 'Content-Type: application/json' -d '{"index.number_of_shards":2}'
request POST "$native/_refresh" 200 '._shards.failed == 0'
request POST "$native/_search" 200 '.hits.total.value == 1 and .hits.hits[0]._id == "p-001"' -H 'Content-Type: application/json' -d '{"query":{"term":{"name.raw":"Mechanical Keyboard"}}}'
request DELETE "$index" 204 '-'
created=false
request DELETE "$index" 404 '.code == "INDEX_NOT_FOUND"'
request GET "$sample/p-001" 404 '.code == "INDEX_NOT_FOUND"'
echo 'PASS Chapter 04 smoke; owned index removed, app and Compose left running'
