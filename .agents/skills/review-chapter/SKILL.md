---
name: review-chapter
description: ตรวจ chapter สองแนวทาง Spring Data OpenSearch reactive และ official async ด้าน wiring, parity, tests และเอกสาร เทียบ approved plan แบบ read-only ก่อน VERIFY
---

# Review chapter

ยึด chapter-specific approved scope ใน AGENTS.md ก่อน default two-approach contract ของ skill นี้ ไม่ทำรายละเอียดข้อยกเว้นประจำบทซ้ำใน skill

REVIEW เป็น read-only stage ตาม [chapter workflow](../../../AGENTS.md#chapter-workflow) ไม่แก้ source, report หรือ roadmap ระหว่าง review แม้ผู้ใช้ขอแก้ findings ด้วย ต้องรายงานผลก่อนแล้วจึงกลับ IMPLEMENT ภายใน scope ที่อนุมัติ

อ่าน [AGENTS.md](../../../AGENTS.md), approved plan/acceptance criteria, requested chapter/source state, [compatibility](../../../docs/compatibility.md), [chapter template](../../../docs/chapter-template.md) และ [roadmap](../../../docs/learning-roadmap.md) เลือกอ่าน [engineering](../../../docs/engineering-standards.md), [testing และ verification](../../../docs/testing-standards.md) ตาม scope

เทียบ implementation กับ approved objectives, scope, endpoints, architecture, dependencies และ test cases ตรวจ correctness, error handling, security, maintainability, missing requirements และ unnecessary complexity พร้อม exact file locations

ตรวจ [dual-approach contract](../../../docs/dual-approach.md): Spring Data path ต้องใช้ OpenSearch reactive implementation จริง บท document สอน repository/operations/template ตาม feature support ไม่ใช่ direct service ที่เปลี่ยนชื่อ เทียบ native/reactive-client/fallback coverage กับ objectives และ route/DTO/error/data parity

ตรวจ accidental Elasticsearch auto-config, eager invocation, fresh retry, sync/async errors, deadlines, cancellation limits, bounds, bean ambiguity, double-close และ converter/ID/date/null semantics ตรวจ exact version APIs โดยเฉพาะ advanced features ไม่สมมติ native support

Tests ต้องเรียกสอง path ต่อ real server พร้อม equivalent isolated fixtures ไม่ถือ Maven success หรือ dependency declaration อย่างเดียวพิสูจน์ wiring ตรวจ Surefire/Failsafe discovery, reports, meaningful assertions และ packaged-app smoke ตาม applicable gates

ตรวจ test source และ existing reports เพื่อประเมิน coverage แยกสิ่งที่อ่านจากสิ่งที่รันจริง การรัน Maven/real OpenSearch/Compose/API checks เป็น VERIFY stage แยกต่างหาก ไม่รับ historical report เป็น current runtime pass ตรวจ README commands/local links, roadmap status, skill references และ CI matrix สำหรับบทที่เพิ่มใหม่

รายงานผลใน chat โดยใช้โครงสร้าง:

- Scope/source state: revision/dirty state, files และสิ่งที่ไม่ได้ตรวจ
- Findings: เรียง severity พร้อม file:line, concrete trigger, impact และ suggested fix แยกข้อสงสัยจาก defects
- Checks/evidence: คำสั่งและผลจริง, tests ที่ไม่ได้รัน, blockers และ coverage สองแนวทาง
- Residual risks: mock/cancellation/performance/platform limits ที่ยังไม่พิสูจน์

หากไม่พบ actionable findings ให้บอกขอบเขตและข้อจำกัด ไม่สรุป runtime pass จากการอ่าน code ไม่เปลี่ยน roadmap โดยอัตโนมัติ

เมื่อพบปัญหาให้รายงานก่อนส่งกลับ IMPLEMENT แก้แล้ว REVIEW ซ้ำ เมื่อไม่มี unresolved issues ให้รายงาน review outcome ก่อนส่งต่อ VERIFY ไม่ถือ review pass เป็น runtime PASS หากเป็น final REVIEW ของ bug-fix workflow ให้ตรวจว่า verification evidence ตรงกับ source ที่แก้ล่าสุด
