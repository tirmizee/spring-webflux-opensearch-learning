> Historical evidence ก่อนปรับ README/smoke diagnostics; Java implementation ไม่ได้เปลี่ยนในการแก้คู่มือ

# Verification — Chapter 03 starter-only

Overall: **Verified** สำหรับ local starter API Key/configuration/JVM trust/lifecycle scope ตามผลจริงด้านล่าง ไม่รวม production, hosted CI หรือ platform อื่น

## Source และ environment

- ตรวจวันที่ 2026-10-26, Asia/Bangkok (+07:00)
- ผู้ใช้อนุมัติให้เหลือ `spring-data-opensearch-starter` แนวทางเดียว ย้าย Maven project มาไว้ที่ directory ของบท ไม่มี core-only project หรือ two-path parity gate
- Branch `codex/chapter-02-transport-refactoring`; HEAD `acba0e394aff6f3205dd685e237beb80e8ef2e38` เป็น base เท่านั้น Chapter 03 ยัง untracked และ foundation มี uncommitted changes จึงไม่ใช้ commit hash อ้างแทน source ใหม่ ไม่ commit/push
- macOS 27.0.1 arm64; Temurin 21.0.12.1; Maven 3.9.11; Docker Server 29.8.2; Compose 5.6.0
- Boot 4.1.2; starter/core transitive 3.1.4; underlying OpenSearch Java Client 3.10.0; OpenSearch image 3.9.0
- Image digest: `opensearchproject/opensearch@sha256:adfa61f85025d06b4aeb562e7e74fde7e31c437039c93c3862c17e9acebd6c7c`

Fingerprint รวมไฟล์ใหม่ที่ยังไม่ track ใช้รายการ `git ls-files --cached --others --exclude-standard -- 03-api-key-authentication` deduplicate/sort chapter-relative paths ตัด `.md` และ ignored/generated files แล้ว SHA-256 ของ `path + NUL + raw file bytes + NUL` ตามลำดับ ไม่ใช่ hash เฉพาะ tracked files

| Item | SHA-256 |
|---|---|
| Source/config/scripts/tests/wrapper 48 non-Markdown files | `4a6cc203c63d5c4fdff345158557cc24ad94728a27fcd5807c286bb165147842` |
| target/api-key-authentication.jar | `f6c214d381742acdd044eae30383d6990cb88a82a6eafe3775d7492a138067b1` |

Markdown/evidence-only edits หลัง VERIFY ไม่เปลี่ยน runtime fingerprint; ตรวจซ้ำก่อนปิดงานแล้ว

## REVIEW — PASS

ตรวจ read-only ตาม review-chapter ก่อน final VERIFY: approved starter-only scope, configuration binding, effective validation, template/converter factory origins, reactive lazy invocation, error mapping, transport ownership, tests, README paths และ CI matrix ไม่พบ actionable findings ค้าง ไม่ใช้ผลอ่าน code แทน runtime evidence

Root AGENTS/docs/skills ระบุข้อยกเว้น starter-only; CI matrix มี Chapter 03 project เดียวที่ directory ของบท ไม่เปลี่ยน Chapter 01/02

## Maven compilation / tests — PASS

รันจาก `03-api-key-authentication`:

```bash
env JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home \
  ./mvnw -B -ntp clean verify \
  -Dmaven.compiler.showDeprecation=true -Dmaven.compiler.failOnWarning=true
```

Log `/private/tmp/ch03-starter-only-verify.log`; exit 0, BUILD SUCCESS, finished `2026-10-26T02:49:56+07:00`, elapsed 01:42 min ตรวจ Surefire/Failsafe XML จริง:

| Suite | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| Unit/reactive/HTTP/configuration | 55 | 0 | 0 | 0 |
| Real OpenSearch integration | 10 | 0 | 0 | 0 |
| Total | 65 | 0 | 0 | 0 |

Unit coverage: HTTPS URI/timeouts/pool validation, API Key file validation, single-prefix binding ของ built-in/additional properties, invalid starter inputs, template/converter origins, close ownership, deferred fresh invocation/errors/cancellation/deadlines และ HTTP contracts

IT coverage: native scoped permissions, denied index/Security administration, successful HTTP info/health, insufficient permissions, invalid token, untrusted CA, unavailable server, cached-key rotation/revocation, expiry bounded polling, trusted-but-wrong-SAN failure และ shutdown Tests สร้าง isolated containers/keys/CA ไม่พึ่ง Compose instance; test-only default SSLContext เปลี่ยนชั่วคราวตอนสร้าง application และ restore ใน finally, class ใช้ @Isolated

ไม่มี compiler deprecation warnings และไม่ suppress เพื่อซ่อนปัญหา Runtime ยังมี Mockito dynamic-agent warning กับ Netty macOS DNS native fallback message; ไม่ใช่ compiler deprecation และไม่ได้อ้างว่า dependency diagnostics ทุกชนิดหาย

Diagnostic attempts ก่อน final source เคย fail จาก package rename ระหว่างแก้ และ @Validated บน effective record bean ที่ทำให้ CGLIB proxy ไม่ได้ แก้ให้ใช้ explicit Validator ก่อนคืน settings แล้ว full final run ด้านบนผ่าน ไม่มีการ skip gates หรือเอา assertions ออก

## Dependency verification — PASS

```bash
env JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home \
  ./mvnw -B -ntp org.apache.maven.plugins:maven-dependency-plugin:3.10.0:tree \
  -DoutputFile=/private/tmp/ch03-starter-only-dependencies.txt
```

Exit 0; log `/private/tmp/ch03-starter-only-dependencies.log` Graph มี starter 3.1.4 และ core 3.1.4 แบบ transitive ตามปกติ ไม่มี core-only application path ไม่มี co.elastic Java client หรือ high-level REST client Runtime beans ใช้ ApacheHttpClient5Transport กับ ReactiveOpenSearchClient และ starter-created template/converter ตาม assertions จริง Low-level opensearch-rest-client ยังเป็น transitive dependency แต่ไม่ได้สร้าง bean ของมัน

ไม่มี dependency ใหม่จาก refactor ครั้งนี้

## Compose / infrastructure — PASS

รันจริงจาก directory ของบท:

```bash
docker compose config --quiet
docker compose up -d opensearch
bash scripts/initialize-security.sh
bash scripts/list-api-keys.sh
```

Compose config exit 0 โดยไม่ dump expanded values; initializer exit 0 พร้อม securityadmin success และ administrator HTTPS readiness (`/private/tmp/ch03-trust-bootstrap.log`) List command exit 0 เก็บเฉพาะ metadata ที่ `/private/tmp/ch03-starter-only-key-metadata.json` ตรวจไม่มี token field

Existing local PKI/data volume ถูกเก็บไว้ ใช้ JDK keytool import public CA เดิมเข้า `truststore.p12` และ unrelated CA เข้า `unrelated-truststore.p12` ไม่ regenerate certificates ของ volume เดิม ส่วน revised prepare-security.sh ที่สร้าง truststores อัตโนมัติรันจริงผ่าน isolated IT fixtures ไม่มี application CA property/loader

## Packaged application smoke — PASS

ใช้ `target/api-key-authentication.jar` ตาม hash ข้างบน ผ่าน local temporary driver `/private/tmp/ch03-starter-only-smoke.py` ซึ่งไม่ได้เพิ่มใน repository:

```bash
python3 /private/tmp/ch03-starter-only-smoke.py
```

Driver เปิด JVM จริงด้วย Java 21, random loopback APP_PORT, OPENSEARCH_URI/API_KEY_FILE และ pool limits 8/2; ส่ง `-Djavax.net.ssl.trustStore=... -Djavax.net.ssl.trustStoreType=PKCS12 -Djavax.net.ssl.trustStorePassword=changeit` ก่อน `-jar` ยกเว้น default-trust checkpoint ที่จงใจไม่ส่ง truststore options แล้วเรียก chapter-owned command:

```bash
APP_BASE_URL=http://127.0.0.1:<allocated-port> bash scripts/smoke-test.sh <mode>
```

Exit 0; `/private/tmp/ch03-starter-only-smoke-suite.log` ผ่าน 13 checkpoints แต่ละ checkpoint ตรวจ info/health status และ fields/error code/detail/instance ไม่ดูแค่ HTTP success:

| Checkpoint | Actual outcome |
|---|---|
| healthy | 200 info/health ด้วย local JVM truststore และ custom pool bounds |
| permissions | Info 200; health 502 AUTHORIZATION_FAILED ด้วย info-only key |
| invalid | 502 AUTHENTICATION_FAILED ทั้งสอง routes |
| ca | Unrelated JVM truststore → 502 TLS_FAILURE |
| default-trust | ไม่ส่ง local truststore → private CA ถูกปฏิเสธ 502 TLS_FAILURE |
| unavailable | URI ที่ติดต่อไม่ได้ → 503 UNAVAILABLE |
| rotation-a-before / rotation-b-before | Key A/B ใช้งานสำเร็จก่อน revoke |
| rotation-a-revoked / rotation-b-after | Cached A ถูกปฏิเสธหลัง native revoke; B ยัง 200 |
| expiry-before / expiry-after | Native 200 ก่อนหมดอายุ แล้ว bounded polling จน native 401; cached application key ถูกปฏิเสธ |
| hostname | CA เชื่อถือได้แต่ SAN ผิด → 502 TLS_FAILURE; native cURL trust check exit 60 |

Logs แยก `/private/tmp/ch03-starter-only-smoke-<checkpoint>.log`, application logs `/private/tmp/ch03-starter-only-app-*.log`, lifecycle `/private/tmp/ch03-starter-only-lifecycle.log` ไม่มี token output คำสั่ง create/revoke รันผ่าน scripts จริงโดยใช้ unique labels แทน labels ตายตัวใน README

Temporary driver เป็น local orchestration เท่านั้น ไม่ใช่ required Python verification framework ใน repository Reusable smoke assertions อยู่ใน Bash/jq ของบท; lifecycle coverage ที่ repeat ผ่าน Maven อยู่ใน ITs

## Additional checks — PASS / limits

- `bash -n` ทุก chapter script ผ่าน
- Ruby YAML ตรวจ CI matrix มี 3 standalone projects พร้อม Maven Wrapper paths จริง และ skill YAML frontmatter ทั้งสามผ่าน
- skill-creator quick_validate.py รันไม่ได้เพราะ local Python ไม่มี PyYAML จึงใช้ Ruby ตรวจ frontmatter แทน ไม่ติดตั้ง tooling/dependency เพิ่ม ไม่อ้างว่า Python validator ผ่าน
- Local Markdown link targets และ `git diff --check` ผ่าน
- JAR inspection ไม่พบ test TLS loader, CA fixture, private key, token หรือ truststore ใน BOOT-INF/classes
- ตรวจ issued keys ที่มี paired token/id files กับ intended repository files และ verification logs โดยไม่พิมพ์ secrets: ไม่พบ leakage
- ไม่มี Chapter 01/02 diff; ไม่มี Python files เพิ่มในบท; ไม่มี spring-data-core/spring-data-starter subprojects หลงเหลือ
- Final scoped cleanup: driver หยุด owned JVMs/hostname container และ revoke keys ที่ออกในการทดสอบทั้งหมด จากนั้น `docker compose down` exit 0 หยุด own Chapter 03 container/network ไม่ใช้ -v เก็บ data volume และ generated PKI/demo keys ไว้

**NOT VERIFIED:** Hosted GitHub Actions, Windows/WSL/remote Docker, production public-CA endpoint, production load, multi-node/network-partition behavior, hot reload และ document/vector mapping ไม่ได้อ้างผลจาก passing local tests แทนสิ่งเหล่านี้

README command variations: ไม่รัน `cp .env.example .env` เพราะใช้ existing/default environment; packaged tests ใช้ random APP_PORT แทน literal 8083 แต่ใช้ documented route/contracts จริง Truststore migration ใช้ CA เดิม และ lifecycle ใช้ unique labels ไม่ overwrite demo monitor/info labels

## Historical evidence

ผลจากโครงสร้างสอง projects ก่อนแก้ไม่ใช่ผลของ source ใหม่:

- [Previous chapter report](before-jvm-truststore-2026-10-26.md)
- [Historical core report](historical-core-2026-10-26.md)
- [Historical starter report](historical-starter-2026-10-26.md)
