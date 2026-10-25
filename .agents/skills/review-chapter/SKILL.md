---
name: review-chapter
description: ตรวจ chapter สองแนวทาง Spring Data OpenSearch reactive และ official async ด้าน wiring, feature parity, tests และเอกสาร โดยไม่แก้ code ใน review-only request
---

# Review chapter

ค่าเริ่มต้นเป็น read-only review ไม่แก้ code, report หรือ roadmap หากผู้ใช้ขอแก้ findings ด้วย ให้ใช้ implement workflow ภายใน scope ที่อนุมัติ

หา root อ่าน [AGENTS.md](../../../AGENTS.md), requested chapter/source state, [compatibility](../../../docs/compatibility.md) และ [chapter contract](../../../docs/chapter-contract.md)

เลือกอ่าน standards ตาม scope:

- [Engineering](../../../docs/engineering-standards.md): mappings/query semantics, validation, HTTP errors, secrets และ TLS
- [Reactive](../../../docs/reactive-opensearch.md): async transport จริง, lazy/fresh retry, blocking/subscriptions, deadlines, cancellation limits, bounds และ cleanup
- [Testing](../../../docs/testing-standards.md): meaningful assertions, mock limits, real-server coverage, test discovery และ deterministic fixtures
- [Verification](../../../docs/verification-standards.md): source state, actual commands/counts เทียบ code/README ไม่รับ historical evidence เป็น current pass

หากรัน checks ให้แยกคำสั่งและผลจริงจาก source inspection ไม่ทำ destructive resets หรือใช้ resources ที่ไม่ใช่ของ review หากไม่ได้รันหรือ environment ไม่พร้อมให้รายงานชัดเจน

ใช้ [review template](../../../docs/templates/chapter-review.md) Findings เรียง severity พร้อม file:line, trigger, impact และ suggested fix แยกข้อสงสัยจาก defects ที่มีหลักฐาน ไม่พบ actionable findings ให้บอกขอบเขตที่ตรวจ tests ที่รัน/ยังไม่รัน และ residual risks ไม่สรุป runtime pass จากการอ่าน code

## Two-approach review

อ่าน [dual-approach contract](../../../docs/dual-approach.md) ตรวจว่า Spring Data path ใช้ OpenSearch reactive implementation จริง และ document use case สาธิต repository/operations/template ไม่ใช่ direct service ที่เปลี่ยนชื่อ Check actual native/reactive-client/fallback coverage เทียบ objectives และตรวจ route/DTO/error/data parity

ตรวจ accidental Elasticsearch auto-config, eager client invocation, converter/ID/date/null mapping differences, bean ambiguity และ double-close Tests ต้องเรียกทั้งสอง path ต่อ real server พร้อม isolated equivalent fixtures ไม่ถือว่า full Maven success หรือ dependency declaration อย่างเดียวพิสูจน์สองแนวทาง

ตรวจ historical evidence หลัง scope expansion: Chapter 01 official-only 25 tests ไม่ใช่ dual-path verification และไม่ควรยังเป็น overall Verified ก่อน migrate ข้อมูลที่ยังไม่ตรวจให้รายงานตามจริง ไม่ใช้ limitation ของ abstraction เป็น silent exception
