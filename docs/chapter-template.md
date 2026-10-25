# Chapter template

ใช้กับ standalone chapter ตาม [architecture](architecture.md) และ [dual-approach contract](dual-approach.md) คัดลอกเฉพาะส่วนที่ใช้กับบทจริง ไม่สร้าง empty packages หรือรายงานผลที่ยังไม่ได้รัน

## Required files

| Path | หน้าที่ |
|---|---|
| README.md | เอกสารภาษาไทยตาม template พร้อม actual project tree |
| VERIFICATION.md | ผลจริงหลัง attempted verification; เริ่ม NOT VERIFIED ได้แต่ห้าม fabricated results |
| pom.xml | Boot parent โดยตรง, Java 21, dependencies และ Surefire/Failsafe |
| mvnw, mvnw.cmd, .mvn/wrapper/maven-wrapper.properties | Wrapper ที่ pin distribution/checksum และรันจากบทได้ |
| docker-compose.yml | Local OpenSearch, readiness, scoped resources และ adjustable ports |
| .env.example | Non-secret configuration และ instructions การตั้ง credentials |
| requests/examples.http | ตัวอย่าง requests ตรง API จริง ไม่มี embedded secrets |
| scripts/smoke-test.sh | Assert status/fields และจบ nonzero เมื่อไม่ตรง expectation |
| src/main/java | Application และ packages ที่มีหน้าที่จริง |
| src/main/resources/application.yml | Typed/documented properties และ explicit defaults |
| src/test/java | Unit/HTTP tests และ real-server *IT |
| src/test/resources/fixtures | Deterministic test data ตามความจำเป็น |

Base package `com.example.learning.opensearch.<topic>` โดย Java identifier ไม่มี hyphen Application class อยู่ root package ใช้ config/controller/service/dto/exception ตามหน้าที่ เพิ่ม domain/query/support เมื่อจำเป็น ไม่บังคับ empty packages

## Infrastructure

Single-node default; no fixed container_name; localhost-bound host ports; configurable app/OpenSearch ports; Compose project per chapter; no destructive reset โดยปริยาย Tests ใช้ random ports ไม่อ้าง external running cluster และไม่ใช้ `latest` image tag

Seed/setup ทำซ้ำได้ พร้อม failure propagation และ resource ownership Readiness ตรวจ API/auth จริง Credentials least privilege ใน security chapter ไม่ใช้ admin เป็น application identity

## README.md

### Chapter NN — Topic

Template: แทนข้อความคำแนะนำด้วยข้อมูลจริงก่อนส่งมอบ README

### ภาพรวมและ learning objectives

อธิบายสิ่งที่รันได้และผลที่พิสูจน์ได้ พร้อม prerequisite concepts และ scope

### Concepts

อธิบาย OpenSearch concepts, async behavior และ trade-offs ที่บทนี้สอน

### Versions และ configuration

Exact versions, properties/env/defaults, ports, credentials/TLS และ prerequisites แยก .env ของ Compose จาก environment ของ host JVM

### โครงสร้างโปรเจกต์

ใส่ expanded actual file tree ครบ source/tests/config ไม่มี ellipsis หรือ invented files อธิบาย class responsibilities และ request flow ต่อจาก tree

### Dataset และ OpenSearch resources

Fields/mappings/settings, stable IDs, expected matches, setup/seed และ ownership/cleanup

### Quick Start

ใส่คำสั่งจริงเรียง enter directory → configuration → Compose up → readiness → ./mvnw clean verify → packaged application startup → API calls → scoped cleanup

### API examples

แต่ละ endpoint มี method/path, copyable cURL, success/error status, expected fields และ assertions ระบุ dynamic fields แยก illustrative response จาก observed output

### Testing และ verification

Unit/HTTP/integration commands และข้อจำกัด อ้าง VERIFICATION.md ของบทเมื่อมีจริง ไม่มี verification claims จาก mocks อย่างเดียว

### Pitfalls และ production considerations

Timeout/retry/idempotency, concurrency, refresh, permissions และ resource limits ตามที่เกี่ยวข้อง

### Exercises

Core exercises พร้อม expected observations แยก optional inference/load/tuning และ prerequisites เพิ่มเติม

### Sources

Official docs ของ exact release และ dataset/model provenance/license ถ้ามี

### Coverage สองแนวทาง

| Objective / feature | Spring Data reactive: API/coverage/evidence | Official async: API/coverage/evidence | Parity assertion / gap |
|---|---|---|---|

กรอกแยกทุก objective ระบุ native abstraction, reactive-client access, explicit fallback หรือ unresolved ตามจริง ใช้ shared semantics/fixtures พร้อม separate route prefixes และ mapping/error/lifecycle comparison ห้ามนับ direct fallback เป็น native Spring Data coverage

แยก examples ตาม approach พร้อมตารางจุดที่ abstraction ต่างกัน Chapter ที่ยัง migrate ไม่ครบต้องระบุ actual routes กับ target routes ชัดเจน ไม่ใส่ planned classes ใน actual file tree

## VERIFICATION.md

### Chapter NN Verification

Template นี้ไม่มีผลทดสอบ กรอกหลัง attempted validation และไม่คัดลอก expected เป็น actual

### Plan และ review context

ระบุ approved plan ที่ใช้ (ไฟล์หรือข้อความใน conversation พร้อม scope/วันที่อนุมัติ ไม่บังคับสร้าง plan file), reviewed source state, review findings และผลแก้ไข อ้าง review summary ที่มีจริง แยกจาก runtime evidence ตาม [chapter workflow](../AGENTS.md#chapter-workflow)

### Source และ environment

- Timestamp พร้อม timezone:
- Branch/revision หรือ unborn HEAD; dirty/untracked state และ source scope:
- Java/Maven/Boot/client/server image tag/digest/test platform versions:
- OS/CPU/Docker/Compose และ preconditions:

### Gates

| Gate | Command / evidence location | Expected | Actual / exit code | Status |
|---|---|---|---|---|
| Environment/dependencies | ยังไม่ได้รัน | Versions/API alignment | ไม่มีหลักฐาน | NOT VERIFIED |
| Build/unit/HTTP tests | ยังไม่ได้รัน | Tests discovered and passing | ไม่มีหลักฐาน | NOT VERIFIED |
| OpenSearch ITs | ยังไม่ได้รัน | Real-server assertions pass | ไม่มีหลักฐาน | NOT VERIFIED |
| Compose configuration | ยังไม่ได้รัน | docker compose config ผ่าน | ไม่มีหลักฐาน | NOT VERIFIED |
| Compose readiness | ยังไม่ได้รัน | API ready | ไม่มีหลักฐาน | NOT VERIFIED |
| Packaged application | ยังไม่ได้รัน | Listening and ready | ไม่มีหลักฐาน | NOT VERIFIED |
| cURL/field assertions | ยังไม่ได้รัน | Status และ data ถูกต้อง | ไม่มีหลักฐาน | NOT VERIFIED |
| README/cleanup | ยังไม่ได้รัน | Commands และ scoped cleanup ผ่าน | ไม่มีหลักฐาน | NOT VERIFIED |

### Test reports และ API evidence

บันทึก suite counts (tests/failures/errors/skips), report locations, exact cURL commands และ observed fields/assertion results Redact secrets ไม่ commit target/log dumps

### Failures, retests และ limitations

ระบุ blockers, checks ที่ไม่ได้รัน, เหตุผล N/A และ retest หลัง fixes รวม cleanup/process/resource state

### Overall status

NOT VERIFIED — แทนตามหลักฐานจริง ใช้เกณฑ์ REVIEW และ mandatory verification gates จาก [testing standards](testing-standards.md#verification) ก่อนตั้ง Verified ใน roadmap บน relevant final source state

### Coverage สองแนวทาง

| Objective / feature | Spring Data reactive: API/coverage/evidence | Official async: API/coverage/evidence | Parity assertion / gap |
|---|---|---|---|

กรอกแยกทุก objective ระบุ native abstraction, reactive-client access, explicit fallback หรือ unresolved ตามจริง ใช้ shared semantics/fixtures พร้อม separate route prefixes และ mapping/error/lifecycle comparison ห้ามนับ direct fallback เป็น native Spring Data coverage

แยก test counts, real-server evidence, smoke success/error ของสอง path และ parity results Existing single-path results ต้องระบุ historical scope ไม่เปลี่ยน overall เป็น Verified ก่อนผ่าน requirement ใหม่

## Completion

ตรวจทั้งสอง approach ตาม [testing และ verification standards](testing-standards.md) อัปเดต [roadmap](learning-roadmap.md) เฉพาะบทที่มีหลักฐานใหม่ ไม่เริ่มบทถัดไปโดยอัตโนมัติ
