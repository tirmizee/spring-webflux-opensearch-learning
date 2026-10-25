---
name: plan-chapter
description: วางแผน chapter ของ spring-webflux-opensearch-learning สองแนวทาง Spring Data OpenSearch reactive และ official async พร้อม coverage/parity และ verification plan โดยไม่ implement
---

# Plan chapter

ใช้เมื่อขอออกแบบบทใหม่หรือเปลี่ยน scope ค่าเริ่มต้นแสดงแผนใน chat ไม่เขียนไฟล์ เว้นแต่ขอให้บันทึก และไม่เปลี่ยน roadmap status จากคำขอ plan

หา repository root อ่าน [AGENTS.md](../../../AGENTS.md), [roadmap](../../../README.md), [compatibility](../../../docs/compatibility.md) และ [chapter contract](../../../docs/chapter-contract.md) ตรวจ working tree และอ่านบทเดิมถ้ามี ไม่บังคับให้บทก่อนหน้าผ่านก่อน

ใช้ [plan template](../../../docs/templates/chapter-plan.md) กำหนด measurable objectives, core/optional scope, exact versions/evidence status, classes, dataset/mappings/resources, endpoint contracts และ expected assertions ตรวจ official APIs เมื่อเป็นข้อมูล version-dependent ไม่เดา

อ่าน [reactive standards](../../../docs/reactive-opensearch.md) สำหรับ lazy async bridge, sync/async errors, retry/idempotency, cancellation limits และ bounds ใช้ [testing](../../../docs/testing-standards.md) และ [verification](../../../docs/verification-standards.md) ออกแบบ tests, readiness, packaged-app smoke และ cleanup พร้อม expected outcomes ไม่แต่ง actual results

ส่งมอบ files พร้อมหน้าที่, command-level verification, risks/trade-offs, assumptions และ unresolved decisions แล้วหยุดที่แผน ไม่สร้าง chapter หรือเริ่ม implementation เอง

## Two-approach planning

อ่าน [dual-approach contract](../../../docs/dual-approach.md) และ [ADR 0004](../../../docs/decisions/0004-two-learning-approaches.md) ก่อนวางแผน ทุก objective มี Spring Data reactive และ official async coverage row ระบุ repository/operations/template/reactive client API ที่ใช้จริง พร้อม candidate versions/source/API gaps ไม่สมมติ native support โดยเฉพาะ vector/hybrid

วาง target route prefixes/classes สองชุดและ shared DTO/fixture semantics พร้อม parity assertions, mapping/error comparison และ transport ownership ตรวจ method invocation ที่อาจ eager แม้คืน Mono หากต้อง fallback ให้ระบุระดับ ไม่เอา direct service มาเปลี่ยนชื่อเป็น SpringData หากทำ mandatory objective สองแนวทางไม่ได้ ให้รายงาน decision ที่ต้องยุติ
