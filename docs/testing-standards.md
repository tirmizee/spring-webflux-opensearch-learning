# Testing standards

Chapter 01 ใช้ Boot-managed JUnit 6 ตามคำอนุมัติผู้ใช้ ดู [ADR 0003](decisions/0003-test-platform-selection.md) ไม่ override JUnit API/engine/platform แยกกัน Tests ต้องมีตั้งแต่บทแรก

| Layer | Tools | Assertions |
|---|---|---|
| Unit | JUnit, Mockito | Validation, query/request construction, DTO/error mapping |
| Reactive | StepVerifier | Lazy invocation, fresh retry, sync/async errors และ cancellation |
| HTTP | WebTestClient | Status, payload, validation และ exception contract |
| Integration | Testcontainers + OpenSearch จริง | Mapping, CRUD, queries, serialization และ relevant transport behavior |
| Smoke | Compose + packaged app + cURL | Flow ตาม README รวม deterministic field assertions |

Unit/slice ใช้ `*Test` และ Surefire; integration ใช้ `*IT` และ Failsafe bind integration-test/verify `./mvnw clean verify` ต้องรันทั้งสอง suite ตรวจ test counts/reports ไม่ใช่ exit code อย่างเดียว ไม่มี skipITs/skipTests ใน full verification และไม่ auto-disable เมื่อ Docker หาย

ใช้ Mockito stub ที่ขอบเขต dependency จริง ไม่ mock ทุก builder chain เพื่อยืนยัน implementation ทีละบรรทัด HTTP slice ใช้ Boot 4 test module/imports ตาม version จริง หรือ bind controller เมื่อเพียงพอ ต้องมี full application tests ด้วย

## Two-approach coverage

ทุกบทมี tests แยก Spring Data reactive กับ official async dependency จริง ตาม [dual-approach contract](dual-approach.md) HTTP tests/IT/smoke ต้องเรียกทั้งสอง prefixes และตรวจว่าบริการไปคนละ integration path ไม่ stub direct service แล้วถือว่า Spring Data ผ่าน

ใช้ shared behavioral cases หรือ parameterized IT เมื่อช่วยลด duplication พร้อมแสดง approach ใน test names/report ตรวจ DTO/status/result IDs และ error parity; Spring Data mapping converters/annotations ต้องมี round-trip assertions ในบทเอกสาร

Write cases ใช้ isolated equivalent indices หรือ reset deterministic fixtures ก่อนแต่ละ approach เพื่อไม่ให้ create/delete/conflict ของ path หนึ่งเปลี่ยน expectation อีก path Native support, reactive-client access และ explicit fallback ต้องรายงานแยกกัน

## Isolation และ determinism

- ใช้ GenericContainer กับ pinned official OpenSearch image เป็น baseline ไม่สมมติ ElasticsearchContainer รองรับ image นี้
- Container ของ tests ไม่พึ่ง Compose ที่ผู้ใช้เปิดไว้ ใช้ getHost/mapped ports และ dynamic app configuration ไม่ hardcode localhost:9200
- ใช้ unique index names, stable document IDs, explicit refresh และ scoped cleanup
- Readiness polling มี timeout และ useful diagnostics ไม่ fixed sleeps
- ไม่ใช้ exact floating relevance score ถ้าไม่ใช่ controlled mathematical fixture ใช้ IDs/order/bounds ตามสิ่งที่ทดสอบจริง
- Pagination ใช้ deterministic tie-breaker field ที่ mapping รองรับ ไม่สมมติ `_id` sort ได้
- Vector tests มี exact baseline, dimension validation และ approximate assertions ที่มีเหตุผล
- Retry tests ใช้ controllable futures/virtual time ไม่พึ่ง network outage เพื่อพิสูจน์ทุก branch

ใช้ parallel execution เฉพาะเมื่อ resource isolation พร้อม ไม่มี shared mutable fixtures/container reset race

## Evidence limits

Mock tests ไม่พิสูจน์ server query semantics; future cancellation ไม่พิสูจน์ request abort; happy-path 200 ไม่พิสูจน์ OpenSearch ถูกเรียก ตรวจ persisted/searchable data และ server response ที่เฉพาะเจาะจง

[Verification](#verification) กำหนดเกณฑ์ Verified เพิ่มจาก Maven tests แหล่งอ้างอิง: [Testcontainers](https://testcontainers.com/getting-started/)

## Verification

ทำ VERIFY แยกจาก read-only REVIEW ตาม [chapter workflow](../AGENTS.md#chapter-workflow) ของบทใหม่ การรัน tests ระหว่าง IMPLEMENT เพื่อวินิจฉัยทำได้ แต่ไม่แทน REVIEW หรือ final VERIFY บน source ที่ review แล้ว หากพบปัญหาให้รายงานและแก้ตาม workflow ก่อนรับผลเป็น final evidence

[Learning roadmap](learning-roadmap.md) เก็บ chapter lifecycle (`Planned`, `Implemented`, `Verified`) ส่วนรายงานใช้ gate statuses `PASS`, `FAIL`, `BLOCKED`, `NOT VERIFIED`, `N/A` สถานะ N/A ต้องมีเหตุผล ไม่แทน mandatory runtime gate ด้วย source inspection

| Gate | สิ่งที่รันและตรวจ |
|---|---|
| Environment | java -version, ./mvnw -version, docker version, docker compose version และ OS/CPU |
| Dependencies | effective POM, relevant dependency tree, exact image tag/digest และ plugins |
| Build/tests | ./mvnw clean verify; ตรวจ Surefire/Failsafe counts, failures, errors และ skips |
| Infrastructure | docker compose config, Compose up, ps และ API readiness ด้วย protocol/auth ที่ถูกต้อง |
| Application | รัน packaged JAR ตาม README แล้วตรวจ readiness และ process ownership |
| API | ทุก implemented endpoint มี success และ representative meaningful failure; assert HTTP status และ fields |
| OpenSearch data | ตรวจ mappings/data/query behavior จริง ห้ามใช้ mock เป็น integration proof |
| README | ทดลอง setup/run/examples/teardown หรือระบุคำสั่งที่ไม่ได้รัน |
| Cleanup | หยุด resources/processes ที่สร้าง ไม่ลบของผู้ใช้หรือ volumes โดยปริยาย |

สร้าง [VERIFICATION report](chapter-template.md#verificationmd) หลัง attempted verification ใช้ timezone ชัดเจน ระบุ commit/branch หรือ unborn HEAD, dirty/untracked state และขอบเขต source ที่ทดสอบ ไม่อ้าง commit ที่ยังไม่เกิด

เก็บ exact commands, exit codes, test counts และ output excerpts เท่าที่จำเป็น Redact credentials แต่คงหลักฐานที่ตรวจได้ Report locations ต้องระบุถ้าเป็น local ephemeral target ที่ไม่ได้ commit อย่า link ไปไฟล์ไม่มีจริงเพื่อให้ดูเหมือนมีหลักฐาน

Verified ต้องผ่าน REVIEW โดยไม่มี unresolved defects หรือ missing requirements ตาม approved plan และผ่าน mandatory objectives ของ Spring Data reactive และ official async พร้อม parity/feature coverage ตาม [dual-approach contract](dual-approach.md) รวมทั้งทุก applicable mandatory gate บน final relevant source state ถ้าแก้ behavior/config/tests หลังตรวจ ให้ rerun affected checks รวม smoke flow หลัง final runtime fix Historical report ไม่พิสูจน์ services ปัจจุบันกำลังรัน

PASS คือรันแล้วผ่าน expected assertions; FAIL คือรันแล้วไม่ผ่าน; NOT VERIFIED คือยังไม่รันหรือไม่มีหลักฐานเพียงพอ; BLOCKED ระบุ precondition ที่ขัดขวางและไม่ถือว่าตรวจผ่าน หาก Docker/OpenSearch ไม่พร้อมต้องระบุ required ITs เป็น BLOCKED/NOT VERIFIED ไม่ skip แล้วประกาศ Verified หากโค้ดมีแล้วแต่ตรวจไม่ครบ lifecycle คง Implemented และระบุข้อจำกัด อย่าแก้ report เก่าให้กลายเป็นผลใหม่โดยไม่ได้รัน

เมื่อ scope ขยายจากหนึ่งเป็นสองแนวทาง ให้คงผลเดิมเป็น historical evidence พร้อม timestamp/source ไม่ลบหรือแก้ expected/actual เก่าให้ดูเหมือนตรวจสอง path แล้ว Overall lifecycle เป็น Implemented จนกว่าจะผ่านข้อกำหนดใหม่ Scope amendment ต้องอยู่ทั้ง README และ VERIFICATION

## คำสั่งและ CI

รันจาก directory ของบทด้วย JDK 21 และ Docker ที่พร้อมใช้งาน:

```bash
cd 01-basic-connection
./mvnw -B -ntp clean verify
```

CI ใช้ explicit chapter matrix ใน `.github/workflows/chapter-verification.yml` เมื่อเพิ่มบทที่ implement แล้วให้เพิ่มชื่อใน matrix ใช้ Maven Wrapper ของบทโดยตรงและเก็บ Surefire/Failsafe reports ไม่มี Python discovery หรือ root verification wrapper

การตรวจ links, skill frontmatter, roadmap และรายการไฟล์เป็นส่วนของ review ไม่มี automatic foundation checker ส่วน Chapter 01 ยังใช้ Python 3.9+ ภายใน chapter-specific smoke assertions ตาม README ของบท

## Troubleshooting

| อาการ | ตรวจและแก้ตามหลักฐาน |
|---|---|
| Java version ไม่ตรง | เทียบ java -version กับ ./mvnw -version ตั้ง JAVA_HOME ของงานให้เป็น JDK 21 |
| No tests / engine linkage error | ตรวจ test platform, effective POM, Surefire/Failsafe และ JUnit API/engine/platform alignment |
| Docker connection refused | ตรวจ daemon/context/permissions ไม่ disable ITs เพื่อผ่าน |
| Image ไม่รองรับ CPU | ตรวจ manifest ของ exact tag เทียบ arm64/amd64 ไม่อ้างว่ารองรับจาก image ชื่อเดียวกัน |
| Container exit / memory/bootstrap error | อ่าน logs, heap และ Docker memory; ตรวจ kernel requirements ของ release |
| Port collision | ปรับ documented env ports ใช้ Compose project แยก; tests ใช้ random mappings |
| Spring Data beans/client ไปผิด cluster | ตรวจ OpenSearch implementation, auto-config, qualifiers, shared transport และ endpoint ของแต่ละ approach |
| Reactive method ยิง request ก่อน subscribe | ตรวจ method body; defer invocation แทนถือว่า Mono lazy เสมอ |
| TLS handshake failure | ตรวจ protocol, CA trust, hostname และ cert validity ไม่ปิด verification เป็นวิธีถาวร |
| HTTP 401/403 จาก OpenSearch | แยก credential กับ permission ใช้ app identity ที่เหมาะสม |
| Compose env มีค่าแต่ app ไม่มี | Compose .env ไม่ส่งเข้า host JVM โดยอัตโนมัติ อ่าน explicit env export/run instructions |
| Get พบแต่ search ไม่พบ | ตรวจ refresh policy/visibility อย่าเพิ่ม sleeps แบบเดา |
| Pool timeout / high memory | ตรวจ bounded concurrency, payload/page/bulk size และ retry amplification |
| Vector dimension error | ตรวจ model/version/preprocessing ให้ตรง fixtures/mapping/query |

อย่าสั่ง stop/delete containers หรือ volumes ที่ไม่ได้สร้างในงาน Cleanup ใช้ project/resource names ของบทและไม่ใส่ volumes removal โดยปริยาย

OpenSearch server ใช้ JVM ใน image แยกจาก JDK ของ app ตรวจ [Docker installation](https://docs.opensearch.org/latest/install-and-configure/install-opensearch/docker/) สำหรับ kernel/memory/security requirements ของ release ที่เลือก ไม่แก้ host settings โดยอัตโนมัติ
