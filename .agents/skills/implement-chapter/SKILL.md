---
name: implement-chapter
description: สร้างหรือแก้ chapter ที่อนุมัติ สองแนวทาง Spring Data OpenSearch reactive และ official async พร้อม tests, parity, README และ verification evidence
---

# Implement chapter

ใช้เมื่อผู้ใช้สั่ง implement หรือแก้บทชัดเจน ทำภายใน scope ที่อนุมัติ คำขอ plan ไม่อนุญาต implementation

อ่าน [AGENTS.md](../../../AGENTS.md), [roadmap](../../../docs/learning-roadmap.md), existing chapter/plan และ git state รักษางานเดิม ตรวจ [compatibility](../../../docs/compatibility.md) และยุติ dependent decisions ก่อนสร้างโค้ดที่พึ่ง decision นั้น ไม่สร้าง probe project เพิ่มเอง

ใช้ [chapter template](../../../docs/chapter-template.md), [engineering](../../../docs/engineering-standards.md), [testing และ verification](../../../docs/testing-standards.md) และ [dual-approach contract](../../../docs/dual-approach.md) ทุกบทมี POM/Wrapper/Compose/fixtures/tests ของตัวเอง ไม่มี cross-chapter runtime dependency

Implement Spring Data reactive และ official OpenSearchAsyncClient ตาม exact release APIs แยก SpringData*/JavaClient* services/controllers และ `/api/spring-data` กับ `/api/java-client` ใช้ shared public contract และ equivalent fixtures บท document ใช้ repository/operations/template ตาม support จริง ไม่เรียก direct service เดิมแล้วนับเป็น Spring Data native coverage

ตรวจ auto-configuration, bean qualifiers, mapper/converter และ transport close owner ใช้ lazy invocation ของทั้งสอง path เมื่อ method eager แม้คืน Mono สร้าง operation ใหม่เมื่อ retry จำกัด concurrency/buffers และ deadlines ห้าม blocking/manual subscribe ใน request flow Seed/setup ต้องมี explicit completion/failure

เพิ่ม unit/reactive/HTTP tests และ real-server parity tests ของทั้งสอง path ตรวจ lazy/resubscription/errors/cancellation และ resource isolation ไม่ disable ITs หรือลด assertions เพื่อผ่าน หาก feature gap ทำ mandatory objective ไม่ได้ให้รายงาน decision ที่ต้องยุติ

รัน `./mvnw -B -ntp clean verify` จาก directory ของบท พร้อม applicable Compose/packaged-app/API checks บันทึก actual commands, source state, suite counts, failures/retests และ cleanup ใน `VERIFICATION.md` ตาม template แยก historical evidence จากผลปัจจุบัน หาก environment ขัดขวางให้รายงาน BLOCKED/NOT VERIFIED

เมื่อเพิ่มบทให้เพิ่มชื่อใน explicit matrix ของ `.github/workflows/chapter-verification.yml` ตรวจ README/local links และปรับ roadmap เป็น Implemented หรือ Verified ตามหลักฐาน สรุปผลแล้วหยุด ไม่เริ่มบทถัดไป ไม่ commit/push เอง
