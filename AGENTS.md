# แนวทางสำหรับ Codex

Repository นี้ใช้เรียน Java 21, Spring Boot 4.x, Spring WebFlux และ OpenSearch 3.x ผ่าน Spring Data OpenSearch reactive และ official OpenSearch Java Client สองแนวทาง ทุก chapter เป็น standalone Maven project

## Chapter-specific approved exception

Chapter 03 `03-api-key-authentication` ใช้ **spring-data-opensearch-starter แนวทางเดียว** ตามคำอนุมัติผู้ใช้ เป็น standalone Maven project ที่ directory ของบท ไม่เพิ่ม core-only หรือ official async application path และไม่บังคับ two-path parity สำหรับบทนี้ ใช้ starter auto-configuration + RestClientBuilderCustomizer สำหรับ API Key ไม่สร้าง client/transport เองและไม่ exclude auto-configurations ใช้ JVM default trust และ configuration ชุดเดียวใต้ `opensearch` ยอมรับ default deprecated REST transport ของ exact release พร้อมเปิดเผย warnings และตรวจ shutdown ตาม native lifecycle ดู [contract exception](docs/dual-approach.md#chapter-03-approved-starter-only-scope)

Chapter 04 `04-index-mappings-and-settings` ใช้ **spring-data-opensearch-starter แนวทางเดียว** ตาม approved plan โดยไม่มี authentication/TLS ใช้ local HTTP และ starter auto-configuration + response-timeout customizer ไม่สร้าง client/transport เอง ไม่มี official application path หรือ two-path parity ตรวจ mapping annotations/converter, reactive operations และ dynamic settings ผ่าน reactive-client tier ตาม [contract exception](docs/dual-approach.md#chapter-04-approved-starter-only-scope)

## Scope และการทำงาน

- อธิบายและเขียนเอกสารการเรียนรู้เป็นภาษาไทย คง technical terms, code และ identifiers เป็น English
- อ่าน [README](README.md), [compatibility](docs/compatibility.md) และ standards ที่เกี่ยวข้องก่อนเปลี่ยนงาน ตรวจ `git status --short --branch` และรักษา uncommitted work
- ทำเฉพาะ scope ที่ผู้ใช้อนุมัติ คำขอ plan ไม่อนุญาต implementation; ไม่เริ่ม chapter ถัดไปเอง
- แต่ละบทต้องรันได้โดยไม่ build หรือ seed บทอื่น ไม่มี root `pom.xml`, shared runtime module หรือ cross-chapter source dependency
- ไม่แก้ completed chapters นอก scope ไม่สร้าง empty chapter directories; links ใน roadmap ชี้เฉพาะบทที่มีจริง
- ไม่ commit, push, publish หรือส่งข้อความภายนอกหากไม่มีคำสั่ง
- ไม่บังคับให้บทก่อนหน้า Verified ก่อนเริ่มบทที่ได้รับอนุมัติ ความรู้ prerequisite ต่างจาก runtime dependency

## Chapter workflow

บทใหม่ต้องผ่าน **PLAN → IMPLEMENT → REVIEW → VERIFY** ตามลำดับ:

1. **PLAN** ใช้ `plan-chapter` อ่าน roadmap/บทที่เกี่ยวข้อง เสนอแผนภาษาไทยพร้อม acceptance criteria ไม่สร้างหรือแก้ implementation files แล้วรอผู้ใช้อนุมัติแผนนั้นอย่างชัดเจน
2. **IMPLEMENT** ใช้ `implement-chapter` ทำตามแผนที่อนุมัติเฉพาะบทนั้น พร้อม source, configuration, tests และเอกสาร การสั่ง implement บทใหม่ที่ยังไม่มีแผนอนุมัติให้เริ่ม PLAN ก่อน
3. **REVIEW** ใช้ `review-chapter` เทียบ implementation กับ approved plan ตรวจแบบ read-only รายงาน findings ตาม severity พร้อม exact file locations ไม่แก้ source หรือรายงานระหว่าง review
4. **VERIFY** เป็นกิจกรรมแยกจาก REVIEW รัน checks จริงตาม [testing standards](docs/testing-standards.md#verification) และบันทึกหลักฐานใน `VERIFICATION.md` ไม่ต้องสร้าง skill เพิ่ม

หาก REVIEW พบปัญหา ต้องรายงานก่อนและกลับไป IMPLEMENT แก้ใน scope เดิม จากนั้น REVIEW ซ้ำก่อน VERIFY หาก VERIFY พบปัญหาให้แก้แล้ว REVIEW/VERIFY ซ้ำตามผลกระทบ ไม่ถือการอ่าน code เป็น runtime verification

Approval gate อยู่ระหว่าง PLAN กับ IMPLEMENT หลังอนุมัติแล้วดำเนิน stages ที่เหลือต่อได้ใน scope เดิม ไม่ขออนุมัติซ้ำสำหรับ fixes ที่อยู่ในแผน หากต้องขยาย scope หรือเปลี่ยน decision ของแผน ให้เสนอส่วนที่เปลี่ยนและรออนุมัติก่อนทำส่วนนั้น

Bug fixes ใช้ **IMPLEMENT → VERIFY → REVIEW** ได้ หาก final review พบปัญหาให้แก้และตรวจซ้ำจนผลสอดคล้องกับ source ล่าสุด Small documentation fixes ใช้ **EDIT → CHECK** ได้ ไม่เปลี่ยน chapter verification status จากการแก้เอกสาร workflow อย่างเดียว ทุกเส้นทางไม่ commit/push โดยอัตโนมัติ

## Engineering invariants

- อ่าน [engineering standards](docs/engineering-standards.md) ซึ่งรวม reactive OpenSearch integration
- ทุกบทต้องออกแบบ Spring Data OpenSearch reactive และ official `OpenSearchAsyncClient` สอง path ตาม [dual-approach contract](docs/dual-approach.md) ใช้ equivalent fixtures/DTO semantics และแยก `/api/spring-data` กับ `/api/java-client` target prefixes
- Official path ใช้ Apache HttpClient 5 async transport; Spring Data ใช้ reactive implementation จริง ตรวจ transport/configuration จาก exact release ไม่บังคับ synchronous repositories
- Defer invocation ของทั้งสอง path เมื่อ method เริ่ม I/O ทันที แม้คืน Mono; สร้าง operation ใหม่ต่อ subscription/retry ตรวจ ownership/qualifiers เพื่อไม่ double-close
- ห้าม `.block()`, `.join()`, `Future.get()`, manual `.subscribe()` หรือ sync OpenSearch calls ใน controller/service pipeline
- ไม่ครอบ async I/O ด้วย `boundedElastic` โดยไม่มีเหตุผล; isolate blocking ancillary work เฉพาะจุดพร้อมอธิบาย
- จำกัด requests in flight, page/bulk sizes และ buffers; future cancellation ไม่รับประกัน server-side cancellation
- ใช้ constructor injection, immutable DTOs และ thin controllers; ไม่สร้าง abstraction โดยไม่มีหน้าที่จริง
- Pin exact versions ก่อน implement ตรวจ resolved APIs และ dependency tree ไม่เดา compatibility; ผู้ใช้อนุมัติ Boot-managed JUnit 6 ใน Chapter 01 แล้ว บทใหม่ต้องตรวจ baseline และ scope ของคำอนุมัติ ดู [test platform decision](docs/decisions/0003-test-platform-selection.md)

## Tests และหลักฐาน

- ใช้ [testing และ verification standards](docs/testing-standards.md) และ [chapter template](docs/chapter-template.md)
- `./mvnw clean verify` ต้องค้นพบและรันทั้ง unit tests กับ integration tests; Docker ไม่พร้อมไม่ใช่เหตุผลให้ skip แล้วประกาศผ่าน
- ห้ามแต่ง logs, test counts หรือ runtime results ไม่ disable tests หรือลด assertions เพื่อให้ผ่าน
- `Verified` ต้องมีผลจริงของทั้งสอง path พร้อม parity/feature coverage ใน `VERIFICATION.md` และ source state; รายงาน FAIL/BLOCKED/NOT VERIFIED ตามจริง
- สรุปสิ่งที่เปลี่ยน checks ที่รันจริง และข้อจำกัด แล้วหยุดที่ scope นั้น

## Skills

- วางแผนบท: [plan-chapter](.agents/skills/plan-chapter/SKILL.md)
- สร้างหรือแก้บทที่อนุมัติ: [implement-chapter](.agents/skills/implement-chapter/SKILL.md)
- ตรวจบท: [review-chapter](.agents/skills/review-chapter/SKILL.md)

เมื่อขยาย scope จากหนึ่งเป็นสอง path ให้รักษาผลเดิมเป็น historical evidence และลด overall status เป็น Implemented จนกว่าจะผ่าน requirement ใหม่ ไม่แต่ง tests ของ path ที่ยังไม่มี

Roadmap และ chapter status อยู่ใน [learning roadmap](docs/learning-roadmap.md) รัน Maven Wrapper จาก directory ของบทโดยตรง เมื่อเพิ่มบทให้ปรับ explicit CI matrix ตรวจ links/skill frontmatter และ diff ตาม scope ไม่มี automatic foundation checker
