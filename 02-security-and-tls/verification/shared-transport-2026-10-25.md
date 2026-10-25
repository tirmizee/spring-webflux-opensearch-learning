# Chapter 02 — Security and TLS verification

Overall: **Verified** สำหรับ cluster metadata/health ผ่าน Spring Data reactive-client tier และ official async client บน environment ด้านล่าง ไม่ใช่ production certification หรือผลทุก platform

## Plan และ REVIEW

- Approved plan: Chapter 02 Security and TLS ใน conversation นี้ ผู้ใช้ตอบ “approve” เมื่อ 2026-10-25 รวม standalone Maven project, สอง paths, strict CA/hostname validation, least privilege, tests/Compose/README และ Boot-managed JUnit 6 ไม่อนุญาต commit/push
- REVIEW ทำแบบ read-only เทียบ approved objectives/API/security/tests ก่อน final VERIFY ไม่ใช้ diagnostic test run แทน review
- P2 ที่พบ: OpenSearchConfiguration.java เดิมบรรทัด 36 ใช้ stripTrailing() ซึ่งเปลี่ยน password spaces; กลับ IMPLEMENT แก้เป็น readPassword() ที่ตัด final LF/CRLF หนึ่งชุด มี regression assertion ว่า spaces คงเดิมและ multiline ถูกปฏิเสธ
- P2 ที่พบ: SecureOpenSearchTestEnvironment.java เดิมบรรทัด 34–39 ไม่ cleanup partial material เมื่อ preparation fail/timeout; แก้ให้หยุด child processes และลบ material ของ test ก่อน throw
- REVIEW ซ้ำหลัง fixes: ไม่พบ unresolved findings ใน approved scope ตรวจ ownership, HTTPS-only properties, endpoint-scoped credentials, strict hostname verification, lazy calls, typed sanitized errors และ coverage ของสอง paths แล้ว

## Source และ environment

- Recorded: 2026-10-25T13:55:30+07:00 (Asia/Bangkok)
- Base commit: 0aa84f6aef54506cbe065f115551e384e2b7dd3d; branch codex/chapter-development-workflow มี untracked Chapter 02 และแก้ CI matrix/roadmap ยังไม่ commit/push
- Runnable-source SHA-256: `8f0a8519533a3b40f39350b7343431920c3062bdd18beffb979d0e919b078ace`
- Fingerprint ครอบคลุม 42 runnable/config/test/script/Wrapper files ของบทจาก git ls-files --cached --others --exclude-standard เรียง chapter-relative paths แล้ว hash `path + NUL + bytes + NUL` ไม่รวม Markdown และ ignored generated material/build outputs ส่วน CI/roadmap ตรวจ diff แยก
- Packaged JAR SHA-256: `04bc9b1d0c80f59f0a35084d9bc09ea715b05ba1b3a211e1e0dd82c0fd037f7f`
- macOS 27.0.1 (26A434), arm64; Temurin 21.0.12.1+1; Maven 3.9.11, Wrapper 3.3.4
- Docker Engine 29.8.2 / Compose 5.6.0; LibreSSL 3.3.6; jq 1.7.1-apple
- Server image: opensearchproject/opensearch:3.9.0, observed arm64 digest `sha256:adfa61f85025d06b4aeb562e7e74fde7e31c437039c93c3862c17e9acebd6c7c`
- Resolved: Boot 4.1.2 / Framework 7.0.10; Spring Data OpenSearch 3.1.4 / Spring Data Elasticsearch 6.1.2; official client 3.10.0; HttpClient 5.6.5 / HttpCore 5.4.4; Jackson 3.1.7; JUnit Jupiter 6.0.3 / Mockito 5.23.0 / Testcontainers 2.0.5
- Classpath ยังมี Jackson 2.21.7 runtime และ low-level opensearch-rest-client transitive; application เลือก Jackson 3 + Apache transport โดย explicit configuration ไม่อ้างว่ามี Jackson major เดียว

## Commands และ final gates

Working directory ของคำสั่งคือ `02-security-and-tls/` ใช้ JDK 21 ผ่าน JAVA_HOME ที่ระบุข้างต้น Final Maven command จริง:

```bash
env JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home \
  ./mvnw -B -ntp clean verify \
  org.apache.maven.plugins:maven-dependency-plugin:3.10.0:tree -DoutputFile=target/dependency-tree.txt \
  org.apache.maven.plugins:maven-help-plugin:3.5.2:effective-pom -Doutput=target/effective-pom.xml
```

| Gate | Actual command / observed result | Status |
|---|---|---|
| Build/unit/HTTP | Final Maven command exit 0, BUILD SUCCESS, 43 Surefire tests | PASS |
| Secure IT | Failsafe 7 tests ต่อ OpenSearch จริง บน random mapped ports ไม่พึ่ง Compose | PASS |
| Dependencies | dependency tree/effective POM สร้างจริงใน target/; exact APIs compile และ runtime ผ่าน | PASS |
| Prepare | bash scripts/prepare-security.sh exit 0; ทดลองซ้ำกับ output เดิม exit 2 โดยไม่ regenerate | PASS |
| Compose | cp .env.example .env; docker compose config --quiet; docker compose up -d opensearch; ps แสดง healthy | PASS |
| Bootstrap | bash scripts/initialize-security.sh exit 0; securityadmin แสดง Done with success และ authenticated readiness ผ่าน ไม่มี -nhnv | PASS |
| Packaged application | java -jar target/security-and-tls.jar เริ่ม Netty ที่ loopback 8082 ใช้ CA/app identity จาก files | PASS |
| Native examples | Monitor info=200, invalid password=401, info-only health=403, monitor PUT /ch02-forbidden-index=403 และ security_exception | PASS |
| Healthy API | bash scripts/smoke-test.sh healthy: four routes 200, field assertions และ info/health parity ผ่าน | PASS |
| Missing permissions | Restart app ด้วย ch02_info_only + info.password; smoke insufficient-permissions: info 200, health 502/OPENSEARCH_AUTHORIZATION_FAILED ทั้งสอง paths | PASS |
| Invalid credentials | Restart app ด้วย invalid.password; smoke invalid-credentials: four routes 502/OPENSEARCH_AUTHENTICATION_FAILED พร้อม parity | PASS |
| Wrong CA | Restart app ด้วย unrelated-ca.pem; smoke tls-failure: four routes 502/OPENSEARCH_TLS_FAILURE พร้อม parity | PASS |
| Wrong hostname | ใช้ mismatch.pem จาก CA เดียวกันแทน generated HTTP leaf ชั่วคราวและ restart owned Compose node; cURL exit 60; packaged smoke tls-failure ผ่าน four routes แล้ว restore original leaf/restart และ healthy smoke ผ่านอีกครั้ง | PASS |
| Dependency unavailable | docker compose stop opensearch; smoke unavailable: four routes 503/OPENSEARCH_UNAVAILABLE พร้อม parity; start และรอ authenticated 200 สำเร็จ | PASS |
| Deadline/cancellation | Unit StepVerifier virtual-time/controllable futures ทั้งสอง services; ไม่อ้าง network-abort หรือ runtime outage timeout จาก unit proof | PASS |
| Cleanup | Owned JAR processes ปิดแล้ว; docker compose down ลบ container/network ของบท ไม่มี running containers จากงาน; เก็บ os-learning-ch02_opensearch-data และ ignored generated certificates ไว้ | PASS |

Packaged failure modes เริ่ม JAR ใหม่ทีละ process ด้วย variables ตาม README และหยุด/รอ child process หลังแต่ละ mode ไม่แก้ credentials ผ่าน API Hostname exercise เปลี่ยนเฉพาะ generated material ของงานและ restore bytes เดิม ไม่แก้ tracked configuration

## Suite counts

| Suite | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| OpenSearchConfigurationTest | 2 | 0 | 0 | 0 |
| OpenSearchPropertiesTest | 9 | 0 | 0 | 0 |
| OpenSearchTlsContextFactoryTest | 2 | 0 | 0 | 0 |
| ClusterConnectionControllerTest | 12 | 0 | 0 | 0 |
| ApiExceptionHandlerTest | 4 | 0 | 0 | 0 |
| JavaClientClusterConnectionServiceTest | 7 | 0 | 0 | 0 |
| SpringDataClusterConnectionServiceTest | 7 | 0 | 0 | 0 |
| OpenSearchTlsFailureIT | 1 | 0 | 0 | 0 |
| SecureOpenSearchConnectionIT | 6 | 0 | 0 | 0 |
| Total | 50 | 0 | 0 | 0 |

## Coverage และ evidence locations

- Spring Data reactive และ official async ผ่าน real metadata/health HTTP tests ด้วย monitor/info-only/invalid credentials และ wrong CA ตรวจ normalized DTO/error parity ทั้งสอง prefixes
- Selected-client real spies ตรวจ no invocation ก่อน subscribe และแต่ละ service ใช้ client ของตัวเอง พร้อม shared transport identity และ shutdown behavior
- Hostname IT ใช้ valid leaf ที่ CA signature ถูกต้อง แต่ SAN เป็น wrong.invalid ตรวจ TLS cause และ mapped 502 ของสอง services โดยไม่ bypass verification
- Native monitor identity ถูกปฏิเสธ index creation ไม่มี document dataset หรือ successful index write ในบทนี้
- Surefire/Failsafe XML อยู่ใน `target/surefire-reports/` และ `target/failsafe-reports/` เป็น local generated evidence ไม่ commit
- Dependency tree/effective POM อยู่ใน `target/` ไม่ commit
- Raw run logs เป็น local ephemeral files: `/private/tmp/ch02-final-verify.log`, `/private/tmp/ch02-bootstrap.log`, `/private/tmp/ch02-smoke-healthy.log`, `/private/tmp/ch02-smoke-insufficient-permissions.log`, `/private/tmp/ch02-smoke-invalid-credentials.log`, `/private/tmp/ch02-smoke-tls-failure.log`, `/private/tmp/ch02-smoke-hostname-mismatch.log`, `/private/tmp/ch02-smoke-unavailable.log`, `/private/tmp/ch02-smoke-healthy-restored.log`
- ตรวจ real generated passwords ไม่อยู่ใน intended tracked files และ logs ของงาน; keys/password files/target ถูก ignore; ไม่มี Chapter 01 diff

## Failures/retests และ limitations

- Unit diagnostic แรก: 42 tests, 2 failures เพราะ copied HTTP tests ยัง assert Chapter 01 timeout title แก้ assertions ให้ตรวจ OPENSEARCH_TIMEOUT ตาม Chapter 02 contract แล้ว diagnostic verify ผ่าน 42+7; หลัง review fixes final clean verify ผ่าน 43+7
- Temporary smoke orchestration helper เคยรายงาน owned process ยังไม่หยุดจาก PID probe หลัง TERM แต่ exec session ยืนยัน exit 143 และ graceful-shutdown log พร้อม port 8082 ปิดแล้ว จึงรัน remaining scenarios ด้วย child-process wait สำเร็จ ไม่ใช่ application/test failure และไม่มีการลด assertions
- มี JVM dynamic-agent warning และ Netty macOS native DNS fallback warning ใน environment นี้ ทั้งสองไม่ทำให้ gates fail ไม่อ้างผลบน Linux/amd64 หรือ remote Docker
- Fixture-preparation failure/timeout cleanup ตรวจจาก source; ไม่ได้ fault-inject ทุก OS process failure branch
- Local keys มี host parent 0700 แต่ mounted files ต้องอ่านได้โดย container user; ไม่ใช่ production secret manager, PKI หรือ shared-host isolation guarantee
- Future cancellation ไม่รับประกัน server-side cancellation; ไม่ทดสอบ certificate rotation, multi-node HA, production load หรือ application-facing TLS/authentication
- GitHub-hosted CI: **NOT VERIFIED** ยังไม่ push/run workflow การเพิ่ม matrix entry ไม่ใช่ hosted CI pass
- REVIEW และ required local gates ผ่านบน runnable source fingerprint ที่ระบุ จึงตั้ง roadmap เป็น Verified เฉพาะ scope/platform นี้
