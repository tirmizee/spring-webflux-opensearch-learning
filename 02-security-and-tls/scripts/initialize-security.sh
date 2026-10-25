#!/usr/bin/env bash
# ใช้เฉพาะ disposable cluster ของบทนี้; การ reapply ทับ security configuration
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
[[ $# -eq 0 ]] || { echo 'Usage: initialize-security.sh' >&2; exit 2; }
port="${OPENSEARCH_PORT:-9202}"
for attempt in {1..60}; do
    if curl --silent --show-error --connect-timeout 1 --max-time 2 \
        --cacert certs/generated/public/ca.pem "https://localhost:$port/" > /dev/null 2>&1; then
        break
    fi
    [[ "$attempt" != 60 ]] || { echo 'TLS endpoint did not become reachable' >&2; exit 1; }
    sleep 2
done
docker compose run --rm --no-deps security-init
for attempt in {1..30}; do
    if curl -fsS --connect-timeout 1 --max-time 5 --cacert certs/generated/public/ca.pem \
        --config certs/generated/secrets/monitor.curl \
        "https://localhost:$port/_cluster/health?wait_for_status=yellow&timeout=3s" > /dev/null; then
        echo 'PASS authenticated HTTPS readiness'
        exit 0
    fi
    sleep 2
done
echo 'Authenticated readiness did not pass' >&2
exit 1
