# Chapter NN Verification

Template นี้ไม่มีผลทดสอบ กรอกหลัง attempted validation และไม่คัดลอก expected เป็น actual

## Source และ environment

- Timestamp พร้อม timezone:
- Branch/revision หรือ unborn HEAD; dirty/untracked state และ source scope:
- Java/Maven/Boot/client/server image tag/digest/test platform versions:
- OS/CPU/Docker/Compose และ preconditions:

## Gates

| Gate | Command / evidence location | Expected | Actual / exit code | Status |
|---|---|---|---|---|
| Environment/dependencies | ยังไม่ได้รัน | Versions/API alignment | ไม่มีหลักฐาน | NOT VERIFIED |
| Build/unit/HTTP tests | ยังไม่ได้รัน | Tests discovered and passing | ไม่มีหลักฐาน | NOT VERIFIED |
| OpenSearch ITs | ยังไม่ได้รัน | Real-server assertions pass | ไม่มีหลักฐาน | NOT VERIFIED |
| Compose readiness | ยังไม่ได้รัน | API ready | ไม่มีหลักฐาน | NOT VERIFIED |
| Packaged application | ยังไม่ได้รัน | Listening and ready | ไม่มีหลักฐาน | NOT VERIFIED |
| cURL/field assertions | ยังไม่ได้รัน | Status และ data ถูกต้อง | ไม่มีหลักฐาน | NOT VERIFIED |
| README/cleanup | ยังไม่ได้รัน | Commands และ scoped cleanup ผ่าน | ไม่มีหลักฐาน | NOT VERIFIED |

## Test reports และ API evidence

บันทึก suite counts (tests/failures/errors/skips), report locations, exact cURL commands และ observed fields/assertion results Redact secrets ไม่ commit target/log dumps

## Failures, retests และ limitations

ระบุ blockers, checks ที่ไม่ได้รัน, เหตุผล N/A และ retest หลัง fixes รวม cleanup/process/resource state

## Overall status

NOT VERIFIED — แทนตามหลักฐานจริง ใช้ Verified ใน roadmap ต่อเมื่อ mandatory gates ผ่านทั้งหมดบน relevant final source state

## Coverage สองแนวทาง

| Objective / feature | Spring Data reactive: API/coverage/evidence | Official async: API/coverage/evidence | Parity assertion / gap |
|---|---|---|---|

กรอกแยกทุก objective ระบุ native abstraction, reactive-client access, explicit fallback หรือ unresolved ตามจริง ใช้ shared semantics/fixtures พร้อม separate route prefixes และ mapping/error/lifecycle comparison ห้ามนับ direct fallback เป็น native Spring Data coverage

แยก test counts, real-server evidence, smoke success/error ของสอง path และ parity results Existing single-path results ต้องระบุ historical scope ไม่เปลี่ยน overall เป็น Verified ก่อนผ่าน requirement ใหม่
