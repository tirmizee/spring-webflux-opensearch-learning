# Chapter 01 Verification — archived official-only snapshot

รายงานนี้เป็น snapshot ก่อน migration ข้อความ current/status ภายในสะท้อนเวลานั้น ผลปัจจุบันดู [VERIFICATION.md](../VERIFICATION.md) ไม่ใช้ counts/source fingerprint ของไฟล์นี้อ้างว่าโค้ดสองแนวทางผ่านแล้ว

## Current requirement scope — 2026-10-25

Overall lifecycle: **Implemented / two-approach verification NOT VERIFIED**

ผู้ใช้เปลี่ยน requirement ให้มี Spring Data OpenSearch reactive และ official async client สองแนวทาง ขณะนี้ยังมีเฉพาะ official path ผล 25 tests ด้านล่างเป็น historical evidence ของ scope/source เดิม ไม่ใช่ผลทดสอบของ Spring Data หรือ parity

ครั้งนี้แก้เฉพาะ requirements/docs/skills ไม่เปลี่ยน runnable files/POM/tests/routes จึงไม่ได้รัน Maven/Docker ใหม่ ต้องเพิ่ม Spring Data implementation, dependency resolution, route comparison, real IT และ smoke ก่อนกลับเป็น Verified ดู [ADR 0004](../../docs/decisions/0004-two-learning-approaches.md)

## Historical official-only verification

## Source และ environment

- Timestamp: 2026-10-25T09:04:32+07:00 (Asia/Bangkok)
- Branch: main; unborn HEAD (`No commits yet on main`) ทุกไฟล์เป็น untracked working tree ไม่มี commit/push
- Scope: Chapter 01 runnable files/tests/config ของ final passing build และ foundation instructions/roadmap ที่ปรับตามผลจริง ไม่มีบทถัดไป
- Source fingerprint SHA-256: b6b397dc35a6375cee0545741720320753fa635376cd08276b9ff1a606356d62
- Fingerprint เรียง relative paths ของไฟล์ในบทที่ Git ไม่ ignore (git ls-files --cached --others --exclude-standard) ยกเว้น README.md และ VERIFICATION.md แล้ว hash `path + NUL + bytes + NUL` เพื่อไม่อ้าง commit ที่ยังไม่มี
- Host: macOS 27.0.1 arm64; Temurin Java 21.0.12.1+1; Maven Wrapper distribution 3.9.11 / wrapper 3.3.4
- Spring Boot 4.1.2 / Framework 7.0.10 / Reactor Core 3.8.8
- OpenSearch Java Client 3.10.0 / Apache HttpClient 5.6.5 / HttpCore 5.4.4 / Jackson 3.1.7
- JUnit Jupiter 6.0.3 (ผู้ใช้อนุมัติแทน 5), Mockito 5.23.0, Testcontainers 2.0.5
- Docker Engine 29.8.2, Desktop 4.95.0, Compose 5.6.0; Docker VM linux/arm64, memory 7934 MB
- Image: opensearchproject/opensearch:3.9.0, arm64
- Resolved RepoDigest: `sha256:adfa61f85025d06b4aeb562e7e74fde7e31c437039c93c3862c17e9acebd6c7c`
- Wrapper ZIP SHA-512 เทียบ official Maven Central checksum ผ่าน; pinned SHA-256: `0d7125e8c91097b36edb990ea5934e6c68b4440eef4ea96510a0f6815e7eeadb`

## Verification gates

คำสั่ง Maven รันภายใน directory บท พร้อม `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home` เพราะ mvn ของ host เลือก Java 27 ต่างจาก java ปกติ

| Gate | Commands / evidence | Expected | Actual | Status |
|---|---|---|---|---|
| Environment | java -version, ./mvnw -version, docker version, docker compose version | Java 21, daemon ready | Versions ตามข้างต้น; exit 0 หลังเปิด Docker | PASS |
| Dependencies | dependency:tree, help:effective-pom และ javap จาก actual client JAR | APIs/versions ตรง code | target/dependency-tree.txt และ target/effective-pom.xml; exit 0 | PASS |
| Full build | ./mvnw -B -ntp clean verify | BUILD SUCCESS | final run 09:02:31 +07, 13.058s; exit 0 | PASS |
| Unit/HTTP | Surefire XML reports | Tests discovered, no skips/errors | 22 tests, failures 0, errors 0, skipped 0 | PASS |
| Real OpenSearch IT | Failsafe XML report | Full app + isolated OpenSearch API assertions | 3 tests, failures 0, errors 0, skipped 0; app random port 50367 | PASS |
| Compose | docker compose -p os-learning-ch01 config --quiet; up -d --wait --wait-timeout 180; ps | API-ready localhost cluster | healthy, 127.0.0.1:9200; exit 0 | PASS |
| Direct readiness | curl --fail-with-body 'http://127.0.0.1:9200/_cluster/health?wait_for_status=yellow&timeout=3s' | Valid health, no timeout | green, nodes=1, data nodes=1, timed_out=false; exit 0 | PASS |
| Final packaged app | Java 21 java -jar target/basic-connection.jar | Listening/readiness | PID 9402, localhost:8080, started in 0.654s | PASS |
| Healthy API | bash scripts/smoke-test.sh healthy | HTTP 200 + metadata/health fields | ทั้งสอง endpoints ผ่าน; exit 0 | PASS |
| Failure API | docker compose -p os-learning-ch01 stop opensearch; bash scripts/smoke-test.sh unavailable | HTTP 503 + sanitized fields หลังเคยเชื่อมต่อสำเร็จ | ทั้งสอง endpoints ผ่าน; exit 0 | PASS |
| README | Default setup/readiness, Wrapper build, app run, examples via smoke, stop/down | คำสั่งและ contracts ตรงจริง | Default path ผ่าน; optional port exercise ไม่ได้รัน | PASS |
| Cleanup | SIGTERM เฉพาะ PID 9402; docker compose -p os-learning-ch01 down | Stop owned app/container/network; keep volume | graceful shutdown complete; Compose resources removed | PASS |
| Foundation regression | bash scripts/check-foundation.sh | Files/links/roadmap/tooling ผ่าน | พบ 1 chapter; tooling 4 tests ผ่าน | PASS |

## Exact build และ dependency commands

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
./mvnw -B -ntp clean verify
./mvnw -B -ntp org.apache.maven.plugins:maven-dependency-plugin:3.10.0:tree -DoutputFile=target/dependency-tree.txt org.apache.maven.plugins:maven-help-plugin:3.5.2:effective-pom -Doutput=target/effective-pom.xml
```

JAVA_HOME ข้างต้นคือเครื่องที่ทดสอบ ไม่ใช่ path ที่ผู้เรียนทุกเครื่องต้องใช้ Reports อยู่ใน target/surefire-reports และ target/failsafe-reports ซึ่งเป็น ephemeral build outputs ไม่ได้ commit เก็บ log ของ final build ไว้ที่ /private/tmp/ch01-verify-final.log ใน session นี้เท่านั้น

## Test counts และสิ่งที่พิสูจน์

| Suite | Tests | ผล |
|---|---|---|
| OpenSearchPropertiesTest | 8 | Invalid endpoint/credentials/path/query/port และ timeout bounds |
| ClusterConnectionServiceTest | 7 | Laziness, DTO mapping, per-subscription invocation, sync/async errors, fresh retry, cancellation และ virtual deadline |
| ClusterConnectionControllerTest | 7 | Info/health contracts, yellow health, failure ของทั้งสอง routes, upstream security error translation และ timeouts |
| OpenSearchConnectionIT | 3 | Actual cluster/node/version/UUID, health และ singleton transport/dynamic endpoint |

Unit/HTTP 22 + IT 3 = 25 tests ไม่มี skips ใช้ HTTP จริงจาก IT ไปยัง full application และ real OpenSearch ไม่ใช่ mocked search server บทนี้ยังไม่มี indices/dataset ที่สร้างเอง จึงไม่มี CRUD/mapping/vector proof

## Observed API results

Smoke script ใช้ cURL capture HTTP status แล้ว Python assert body จริงจาก final packaged app:

```json
{"clusterName":"learning-ch01","clusterUuid":"CdFEwMunSZWZtOA0zZr6xA","nodeName":"learning-ch01-node","distribution":"opensearch","version":"3.9.0"}
```

```json
{"clusterName":"learning-ch01","status":"green","numberOfNodes":1,"numberOfDataNodes":1,"timedOut":false}
```

UUID/status เป็นค่าจาก run นี้ ไม่ hardcode UUID ใน tests ทั้งสอง routes หลังหยุด dependency ตอบ HTTP 503 ด้วย fields ต่อไปนี้ (instance แตกต่างตาม route):

```json
{"detail":"The OpenSearch dependency could not serve this request.","instance":"/api/cluster/info","status":503,"title":"OpenSearch unavailable"}
```

504 ตรวจแบบ deterministic ใน service virtual-time และ HTTP tests ไม่ได้ทำ real-server timeout injection Cancellation พิสูจน์ future ถูก cancel ไม่ใช่ server abort

## Failures และ retests

1. Docker daemon ไม่ทำงานตอนเริ่ม เปิด Desktop ด้วย permission แล้วเชื่อมต่อได้ ใช้ escalation สำหรับ network/cache/Docker socket ตาม sandbox
2. Retry test แรกมี nested Mockito stubbing ทำให้ UnfinishedStubbingException แก้โดยสร้าง response ก่อนเริ่ม stub ไม่มีการลด assertions
3. Error test เคย mock Throwable ทำให้ Reactor error tracking ใช้ stack trace ไม่ได้ แก้เป็น OpenSearchException จริง status 401 และพิสูจน์ application ตอบ 503
4. Full IT ครั้งแรกไม่มี WebTestClient bean แก้เป็น explicit bindToServer ไปยัง random application port แล้วรัน full clean verify ผ่าน
5. Image pull ครั้งแรกช้า layer หลัก 827 MB แต่ดาวน์โหลดสำเร็จ ไม่ skip tests; final cached run ผ่านใน 13.058s

หลัง final code fix รันทุก tests ใหม่ และ restart final JAR เพื่อทำ healthy/failure smoke ทั้งสอง routes อีกครั้ง

## Cleanup และข้อจำกัด

Final application PID 9402 หยุดด้วย SIGTERM (process exit 143 ตาม signal ไม่ใช่ test failure) มี graceful shutdown complete ก่อนหน้านั้นหยุด initial test app PID 8336 เช่นกัน Testcontainers temporary containers/Ryuk จัดการ lifecycle ของตัวเอง ตรวจ `docker ps` หลัง cleanup ไม่พบ running containers และ lsof ไม่พบ listeners ที่ ports 8080/9200

Compose data volume `os-learning-ch01_opensearch-data` คงไว้ตาม README Docker images, Maven caches และ Docker Desktop คงไว้ ไม่ลบ resources ของงานอื่น

มี Mockito dynamic-agent warning บน Java 21 และ Netty macOS native DNS resolver fallback ใน IT ผล tests ผ่าน แต่ไม่ได้พิสูจน์ external DNS behavior ทดสอบเฉพาะ localhost/macOS arm64 และ Docker linux/arm64 ไม่ได้รัน hosted GitHub CI, TLS/auth, production load หรือ platform อื่น ไม่มี claims ว่าทุก OpenSearch 3.x feature รองรับแล้ว

## Overall status

Historical Verified สำหรับ official-only scope ของ Chapter 01 ตาม mandatory gates เดิมและ source fingerprint ที่ระบุ; current two-approach lifecycle เป็น Implemented ไม่ได้เริ่ม Chapter 02 และไม่มี commit/push
