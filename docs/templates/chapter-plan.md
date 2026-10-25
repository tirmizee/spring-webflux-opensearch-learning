# แผน Chapter NN — Topic

Template สำหรับการออกแบบ ไม่ใช่หลักฐาน implementation กรอกทุกส่วนตามบทจริง

## Scope

- เป้าหมายและ learning objectives ที่สังเกตผลได้
- Prerequisite concepts; สิ่งที่ไม่อยู่ใน scope; core vs optional exercises
- Existing work/source state และ scope ที่อนุมัติ

## Compatibility

Exact Java/Boot/client/server/test platform candidates, official sources และสถานะ Documented/Resolved/Unresolved ไม่ใช้คำว่า verified หากยังไม่รัน

## Design

- รายการไฟล์/classes พร้อมหน้าที่และ base package
- Dataset, IDs, mappings/settings, index/pipeline/alias names และ setup/reset
- ตาราง HTTP method/path, input/validation, success/error status และ expected fields
- Request flow, lazy async bridge, lifecycle, deadlines, retry/idempotency และ concurrency bounds
- Env vars, ports, TLS/auth และ credential ownership

## Tests และ verification

ระบุ test cases ราย layer รวม boundaries, sync/async failures และ assertions ที่พิสูจน์ feature บน real server

แสดง commands สำหรับ dependencies/build/tests, Compose readiness, packaged app, cURL field assertions และ scoped cleanup พร้อม expected outcomes แต่ไม่แต่ง actual results

## Acceptance และ open questions

Definition of done, risks/trade-offs, assumptions และ decisions ที่ต้องยุติ หยุดตาม scope ไม่เริ่ม implement จากคำขอ plan

## Coverage สองแนวทาง

| Objective / feature | Spring Data reactive: API/coverage/evidence | Official async: API/coverage/evidence | Parity assertion / gap |
|---|---|---|---|

กรอกแยกทุก objective ระบุ native abstraction, reactive-client access, explicit fallback หรือ unresolved ตามจริง ใช้ shared semantics/fixtures พร้อม separate route prefixes และ mapping/error/lifecycle comparison ห้ามนับ direct fallback เป็น native Spring Data coverage

ระบุ candidate Spring Data/Spring Data Elasticsearch versions จาก matrix และ exact artifact graph ที่ต้องพิสูจน์ หากทำสองแนวทางไม่ได้ให้ยุติ mandatory-objective exception ก่อน implement
