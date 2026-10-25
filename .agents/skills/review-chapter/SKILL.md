---
name: review-chapter
description: ตรวจ chapter สองแนวทาง Spring Data OpenSearch reactive และ official async ด้าน wiring, parity, tests และเอกสาร โดยไม่แก้ไฟล์ใน review-only request
---

# Review chapter

ค่าเริ่มต้นเป็น read-only review ไม่แก้ code, report หรือ roadmap หากผู้ใช้ขอแก้ findings ด้วยให้ทำภายใน scope ที่อนุมัติ

อ่าน [AGENTS.md](../../../AGENTS.md), requested chapter/source state, [compatibility](../../../docs/compatibility.md), [chapter template](../../../docs/chapter-template.md) และ [roadmap](../../../docs/learning-roadmap.md) เลือกอ่าน [engineering](../../../docs/engineering-standards.md), [testing และ verification](../../../docs/testing-standards.md) ตาม scope

ตรวจ [dual-approach contract](../../../docs/dual-approach.md): Spring Data path ต้องใช้ OpenSearch reactive implementation จริง บท document สอน repository/operations/template ตาม feature support ไม่ใช่ direct service ที่เปลี่ยนชื่อ เทียบ native/reactive-client/fallback coverage กับ objectives และ route/DTO/error/data parity

ตรวจ accidental Elasticsearch auto-config, eager invocation, fresh retry, sync/async errors, deadlines, cancellation limits, bounds, bean ambiguity, double-close และ converter/ID/date/null semantics ตรวจ exact version APIs โดยเฉพาะ advanced features ไม่สมมติ native support

Tests ต้องเรียกสอง path ต่อ real server พร้อม equivalent isolated fixtures ไม่ถือ Maven success หรือ dependency declaration อย่างเดียวพิสูจน์ wiring ตรวจ Surefire/Failsafe discovery, reports, meaningful assertions และ packaged-app smoke ตาม applicable gates

หากรัน checks ให้แยก actual commands/results จาก source inspection ไม่ reset/delete resources ของผู้ใช้ ไม่รับ historical report เป็น current runtime pass ตรวจ README commands/local links, roadmap status, skill references และ CI matrix สำหรับบทที่เพิ่มใหม่

รายงานผลใน chat โดยใช้โครงสร้าง:

- Scope/source state: revision/dirty state, files และสิ่งที่ไม่ได้ตรวจ
- Findings: เรียง severity พร้อม file:line, concrete trigger, impact และ suggested fix แยกข้อสงสัยจาก defects
- Checks/evidence: คำสั่งและผลจริง, tests ที่ไม่ได้รัน, blockers และ coverage สองแนวทาง
- Residual risks: mock/cancellation/performance/platform limits ที่ยังไม่พิสูจน์

หากไม่พบ actionable findings ให้บอกขอบเขตและข้อจำกัด ไม่สรุป runtime pass จากการอ่าน code ไม่เปลี่ยน roadmap โดยอัตโนมัติ
