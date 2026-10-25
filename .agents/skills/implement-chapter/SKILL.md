---
name: implement-chapter
description: สร้างหรือแก้ chapter ที่ได้รับอนุมัติให้มี Spring Data OpenSearch reactive และ official async สองแนวทาง พร้อม tests, parity, README ภาษาไทย และ verification evidence
---

# Implement chapter

ใช้เมื่อผู้ใช้สั่ง implement/แก้บทชัดเจน การอนุมัติ foundation หรือ plan-only ไม่รวม chapter ไม่ถามอนุมัติซ้ำภายใน scope ที่อนุมัติแล้ว

หา root อ่าน [AGENTS.md](../../../AGENTS.md), [roadmap](../../../README.md), existing chapter/plan และ git state รักษางานเดิม อ่าน [compatibility](../../../docs/compatibility.md) และยุติ dependent decisions โดยเฉพาะ JUnit ก่อนสร้างโค้ดที่พึ่ง decision นั้น ไม่สร้าง probe project เพิ่มเอง

สร้างเฉพาะบทที่ขอตาม [chapter contract](../../../docs/chapter-contract.md), [engineering](../../../docs/engineering-standards.md) และ [reactive standards](../../../docs/reactive-opensearch.md) ตรวจ exact dependency APIs ใช้ own POM/Wrapper/Compose/fixtures ไม่มี common runtime module ใช้ lazy OpenSearchAsyncClient, bounded concurrency/buffers, deadlines และ resource lifecycle ไม่มี blocking/manual subscribe ใน request flow Seed/setup ต้องมี explicit completion/failure

เพิ่ม tests ตาม [testing standards](../../../docs/testing-standards.md) ไม่ disable ITs หรือลด assertions เพื่อผ่าน ใช้ [README template](../../../docs/templates/chapter-readme.md) ให้คำสั่งตรง implementation

รัน foundation checks, full Maven verify และ applicable Compose/packaged-app/API checks ตาม [verification standards](../../../docs/verification-standards.md) แก้ issues ใน scope และ rerun affected checks ใช้ [verification template](../../../docs/templates/chapter-verification.md) บันทึก actual commands, source state, counts, failures/retests และ cleanup หาก environment ขัดขวางให้รายงาน BLOCKED/NOT VERIFIED

อัปเดต roadmap เป็น Implemented หรือ Verified ตามหลักฐาน สรุปผลแล้วหยุด ไม่เริ่มบทถัดไป ไม่ commit/push เอง

## Two-approach implementation

อ่าน [dual-approach contract](../../../docs/dual-approach.md) ทุกบทต้องมี Spring Data OpenSearch reactive และ official OpenSearchAsyncClient ใน standalone project เดียว Implement ตาม actual release APIs และ coverage matrix ไม่เพิ่ม synchronous repository หรือ legacy transport เพียงเพราะตัวอย่าง default ใช้เช่นนั้น

แยก SpringData*/JavaClient* services/controllers และ target /api/spring-data กับ /api/java-client ใช้ shared public contract ไม่ปลอม native Spring Data path ด้วยการเรียก direct service เดิม ตรวจ bean qualifiers, auto-configuration, mapper/converter และ transport close owner จะ share หรือแยกต้องมีเหตุผลและหลักฐาน

Defer invocation ที่ eager ทั้งสอง path แม้ library คืน Mono แล้ว ทดสอบ lazy/resubscription/error/cancel แยก path เพิ่ม real-server behavioral parity และ packaged-app smoke สำหรับทุก implemented route Write fixtures ต้องไม่รบกวนกัน

Existing official-only Chapter 01 มี historical verification แต่ current scope ยังไม่ครบ เมื่อ migrate ให้ระบุ legacy-route policy และเก็บหลักฐานเดิม ตรวจ dependency tree/full verify/smoke ใหม่ก่อน Verified หาก feature gap ทำ mandatory objective ไม่ได้ ต้องเปิดเผยและยุติ exception decision ไม่ลด assertions หรือใส่ N/A เอง
