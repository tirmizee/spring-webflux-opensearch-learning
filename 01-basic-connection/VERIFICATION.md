# Chapter 01 — Dual-approach verification

Overall: **Verified** สำหรับ Spring Data reactive-client path และ official async-client path รวม legacy aliases ตาม feature scope ด้านล่าง

Formatting-only updates 2026-10-25: จัด record components ของ ClusterInfoResponse และ ClusterHealthResponse เป็นหนึ่ง component ต่อบรรทัด และปรับ OpenSearchProperties ให้เยื้อง 4 spaces ตาม code standard ใหม่ ตรวจ record declarations ทั้ง repository ครบ 3 records แล้ว ไม่เปลี่ยน names/types/order/annotations หรือ behavior ไม่รัน Maven/runtime tests ซ้ำ ผลและ source/JAR fingerprints ด้านล่างเป็นหลักฐานของ build ก่อนการจัดรูปแบบเหล่านี้

Formatting-only update เพิ่มเติม 2026-10-25: เว้นบรรทัดว่างหลังเปิด type body ก่อน annotation แรกใน OpenSearchConfiguration และ OpenSearchProperties จากนั้นตรวจ Java ทั้ง repository ครบ 16 files และแก้เพิ่ม 12 classes ใน main และ tests ให้เว้นบรรทัดว่างก่อนสมาชิกแรกตาม standard ตรวจแล้วว่า Java เปลี่ยนเฉพาะ whitespace และไม่พบ nonempty class/record body ที่ขาดบรรทัดว่างดังกล่าว ไม่รัน tests ซ้ำ และไม่เปลี่ยน source/JAR fingerprints ของ build ที่ตรวจไว้ด้านล่าง

## Source และ environment

- Timestamp: 2026-10-25T10:24:07+07:00 (Asia/Bangkok)
- Branch main; unborn HEAD ไม่มี commit/push; source เป็น untracked working tree ของ Chapter 01 และ foundation
- Runnable-source SHA-256: 75ac673c8a613afd1664ce4d3d9b1945790b485a4f566454c00b0e5a97366016
- Fingerprint ใช้ git ls-files --cached --others --exclude-standard เฉพาะบท เรียง relative paths ไม่รวม Markdown/ignored build outputs แล้ว hash `path + NUL + bytes + NUL` ไม่รวม ignored IDE settings
- Packaged JAR SHA-256: 9c04f3a4331b795c5c3f700fc19d4bc34fb64b4b3a96b96e6cd0d718ae36a19f
- macOS 27.0.1 arm64; Temurin 21.0.12.1+1; Maven 3.9.11 / Wrapper 3.3.4
- Spring Boot 4.1.2 / Framework 7.0.10 / Reactor Core 3.8.8
- Spring Data OpenSearch 3.1.4 / Spring Data Elasticsearch 6.1.2 / Spring Data Commons 4.1.2
- OpenSearch Java Client 3.10.0 / HttpClient 5.6.5 / HttpCore 5.4.4 / Jackson 3.1.7
- JUnit Jupiter 6.0.3 ตามคำอนุมัติผู้ใช้ / Mockito 5.23.0 / Testcontainers 2.0.5
- Docker Engine 29.8.2 / Docker Desktop 4.95.0 / Compose 5.6.0; Docker VM linux/arm64, memory 7934 MB
- OpenSearch image opensearchproject/opensearch:3.9.0, arm64
- RepoDigest: sha256:adfa61f85025d06b4aeb562e7e74fde7e31c437039c93c3862c17e9acebd6c7c

## Feature coverage และ abstraction tier

| Objective | Spring Data path | Official path | Actual evidence |
|---|---|---|---|
| Cluster info | ReactiveOpenSearchClient.info | OpenSearchAsyncClient.info | Parameterized selected-client IT, real DTO parity และ smoke |
| Cluster health | ReactiveOpenSearchClusterClient.health | OpenSearchClusterAsyncClient.health | Single-node assertions + parity + smoke |
| Lazy invocation / resubscription | Defer Spring Data method call | Defer future creation | 7 service cases แยกแต่ละ path |
| Sync/async failures, retry, cancel/deadline | Reactive wrapper behavior | Future bridge behavior | Unit/virtual-time cases ไม่มี blocking request flow |
| HTTP error semantics | 503/504 + sanitized ProblemDetail | Contract เดียวกัน | HTTP slice ทุก prefix และ real 503 หลังหยุด dependency |
| Lifecycle | Wrapper ไม่ own transport shutdown | Transport bean owner เดียว | Real context shutdown และ shared identity assertions |
| Backward compatibility | ไม่ใช้ legacy aliases | /api/cluster/* aliases | Slice/real IT/smoke ทั้ง info และ health |

Spring Data ใช้ reactive-client tier ตาม contract สำหรับ metadata ไม่มี repository/template operation ที่เหมาะสม บทนี้ไม่อ้าง native repository, entity mapping หรือ converter coverage ไม่มี direct-client fallback ที่ปลอมเป็น Spring Data service

## Commands และ gates ที่รันจริง

ทุก Maven command รันใน chapter ด้วย JAVA_HOME ชี้ JDK 21 เพราะ host Maven เดิมเลือก Java 27

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
./mvnw -B -ntp clean verify
./mvnw -B -ntp org.apache.maven.plugins:maven-dependency-plugin:3.10.0:tree -DoutputFile=target/dependency-tree.txt org.apache.maven.plugins:maven-help-plugin:3.5.2:effective-pom -Doutput=target/effective-pom.xml
```

JAVA_HOME เป็น path ของเครื่องที่ตรวจ ผู้เรียนตั้งตาม JDK ของตัวเอง Full build log อยู่ใน /private/tmp/ch01-dual-verify.log และ dependency log ใน /private/tmp/ch01-dual-dependencies.log เป็น ephemeral session artifacts ไม่ commit

| Gate | Command / evidence | Actual | Status |
|---|---|---|---|
| Environment | java -version, ./mvnw -version, docker version, docker compose version | Versions ตามข้างต้น; Docker ready | PASS |
| Dependency linkage | resolve/tree/effective-pom และ javap ของ actual Spring Data 3.1.4 JAR | Resolve/compile สำเร็จ; target/dependency-tree.txt และ target/effective-pom.xml | PASS |
| Full Maven verify | ./mvnw -B -ntp clean verify | BUILD SUCCESS, 15.678s, finished 09:42:41 +07; exit 0 | PASS |
| Unit/HTTP/lifecycle | target/surefire-reports | 41 tests, failures 0, errors 0, skipped 0 | PASS |
| Real OpenSearch IT | target/failsafe-reports | 8 tests, failures 0, errors 0, skipped 0 | PASS |
| Compose readiness | docker compose -p os-learning-ch01 config --quiet; up -d --wait --wait-timeout 180; ps | Healthy, 127.0.0.1:9200; exit 0 | PASS |
| Direct API readiness | curl --fail-with-body 'http://127.0.0.1:9200/_cluster/health?wait_for_status=yellow&timeout=3s' | green, nodes=1, timed_out=false; exit 0 | PASS |
| Packaged app | Java 21 java -jar target/basic-connection.jar | PID 11096, localhost:8080, startup 0.77s | PASS |
| Healthy smoke | bash scripts/smoke-test.sh healthy | ทุก 6 routes HTTP 200, field assertions และ 2 parity comparisons ผ่าน; exit 0 | PASS |
| Dependency failure | docker compose -p os-learning-ch01 stop opensearch; bash scripts/smoke-test.sh unavailable | ทุก 6 routes HTTP 503, sanitized fields และ error parity ผ่าน; exit 0 | PASS |
| README flow | Setup/readiness, full verify, packaged app, cURL ผ่าน smoke, stop/down | Default path ผ่าน ไม่ได้รัน optional port exercise | PASS |
| Cleanup | SIGTERM PID 11096; docker compose -p os-learning-ch01 down | Graceful shutdown complete, container/network removed, volume preserved | PASS |

## Test counts และ selected-client proof

| Suite | Cases | สิ่งที่พิสูจน์ |
|---|---|---|
| OpenSearchPropertiesTest | 8 | Endpoint/timeout validation |
| OpenSearchConfigurationTest | 1 | ทั้งสอง clients อ้าง transport เดียวกัน หลัง context shutdown reactive request ล้มทันทีด้วย IllegalStateException แทน network timeout |
| JavaClientClusterConnectionServiceTest | 7 | Cold future bridge, mapping, failures, fresh retry, cancellation/deadline |
| SpringDataClusterConnectionServiceTest | 7 | Deferred reactive method invocation, mapping, failures, fresh retry, cancellation/deadline |
| ClusterConnectionControllerTest | 18 | 6 behavior groups x 3 prefixes พร้อม service isolation |
| OpenSearchConnectionIT | 8 | 6 route/client cases, DTO equality case และ transport/dependency wiring case |

IT ใช้ @MockitoSpyBean บน actual clients ที่ call real methods ไม่มี stubbing responses เมื่อเรียก Spring Data route ต้องเห็น ReactiveOpenSearchClient invocation และไม่มี invocation ไป OpenSearchAsyncClient wrapper ของ request นั้น (และกลับกัน) ทุก path ต่อ same real OpenSearch ผ่าน shared transport เป็น design ที่ตั้งใจ

IT app random port 50549 และ OpenSearch random port 50532 พิสูจน์ว่า tests ไม่พึ่ง Compose ที่ localhost:9200 Parameterized DTO checks ใช้ expected cluster learning-ch01-it/node learning-ch01-it-node ไม่ hardcode dynamic UUID

## Observed smoke evidence

Healthy output ของ info เหมือนกันทั้งสาม prefixes:

```json
{"clusterName":"learning-ch01","clusterUuid":"CdFEwMunSZWZtOA0zZr6xA","nodeName":"learning-ch01-node","distribution":"opensearch","version":"3.9.0"}
```

Health เหมือนกัน:

```json
{"clusterName":"learning-ch01","status":"green","numberOfNodes":1,"numberOfDataNodes":1,"timedOut":false}
```

หลัง stop OpenSearch แต่ app ยังรัน ทั้ง 6 routes ตอบ 503 พร้อม body รูปแบบนี้ (instance ตรง route ที่เรียก):

```json
{"detail":"The OpenSearch dependency could not serve this request.","instance":"/api/spring-data/cluster/info","status":503,"title":"OpenSearch unavailable"}
```

Smoke assert status/title/detail เท่ากัน โดยตรวจ instance แยกตาม route ไม่เปรียบเทียบ instance ข้าม routes Logs ของ session นี้อยู่ /private/tmp/ch01-dual-healthy.log และ /private/tmp/ch01-dual-unavailable.log

## Compatibility findings และ limitations

- ใช้ Spring Data core library และ explicit ReactiveOpenSearchClient(OpenSearchTransport) ไม่ใช้ default reactive configuration/starter จึงเลือก Apache HttpClient 5 transport เดียวกับ direct client ได้จริงใน scope นี้
- Exclude legacy opensearch-rest-high-level-client; IT ตรวจว่า RestHighLevelClient และ Elasticsearch Java Client ไม่อยู่ใน classpath ยังมี opensearch-rest-client 3.8.0 เป็น transitive library แต่ application configuration ไม่สร้าง RestClient/RestClientTransport
- Jackson 2 runtime dependencies ยังมีใน graph แต่ transport ใช้ Jackson 3 mapper อย่างชัดเจน Metadata decode/DTO HTTP encode ผ่าน ไม่อ้าง document converter round trip
- ไม่มี sync OpenSearch calls, block/join/get/manual subscribe ใน main request flow ไม่มีการใช้ boundedElastic ครอบ async I/O
- Configuration close owner คือ OpenSearchTransport bean; disable destroy callback ของ Spring Data reactive wrapper ไม่สร้าง client ต่อ request
- Cancellation tests พิสูจน์ future cancellation ไม่รับประกัน network abort/server rollback 504 เป็น deterministic unit/HTTP tests ไม่ได้ inject real network timeout
- มี Mockito dynamic-agent warning และ Netty macOS native DNS fallback ขณะ IT ผล tests ผ่าน แต่ไม่พิสูจน์ external DNS หรือ platform อื่น
- ไม่ได้รัน hosted GitHub CI, TLS/auth, repository/template converters, vector/hybrid หรือ load tests

## History และ cleanup

เก็บผล 25 tests เดิมไว้ใน [official-only snapshot](verification/official-only-2026-10-25.md) ไม่รวมเป็นผลใหม่สองแนวทาง ไม่มี build/test failures ใน migration run นี้ ไม่ skip หรืออ่อน assertions หลังเพิ่ม Spring Data

หลัง smoke จบ SIGTERM เฉพาะ owned application PID 11096 เกิด graceful shutdown และ process exit 143 ตาม signal ไม่ใช่ test failure Compose down ลบเฉพาะ container/network ของบท เก็บ os-learning-ch01_opensearch-data ไว้ ตรวจ docker ps ไม่พบ running containers และ lsof ไม่พบ listeners ports 8080/9200 Maven caches/images/Docker Desktop คงไว้ ไม่มี commit/push และไม่เริ่ม Chapter 02
