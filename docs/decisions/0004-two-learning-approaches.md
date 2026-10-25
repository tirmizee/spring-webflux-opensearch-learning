# ADR 0004: สองแนวทางในทุก chapter

Status: Accepted — ผู้ใช้ขอแก้ข้อกำหนดทั้ง repository เมื่อ 2026-10-25

## Decision

แต่ละ standalone chapter ออกแบบ Spring Data OpenSearch reactive และ official OpenSearchAsyncClient สอง path ภายใน project เดียว ใช้ objectives/fixtures/DTO semantics ร่วมกันและแยก prefixes/classes เพื่อเปรียบเทียบจริง ตาม [dual-approach contract](../dual-approach.md)

Decision นี้แทน single-path choice เดิม หลัก architecture ปัจจุบันอยู่ใน [architecture](../architecture.md) แต่คง native async I/O เป็น invariant ไม่มี synchronous repository path ใน request flow

## Consequences

ต้องตรวจ dependency/auto-configuration, mapping converters, transport ownership, reactive laziness และ feature parity เพิ่ม การที่ Spring Data ใช้ Spring Data Elasticsearch interfaces ไม่ใช่เหตุผลให้ใช้ Elasticsearch client ต่อ OpenSearch

Advanced feature ที่ abstraction ไม่ครอบคลุมต้องแสดงระดับ fallback ไม่สร้าง fake parity หรือสมมติ native support หากทำ mandatory objective ไม่ครบ ต้องรายงานและยุติ exception decision ก่อนประกาศ Verified

## Migration scope

ครั้งนี้เปลี่ยน requirements/docs/skills และสถานะ coverage ไม่เปลี่ยน Java/POM/routes/tests ของ Chapter 01 Root status กลับเป็น Implemented เพราะสองแนวทางยังไม่ครบ ผล official-only 25 tests คงเป็น historical evidence ไม่รัน Maven/Docker ซ้ำจากการแก้เอกสาร

## Chapter 01 migration completion

Chapter 01 migrate ตาม decision นี้แล้ว ใช้ Spring Data reactive-client tier สำหรับ metadata พร้อม official async path และ backward-compatible aliases ผ่าน 49 tests และ real smoke/parity ดู [current verification](../../01-basic-connection/VERIFICATION.md) Scope paragraph ด้านบนเป็นประวัติของ requirements-only update ก่อน migration
