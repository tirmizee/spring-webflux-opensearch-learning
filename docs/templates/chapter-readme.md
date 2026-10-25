# Chapter NN — Topic

Template: แทนข้อความคำแนะนำด้วยข้อมูลจริงก่อนส่งมอบ README

## ภาพรวมและ learning objectives

อธิบายสิ่งที่รันได้และผลที่พิสูจน์ได้ พร้อม prerequisite concepts และ scope

## Concepts

อธิบาย OpenSearch concepts, async behavior และ trade-offs ที่บทนี้สอน

## Versions และ configuration

Exact versions, properties/env/defaults, ports, credentials/TLS และ prerequisites แยก .env ของ Compose จาก environment ของ host JVM

## โครงสร้างโปรเจกต์

ใส่ expanded actual file tree ครบ source/tests/config ไม่มี ellipsis หรือ invented files อธิบาย class responsibilities และ request flow ต่อจาก tree

## Dataset และ OpenSearch resources

Fields/mappings/settings, stable IDs, expected matches, setup/seed และ ownership/cleanup

## Quick Start

ใส่คำสั่งจริงเรียง enter directory → configuration → Compose up → readiness → ./mvnw clean verify → packaged application startup → API calls → scoped cleanup

## API examples

แต่ละ endpoint มี method/path, copyable cURL, success/error status, expected fields และ assertions ระบุ dynamic fields แยก illustrative response จาก observed output

## Testing และ verification

Unit/HTTP/integration commands และข้อจำกัด อ้าง VERIFICATION.md ของบทเมื่อมีจริง ไม่มี verification claims จาก mocks อย่างเดียว

## Pitfalls และ production considerations

Timeout/retry/idempotency, concurrency, refresh, permissions และ resource limits ตามที่เกี่ยวข้อง

## Exercises

Core exercises พร้อม expected observations แยก optional inference/load/tuning และ prerequisites เพิ่มเติม

## Sources

Official docs ของ exact release และ dataset/model provenance/license ถ้ามี

## Coverage สองแนวทาง

| Objective / feature | Spring Data reactive: API/coverage/evidence | Official async: API/coverage/evidence | Parity assertion / gap |
|---|---|---|---|

กรอกแยกทุก objective ระบุ native abstraction, reactive-client access, explicit fallback หรือ unresolved ตามจริง ใช้ shared semantics/fixtures พร้อม separate route prefixes และ mapping/error/lifecycle comparison ห้ามนับ direct fallback เป็น native Spring Data coverage

แยก examples ตาม approach พร้อมตารางจุดที่ abstraction ต่างกัน Chapter ที่ยัง migrate ไม่ครบต้องระบุ actual routes กับ target routes ชัดเจน ไม่ใส่ planned classes ใน actual file tree
