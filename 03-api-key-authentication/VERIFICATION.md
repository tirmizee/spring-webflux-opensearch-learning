# Verification — starter auto-configuration + API Key customizer

Overall: **Verified** สำหรับ local Chapter 03 starter-only scope ตามผลจริงด้านล่าง ไม่รวม production, hosted CI หรือ platform อื่น

## Approved change และ source state

ผู้ใช้เลือก starter auto-configuration + customizer เพื่อโฟกัส API Key:

- ใช้ @SpringBootApplication ปกติ ไม่มี excludeName และไม่มี application-managed client/transport/template beans
- ลบ OpenSearchTransportFactory, application OpenSearchProperties effective record และ StarterOpenSearchConfiguration ใช้ ApiKeyConfiguration ประกาศ RestClientBuilderCustomizer bean เดียว
- Additional properties เหลือ api-key-file/request-timeout; native uris/connection-timeout/socket-timeout อยู่ใต้ prefix opensearch ชุดเดียว ไม่เพิ่ม CA loader/custom trust manager
- ใช้ library-default pool แทน custom pool settings; tests ของ deleted pool validation ถูกแทนด้วย native binding/URI/timeout และ auto-configuration origin checks ไม่ลบ API Key behavioral ITs
- ปรับ error mapping จาก ApacheHttpClient5Transport-specific ResponseException เป็น low-level REST ResponseException ตาม default transport จริง
- อัปเดต README, AGENTS, architecture/engineering/testing/compatibility/roadmap และ skill routing ให้ยึด approved exception จาก AGENTS จุดเดียว CI path เดิมยังถูกต้อง ไม่แก้ Chapter 01/02 ไม่ commit/push

ตรวจวันที่ 2026-10-26, Asia/Bangkok (+07:00) Branch `codex/chapter-02-transport-refactoring`; HEAD `acba0e394aff6f3205dd685e237beb80e8ef2e38` เป็น base เท่านั้น Chapter 03 ยัง untracked และ foundation มี uncommitted work จึงไม่อ้าง commit hash เพียงอย่างเดียว

Environment: macOS 27.0.1 arm64, Temurin 21.0.12.1, Maven 3.9.11, Docker 29.8.2, Compose 5.6.0; Boot 4.1.2 / starter 3.1.4 / underlying OpenSearch Java Client 3.10.0 / OpenSearch 3.9.0 ไม่เพิ่ม dependency ใหม่

Fingerprint รวมไฟล์ใหม่: `git ls-files --cached --others --exclude-standard -- 03-api-key-authentication`, deduplicate/sort chapter-relative paths, exclude Markdown/ignored generated files แล้ว SHA-256 ของ `path + NUL + raw bytes + NUL`:

| Item | SHA-256 |
|---|---|
| 46 source/config/script/test/wrapper files | 0c40952eed850e8cc29d1a700367b367c0a32a14fd04561cd810c0f157d22e88 |
| target/api-key-authentication.jar | fe5dcd16c57e793210c25410941514bbad83a4859912c209cbd45ac71d926f15 |

มี formatting edits ระหว่าง diagnostic/final runs จึงรักษางานนั้นไว้และรัน final Maven + packaged suite ใหม่บน source hash ข้างบน ตรวจ raw fingerprint ก่อน/หลังตรงกัน ไม่ใช้ผลจาก JAR ก่อนจัด format อ้างแทน artifact ใหม่

## REVIEW — PASS

ตรวจแบบ read-only ตาม review-chapter ก่อน final VERIFY: approved scope, native auto-configuration conditions, callback chain, API Key/URI validation, JVM trust, reactive lazy invocation, error types, lifecycle, tests และเอกสาร ไม่พบ actionable findings ค้าง Native lifecycle ของ exact release อาจ close underlying REST client ผ่านทั้ง transport และ RestClient beans; ไม่อ้างว่า application ออกแบบ single close owner หรือสร้าง transport เอง Context shutdown ผ่านจริงตาม IT ไม่เพิ่ม lifecycle post-processor

## Compilation / unit / real integration — PASS with disclosed warning

รันจาก directory ของบท:

```bash
env JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home \
  ./mvnw -B -ntp clean verify -Dmaven.compiler.showDeprecation=true
```

Final log `/private/tmp/ch03-auto-formatted-final.log`; exit 0, BUILD SUCCESS, finished `2026-10-26T07:01:08+07:00`, elapsed 01:43 min ตรวจ Surefire/Failsafe XML จริง:

| Suite | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| Unit/reactive/HTTP/configuration | 53 | 0 | 0 | 0 |
| Real OpenSearch integration | 10 | 0 | 0 | 0 |
| Total | 63 | 0 | 0 | 0 |

Unit coverage: API Key file validation, extra deadline validation, native URI/timeout/credential/path-prefix constraints, startup failure for missing key, response-timeout customization, lazy/fresh reactive invocation, errors/cancellation/deadlines และ sanitized HTTP contracts

Assertions ยืนยันว่า RestClient, RestClientTransport, ReactiveOpenSearchClient, ReactiveOpenSearchTemplate และ converter มาจาก starter factory origins ไม่ใช่ mocks หรือ application beans Reactive client อ้าง transport เดียวกับ auto-configured bean

ITs ใช้ isolated real OpenSearch: native least privilege/denied index/admin operations, success/invalid/insufficient permissions, untrusted CA, unavailable server, cached-key rotation/revocation, expiry bounded polling, trusted-but-wrong-SAN และ context shutdown ไม่ลด assertions ของ API Key behavior เพื่อให้ผ่าน

**Compiler warning ที่ยอมรับตาม decision:** ApiKeyConfiguration ใช้ `org.opensearch.client.RestClientBuilder` ที่ deprecated ผ่าน extension point ของ starter รุ่นนี้ Final compiler แสดง warning ที่ ApiKeyConfiguration.java ไม่ suppress และไม่อ้างว่า warning หาย ไม่ใช้ failOnWarning เป็น PASS gate สำหรับ accepted deprecated extension point; ค่า default transport คือ deprecated RestClientTransport ไม่ใช่ ApacheHttpClient5Transport

Runtime ยังมี Mockito dynamic-agent และ Netty macOS native DNS fallback diagnostics ไม่ใช่ compiler deprecation ไม่เพิ่ม flags เพื่อซ่อน warnings

Diagnostic attempts ก่อน final เคย compile test fixture ไม่ผ่านจาก StatusLine constructor และพบว่า native builder ไม่รับ trailing-slash URI ปรับ fixture ให้ตรง resolved API และกำหนด URI contract ไม่มี trailing slash จากนั้น rerun เต็มผ่าน ไม่ skip tests

## Dependency / source verification — PASS

```bash
env JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home \
  ./mvnw -B -ntp org.apache.maven.plugins:maven-dependency-plugin:3.10.0:tree \
  -DoutputFile=/private/tmp/ch03-auto-dependencies.txt
```

Exit 0; log `/private/tmp/ch03-auto-dependencies.log` Starter 3.1.4 รวม core 3.1.4; SDK 3.10.0 และ low-level REST client 3.8.0 ไม่มี co.elastic Java client/high-level REST client การมี low-level REST client เป็นสิ่งที่ default starter path ต้องใช้ ไม่ใช่ accidental client ใน configuration ใหม่นี้

ตรวจ exact source JARs 3.1.4/3.8.0/3.10.0 ที่ resolve: starter builder customizers ถูกเรียกแบบ ordered chain, default connection-timeout map ไป connection/pool lease, socket-timeout ต้องตั้ง response timeout ผ่าน customizer ในรุ่นนี้; RestClient ใช้ JVM default SSLContext Reactive wrapper close เป็น no-op และ transport close delegate ไป REST client

## Packaged application / README — PASS

Existing Chapter 03 Compose cluster เปิดอยู่เมื่อเริ่ม final runtime checks จึงใช้ต่อโดยไม่ bootstrap ใหม่ ไม่ regenerate certificates/volume Read-only administrator HTTPS readiness ได้ HTTP 200 แม้ Docker healthcheck เดิมรายงาน unhealthy ไม่ถือ healthcheck status แทน actual authenticated API checks

ใช้ local temporary driver ซึ่งไม่ได้เพิ่มใน repository:

```bash
python3 /private/tmp/ch03-auto-formatted-smoke.py
```

Exit 0; `/private/tmp/ch03-auto-formatted-smoke-suite.log` Driver เปิด JAR ตาม hash ข้างบนด้วย Java 21, random loopback APP_PORT, local JVM truststore options ก่อน -jar และ explicit CLI `--opensearch.api-key-file` หลัง JAR จงใจตั้ง OPENSEARCH_API_KEY_FILE เป็น missing stale path เพื่อยืนยัน CLI precedence ตาม README ไม่ใช้ old pool environment inputs อีก

ทุก checkpoint เรียก `APP_BASE_URL=... bash scripts/smoke-test.sh <mode>` ตรวจ info/health status, fields หรือ sanitized error contract:

| Checkpoints | Actual result |
|---|---|
| healthy | Info/health 200 ด้วย starter-created client และ API Key header |
| permissions | Info 200, health 502 AUTHORIZATION_FAILED |
| invalid | ทั้งสอง routes 502 AUTHENTICATION_FAILED |
| ca / default-trust | Unrelated truststore หรือไม่ส่ง local truststore → 502 TLS_FAILURE |
| unavailable | ทั้งสอง routes 503 UNAVAILABLE |
| rotation-a-before / rotation-b-before | A และ B ใช้งานสำเร็จก่อน revoke |
| rotation-a-revoked / rotation-b-after | Native A 401, cached A ถูกปฏิเสธ; B ยัง 200 |
| expiry-before / expiry-after | Native 200 ก่อน expiry แล้ว bounded polling จน 401; cached app key ถูกปฏิเสธ |
| hostname | Native cURL exit 60 และ application 502 TLS_FAILURE เมื่อ SAN ผิดแต่ CA ถูกต้อง |

รวม 13 runtime checkpoints และ negative diagnostic checks เพิ่มอีก 2: insufficient-permissions mode กับ invalid key และ wrong trust ต้อง exit 1 พร้อม info 502 และ code AUTHENTICATION_FAILED/TLS_FAILURE ตามสาเหตุ ไม่เอา failure มานับว่าผ่าน permissions exercise

Logs `/private/tmp/ch03-auto-formatted-smoke-<checkpoint>.log`, `/private/tmp/ch03-auto-formatted-diagnostic-{invalid,ca}.log`, `/private/tmp/ch03-auto-formatted-app-*.log` และ `/private/tmp/ch03-auto-formatted-lifecycle.log` ไม่มี secret output

Command variations: ใช้ unique labels/random ports แทน literal labels/8083; ใช้ existing PKI ไม่รัน prepare-security.sh/cp .env.example .env ซ้ำ ไม่อ้างว่าทดลองกด Ctrl-C จริง Driver stop/restart own JVMs เพื่อทดสอบ lifecycle เดียวกัน Local driver เป็น orchestration ชั่วคราว ไม่ใช่ Python verification framework ของ repository

## Checks และ cleanup — PASS

- `docker compose config --quiet`, README Bash syntax, local Markdown links, skill YAML frontmatter และ git diff --check ผ่าน
- CI matrix มี Chapter 03 standalone project path เดิมหนึ่งรายการ ไม่ต้องแก้ matrix เพิ่ม แต่ไม่ได้รัน hosted job
- JAR inspection ไม่พบ old custom transport factory/configuration, test CA loader, cert fixture, private key, token หรือ truststore ใน BOOT-INF/classes
- ตรวจ intended repository files และ verification logs เทียบ issued keys ที่มี paired token/id files ไม่พบ token leakage ไม่พิมพ์ secret values
- Driver หยุด own application JVMs/hostname fixture และ revoke test keys ทั้งหมด เก็บ existing Compose cluster/volume/demo keys ไว้ ไม่สั่ง compose down กับ cluster ที่เปิดอยู่ก่อนงานนี้
- ไม่มี Chapter 01/02 changes จากงานนี้ ไม่มี dependency/framework ใหม่ ไม่ commit/push

**NOT VERIFIED:** Hosted CI, Windows/WSL/remote Docker, production public-CA endpoint, production load, multi-node revocation/network partition, hot reload และ document/vector repository behavior Future cancellation ไม่รับประกัน I/O abort; HTTP-layer retry ถูกปิดแต่ low-level REST client ยังมี retry/failover semantics ของ library

[Historical custom-transport report](verification/before-auto-configuration-2026-10-26.md) เก็บผลก่อน scope ใหม่นี้ ไม่ใช้เป็นหลักฐานว่า default starter path ผ่าน

## POM formatting cleanup — 2026-10-26

จัด blank lines แยก sections/dependencies/plugins และขยาย nested configuration/goals ที่อัดบรรทัดเดียว ลบ comment ที่ซ้ำกับคำอธิบายใน README ไม่มีการเปลี่ยน dependency order/versions/exclusions/scopes หรือ build/test configuration

PASS: เปรียบเทียบ XML tree ก่อน/หลัง โดยตัดเฉพาะ whitespace/comment ได้ model เดียวกัน และรัน `env JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw -B -ntp validate` จาก directory ของบท exit 0 BUILD SUCCESS เวลา 2026-10-26T07:15:16+07:00; log `/private/tmp/ch03-pom-clean-validate.log`

Raw source fingerprint หลังจัด POM (46 non-Markdown files ตามวิธีเดิม): `ccd3ac2348bfcf1a3685928ac15f1b72cc41bf7abd5be9306d63d64862bab099` ผล full tests/smoke ด้านบนเป็นผลก่อน formatting ไม่ได้รัน suites ซ้ำสำหรับการจัด XML ที่ model ไม่เปลี่ยน
ตรวจแล้ว non-Markdown files อื่นตรงกับ previous verified fingerprint เมื่อแทน POM ด้วย bytes ก่อนจัดรูปแบบ
