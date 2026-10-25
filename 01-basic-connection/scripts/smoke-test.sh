#!/usr/bin/env bash
# ตรวจทั้งสอง approach และ legacy aliases ของ application ที่ผู้ใช้เริ่มไว้
set -euo pipefail
mode="${1:-healthy}"
if [[ $# -gt 1 || ( "$mode" != healthy && "$mode" != unavailable ) ]]; then
    echo 'Usage: bash scripts/smoke-test.sh [healthy|unavailable]' >&2
    exit 2
fi
base_url="${BASE_URL:-http://127.0.0.1:8080}"
cluster_name="${EXPECTED_CLUSTER_NAME:-learning-ch01}"
response_dir="$(mktemp -d)"
trap 'rm -f -- "$response_dir"/*.json; rmdir -- "$response_dir"' EXIT
for approach in java-client spring-data legacy; do
    prefix="/api/$approach/cluster"
    if [[ "$approach" == legacy ]]; then prefix=/api/cluster; fi
    for endpoint in info health; do
        response_file="$response_dir/$approach-$endpoint.json"
        status="$(curl --silent --show-error --max-time 10 -o "$response_file" -w '%{http_code}' "$base_url$prefix/$endpoint")"
        python3 - "$response_file" "$status" "$mode" "$endpoint" "$cluster_name" "$prefix" <<'ASSERT'
import json
import sys
from pathlib import Path
file, status, mode, endpoint, cluster, prefix = sys.argv[1:]
body = json.loads(Path(file).read_text())
if mode == 'healthy':
    assert status == '200', (status, body)
    assert body['clusterName'] == cluster, body
    if endpoint == 'info':
        assert body['distribution'] == 'opensearch' and body['version'] == '3.9.0', body
        assert body['clusterUuid'] and body['clusterUuid'] != '_na_', body
        assert body['nodeName'] == 'learning-ch01-node', body
    else:
        assert body['status'] in ('green', 'yellow'), body
        assert body['numberOfNodes'] == 1 and body['numberOfDataNodes'] == 1, body
        assert body['timedOut'] is False, body
else:
    assert status == '503', (status, body)
    assert body['status'] == 503 and body['title'] == 'OpenSearch unavailable', body
    assert body['detail'] == 'The OpenSearch dependency could not serve this request.', body
    assert body['instance'] == f'{prefix}/{endpoint}', body
print(f'PASS {mode} {prefix}/{endpoint}: HTTP {status}; {json.dumps(body)}')
ASSERT
    done
done
python3 - "$response_dir" "$mode" <<'PARITY'
import json
import sys
from pathlib import Path
root, mode = Path(sys.argv[1]), sys.argv[2]
for endpoint in ('info', 'health'):
    responses = [json.loads((root / f'{approach}-{endpoint}.json').read_text())
                 for approach in ('java-client', 'spring-data', 'legacy')]
    if mode == 'unavailable':
        responses = [{key: body[key] for key in ('status', 'title', 'detail')} for body in responses]
    assert responses[0] == responses[1] == responses[2], responses
    print(f'PASS {mode} parity: {endpoint}; Spring Data = official client = legacy alias')
PARITY
