# แนวทางสำหรับ Codex

Repository นี้ใช้เรียน Java 21, Spring Boot 4.x, Spring WebFlux และ OpenSearch 3.x ผ่าน Spring Data OpenSearch reactive และ official OpenSearch Java Client สองแนวทาง ทุก chapter เป็น standalone Maven project

## Scope และการทำงาน

- อธิบายและเขียนเอกสารการเรียนรู้เป็นภาษาไทย คง technical terms, code และ identifiers เป็น English
- อ่าน [README](README.md), [compatibility](docs/compatibility.md) และ standards ที่เกี่ยวข้องก่อนเปลี่ยนงาน ตรวจ `git status --short --branch` และรักษา uncommitted work
- ทำเฉพาะ scope ที่ผู้ใช้อนุมัติ คำขอ plan ไม่อนุญาต implementation; ไม่เริ่ม chapter ถัดไปเอง
- แต่ละบทต้องรันได้โดยไม่ build หรือ seed บทอื่น ไม่มี root `pom.xml`, shared runtime module หรือ cross-chapter source dependency
- ไม่แก้ completed chapters นอก scope ไม่สร้าง empty chapter directories; links ใน roadmap ชี้เฉพาะบทที่มีจริง
- ไม่ commit, push, publish หรือส่งข้อความภายนอกหากไม่มีคำสั่ง
- ไม่บังคับให้บทก่อนหน้า Verified ก่อนเริ่มบทที่ได้รับอนุมัติ ความรู้ prerequisite ต่างจาก runtime dependency

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
