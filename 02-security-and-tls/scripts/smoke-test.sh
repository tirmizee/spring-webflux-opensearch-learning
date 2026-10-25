#!/usr/bin/env bash
# ตรวจ application ที่ผู้ใช้เริ่มไว้ ไม่เริ่ม/หยุด process หรือแก้ credentials
set -euo pipefail
mode="${1:-healthy}"
[[ $# -le 1 ]] || exit 2
case "$mode" in
    healthy|insufficient-permissions|invalid-credentials|tls-failure|unavailable) ;;
    *) echo 'Usage: smoke-test.sh [healthy|insufficient-permissions|invalid-credentials|tls-failure|unavailable]' >&2; exit 2 ;;
esac
base_url="${BASE_URL:-http://127.0.0.1:8082}"
work="$(mktemp -d)"
trap 'rm -f -- "$work"/*.json; rmdir -- "$work"' EXIT
for approach in spring-data java-client; do
    for endpoint in info health; do
        path="/api/$approach/cluster/$endpoint"
        file="$work/$approach-$endpoint.json"
        status="$(curl -sS --max-time 15 -o "$file" -w '%{http_code}' "$base_url$path")"
        expected=200
        code=''
        case "$mode" in
            insufficient-permissions) [[ "$endpoint" != health ]] || { expected=502; code=OPENSEARCH_AUTHORIZATION_FAILED; } ;;
            invalid-credentials) expected=502; code=OPENSEARCH_AUTHENTICATION_FAILED ;;
            tls-failure) expected=502; code=OPENSEARCH_TLS_FAILURE ;;
            unavailable) expected=503; code=OPENSEARCH_UNAVAILABLE ;;
        esac
        [[ "$status" == "$expected" ]] || { echo "FAIL $path: HTTP $status, expected $expected" >&2; exit 1; }
        if [[ "$expected" == 200 ]]; then
            jq -e '.clusterName == "learning-ch02"' "$file" > /dev/null
            if [[ "$endpoint" == info ]]; then
                jq -e '.distribution == "opensearch" and .version == "3.9.0" and .nodeName == "learning-ch02-node" and (.clusterUuid | length > 0) and .clusterUuid != "_na_"' "$file" > /dev/null
            else
                jq -e '(.status == "green" or .status == "yellow") and .numberOfNodes == 1 and .numberOfDataNodes == 1 and .timedOut == false' "$file" > /dev/null
            fi
        else
            jq -e --arg code "$code" --arg path "$path" --argjson status "$expected" \
                '.status == $status and .code == $code and .instance == $path and .title == "OpenSearch dependency failure" and .detail == "The OpenSearch dependency could not serve this request."' "$file" > /dev/null
        fi
        echo "PASS $mode $path HTTP $status"
    done
done
for endpoint in info health; do
    diff <(jq -S 'del(.instance)' "$work/spring-data-$endpoint.json") \
         <(jq -S 'del(.instance)' "$work/java-client-$endpoint.json")
    echo "PASS parity $endpoint"
done
