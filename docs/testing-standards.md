# Testing standards

Chapter 01 ใช้ Boot-managed JUnit 6 ตามคำอนุมัติผู้ใช้ ดู [ADR 0003](decisions/0003-test-platform-selection.md) ไม่ override JUnit API/engine/platform แยกกัน Tests ต้องมีตั้งแต่บทแรก

| Layer | Tools | Assertions |
|---|---|---|
| Unit | JUnit, Mockito | Validation, query/request construction, DTO/error mapping |
| Reactive | StepVerifier | Lazy invocation, fresh retry, sync/async errors และ cancellation |
| HTTP | WebTestClient | Status, payload, validation และ exception contract |
| Integration | Testcontainers + OpenSearch จริง | Mapping, CRUD, queries, serialization และ relevant transport behavior |
| Smoke | Compose + packaged app + cURL | Flow ตาม README รวม deterministic field assertions |

Unit/slice ใช้ `*Test` และ Surefire; integration ใช้ `*IT` และ Failsafe bind integration-test/verify `./mvnw clean verify` ต้องรันทั้งสอง suite ตรวจ test counts/reports ไม่ใช่ exit code อย่างเดียว ไม่มี skipITs/skipTests ใน full verification และไม่ auto-disable เมื่อ Docker หาย

ใช้ Mockito stub ที่ขอบเขต dependency จริง ไม่ mock ทุก builder chain เพื่อยืนยัน implementation ทีละบรรทัด HTTP slice ใช้ Boot 4 test module/imports ตาม version จริง หรือ bind controller เมื่อเพียงพอ ต้องมี full application tests ด้วย

## Two-approach coverage

ทุกบทมี tests แยก Spring Data reactive กับ official async dependency จริง ตาม [dual-approach contract](dual-approach.md) HTTP tests/IT/smoke ต้องเรียกทั้งสอง prefixes และตรวจว่าบริการไปคนละ integration path ไม่ stub direct service แล้วถือว่า Spring Data ผ่าน

ใช้ shared behavioral cases หรือ parameterized IT เมื่อช่วยลด duplication พร้อมแสดง approach ใน test names/report ตรวจ DTO/status/result IDs และ error parity; Spring Data mapping converters/annotations ต้องมี round-trip assertions ในบทเอกสาร

Write cases ใช้ isolated equivalent indices หรือ reset deterministic fixtures ก่อนแต่ละ approach เพื่อไม่ให้ create/delete/conflict ของ path หนึ่งเปลี่ยน expectation อีก path Native support, reactive-client access และ explicit fallback ต้องรายงานแยกกัน

## Isolation และ determinism

- ใช้ GenericContainer กับ pinned official OpenSearch image เป็น baseline ไม่สมมติ ElasticsearchContainer รองรับ image นี้
- Container ของ tests ไม่พึ่ง Compose ที่ผู้ใช้เปิดไว้ ใช้ getHost/mapped ports และ dynamic app configuration ไม่ hardcode localhost:9200
- ใช้ unique index names, stable document IDs, explicit refresh และ scoped cleanup
- Readiness polling มี timeout และ useful diagnostics ไม่ fixed sleeps
- ไม่ใช้ exact floating relevance score ถ้าไม่ใช่ controlled mathematical fixture ใช้ IDs/order/bounds ตามสิ่งที่ทดสอบจริง
- Pagination ใช้ deterministic tie-breaker field ที่ mapping รองรับ ไม่สมมติ `_id` sort ได้
- Vector tests มี exact baseline, dimension validation และ approximate assertions ที่มีเหตุผล
- Retry tests ใช้ controllable futures/virtual time ไม่พึ่ง network outage เพื่อพิสูจน์ทุก branch

ใช้ parallel execution เฉพาะเมื่อ resource isolation พร้อม ไม่มี shared mutable fixtures/container reset race

## Evidence limits

Mock tests ไม่พิสูจน์ server query semantics; future cancellation ไม่พิสูจน์ request abort; happy-path 200 ไม่พิสูจน์ OpenSearch ถูกเรียก ตรวจ persisted/searchable data และ server response ที่เฉพาะเจาะจง

[Verification standards](verification-standards.md) กำหนดเกณฑ์ Verified เพิ่มจาก Maven tests แหล่งอ้างอิง: [Testcontainers](https://testcontainers.com/getting-started/)
