# Chapter 02 — Apache transport migration verification

Overall: **Verified** เฉพาะ local metadata/health, TLS/security, parity และ lifecycle ของ source ที่ระบุ ผลเดิมอยู่ใน [historical timeout report](connection-timeout-2026-10-25.md)

ผู้ใช้อนุมัติการเปลี่ยนจาก ReactiveOpenSearchConfiguration/ClientConfiguration ที่สร้าง deprecated RestClientTransport ไปใช้ ApacheHttpClient5Transport สำหรับ Spring Data path ใน conversation นี้ Spring Data reactive client/template และ inherited mapping converter ยังคงเป็น library implementations จริง แยก transport/close owner จาก official path ไม่เปลี่ยน API contracts หรือ dependency versions

## Source และ environment

- Base commit: `866cb87aff1ccea1062b290aa51281cb1bbd0f26`; branch `codex/chapter-02-security-and-tls`; Chapter 02 มี uncommitted work ไม่ commit/push รอบนี้
- Runnable source: 45 files; SHA-256 `9835c015f5e2d5477ef76587ec7efa6f8324d9ba233b5e530fa635124302690d`
- Hash: chapter-relative paths จาก `git ls-files --cached --others --exclude-standard -- 02-security-and-tls` เรียงและตัดซ้ำ ไม่รวม `.md`/ignored files; hash `path + NUL + bytes + NUL`
- Final JAR SHA-256: `a37a36c6ae67fa1d5e62018f59d202846f9a76e530f236300cdfa5a5778575e3`
- Local macOS arm64; Temurin 21.0.12.1+1; Maven 3.9.11/Wrapper 3.3.4; Docker Desktop/local socket
- Dependency pins ไม่เปลี่ยน: Boot 4.1.2, Spring Data OpenSearch 3.1.4, official client 3.10.0; server image `opensearchproject/opensearch:3.9.0` เหมือน historical report ไม่อ้างว่า resolve dependency tree ใหม่ใน run นี้
- `opensearch-rest-client` ยังเป็น transitive dependency; migration นี้เลิกสร้าง/อ้าง deprecated RestClient/RestClientTransport ใน chapter source ไม่ใช่ patch third-party JARs

## Maven verification — PASS

รันจาก chapter directory:

```bash
env JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home \
  ./mvnw -B -ntp clean verify \
  -Dmaven.compiler.showDeprecation=true -Dmaven.compiler.failOnWarning=true
```

Final log: `/private/tmp/ch02-apache-final-verify.log`; exit 0, BUILD SUCCESS; finished `2026-10-25T21:42:26+07:00`, elapsed 01:33 min ตรวจ XML reports และไม่มี compiler deprecation warnings ไม่ใช้ `@SuppressWarnings`/disable tests

| Suite | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| Surefire: unit/reactive/HTTP/configuration | 46 | 0 | 0 | 0 |
| Failsafe: real TLS/security/parity/lifecycle | 7 | 0 | 0 | 0 |
| Total | 53 | 0 | 0 | 0 |

- Wiring: Apache transport สอง instance; selected reactive/official client ใช้ bean ของตัวเอง; actual ReactiveOpenSearchTemplate และ converter สร้างได้
- Lifecycle: transport beans เป็น close owners; reactive wrapper ไม่มี destroy callback; ทั้งสอง real clients เรียกสำเร็จใน context และ fail หลัง shutdown
- Real server: success, untrusted CA, invalid credentials, insufficient permissions, denied index creation, trusted certificate with wrong SAN และ normalized HTTP/parity
- Compilation ผ่าน strict warnings gate ทั้ง main/test source; JVM dynamic-agent และ macOS Netty native DNS fallback messages ยังเกิดตาม environment เดิม ไม่อ้างว่า runtime logs ไม่มี warnings ทุกชนิด

## Compose และ packaged verification

```bash
docker compose config --quiet
docker compose ps
```

Config exit 0; existing Compose `os-learning-ch02-opensearch-1` healthy บน `127.0.0.1:9202` ไม่ initialize security ใหม่หรือหยุด cluster เดิม การอ่าน Docker socket ครั้งหนึ่งใน sandbox ถูกปฏิเสธ แล้วรัน `docker compose ps` ด้วย permission ที่รองรับสำเร็จ

Packaged runs ใช้ Java 21 `java -jar target/security-and-tls.jar` กับ `APP_PORT` แบบ random loopback และ `BASE_URL` ตรงกัน แล้วรัน `bash scripts/smoke-test.sh <mode>` แต่ละ run ตรวจสี่ routes และสอง parity comparisons ใช้ temporary local driver `/private/tmp/ch02-apache-smoke.py` ไม่เพิ่ม tooling/framework ลง repository

| Scenario | Overrides จาก defaults ใน README | Smoke mode |
|---|---|---|
| Healthy | Monitor identity, trusted CA, existing cluster | healthy |
| Permissions | OPENSEARCH_USERNAME=ch02_info_only; OPENSEARCH_PASSWORD_FILE=certs/generated/secrets/info.password | insufficient-permissions |
| Credentials | OPENSEARCH_PASSWORD_FILE=certs/generated/secrets/invalid.password | invalid-credentials |
| CA | OPENSEARCH_CA_CERTIFICATE=certs/generated/public/unrelated-ca.pem | tls-failure |
| Unavailable | OPENSEARCH_URI=https://localhost:1 | unavailable |
| Hostname | Temporary isolated server + copied mismatch leaf/key; original trusted CA; random HTTPS port | tls-failure |

Final packaged logs: `/private/tmp/ch02-apache-app-{healthy,permissions,credentials,ca,unavailable,hostname}.log` และ `/private/tmp/ch02-apache-smoke-{healthy,permissions,credentials,ca,unavailable,hostname}.log`

## Diagnostics และ limitations

- Initial strict Maven run ผ่าน 53 tests; เก็บ blank-line formatting ที่หลงเหลือใน error handler แล้วรัน final clean verify ซ้ำตาม source hash ด้านบน ไม่มี behavior changes หลัง final build
- Initial temporary hostname smoke driver ตรวจเร็วเกินไป ได้ cURL exit 35 ก่อน TLS server พร้อม จึงไม่นับเป็น PASS แก้เฉพาะ temporary readiness loop ให้รอ cURL exit 60 และ rerun ไม่ลด hostname verification หรือแก้ application เพื่อให้ผ่าน
- Metadata/health endpoints ใช้ Spring Data reactive-client tier; template/converter wiring ไม่ใช่ proof ของ CRUD/repositories/mapping round-trip
- ไม่ทดสอบ production load, remote Docker, secret rotation หรือ platform อื่น Future cancellation ไม่รับประกัน server-side abort
- GitHub CI: **NOT VERIFIED** ไม่ commit/push หรือ dispatch workflow
- Historical results ของ source ก่อน migration อยู่ใน `verification/` ไม่รวมเป็นผลปัจจุบัน

## Final gates และ REVIEW

| Gate | Result |
|---|---|
| Strict main/test compile + 53 Maven tests | PASS |
| Real-server TLS/security/HTTP parity/lifecycle | PASS |
| Compose configuration และ existing healthy cluster | PASS |
| Final JAR: healthy, permissions, credentials, CA, unavailable, hostname smoke | PASS ทั้ง 6 scenarios; four routes และ two parity comparisons ต่อ scenario |
| Source/JAR fingerprints และ local documentation links | PASS |
| Generated password/encoded credential scan ใน intended chapter files และ final packaged logs | PASS |
| Cleanup | Owned app processes terminated/waited; temporary hostname container และ copied keys cleanup; IT material cleanup; existing Compose preserved |
| GitHub-hosted CI | NOT VERIFIED |

Final read-only REVIEW ใช้ review-chapter ตรวจ approved migration scope, configuration/bean ownership, service lazy invocation, TLS/auth/error contracts, test coverage และเอกสาร ไม่พบ actionable findings ค้างใน scope บน dirty branch ที่ระบุ source hash ตรงกับ final Maven/JAR evidence; ไม่แก้ implementation หลัง verification

Recorded: 2026-10-25T21:51:53+07:00
