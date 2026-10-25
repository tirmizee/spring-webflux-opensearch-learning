# Chapter NN Review

## Scope และ source state

ระบุบท, revision/dirty state, files ที่อ่าน, standards และสิ่งที่อยู่นอกขอบเขต

## Findings

แต่ละ finding ระบุ severity, file:line, concrete trigger, impact และ suggested fix เรียงตามความรุนแรง แยกปัญหาที่พิสูจน์แล้วจากคำถาม

ถ้าไม่มี actionable findings ให้บอกขอบเขตที่ตรวจ ไม่เท่ากับ runtime ผ่าน

## Checks และ evidence

แยก source inspection, commands ที่รันจริงและผล, tests ที่ไม่ได้รัน และ blockers ตรวจ async lifecycle, query semantics, API contracts, security, test discovery, README และ verification provenance

## Residual risks

ข้อจำกัดของ mocks, cancellation, performance และ version/platform coverage ที่ยังไม่พิสูจน์ ไม่เปลี่ยน roadmap หรือแก้ code โดยอัตโนมัติจาก review-only request

## Coverage สองแนวทาง

| Objective / feature | Spring Data reactive: API/coverage/evidence | Official async: API/coverage/evidence | Parity assertion / gap |
|---|---|---|---|

กรอกแยกทุก objective ระบุ native abstraction, reactive-client access, explicit fallback หรือ unresolved ตามจริง ใช้ shared semantics/fixtures พร้อม separate route prefixes และ mapping/error/lifecycle comparison ห้ามนับ direct fallback เป็น native Spring Data coverage

ตรวจ wiring ว่า Spring Data path เรียก OpenSearch reactive implementation จริง ไม่ได้เป็น direct service ที่เปลี่ยนชื่อ ตรวจ auto-config, double-close, laziness, converter semantics และ version-specific feature gaps
