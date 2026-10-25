# Chapter 02 — Transport factory, pool, timeout และ test fixture verification

Overall: **Verified** สำหรับ local scope ที่ระบุ ผลก่อน refactoring อยู่ใน [historical Apache migration report](verification/apache-migration-2026-10-25.md) ไม่ใช่หลักฐาน source ใหม่

## Scope และ source state

ผู้ใช้อนุมัติ refactoring เจ็ดข้อใน conversation นี้: แยก OpenSearchTransportFactory, configurable bounded pool, timeout-order validation, password lifecycle documentation, tool-free unit certificate fixture, integration test helpers และ actual verification evidence ไม่เปลี่ยน API contracts/dependencies หรือ chapter อื่น ไม่ commit/push

- Base revision: `866cb87aff1ccea1062b290aa51281cb1bbd0f26`; branch `codex/chapter-02-security-and-tls`
- Working tree เป็น dirty รวม tracked modifications และ untracked source/tests/fixture/reports; base revision อย่างเดียวไม่ระบุ source ที่ทดสอบ
- Runnable source 47 files SHA-256: `0a19057d4f0f8d97b766a00e029c89a8df8d77b1c4b10d1cbe1a73175db6bbd4`
- Packaged JAR SHA-256: `68ffe082ff87f0fec5c1a9974dea8b20e09ee0cde9ebff28c55225df173af881`
- Fingerprint คำนวณจาก `git ls-files --cached --others --exclude-standard -- 02-security-and-tls` ตัดซ้ำ/เรียง paths แล้ว hash `chapter-relative path + NUL + bytes + NUL` ทุก non-Markdown file รวม untracked Java และ public PEM fixture ไม่รวม ignored outputs/secrets/target
- หลัง final build เปลี่ยนเฉพาะ Markdown report/README ไม่มี runtime/config/test changes

## Environment และ dependencies — PASS

รัน `./mvnw -version`, `uname -sm`, `sw_vers -productVersion`, `docker version --format '{{.Server.Version}}'`, `docker compose version --short` และ `docker image inspect opensearchproject/opensearch:3.9.0 --format '{{index .RepoDigests 0}}'`

Observed: macOS 27.0.1 arm64, Temurin 21.0.12.1, Maven 3.9.11; Docker 29.8.2, Compose 5.6.0, local Docker Desktop socket

Image: `opensearchproject/opensearch:3.9.0`; digest `opensearchproject/opensearch@sha256:adfa61f85025d06b4aeb562e7e74fde7e31c437039c93c3862c17e9acebd6c7c`

รันจาก chapter directory ด้วย `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home`:

```bash
./mvnw -B -ntp org.apache.maven.plugins:maven-dependency-plugin:3.10.0:tree \
  -DoutputFile=target/dependency-tree.txt \
  org.apache.maven.plugins:maven-help-plugin:3.5.2:effective-pom \
  -Doutput=target/effective-pom.xml
```

Exit 0, BUILD SUCCESS, finished 2026-10-25T22:13:17+07:00; log `/private/tmp/ch02-foundation-dependencies.log` และ generated reports ใน `target/`

Resolved: Spring Data OpenSearch 3.1.4, official client 3.10.0, HttpClient 5.6.5/HttpCore 5.4.4, JUnit 6.0.3, Mockito 5.23.0, Testcontainers 2.0.5; ไม่เปลี่ยน POM หรือเพิ่ม dependency เช่น BouncyCastle

## Maven compilation/tests — PASS

```bash
env JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home \
  ./mvnw -B -ntp clean verify \
  -Dmaven.compiler.showDeprecation=true -Dmaven.compiler.failOnWarning=true
```

Exit 0, BUILD SUCCESS, finished `2026-10-25T22:09:33+07:00`, elapsed 01:32 min; log `/private/tmp/ch02-foundation-verify.log` ตรวจ XML counts จริง:

| Suite | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| Surefire unit/HTTP/reactive/configuration | 60 | 0 | 0 | 0 |
| Failsafe real OpenSearch integration | 7 | 0 | 0 | 0 |
| Total | 67 | 0 | 0 | 0 |

- Pool tests: positive/zero/negative/invalid order, default 16/16, custom 8/2 binding และ invalid startup
- Timeout tests: request > response, equality, request < response และ startup rejection
- Unit tests โหลด public CA จาก classpath ไม่เรียก OpenSSL/Bash/Docker ไม่มี Assumptions/disabled/skipped tests
- Real ITs คง native 401/403/denied index creation, four routes บน success/credentials/permissions/CA, trusted-but-wrong-SAN failure, parity, separate transports, template/converter wiring, lazy real-client spies และ client calls ที่ fail หลัง context ปิด
- Strict main/test compilation ผ่านโดยไม่ suppress deprecation warnings JVM dynamic-agent/macOS Netty DNS fallback diagnostics ยังเกิดตาม environment ไม่อ้างว่า runtime logs ไม่มี warnings ทุกชนิด

Diagnostic ก่อน full run: `./mvnw -B -ntp test -Dmaven.compiler.showDeprecation=true -Dmaven.compiler.failOnWarning=true` ใน sandbox **FAIL** เพราะ Mockito inline agent attach ใช้ไม่ได้ ไม่ใช่เหตุผล skip tests รัน full command ด้านบนด้วย permissions ที่รองรับแล้วผ่าน Log diagnostic `/private/tmp/ch02-foundation-unit.log`

## Compose และ packaged smoke

รัน `docker compose config --quiet` exit 0, `docker compose ps` แสดง existing `os-learning-ch02-opensearch-1` healthy บน loopback 9202; `bash -n scripts/prepare-security.sh scripts/initialize-security.sh scripts/smoke-test.sh` exit 0

ใช้ existing Compose CA/identities โดยไม่ regenerate/initialize security หรือหยุด resources เดิม Real ITs สร้าง/initialize isolated OpenSearch ของตัวเอง ไม่ใช้ Compose เป็น IT fixture

Packaged command: Java 21 `java -jar target/security-and-tls.jar`, `APP_PORT` random loopback, `BASE_URL` ตรงกัน แล้ว `bash scripts/smoke-test.sh <mode>` Temporary driver `/private/tmp/ch02-foundation-smoke.py` เป็น local orchestration เท่านั้น ไม่เพิ่ม scripts/framework ลง repository

| Scenario | Overrides จาก default README | Smoke mode |
|---|---|---|
| Healthy | OPENSEARCH_MAX_CONNECTIONS=8; OPENSEARCH_MAX_CONNECTIONS_PER_ROUTE=2 | healthy |
| Permissions | OPENSEARCH_USERNAME=ch02_info_only; OPENSEARCH_PASSWORD_FILE=certs/generated/secrets/info.password | insufficient-permissions |
| Credentials | OPENSEARCH_PASSWORD_FILE=certs/generated/secrets/invalid.password | invalid-credentials |
| CA | OPENSEARCH_CA_CERTIFICATE=certs/generated/public/unrelated-ca.pem | tls-failure |
| Unavailable | OPENSEARCH_URI=https://localhost:1 | unavailable |
| Hostname | Isolated temporary server: trusted CA แต่ copied mismatch leaf/key; random HTTPS port; native cURL exit 60 ก่อนทดสอบ | tls-failure |

Logs: `/private/tmp/ch02-foundation-app-{healthy,permissions,credentials,ca,unavailable,hostname}.log` และ `/private/tmp/ch02-foundation-smoke-{healthy,permissions,credentials,ca,unavailable,hostname}.log`

## ข้อจำกัด

- Unit fixture มี public certificate เท่านั้น disposable private keystore ถูกลบแล้ว Provenance/validity/fingerprint อยู่ใน [fixture documentation](src/test/resources/certs/README.md)
- Metadata endpoints ใช้ reactive-client tier; template/converter wiring ไม่พิสูจน์ CRUD/repository mapping round-trip
- Timeout validation ไม่พิสูจน์ transport abort/server cancellation หรือ production latency ภายใต้ load; configurable pool ไม่ใช่ application request concurrency limit
- Password String/credential copies ใน heap ยังเป็นข้อจำกัดตาม README ไม่มี guarantee ว่าล้างทุกสำเนาได้
- Full IT/setup ยังต้องมี Bash/OpenSSL/Docker; Windows/WSL/remote Docker และ OS อื่น **NOT VERIFIED** ไม่ skip เพื่อประกาศผ่าน
- Fresh Compose setup/teardown ไม่รันซ้ำกับ resources เดิมของผู้ใช้ ใช้ isolated IT setup และ existing Compose packaged scenarios ตาม scope ของ refactoring
- GitHub-hosted CI **NOT VERIFIED** ไม่ commit/push หรือ dispatch workflow

## Final REVIEW และ gates

Workflow: IMPLEMENT → VERIFY → REVIEW ตาม user-authorized refactoring scope ใช้ review-chapter แบบ read-only หลัง runtime checks ตรวจ dirty source state, factory/configuration separation, qualifiers/close owners, validation, strict TLS/credentials, unit fixture provenance, IT assertions/parity และ README ไม่พบ actionable findings ค้างใน scope ไม่ใช้ source inspection แทน runtime evidence

| Gate | Result |
|---|---|
| Strict compile + unit/HTTP tests + real OpenSearch ITs | PASS: 67 tests, 0 failures/errors/skips |
| Packaged JAR smoke | PASS: healthy/custom pool, permissions, credentials, CA, unavailable, hostname; four routes และ two parity comparisons ต่อ scenario |
| Compose config/readiness | PASS: config --quiet และ existing healthy Compose cluster |
| Dependencies/effective POM/environment | PASS ตาม commands/results ด้านบน |
| Source/JAR/fixture hashes และ local Markdown links | PASS |
| Actual generated passwords/encoded Basic credentials ใน intended chapter files และ foundation logs | PASS: ไม่พบ |
| Resource cleanup | PASS: owned JAR processes terminated/waited; temporary hostname container/copied keys และ ephemeral IT keys cleanup; disposable fixture keystore removed; existing Compose preserved |
| Scope และ diff formatting | PASS: git diff --check; ไม่มี modifications นอก Chapter 02 |
| Windows/WSL/remote Docker, production load/rotation, hosted CI | NOT VERIFIED |

No dependency additions, no commit/push. Base revision เป็น context เท่านั้น source fingerprint รวม dirty/untracked files คือหลักฐาน source ของ run นี้

Recorded: 2026-10-25T22:15:07+07:00

## PR publication provenance

หลังผู้ใช้สั่งเปิด PR สร้าง branch `codex/chapter-02-transport-refactoring` จาก `origin/main` revision `053367a9f5e1c9ba4c0b77b5acadc5d2151a4082` ซึ่งมี runnable tree เดียวกับ base revision ของ tests ด้านบน การอนุญาต commit/push เกิดหลัง verification ไม่ได้แก้ข้อความ historical runs ให้กลายเป็น runs ใหม่

Git normalize public `src/test/resources/certs/unit-ca.pem` จาก CRLF เป็น LF ใน index ตรวจ byte comparison แล้วต่างเฉพาะ line endings; certificate DER/fingerprint ไม่เปลี่ยน ไม่มี Java/config/test behavior changes และไม่ได้อ้างว่ารันทดสอบ 67 tests ใหม่หลัง staging

- Tested working-tree fingerprint เดิม: `0a19057d4f0f8d97b766a00e029c89a8df8d77b1c4b10d1cbe1a73175db6bbd4`
- Published index runnable-source fingerprint: `495e8e9ed8b31bba5bdbe16cac304177a69e2ad1d87db963b59790002f182d42`
- Index ใช้อัลกอริทึม paths เดิม แต่ hash bytes จาก `git show :<path>` ของทุก non-Markdown file รวม source/fixture ใหม่ที่ stage แล้ว
- Test JAR hash และ local results ด้านบนยังเป็น artifact ของ original test run Hosted CI status ต้องอ่านจาก PR แยก ไม่เปลี่ยน report ให้เป็น PASS ล่วงหน้า
