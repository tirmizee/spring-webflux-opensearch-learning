# Chapter 02 — ConnectionConfig timeout correction verification

Overall: **Verified** สำหรับ metadata/health, strict TLS และ application identities ผ่าน Spring Data framework configuration และ official async configuration แยก transports บน local environment ที่บันทึก ไม่ครอบคลุม document mapping/repositories หรือทุก platform

## Scope, IMPLEMENT และ REVIEW

ผู้ใช้ส่งภาพ deprecated RequestConfig.Builder.setConnectTimeout และขอแก้ใน conversation นี้เมื่อ 2026-10-25 จึงย้าย connect timeout ไป ConnectionConfig ที่ผูกกับ connection manager จริง ไม่ลบ timeout และไม่ suppress warning ของ setter เดิม

- ใช้ supported OpenSearchRestClientConfigurationCallback แทน HTTP callback ของ helper เพราะ helper 3.1.4 เขียนทับ connection manager หลัง HTTP callback
- Framework ยังคงสร้าง reactive client/template/converter และ RestClientTransport ส่วน callback กำหนด strict TLS และ bounded pool 25/5 พร้อม ConnectionConfig connect timeout; RequestConfig ตั้งเฉพาะ pool acquisition/response timeout
- ตรวจ compiled class ด้วย javap -c -p พบ ConnectionConfig$Builder.setConnectTimeout และ setDefaultConnectionConfig/setConnectionManager แต่ไม่มี RequestConfig$Builder.setConnectTimeout
- Bug-fix workflow: IMPLEMENT → VERIFY → read-only REVIEW; ทบทวน TLS/auth/header handling, resource ownership และ source scope หลัง final tests/smoke ไม่พบ unresolved findings ใน scope
- ผล framework configuration ก่อนแก้ 53 tests อยู่ใน [historical framework report](framework-configuration-2026-10-25.md) ไม่ใช่หลักฐานของ source ปัจจุบัน


ผู้ใช้ขอแก้ Spring Data path ให้ใช้ configuration ของ library หลังอธิบายข้อจำกัดของ shared manual configuration อนุมัติให้ใช้ ReactiveOpenSearchConfiguration/ClientConfiguration แยกจาก official path ใน conversation นี้เมื่อ 2026-10-25 ไม่เปลี่ยน API/security contracts และไม่อนุญาต commit/push รอบนี้

- SpringDataOpenSearchConfiguration extends ReactiveOpenSearchConfiguration; framework สร้าง reactive client, ReactiveOpenSearchTemplate, converter และ RestClientTransport ผ่าน ClientConfiguration
- OpenSearchConfiguration สร้างเฉพาะ official OpenSearchAsyncClient กับ ApacheHttpClient5Transport
- ใช้ qualifiers springDataTransport/javaClientTransport และ transport close owners แยกกัน ปิด inferred close ของ Spring Data RestClient/reactive wrapper เพื่อไม่ให้ Spring ลงทะเบียน shutdown ซ้ำ
- API errors รองรับทั้ง direct transport ResponseException และ low-level RestClient ResponseException โดยไม่ parse raw message
- REVIEW แบบ read-only เทียบ scope, source/configuration, lazy invocation, TLS, credentials, qualifiers, timeout callback, converter wiring และ test coverage ก่อน final VERIFY แล้ว ไม่พบ unresolved source findings
- หลัง VERIFY ทบทวนว่า runtime evidence ตรงกับ source ที่ review และไม่มี behavior/config/test edits ตามหลัง run นี้

ผล shared transport เดิม **50 tests** อยู่ใน [historical report](shared-transport-2026-10-25.md) ใช้อ้างได้เฉพาะ source scope เดิม ไม่รวมเป็นผลใหม่และไม่ถือเป็น proof ของ framework configuration นี้

## Source และ environment

- Recorded: 2026-10-25T14:50:31+07:00 (Asia/Bangkok)
- Base commit: 866cb87aff1ccea1062b290aa51281cb1bbd0f26; branch codex/chapter-02-security-and-tls มี uncommitted configuration/tests/docs ของ Chapter 02 ไม่ commit/push รอบนี้
- Runnable-source SHA-256: `57af5830a50d70e279dd4218ae5fddcb59056049e2870bc8392d7a076140a1c5`
- Scope: 45 runnable/config/test/script/Wrapper files ของบท จาก git ls-files --cached --others --exclude-standard เรียง chapter-relative paths แล้ว hash `path + NUL + bytes + NUL` ไม่รวม Markdown/ignored generated material/target
- Packaged JAR SHA-256: `ef21ce46738bc16fd711c177421ede1298d95cfdd0bd9897e3ca2236448018f0`
- Environment เดียวกับ historical run: macOS 27.0.1 arm64, Temurin 21.0.12.1+1, Maven 3.9.11, Wrapper 3.3.4; Docker Engine 29.8.2/Compose 5.6.0
- Pinned server image: opensearchproject/opensearch:3.9.0; image เดิมจาก historical report ไม่มีการเปลี่ยน tag หรือ dependency versions
- Resolved graph: Boot 4.1.2 / Framework 7.0.10, Spring Data OpenSearch 3.1.4 / Spring Data Elasticsearch 6.1.2, official client 3.10.0, low-level RestClient 3.8.0, HttpClient 5.6.5 / HttpCore 5.4.4, Jackson 3.1.7, JUnit 6.0.3, Mockito 5.23.0, Testcontainers 2.0.5
- ไม่มี Elasticsearch Java Client/legacy high-level client bean ทั้งสอง transports ส่ง async requests ผ่าน HttpClient 5 ใน graph นี้ RestClient-based ไม่ได้แปลว่าใช้ synchronous calls

## Final Maven command

รันจาก directory ของบท:

```bash
env JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home \
  ./mvnw -B -ntp clean verify -Dmaven.compiler.showDeprecation=true

./mvnw -B -ntp org.apache.maven.plugins:maven-dependency-plugin:3.10.0:tree \
  -DoutputFile=target/dependency-tree.txt
```

Observed: exit 0, BUILD SUCCESS, finished 2026-10-25T14:49:08+07:00; Surefire 46 + Failsafe 7 = **53 tests**, failures/errors/skips 0

| Suite | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| OpenSearchConfigurationTest | 2 | 0 | 0 | 0 |
| OpenSearchPropertiesTest | 9 | 0 | 0 | 0 |
| OpenSearchTlsContextFactoryTest | 2 | 0 | 0 | 0 |
| SpringDataOpenSearchConfigurationTest | 3 | 0 | 0 | 0 |
| ClusterConnectionControllerTest | 12 | 0 | 0 | 0 |
| ApiExceptionHandlerTest | 4 | 0 | 0 | 0 |
| JavaClientClusterConnectionServiceTest | 7 | 0 | 0 | 0 |
| SpringDataClusterConnectionServiceTest | 7 | 0 | 0 | 0 |
| OpenSearchTlsFailureIT | 1 | 0 | 0 | 0 |
| SecureOpenSearchConnectionIT | 6 | 0 | 0 | 0 |
| Total | 53 | 0 | 0 | 0 |

## Gates และ observed results

| Gate | Actual | Status |
|---|---|---|
| Deprecated setter | javap และ compiler deprecation details ยืนยันไม่มี RequestConfig.setConnectTimeout reference ใน compiled application configuration; ใช้ ConnectionConfig setter ผูก manager จริง | PASS |
| Framework configuration | Native ClientConfiguration มี SSL context/verifier/auth; สร้าง ReactiveOpenSearchTemplate และ converter จริงใน Spring context | PASS |
| Separate transport wiring | Two distinct beans: Spring Data RestClientTransport และ official ApacheHttpClient5Transport; selected client ใช้ transport ของตัวเอง | PASS |
| Lifecycle | Spring Data RestClient.isRunning เป็น true ใน context และ false หลัง shutdown; owner definitions กำหนด close เฉพาะ transports; actual client calls ของสอง paths fail หลัง context ปิด | PASS |
| Security/HTTP IT | Monitor success, info-only health denied, wrong credentials, wrong CA, normalized fields/status และ parity บน real secure OpenSearch | PASS |
| TLS hostname IT | Valid leaf จาก trusted CA แต่ SAN=wrong.invalid ถูกปฏิเสธทั้งสอง clients พร้อม TLS cause และ mapped 502 | PASS |
| Native permissions | Real container: monitor info 200; invalid credentials 401; info-only health 403; monitor create index 403 | PASS |
| Compose configuration | docker compose config --quiet exit 0; existing cluster healthy ใช้ CA/credentials เดิม ไม่มีการ initialize security ซ้ำ | PASS |
| Packaged healthy | BASE_URL=random loopback port, smoke-test.sh healthy: four 200 routes, fields และ two parity comparisons | PASS |
| Packaged authorization | ch02_info_only/info.password: info 200, health 502/OPENSEARCH_AUTHORIZATION_FAILED ทั้งสอง paths | PASS |
| Packaged authentication | invalid.password: four routes 502/OPENSEARCH_AUTHENTICATION_FAILED พร้อม parity | PASS |
| Packaged CA trust | unrelated-ca.pem: four routes 502/OPENSEARCH_TLS_FAILURE พร้อม parity | PASS |
| Packaged hostname | Temporary isolated server ด้วย mismatch leaf, cURL exit 60, four routes 502/OPENSEARCH_TLS_FAILURE พร้อม parity | PASS |
| Packaged unavailable | OPENSEARCH_URI=https://localhost:1: four routes 503/OPENSEARCH_UNAVAILABLE พร้อม parity ไม่หยุด existing cluster | PASS |
| Secrets/source scope | Real generated passwords และ encoded Basic credentials ไม่อยู่ใน intended tracked files/new runtime logs; no Chapter 01 edits | PASS |
| Cleanup | Owned JAR child processes รอ terminate แล้ว; temporary hostname container และ copied keys cleanup; IT containers/material ปิดตาม lifecycle | PASS |

Packaged runs ใช้ random APP_PORT และ BASE_URL เพื่อไม่ชน application เดิมที่ port 8082 ตัวอย่าง variables ใช้ชุดเดียวกับ README ได้แก่ OPENSEARCH_USERNAME/OPENSEARCH_PASSWORD_FILE/OPENSEARCH_CA_CERTIFICATE ส่วน unavailable ใช้ loopback endpoint ที่ไม่มี service และ hostname case ใช้ temporary container ไม่เปลี่ยน certificate ของ Compose cluster เดิม

## Evidence และ retests

- Final Maven log: `/private/tmp/ch02-connect-timeout-final.log`; reports และ dependency tree อยู่ใน `target/` เป็น local generated evidence ไม่ commit
- Packaged logs: `/private/tmp/ch02-timeout-app-{healthy,permissions,credentials,ca,unavailable,hostname}.log` และ `/private/tmp/ch02-timeout-smoke-{healthy,permissions,credentials,ca,unavailable,hostname}.log`
- Diagnostic unit run แรกมี 46 tests/1 failure เนื่องจาก BeanPostProcessor spy ไม่ใช่ destruction target ของ Spring จึงไม่ใช่ close-count evidence ที่ถูกต้อง เปลี่ยนเป็นตรวจ resource.isRunning จริงก่อน/หลัง shutdown พร้อม owner definitions และ real calls หลัง close ไม่เปลี่ยน production behavior เพื่อทำให้ test ผ่าน
- Diagnostic retry และ final clean verify ผ่านครบ ไม่ skip/disable tests
- Exact 3.1.4 helper source แสดงว่า HTTP callback มาก่อนการกำหนด connection manager; จึงใช้ RestClient-level hook กำหนด manager/TLS/ConnectionConfig ส่วน reactive client/template/converter/RestClientTransport ยังคงสร้างผ่าน framework factories

## Limitations และ cleanup context

- Metadata endpoints ยังใช้ reactive-client tier แม้ template/converter infrastructure มีจริง ไม่ใช่ document mapping/repository feature verification
- Spring Data callback กำหนด bounded pool 25 total/5 per route โดย explicit configuration ส่วน official กำหนด 16/16 ไม่อ้าง performance equivalence หรือ identical retry/connection semantics นอก tested scope
- Optional diagnostic `clean verify -Dmaven.compiler.showDeprecation=true -Dmaven.compiler.failOnWarning=true` หยุดที่ compile เนื่องจาก RestClient/RestClientBuilder types deprecated ใน library baseline ไม่ได้รัน tests ใน diagnostic นี้ ไม่ใช่ warning ของ RequestConfig.setConnectTimeout อีกแล้ว
- Final normal verification เปิด showDeprecation=true และผ่าน 53 tests ไม่ suppress warnings Warning ที่เหลือเป็น RestClient/RestClientBuilder/RestClientTransport และ test getter getHostNameVerifier ที่ framework รุ่นนี้ยังใช้ จึงไม่อ้างว่าโครงการ warning-free การย้าย API ภายในของ third-party ออกทั้งหมดอยู่นอก scope ของ setter fix
- JVM dynamic-agent/macOS native DNS fallback warnings ยังมีตาม environment เดิม ไม่มี test failures
- Existing application PID 22115 บน port 8082 และ Compose os-learning-ch02-opensearch-1 เปิดอยู่ก่อน smoke รอบนี้ จึงรักษาไว้ ไม่หยุด containers/volume/network ของผู้ใช้ ไม่เปลี่ยน existing CA/password files
- GitHub-hosted CI: **NOT VERIFIED** ไม่ push/run workflow ในงานนี้
- Future cancellation ไม่รับประกัน server-side abort; ไม่ทดสอบ rotation, remote Docker, production load, application-facing TLS/authentication หรือ platform อื่น

REVIEW และ required local gates ผ่านบน runnable source fingerprint ที่ระบุ จึงคืน roadmap เป็น Verified เฉพาะ scope/platform นี้
