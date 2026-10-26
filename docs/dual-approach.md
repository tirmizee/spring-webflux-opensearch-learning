# ข้อกำหนดสองแนวทาง

Status: Accepted — ผู้ใช้ขอปรับข้อกำหนดทั้ง repository เมื่อ 2026-10-25

## เป้าหมายร่วม

ทุก chapter ต้องออกแบบสองแนวทางภายใน standalone Maven project เดียวกัน เพื่อเปรียบเทียบ abstraction, mapping, query construction, error semantics และ resource lifecycle ด้วย use case, fixtures และ assertions เดียวกัน ความเป็น independent หมายถึงไม่พึ่ง chapter อื่น ไม่ได้แปลว่าต้องแยกสอง Maven modules

| Approach | Application boundary | สิ่งที่ต้องเรียน |
|---|---|---|
| Spring Data OpenSearch | Reactive repository / operations / template; reactive client เฉพาะ API ที่ abstraction ไม่ครอบคลุม | Object mapping, annotations, conversion, repository queries และ abstraction limits |
| Official Java Client | OpenSearchAsyncClient + lazy CompletableFuture bridge | Explicit requests/Query DSL, response mapping, transport และ async semantics |

Spring Data OpenSearch สร้างบน Spring Data Elasticsearch จึงอาจมี interface/annotation/package ที่ชื่อ Elasticsearch ต้องอธิบาย provenance และ configuration ที่ใช้ OpenSearch implementation จริง ห้ามเปลี่ยนชื่อ API ในเอกสารให้ดูเหมือนมี class ที่ไม่มีจริง

## Naming และ API contract

Target convention สำหรับบทใหม่หรือ migration:

- SpringData*Controller / SpringData*Service → `/api/spring-data/...`
- JavaClient*Controller / JavaClient*Service → `/api/java-client/...`
- DTO และ domain semantics ร่วมกันเมื่อมีความหมายเดียวกัน ไม่บังคับ generic service interface เพื่อ routing
- ต่างเฉพาะ prefix; HTTP statuses, validation และ response fields ต้องเทียบกันได้ ไม่ใส่ approach metadata ใน response โดยไม่จำเป็น
- Class/package diagram แสดง boundary จริง `service/springdata`, `service/javaclient`, `repository`, `query` เพิ่มเมื่อจำเป็น ไม่สร้าง empty packages

Chapter 01 implement `/api/spring-data/cluster/info`, `/api/spring-data/cluster/health`, `/api/java-client/cluster/info`, `/api/java-client/cluster/health` แล้ว และเก็บ `/api/cluster/*` เป็น official aliases ทั้งหก routes ผ่าน real smoke/parity ตาม [report](../01-basic-connection/VERIFICATION.md) บทอื่นต้องแยก planned routes จาก implemented routes และระบุ legacy-route policy ตามจริง

## Async และ transport

ทั้งสองแนวทางห้าม blocking ใน request flow Spring Data ต้องใช้ reactive implementation จริง ไม่เอา synchronous repository ไปครอบ Mono แล้วเรียกว่า reactive

Official path ใช้ OpenSearchAsyncClient กับ Apache HttpClient 5 async transport เป็น baseline Spring Data path ให้ตรวจว่า reactive client/template ที่เลือกใช้ async transport จริงและรองรับการ inject transport นี้หรือไม่ โดยตั้งเป้าหมายใช้ Apache HttpClient 5 ร่วมกัน หาก integration ต้องต่าง transport ให้บันทึก version-specific rationale และตรวจ lifecycle แยก ไม่ลด requirement เรื่อง nonblocking

Reactive return type ไม่รับประกัน cold publisher ต้อง defer method invocation ที่เริ่ม I/O ทันที สร้าง operation ใหม่เมื่อ resubscribe/retry และทดสอบทั้งก่อน subscription, synchronous throw, asynchronous errors, cancellation และ deadlines แยกทั้งสอง path

Sharing transport ทำได้เมื่อ option/mapper/security policies เข้ากันและมี close owner เดียว แยก transport ได้เมื่อมีเหตุผล แต่ระบุ bean names/qualifiers, pool/timeouts และ ownership ห้ามเกิด double-close หรือ auto-config client ที่ชี้ cluster คนละตัว

## Feature coverage และ fallback

ทุก learning objective ต้องมี coverage row แยกสอง approach และระบุระดับจริง:

1. Native abstraction: repository/operations/template ทำ feature ได้ตรงโจทย์
2. Reactive client API: ใช้ Spring Data reactive client เพราะ high-level abstraction ไม่ครอบคลุม ต้องระบุว่าลดระดับแล้ว
3. Explicit fallback: หากต้องเรียก official client หรือ raw API ให้แยกเป็น fallback อย่างชัดเจน ไม่ถือเป็น Spring Data native implementation
4. Unsupported / Unresolved: ยังไม่รองรับหรือยังตรวจไม่ได้ ต้องมีหลักฐาน/คำถาม ไม่ใส่ N/A เพื่อให้บทผ่าน

สำหรับ vector/hybrid/index administration ต้องตรวจ exact release APIs ไม่สมมติว่า abstraction รองรับทุก feature ถ้า objective สำคัญทำสอง approach ไม่ได้ ให้เสนอปรับ objective/exception อย่างชัดเจนและขอผู้ใช้ตัดสินใจ ไม่ silently ยกเว้น approach นั้น

## Parity และ isolation

- ใช้ dataset/mappings/query intent เดียวกัน แต่ request builders ไม่จำเป็นต้องแชร์ object type หากทำให้ coupling สูง
- Read-only comparison ใช้ index เดียวกันได้ Write tests ต้อง reset fixtures หรือแยก equivalent indices ต่อ approach เพื่อไม่รบกวนกัน
- Assert IDs/counts/status/error semantics และ normalized DTOs ไม่ต้อง byte-identical raw responses หรือ exact floating relevance scores
- Unit tests mock dependency ของแต่ละ approach แยกกัน; HTTP tests มีทั้งสอง prefix; IT และ packaged-app smoke ต้องเรียกทั้งสอง path จริง
- Full verify และการมี Spring Data dependency ใน POM ไม่พิสูจน์ว่า Spring Data path ถูกใช้ ต้องตรวจ bean/implementation wiring และ behavior ของ path นั้น

## Definition of done

Verified ต้องมีสอง path ที่ implement แล้ว พร้อม feature coverage/parity, real OpenSearch integration และ smoke/error evidence ตาม [verification standards](testing-standards.md#verification) หากมี approved limitation ให้บันทึกขอบเขตชัดเจน

Chapter 01 มี historical official-only result 25 tests และ current dual-approach verification 49 tests พร้อม smoke/parity แล้ว Overall status เป็น Verified ตาม report ปัจจุบัน บทนี้ใช้ reactive-client tier สำหรับ metadata ไม่อ้าง repository/template mapping coverage

Roadmap และ coverage เป้าหมายอยู่ใน [learning roadmap](learning-roadmap.md) ตารางเป็น design targets ไม่ใช่การรับรอง API support

## Sources และ compatibility scope

Spring Data ที่เลือกสำหรับ Chapter 01 คือ 3.1.4 ตาม [release](https://github.com/opensearch-project/spring-data-opensearch/releases/tag/v3.1.4) และ [tagged compatibility matrix](https://github.com/opensearch-project/spring-data-opensearch/blob/v3.1.4/README.md) Resolve และ runtime-test ผ่านสำหรับ metadata/health ของ Chapter 01 แล้ว แต่ไม่พิสูจน์ APIs ของบทถัดไป

ตรวจ source ของ [ReactiveOpenSearchConfiguration](https://github.com/opensearch-project/spring-data-opensearch/blob/v3.1.4/spring-data-opensearch/src/main/java/org/opensearch/data/client/osc/ReactiveOpenSearchConfiguration.java) พบ default transport เป็น RestClient-based ต้องไม่กล่าวว่า default คือ Apache HttpClient 5

[ReactiveOpenSearchClient.info](https://github.com/opensearch-project/spring-data-opensearch/blob/v3.1.4/spring-data-opensearch/src/main/java/org/opensearch/data/client/osc/ReactiveOpenSearchClient.java) เรียก performRequestAsync ก่อนส่ง future ให้ Mono จึงต้อง defer invocation สำหรับ cold contract ที่เราต้องการ [ReactiveOpenSearchTemplate](https://github.com/opensearch-project/spring-data-opensearch/blob/v3.1.4/spring-data-opensearch/src/main/java/org/opensearch/data/client/osc/ReactiveOpenSearchTemplate.java) ใช้ ReactiveElasticsearchOperations abstraction โดยมี OpenSearch implementation

## Chapter 03: approved starter-only scope

Chapter 03 เป็นข้อยกเว้นที่ผู้ใช้อนุมัติ ใช้ `spring-data-opensearch-starter` แนวทางเดียวใน standalone Maven project ที่ `03-api-key-authentication` ไม่เพิ่ม core-only project หรือ official async application path ไม่ต้องมี cross-approach parity

ใช้ ReactiveOpenSearchClient สำหรับ cluster metadata และต้องผ่าน API Key permission/error/lifecycle fixtures, JVM trust/hostname failure tests และ packaged smoke ไม่อ้าง repository mapping coverage จาก template wiring Tests ต้องยืนยัน client/transport/template/converter bean factory origins จริง ใช้ starter auto-configuration + RestClientBuilderCustomizer เพิ่ม API Key ไม่สร้าง client/transport เอง ไม่ exclude auto-configurations และยอมรับ default deprecated REST transport ของ exact release โดยรายงาน warnings และ native lifecycle ตามจริง

Configuration มีชุดเดียวใต้ `opensearch` ใช้ JVM default trust ไม่มี application CA property/loader; local private CA เตรียมผ่าน JVM truststore ภายนอก application Infrastructure ไม่พึ่ง Chapter 02 สถานะ Verified ต้องมี REVIEW และ mandatory gates ของ starter ตาม testing standards

## Chapter 04: approved starter-only scope

Chapter 04 ใช้ standalone `spring-data-opensearch-starter` project แนวทางเดียวตาม approved plan ไม่มี official application path หรือ cross-path parity ใช้ local HTTP ปิด security ใน Compose/Testcontainers ไม่เพิ่ม authentication/TLS หรือ infrastructure dependency จากบทอื่น

ใช้ mapping annotations + converter, `ReactiveIndexOperations` สำหรับ lifecycle/mapping/settings read และ `ReactiveElasticsearchOperations` สำหรับ sample save/get Dynamic settings write ใช้ `ReactiveOpenSearchClient.indices().putSettings` ต้องระบุเป็น reactive-client tier ใช้ starter auto-configuration และ timeout customizer ไม่สร้าง client/transport เอง ยอมรับ native deprecated REST defaults พร้อมเปิดเผย warnings และตรวจ shutdown

Required coverage คือ generated strict mapping และ `_class`, converter round-trip/ID metadata, persisted sample/keyword multi-field, invalid numeric strings/unknown fields ที่ native server ปฏิเสธ, dynamic settings read-back, final shard/field type rejection, no auto-create, HTTP failures และ packaged smoke ทุก API เกณฑ์ Verified ใช้ gates เดิมตาม testing standards ภายใต้ approved scope นี้
