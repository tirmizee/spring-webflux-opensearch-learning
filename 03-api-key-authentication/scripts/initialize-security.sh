#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
[[ $# -eq 0 ]] || exit 2
uri="${OPENSEARCH_URI:-https://localhost:${OPENSEARCH_PORT:-9203}}"
for attempt in {1..60}; do
    if curl --silent --show-error --connect-timeout 1 --max-time 2 --cacert certs/generated/public/ca.pem "$uri/" > /dev/null 2>&1; then break; fi
    [[ "$attempt" != 60 ]] || { echo 'TLS endpoint not ready' >&2; exit 1; }
    sleep 2
done
docker compose run --rm --no-deps security-init
for attempt in {1..30}; do
    if curl -fsS --max-time 5 --config certs/generated/secrets/admin.curl "$uri/" > /dev/null; then
        echo 'PASS administrator HTTPS readiness; issue scoped application keys next'
        exit 0
    fi
    sleep 1
done
exit 1
