> Historical evidence ของโครงสร้างก่อนเปลี่ยนเป็น starter-only ไม่ใช่ผล verification ของ source ปัจจุบัน

# Spring Data starter — verification

Overall: **Verified** สำหรับ local API Key/TLS/lifecycle scope ที่ระบุ ดู [historical chapter evidence](before-jvm-truststore-2026-10-26.md) สำหรับ shared infrastructure, cross-project parity และ cleanup

## Scope/source/environment

- Approved: Chapter 03 เปรียบเทียบ core/starter สอง standalone Maven projects ไม่มี official async application path
- Base HEAD `acba0e394aff6f3205dd685e237beb80e8ef2e38`, branch `codex/chapter-02-transport-refactoring`; dirty/untracked Chapter 03 พร้อม instructions/roadmap/CI amendments ไม่ commit/push
- Runnable-source fingerprint (47 files รวม chapter-common infrastructure): `8bb13f8f08e1531d327552e607bffdb949c86ad9350fe322b1a37ae9575b992d`
- Final JAR SHA-256: `798980396eaf87f4ea7a4dfb5bd080166dbed53c95f303d9a1d9a9a2c737e748`
- Hash paths จาก `git ls-files --cached --others --exclude-standard -- 03-api-key-authentication` เรียง chapter-relative paths แล้ว hash `path + NUL + bytes + NUL` ของ non-Markdown files ใน project นี้และ chapter-common files ไม่รวม sibling project/ignored generated material/target
- Local macOS 27.0.1 arm64, Temurin 21.0.12.1, Maven 3.9.11/Wrapper 3.3.4, Docker 29.8.2/Compose 5.6.0
- Server `opensearchproject/opensearch:3.9.0`; digest อยู่ใน chapter report ไม่มี test result claims ของ OS อื่น

## Final Maven VERIFY — PASS

รันจาก `spring-data-starter`:

```bash
env JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home \
  ./mvnw -B -ntp clean verify \
  -Dmaven.compiler.showDeprecation=true -Dmaven.compiler.failOnWarning=true
```

Exit 0, BUILD SUCCESS, finished `2026-10-26T02:10:49+07:00`, elapsed 01:43 min; log `/private/tmp/ch03-starter-final.log`; generated Surefire/Failsafe reports ใน target/ ไม่ commit

| Suite | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| Unit/HTTP/reactive/configuration | 52 | 0 | 0 | 0 |
| Real OpenSearch integration | 10 | 0 | 0 | 0 |
| Total | 62 | 0 | 0 | 0 |

Native ITs พิสูจน์ monitor 200, info-only health 403, index creation/Security API denied 403 และ list ไม่มี plaintext token; HTTP cases success/info-only/invalid/CA/unavailable; expiry native 200→401 และ application 502; cached old-key revoke/rotation พร้อม new-key success; trusted-but-wrong-SAN TLS failure; closed-context transport failure

Unit tests ตรวจ key-file whitespace/length/missing-file, URI/pool/timeout validation, startup/configuration, lazy invocation/fresh retry/sync+async failures/cancellation/deadline และ HTTP/error contracts

Starter tests เพิ่ม unsafe built-in URI/multiple URIs/Basic credentials/timeout rejection; ตรวจ bean factory origins ของ template/converter ว่ามาจาก OpenSearchDataConfiguration จริง

## Dependencies/artifact checks — PASS

```bash
./mvnw -B -ntp org.apache.maven.plugins:maven-dependency-plugin:3.10.0:tree \
  -DoutputFile=target/dependency-tree.txt \
  org.apache.maven.plugins:maven-help-plugin:3.5.2:effective-pom \
  -Doutput=target/effective-pom.xml
```

ใช้ JAVA_HOME เดียวกัน exit 0, BUILD SUCCESS, finished 2026-10-26T02:10:14+07:00; log `/private/tmp/ch03-starter-dependencies.log`

Resolved Spring Data 3.1.4, Boot 4.1.2, opensearch-java 3.10.0 และ HttpClient 5.6.5 ไม่มี Elasticsearch Rest5/high-level client ใน graph; core ไม่มี starter ส่วน starter graph รวม core จริง ตรวจ JAR application classes ไม่มี test support/CA fixtures/private keys/token files

Strict main/test compile ไม่มี deprecation warnings และไม่ suppress warnings JVM dynamic-agent และ macOS Netty native DNS fallback messages ยังเกิด ไม่อ้างว่าทุก runtime log warning-free

## REVIEW/limitations

Read-only review ก่อน final VERIFY ตรวจ approved objectives, transport/credential ownership, reactive behavior, tests/fixtures และ README ไม่พบ unresolved findings ที่ขวาง VERIFY; final fingerprint ตรวจเทียบ source อีกครั้งหลัง tests

Metadata/health เป็น reactive-client tier ไม่ใช่ CRUD/repository mapping coverage ไม่มี production load/multi-node/rotation hot reload proof GitHub-hosted CI, Windows/WSL/remote Docker **NOT VERIFIED**


## Packaged parity/final state — PASS

Final packaged suite หลัง final bootstrap configuration ผ่าน 12 checkpoints ทั้งสอง projects: healthy/custom pool, permissions, invalid, CA, unavailable, rotation A/B ก่อน revoke, cached A หลัง revoke, B หลัง revoke A, expiry ก่อน/หลัง และ hostname mismatch ทุก checkpoint ตรวจ two endpoints/project และ cross-project parity รายละเอียด commands/logs อยู่ใน chapter report

Final source/JAR fingerprints ตรงกับ final Maven artifacts; ไม่มี runtime source edits หลัง final verification REVIEW ไม่มี unresolved findings ใน approved scope เก็บ environment/platform limitations ตามจริง ไม่ถือว่า hosted CI ผ่าน

Recorded: 2026-10-26T02:18:42+07:00
