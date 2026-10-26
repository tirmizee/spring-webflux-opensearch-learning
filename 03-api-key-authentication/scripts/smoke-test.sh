#!/usr/bin/env bash
# Tests already running apps; does not print credentials or own process lifecycle.
set -euo pipefail
[[ $# -le 1 ]] || exit 2
mode="${1:-healthy}"
case "$mode" in healthy|insufficient-permissions|invalid-key|tls-failure|unavailable) ;; *) exit 2 ;; esac
work="$(mktemp -d)"
trap 'rm -rf -- "$work"' EXIT
base="${APP_BASE_URL:-http://127.0.0.1:8083}"
for endpoint in info health; do
    path="/api/spring-data/cluster/$endpoint"
    file="$work/$endpoint.json"
    status="$(curl -sS --max-time 15 -o "$file" -w '%{http_code}' "$base$path")"
    expected=200; code=''
    case "$mode" in
        insufficient-permissions) [[ "$endpoint" != health ]] || { expected=502; code=OPENSEARCH_AUTHORIZATION_FAILED; } ;;
        invalid-key) expected=502; code=OPENSEARCH_AUTHENTICATION_FAILED ;;
        tls-failure) expected=502; code=OPENSEARCH_TLS_FAILURE ;;
        unavailable) expected=503; code=OPENSEARCH_UNAVAILABLE ;;
    esac
    if [[ "$status" != "$expected" ]]; then
        # Print only known application codes, never raw upstream messages or response bodies.
        code_received="$(jq -r '.code // empty' "$file" 2>/dev/null || true)"
        case "$code_received" in
            OPENSEARCH_AUTHENTICATION_FAILED)
                hint='API Key ไม่ถูกยอมรับ: อาจหมดอายุ ถูก revoke หรือแอปยังใช้ key เก่า สร้าง key ใหม่แล้ว restart แอป' ;;
            OPENSEARCH_AUTHORIZATION_FAILED)
                hint='API Key ไม่มีสิทธิ์ของ endpoint นี้ ตรวจ specification และ key file ที่ส่งให้แอป' ;;
            OPENSEARCH_TLS_FAILURE)
                hint='ตรวจ JVM truststore และ hostname; JVM options ต้องอยู่ก่อน -jar' ;;
            OPENSEARCH_UNAVAILABLE)
                hint='ตรวจ OpenSearch readiness และ OPENSEARCH_URI' ;;
            OPENSEARCH_TIMEOUT)
                hint='ตรวจ OpenSearch readiness และ timeout settings' ;;
            *) code_received=UNEXPECTED_OR_MISSING_ERROR_CODE
                hint='ตรวจว่า port นี้เป็นแอป Chapter 03 และใช้ JAR/configuration ล่าสุด' ;;
        esac
        echo "FAIL $endpoint: HTTP $status expected $expected; code=$code_received" >&2
        echo "$hint" >&2
        if [[ "$mode" == insufficient-permissions ]]; then
            echo 'โหมดนี้ตรวจแอปที่รันอยู่ ไม่เปลี่ยน key ให้: ต้องใช้ info-only key ที่ยังไม่หมดอายุ โดย info=200 และ health=502 AUTHORIZATION_FAILED' >&2
        fi
        exit 1
    fi
    if [[ "$expected" == 200 ]]; then
        jq -e '.clusterName == "learning-ch03"' "$file" > /dev/null
        if [[ "$endpoint" == info ]]; then
            jq -e '.distribution == "opensearch" and .version == "3.9.0" and .nodeName == "learning-ch03-node" and (.clusterUuid|length)>0 and .clusterUuid!="_na_"' "$file" > /dev/null
        else
            jq -e '(.status=="green" or .status=="yellow") and .numberOfNodes==1 and .numberOfDataNodes==1 and .timedOut==false' "$file" > /dev/null
        fi
    else
        jq -e --arg code "$code" --arg path "$path" --argjson status "$status" \
            '.code==$code and .instance==$path and .status==$status and .detail=="The OpenSearch dependency could not serve this request."' "$file" > /dev/null
    fi
    echo "PASS $mode $endpoint HTTP $status"
done
