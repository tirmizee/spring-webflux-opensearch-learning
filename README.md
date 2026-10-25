# spring-webflux-opensearch-learning

Repository สำหรับเรียน Java 21, Spring Boot 4.x, Spring WebFlux และ OpenSearch 3.x แบบ hands-on ผ่าน **Spring Data OpenSearch reactive** และ **official OpenSearchAsyncClient** แต่ละ chapter เป็น standalone Maven project มี configuration และ tests ของตัวเอง

## เริ่มเรียน

เริ่มจาก [Chapter 01: Basic connection](01-basic-connection/) อ่าน prerequisites และ setup ใน README ของบทก่อนใช้คำสั่ง:

```bash
cd 01-basic-connection
./mvnw -B -ntp clean verify
```

Chapter 01 ใช้ JDK 21, Docker และ Maven Wrapper; Compose/packaged-app smoke ใช้ Bash, cURL และ Python 3.9+ ตาม README ของบท Unit/HTTP tests และ OpenSearch integration tests รันผ่าน Maven โดยไม่ต้อง build บทอื่น

ลำดับและสถานะของ 20 บทอยู่ใน [learning roadmap](docs/learning-roadmap.md) หลักฐานแต่ละบทอยู่ใน `VERIFICATION.md` โดย [Chapter 01 report](01-basic-connection/VERIFICATION.md) ระบุผลสองแนวทางและขอบเขตที่ตรวจไว้

## เอกสาร

- [Architecture](docs/architecture.md) และ [chapter template](docs/chapter-template.md)
- [Engineering standards](docs/engineering-standards.md) รวม async integration และ Java formatting
- [Testing และ verification standards](docs/testing-standards.md)
- [Dual-approach contract](docs/dual-approach.md) และ [compatibility baseline](docs/compatibility.md)
- [Codex instructions](AGENTS.md) และ [reusable skills](.agents/skills/)

## Workflow

ใช้ `$plan-chapter` เพื่อออกแบบ, `$implement-chapter` เพื่อสร้างหรือแก้บทที่อนุมัติ และ `$review-chapter` เพื่อตรวจบท คำขอ plan ไม่อนุญาต implementation และไม่เริ่มบทถัดไปเอง

ตรวจ working tree ก่อนแก้ เลือก scope และ versions จากหลักฐาน จากนั้นรัน Maven verification ของบทพร้อม Compose/packaged-app checks ที่เกี่ยวข้อง บันทึกผลจริงใน `VERIFICATION.md` แล้วอัปเดต roadmap ตามหลักฐาน ไม่ commit หรือ push หากไม่มีคำสั่ง หากสร้าง branch ใช้ `codex/` prefix เว้นแต่ผู้ใช้กำหนดชื่ออื่น

เมื่อเพิ่มบทใหม่ ให้เพิ่มชื่อใน explicit matrix ของ `.github/workflows/chapter-verification.yml` CI รัน unit/HTTP และ OpenSearch integration tests ผ่าน Maven Wrapper ของแต่ละบท ส่วน smoke และ verification gates อื่นต้องมีหลักฐานแยกตาม testing standards

เอกสารใช้ภาษาไทย คง technical terms และ identifiers เป็น English ตรวจ local links หลังย้ายไฟล์ ไม่เก็บ secrets, build outputs หรือ downloaded models ใน Git

## Reference และสิทธิ์การใช้งาน

แนวคิดเริ่มต้นอ้างอิง [spring-webflux-elasticsearch-learning](https://github.com/tirmizee/spring-webflux-elasticsearch-learning) โครงการนี้ปรับเป็น OpenSearch และสอง integration approaches ยังไม่เลือก repository license ต้องตรวจสิทธิ์ก่อน reuse source code, datasets หรือ embedding models
