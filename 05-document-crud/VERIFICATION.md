# Chapter 05 Verification

Overall: **Verified — PASS** ภายใน approved starter-only scope ไม่มี official application path และ two-path parity เป็น N/A ตาม exception ไม่ใช้ผลบทอื่นแทนหลักฐานของบทนี้

## Approved plan และ REVIEW

ผู้ใช้ขอเปลี่ยนแผนเป็น Spring Data OpenSearch starter แนวทางเดียวและอนุมัติด้วยข้อความ “ดำเนินตามแผนนี้” ใน conversation วันที่ 2026-10-26 Scope: standalone Java 21/WebFlux, repository save/find, operations delete, converter-backed reactive-client atomic create-only, duplicate/missing/validation errors และ deterministic refresh visibility ไม่มี partial update/OCC/bulk/pagination

ใช้ implement-chapter → review-chapter → VERIFY ตาม AGENTS.md REVIEW เป็น read-only แยกจากการรัน runtime checks โดยตรวจไฟล์ของบททั้งหมดและ repository documentation/CI ที่เกี่ยวข้อง:

- รอบแรกพบ P2 ที่ application.yml:5 ยังไม่ปิด float-to-integer coercion และ P2 ที่ ProductServiceTest.java:72 ยังขาด fresh actual CRUD invocation assertions หลัง retry
- กลับ IMPLEMENT ตั้ง accept-float-as-int=false เพิ่ม real HTTP decimal rejection test/smoke และ repository/delete fresh-retry assertions
- REVIEW ซ้ำบน final source ไม่พบ actionable findings ค้าง จากนั้นจึงรัน final VERIFY ใหม่ ไม่ใช้ diagnostic build เป็น final gate
- ตรวจเอกสารท้ายรอบพบ record 2 components ยังเป็นหลายบรรทัด จัด declaration ให้ตรง engineering standards โดยไม่เปลี่ยน Java tokens แล้ว read-only REVIEW ซ้ำเฉพาะ diff ก่อนรัน final clean verify และ packaged README/smoke ใหม่ทั้งชุด
- ตรวจ boundaries: starter-created infrastructure ไม่มี manual client/transport/template factory หรือ auto-configuration exclusion; Reactivity, request limit, converter/ID/date และ native lifecycle ตรงตามแผน

## Source และ environment

Timestamp: `2026-10-26T21:20:03+07:00` (Asia/Bangkok)

Branch ขณะทำงาน: `codex/ch04-readme-step-by-step`; HEAD `785e06da0f2f197ab695f0d41dda181ad22ef025` มี untracked Chapter 05 และ dirty docs/CI ตาม manifest ไม่มี commit/push งานนี้ Chapter 03 ApiExceptionHandler formatting edit ที่ค้างมาก่อนอยู่นอก scope และไม่ได้แก้

| Component | Observed/resolved |
|---|---|
| OS / CPU | macOS 27.0.1 build 26A434 / arm64 |
| JDK | Temurin 21.0.12.1+1-LTS |
| Maven / Wrapper | 3.9.11 / 3.3.4 |
| Spring Boot / Spring Data Elasticsearch | 4.1.2 / 6.1.2 |
| Spring Data OpenSearch starter/core | 3.1.4 |
| OpenSearch Java / REST client | 3.10.0 / 3.8.0 |
| HttpClient / HttpCore | 5.6.5 / 5.4.4 |
| JSON | Jackson 3.1.7 application; Jackson 2.21.7 runtime transitive สำหรับ native starter stack |
| JUnit / Mockito / Testcontainers | 6.0.3 / 5.23.0 / 2.0.5 |
| Docker Engine / Compose | 29.8.2 / 5.6.0 |
| Server | opensearchproject/opensearch:3.9.0 |
| Image digest | opensearchproject/opensearch@sha256:adfa61f85025d06b4aeb562e7e74fde7e31c437039c93c3862c17e9acebd6c7c |

ตรวจ exact source JAR ของ starter/core 3.1.4 และ Spring Data Elasticsearch 6.1.2: repository.save เป็น upsert; ไม่มี create-only option บน reactive save; repository doRefresh ใช้ template policy; operations.delete คืน empty เมื่อไม่พบ Starter customization ใช้ BeanPostProcessor ตั้ง NONE บน template เดิมและปรับ connection manager เดิมผ่าน RestClientBuilderCustomizer ไม่สร้าง infrastructure แทน

Resolved dependency tree ไม่มี elasticsearch-rest5-client หรือ opensearch-rest-high-level-client มี Spring Data Elasticsearch abstractions ตามที่ OpenSearch integration ใช้ Starter อาจสร้าง synchronous infrastructure beans ด้วย แต่ไม่มี synchronous application service/repository call

## Gates และ exact commands

คำสั่งต่อไปนี้รันจาก `05-document-crud` เว้นแต่ระบุไว้ ทุกคำสั่ง exit `0` เว้น long-running app/SIGTERM ที่อธิบายด้านล่าง Maven/localhost runtime รันนอก sandbox ตามสิทธิ์ที่ผู้ใช้อนุญาต ไม่แก้ assertions หรือ skip tests เพื่อหลบข้อจำกัด

```bash
/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/bin/java -version
env JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw -version
uname -sm
sw_vers
docker version --format '{{.Server.Version}}'
docker compose version
docker image inspect opensearchproject/opensearch:3.9.0 --format '{{json .RepoDigests}}'
if [ ! -f .env ]; then cp .env.example .env; fi
docker compose config --quiet
docker compose up -d --wait opensearch
docker compose ps
curl -fsS 'http://localhost:9205/_cluster/health?wait_for_status=yellow&timeout=3s'
env JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw -B -ntp clean verify -Dmaven.compiler.showDeprecation=true
env JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw -B -ntp help:effective-pom -Doutput=target/effective-pom.xml dependency:tree -DoutputFile=target/dependency-tree.txt
/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/bin/java -jar target/document-crud.jar
python3 /private/tmp/ch05-readme-walkthrough.py
kill -TERM 77226
docker compose down
```

| Gate | Actual / evidence | Status |
|---|---|---|
| Environment/dependencies | Versions ด้านบน; effective-pom.xml และ dependency-tree.txt ใน target; log /private/tmp/ch05-dependencies.log | PASS |
| Build/tests | Final clean verify: BUILD SUCCESS, 43 unit/HTTP + 10 ITs ไม่มี failures/errors/skips | PASS |
| Infrastructure | Compose os-learning-ch05 healthy, localhost:9205; health green, timed_out=false, nodes=1 | PASS |
| Packaged app | PID 77226 เปิด localhost:8085; log พบ 1 reactive repository, 0 sync repositories; app started | PASS |
| CRUD/native data | ทุก API success/error; mapping/converter/persisted source และ visibility checks จริง | PASS |
| README | รัน Bash examples ของ Step 5–12 ทั้ง 15 blocks โดยตรงจาก README | PASS |
| Smoke | 34 HTTP status/payload assertions พร้อม native persisted/search data checks | PASS |
| Cleanup | owned index removed, app graceful shutdown, Compose container/network removed, volume retained | PASS |
| Document/source checks | README anchors/local links, step sequence, actual tree, Bash/JSON syntax, skill frontmatter, CI matrix และ diff scope | PASS |
| Official path/parity | ไม่มี second path ตาม approved exception | N/A |

## Test reports

Final log: `/private/tmp/ch05-final-formatted-verify.log` สิ้นสุด `2026-10-26T21:18:41+07:00`, total time 20.663s ตรวจ XML reports จริงใน target/surefire-reports และ target/failsafe-reports:

| Suite | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| ConfigurationTest | 18 | 0 | 0 | 0 |
| ProductControllerTest | 3 | 0 | 0 | 0 |
| ProductServiceTest | 14 | 0 | 0 | 0 |
| RequestLimitTest | 2 | 0 | 0 | 0 |
| ApiExceptionHandlerTest | 6 | 0 | 0 | 0 |
| DocumentCrudIT | 10 | 0 | 0 | 0 |
| **Total** | **53** | **0** | **0** | **0** |

IT เปิด GenericContainer OpenSearch 3.9.0 ของตัวเองบน random mapped ports ใช้ unique ch05-it-* index ไม่พึ่ง Compose และ cleanup เฉพาะ resources ที่สร้าง Test contexts แยก Netty resources เพื่อไม่ให้ shutdown/failure test ปิด healthy server ของอีก context

Configuration tests ตรวจ bean origins, native REST transport, converter/template, repository wiring, response timeout, pool limits และ invalid configuration การเริ่ม context กับ unavailable backend ไม่สร้าง index อัตโนมัติ

Unit/reactive tests ตรวจ no invocation ก่อน subscribe, fresh precondition/client/repository/delete call หลัง retry, sync/async errors, cancellation/deadline และคืน in-flight permits เมื่อ success/error/cancel ไม่มี application automatic retry

## Coverage และ API evidence

| Objective | Actual tier | Observed assertion |
|---|---|---|
| Create-only | Reactive client index(OpType.Create) + converter.mapObject | POST 201 + Location; ID ซ้ำ 409 PRODUCT_ALREADY_EXISTS; GET ยังได้ข้อมูลแรก |
| Full replacement/upsert | Reactive repository save | PUT ID ใหม่ 200; PUT เดิมเปลี่ยนทุก business field และเอา mapped legacyNote ที่ไม่ได้ส่งออกจาก _source |
| Read | Reactive repository findById | ID เติมจาก metadata, UTC date/long/integer/boolean round trip; missing 404 |
| Delete | Reactive operations delete | 204 ไม่มี body; GET/DELETE หลังลบเป็น 404 PRODUCT_NOT_FOUND |
| Validation | DTO + HTTP codec | Missing fields/negative/unknown/decimal input และ invalid ID เป็น 400 โดยไม่ persist |
| Index precondition | Reactive index operations exists | ทั้งสี่ methods ได้ 503 INDEX_NOT_READY เมื่อ index ไม่มี และไม่ auto-create |
| Mapping/converter | Starter template/converter | Generated annotations mapping ตรง setup resource รวม _class index=false/doc_values=false; _id ไม่ซ้ำใน _source |
| Refresh | NONE policy + explicit native refresh/search | ปิด periodic refresh: GET เห็นการ create/replace/delete ทันที แต่ search คง snapshot เดิมจน explicit refresh |
| Error/lifecycle | Starter native client/transport | Backend unavailable 503 sanitized; context close ทำให้ client ใช้งานไม่ได้ |

README runner `/private/tmp/ch05-readme-walkthrough.py` อ่านและรัน Bash blocks จริงของ Step 5–12 โดยตัดเฉพาะ cd ที่ซ้ำกับ cwd ไม่มีการเปลี่ยน default ports/index ใช้ ch05-products บน volume ที่เพิ่งสร้าง Preflight runner ต้องพบ index absent ก่อนเริ่มจึงไม่ลบข้อมูลเดิม รัน setup ซ้ำแล้วได้ข้อความ already exists โดยไม่แก้ index และลอง smoke ระหว่างที่มี product เพื่อยืนยัน exit 1 พร้อมข้อความ target index must be absent โดยข้อมูลยังอ่านกลับได้

Log `/private/tmp/ch05-final-readme-walkthrough.log` มี request/response และผล assertions จริง ลงท้าย:

```text
PASS Chapter 05 smoke; owned index removed, app and Compose left running
PASS all 15 README Bash blocks (Steps 5–12), setup rerun and smoke ownership guard
```

Smoke ผ่านการเรียกจาก README block สุดท้าย ตรวจ 34 HTTP requests รวม decimal rejection, native _source, before/after refresh ของ create/replace/delete และลบเฉพาะ owned index

## Failures และ retests ระหว่าง IMPLEMENT

ไม่ใช้ diagnostic failures ด้านล่างเป็น final pass และไม่มีการปิด tests:

- `/private/tmp/ch05-unit-diagnostic.log`: expected status ใน test ที่ปรับจากบท 04 ยังเป็น 404 สำหรับ missing index แก้ให้ตรง approved 503 INDEX_NOT_READY
- `/private/tmp/ch05-diagnostic-verify.log`: พบ native REST duplicate 409 ถูก normalize เป็น 503 และ _class mapping resource ไม่ตรง generated metadata แก้ production error normalization พร้อม regression test และแก้ resource ให้ตรง generated mapping
- `/private/tmp/ch05-diagnostic-verify2.log`: failure/shutdown context ใช้ Netty global resources ทำให้ healthy server ของ test อื่นหยุด แก้ test isolation โดยแยก resource factory ต่อ context ไม่เปลี่ยน application transport
- `/private/tmp/ch05-diagnostic-verify3.log`: diagnostic checks ผ่านก่อน REVIEW
- `/private/tmp/ch05-review-fix-diagnostic.log`: checks หลังแก้ REVIEW findings ผ่าน จากนั้น REVIEW ซ้ำและรัน `/private/tmp/ch05-final-formatted-verify.log` ใหม่บน final source

## Cleanup และ limitations

Smoke ลบ ch05-products แล้ว ส่ง TERM เฉพาะ PID 77226 ที่สร้างเอง process exit 143 ตาม SIGTERM โดย log `/private/tmp/ch05-final-app.log` ระบุ `Graceful shutdown complete` เวลา 2026-10-26T21:19:48.167+07:00+07:00 Compose down exit 0 ลบเฉพาะ os-learning-ch05 container/network ไม่ใส่ -v จึงยังเก็บ os-learning-ch05_opensearch-data volume และ ignored .env ของบท ไม่มี app/container ของบทนี้เหลือรันจากงานนี้

Native REST builder/transport deprecated ใน exact release คง compiler warnings ไว้ นอกจากนี้ Mockito แสดง dynamic-agent warning และ test compilation มี unchecked generic mock warnings ไม่ suppress และไม่อ้าง warning-free build

NOT VERIFIED: Linux/amd64/Windows runtime, remote cluster, custom ports, restart/resume ด้วยข้อมูลเดิม, optional exercises, load/performance และ GitHub-hosted CI ของ source นี้ ยังไม่ commit/push N/A: auth/TLS, partial update/OCC และ official two-path parity ตาม approved scope

Semaphore จำกัด logical subscriptions ขณะที่ timeout/cancel ไม่รับประกัน network I/O abort หรือ write rollback; mocks ตรวจ cancellation ของ publisher ไม่พิสูจน์ server-side cancellation การ concurrent ลบ/recreate index จากภายนอกระหว่าง preflight กับ CRUD ไม่อยู่ใน contract ของบท ระบบนี้เป็น local learning ไม่ใช่ HA/performance proof

Logs/runner และ target reports เป็น local ephemeral evidence อาจถูกล้างภายหลัง Manifest ด้านล่างระบุ source ที่ตรวจจริง ไม่ผูกกับ commit ที่ยังไม่ได้สร้าง

## Source manifest

SHA-256 ของ sorted lines `hash  relative-path` พร้อม trailing newline; ไม่รวม VERIFICATION.md, target และ .env รายงาน fingerprint ของ 33 files: `8d9a97b481f57d76bb6b93e67818e070733c3912970df0c1cc55acc881652bd1`

```text
35834ef697ca12cb13803836f05ff50f73a40e3497db884408b3297ed8199498  .env.example
b93aeef4611294db24c8e8cd25c9bbcb3d7acf3ad65511051c0f3dd878b6affc  .gitignore
bb0b8f16b2661c3676ed5a6a15f5b11d864460c4e77cfb50c583f54708f944e7  .mvn/wrapper/maven-wrapper.properties
28f292afac9cd01e240e07e34c727c4152160bbf3ec20c83dc80b02aee38b646  README.md
067d3a1ce62b0ce2f41097cedad012e49743e7b78ff1dab495a8c0df64cd3f99  docker-compose.yml
b16801d6736a113560c794b17ef8827fb8e407b9fd71411fb1cc2ec1a5612008  fixtures/product-replacement.json
2f2160b085c649895ab808b431a44978d95d6f62d2d02bc6a2fdf5f0b0e5e578  fixtures/product.json
cae96cef89ebea3531221f4ae17c23cf8edf67d00eae8306d4186ae1bbed4d02  mvnw
46eedb8419bd14fe70d5bb2916d7b6f51806e51b39d5b76a42610384ca929c1c  mvnw.cmd
48067ebe9bbc09a6260353ee6fb62d3d9db4e2b9bf361caf9287144ace2bccc7  pom.xml
b9037f47827054014e079c236c2189c88d025c1795e4d253a27af2a209a2562c  requests/examples.http
f6ba78a3eebfdf9ff65e01552cf822d498e027e5604da8c24fa626a3584da61a  scripts/initialize-indices.sh
bf777544abcafde2a73e556d4430723d8ea233fffc738e339f9258313284ab59  scripts/smoke-test.sh
16f85bab169e32cd76df51a9a0f7570c77b8216151165ea95572391c8034c858  src/main/java/com/example/learning/opensearch/documentcrud/DocumentCrudApplication.java
1670fc88603bee9a3c7cd7df4ac7ec63c97f7fc9e6132400e5a265b470811916  src/main/java/com/example/learning/opensearch/documentcrud/config/LearningProperties.java
67d1461a308c1d0d996003a65d3bb9d1bb6e158ad82bb0abe2196033d6b9851c  src/main/java/com/example/learning/opensearch/documentcrud/config/OpenSearchConfiguration.java
3ea50f0652052aee52d80f4c73b3d8c9d80819f66c43a1b7d5a24b2acf8db2ed  src/main/java/com/example/learning/opensearch/documentcrud/controller/ProductController.java
48ea058867160845b4609f96e4f05fa0cee7e97098915648cfacd6feb52aabb9  src/main/java/com/example/learning/opensearch/documentcrud/domain/ProductDocument.java
0d5ffabaceb97dce8c6d15a9bbfe8ee6f9f45b91bc01d16e40a691214575bc90  src/main/java/com/example/learning/opensearch/documentcrud/dto/ProductRequest.java
5aa7e09e89e2a34605efd01a8f3644a21dd501a8ff501b74d6eb5c3e36e88d98  src/main/java/com/example/learning/opensearch/documentcrud/dto/ProductResponse.java
e46b4ca92c520dfc972da03fe4a26c2f43a81f2098bd38fbcdfbe68e277f1e3f  src/main/java/com/example/learning/opensearch/documentcrud/exception/ApiExceptionHandler.java
59d46b55ec6e3b85f929fbddb819377428eba383dbc1f9faa3c7aa9680745a66  src/main/java/com/example/learning/opensearch/documentcrud/exception/ChapterException.java
629976669dc7bbc16232d63abf46ab2800433380e1e5fc573f2f572599d69c4d  src/main/java/com/example/learning/opensearch/documentcrud/repository/ProductRepository.java
8590ec21eafbd0c368f5befa523221b7c8078d8b79a1e2f3d5508df08f728689  src/main/java/com/example/learning/opensearch/documentcrud/service/ProductService.java
397420ac8f70250618d20f996a78de4982e6559a15a3028c576ea602851f7846  src/main/java/com/example/learning/opensearch/documentcrud/service/RequestLimit.java
c5da71110633a0c3f0a84350fd1c887b65d5380ec1e2f23417029e643fca83b0  src/main/resources/application.yml
a052241deab250a4f9fe4737c3df7ffa9315cf8197d8a4eb7f6d23f1255efbff  src/main/resources/opensearch/product-index.json
a16896940caa121049a8d25383470e6395758e61babb3bdcd9b78a5c1f9d2932  src/test/java/com/example/learning/opensearch/documentcrud/config/ConfigurationTest.java
d1b455ec7054d916c07818f7476354897c4004a55e821aaacbea19b84bcfe66a  src/test/java/com/example/learning/opensearch/documentcrud/controller/ProductControllerTest.java
41668659b6755729353dc247cbf0846aa46e1d94ca9f5888568d55e52088cc5d  src/test/java/com/example/learning/opensearch/documentcrud/exception/ApiExceptionHandlerTest.java
0f546c58fab02f4f5226081901a700718fe91b96cd6407328b81830d00037d68  src/test/java/com/example/learning/opensearch/documentcrud/integration/DocumentCrudIT.java
c00d3d3effe3b049f99dad1ffdb66e48738a4596ce6ef8ebd3c55f01c3560ca1  src/test/java/com/example/learning/opensearch/documentcrud/service/ProductServiceTest.java
a504c8c013186ae94d6a4f9af015eb97a7a7be4370e9f4546abb2fdfc86eb58d  src/test/java/com/example/learning/opensearch/documentcrud/service/RequestLimitTest.java
```

Root documentation/CI ที่เปลี่ยนใน scope (ไม่รวม Chapter 03 edit):

```text
9a7423e4db2db2d81122c24c64620540641c35245f6a3cc0a14ac24b01e96517  AGENTS.md
d8cfa6800d849d8f2bf3b64850396124dfa49a31ea4e9475fa3fb24ea94bd98e  README.md
58d321e528e72b83504b83af327d9a390b73d55022e966d867d9e12bdabf53e1  docs/architecture.md
80bb46c0a6ee846e128e15e19ad2137744b8574b99b36daa5ad3cf3998938f7c  docs/compatibility.md
c005f69b8e3c13093164e98e5cdadd17c298695ff4ff5c3e4033af64af9cf442  docs/dual-approach.md
5456ff597c6d323b7f4f7a72020aacb83c5b4178ee95f580e965918701c6bfd6  docs/learning-roadmap.md
8efb259cbcc6a4dd0e403e293d7aa66f53e20a842bca74386c2fb50a48b6d3e6  docs/decisions/0003-test-platform-selection.md
649ef5afb8ab6f83d707a92bb294b8f2ac4e5c6984dfee140e50fff45b413d8f  .github/workflows/chapter-verification.yml
```


## Publication amendment — 2026-10-26

ผู้ใช้สั่งเปิด PR และ push changes ทั้งหมด จึงสร้าง branch `codex/ch05-document-crud` จาก `origin/main` ที่ `e4cb342` ข้อความ branch/dirty/no-push ด้านบนเป็นสถานะขณะ verification เดิม

พบ formatting edit หลัง verification ใน `src/main/java/com/example/learning/opensearch/documentcrud/domain/ProductDocument.java` เพียงไฟล์เดียว: รวม annotation ของ name เป็นบรรทัดเดียวและเพิ่ม trailing blank line ตรวจโดย reconstruct bytes เดิมให้ตรง historical SHA-256 `48ea058867160845b4609f96e4f05fa0cee7e97098915648cfacd6feb52aabb9` แล้วเปรียบเทียบ whitespace-free content กับไฟล์ปัจจุบัน ผลตรงกัน ไม่เปลี่ยน Java tokens หรือ mapping semantics ไม่แก้ historical manifest และไม่อ้างว่ารัน runtime checks ใหม่หลัง formatting นี้

Current ProductDocument.java SHA-256: `24e5828ec1823e228fbe1147cb9954d7b9aa79d9f1b43cbfa7039924772ebfd4` อีก 32 chapter files ตรง historical manifest Current publication fingerprint ของ 33 files (algorithm/exclusions เดิม): `6ac7eced99b08ce302d91e5d1944c6b61fa2e8d18601e48b5cf5293604c38969`

รวม Chapter 03 ApiExceptionHandler formatting edit ที่ค้างอยู่ตามคำสั่ง “ทั้งหมด” ตรวจ whitespace-free content เทียบ HEAD แล้วเหมือนกัน ไม่มี Java tokens เปลี่ยน ไม่ได้รัน Chapter 03 tests ใหม่ งาน runtime verification 53 tests/README/smoke ด้านบนเป็นผลจริงก่อน formatting ที่มี tokens เดียวกัน
