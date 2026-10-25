---
name: plan-chapter
description: วางแผน chapter สองแนวทาง Spring Data OpenSearch reactive และ official async พร้อม coverage, parity และ verification plan โดยไม่ implement
---

# Plan chapter

ใช้เมื่อออกแบบบทใหม่หรือเปลี่ยน scope แสดงแผนใน chat ไม่เขียนไฟล์เว้นแต่ผู้ใช้ขอให้บันทึก และไม่เปลี่ยน chapter status จากคำขอ plan

อ่าน [AGENTS.md](../../../AGENTS.md), [roadmap](../../../docs/learning-roadmap.md), [compatibility](../../../docs/compatibility.md), [chapter template](../../../docs/chapter-template.md) รวม [architecture](../../../docs/architecture.md) และบทที่เกี่ยวข้อง ตรวจ existing chapter/plan, working tree และ scope ที่ร้องขอ

ใช้ [engineering](../../../docs/engineering-standards.md), [testing](../../../docs/testing-standards.md) และ [dual-approach contract](../../../docs/dual-approach.md) ออกแบบแผนที่มี:

- Measurable objectives, prerequisite concepts, core/optional scope และสิ่งที่ไม่ทำ
- Exact version candidates, official sources และ evidence status ตรวจ version-dependent APIs ไม่เดา support
- รายการไฟล์/classes พร้อมหน้าที่, base package, request flow และ transport lifecycle
- Dataset, IDs, mappings/settings, index/pipeline/alias ownership, setup/reset และ cleanup
- HTTP methods/prefixes, input validation, success/error contract, configuration/env/TLS และ expected assertions
- Lazy invocation, fresh retry, sync/async failures, deadlines, cancellation limits และ concurrency/buffer bounds
- Tests ราย layer และ command-level verification สำหรับ Maven, readiness, packaged app, API assertions และ cleanup ระบุ expected outcomes แยกจาก actual results

ทุก objective ต้องมี coverage row ของ Spring Data reactive และ official async ระบุ actual API tier: repository/operations/template, reactive client, explicit fallback หรือ unresolved โดยเฉพาะ vector/hybrid ไม่ถือ direct fallback เป็น native Spring Data coverage วาง shared DTO/fixture semantics และ parity assertions พร้อม separate route prefixes

ส่งมอบ files/responsibilities, acceptance criteria, risks/trade-offs, edge cases, assumptions และ unresolved decisions หาก mandatory objective ทำสองแนวทางไม่ได้ให้เสนอ decision แก่ผู้ใช้ ไม่ silently ยกเว้น approach นำเสนอแผนภาษาไทย ระบุแผนและ acceptance criteria ให้ผู้ใช้ตรวจได้ แล้วหยุดรอ explicit approval ตาม [chapter workflow](../../../AGENTS.md#chapter-workflow) ก่อนส่งต่อ IMPLEMENT ห้ามสร้างหรือแก้ implementation files การขอ implement โดยยังไม่มีแผนอนุมัติไม่ข้าม gate นี้
