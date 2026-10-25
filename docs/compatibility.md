# Compatibility และ dependency baseline

ผลด้านล่างเป็น baseline ที่ทดสอบกับ Chapter 01 สองแนวทางแล้ว จำกัดเฉพาะ metadata/health/lifecycle และ platform ที่บันทึก

ปรับ baseline สำหรับ Chapter 01: 2026-10-25 (Asia/Bangkok) Chapter 01 ผ่าน 49 tests รวม real-server integration, cross-path parity และ Compose/packaged-app smoke แล้ว ผลและขอบเขต runtime อยู่ใน [Chapter 01 verification](../01-basic-connection/VERIFICATION.md)

## ความหมายของสถานะ

- `Required`: requirement ของ repository
- `Documented`: พบใน official documentation แต่ยังไม่ resolve ในโครงการ
- `Resolved`: ตรวจ dependency tree/effective POM จริงแล้ว ไม่เท่ากับ runtime ผ่าน
- `Runtime verified`: ผ่าน applicable tests พร้อมหลักฐาน exact source/environment
- `Unresolved`: ต้องตัดสินใจหรือตรวจเพิ่มก่อนใช้งาน

## Matrix

| Component | Requirement / candidate | Evidence status | สิ่งที่ยังต้องทำ |
|---|---|---|---|
| Application Java | Temurin 21.0.12.1, compiler release 21 | Runtime verified | เฉพาะ Chapter 01/platform ที่บันทึก |
| Spring Boot / Framework | 4.1.2 / 7.0.10 | Runtime verified | Full application ผ่าน |
| WebFlux / Reactor Core | Boot 4.1.2 / 3.8.8 | Runtime verified | Unit/HTTP และ IT ผ่าน |
| OpenSearch server | opensearchproject/opensearch:3.9.0 | Runtime verified | Digest อยู่ใน Chapter 01 report |
| Spring Data OpenSearch | 3.1.4 | Runtime verified | Reactive client metadata/health บน shared Apache transport |
| Spring Data Elasticsearch abstraction | Boot-managed 6.1.2 | Resolved | ไม่ใช้ Elasticsearch Java client; ยังไม่ทดสอบ repository/template mapping |
| OpenSearch Java Client | 3.10.0 | Runtime verified | Metadata/health กับ 3.9.0 |
| HttpClient / HttpCore | 5.6.5 / 5.4.4 | Runtime verified | Apache async metadata/health flow |
| JSON | Jackson 3.1.7 + client jackson3.JacksonJsonpMapper | Runtime verified | Metadata decode และ DTO HTTP encode |
| Maven / Wrapper | 3.9.11 / 3.3.4 | Resolved | distribution SHA-256 pin เทียบ official SHA-512 |
| JUnit Jupiter / Mockito | 6.0.3 / 5.23.0 | Runtime verified | ผู้ใช้อนุมัติ JUnit 6; ทั้ง 49 tests ผ่าน |
| Testcontainers | 2.0.5 | Runtime verified | GenericContainer/OpenSearch IT ผ่าน |
| Docker / Compose | Engine 29.8.2 / Compose 5.6.0 | Observed | macOS 27.0.1 arm64, Desktop 4.95.0 |

Official matrix ระบุ OpenSearch client 3.x กับ server 1.x–3.x และ JDK 21 ไม่รับประกันทุก feature หรือ dependency combination ไม่ต้องทำเลข minor ของ client ให้เท่ากับ server

## แหล่งอ้างอิง

- [Spring Boot requirements](https://docs.spring.io/spring-boot/system-requirements.html): หน้าที่ตรวจระบุ Boot 4.1.2 รองรับ Java 21 และ Maven 3.6.3+; repository ตั้งใจเลือก Maven 3.9.x
- [Spring Boot testing](https://docs.spring.io/spring-boot/reference/testing/): JUnit baseline ของ Boot ไม่ใช่หลักฐานว่า JUnit 5 override ใช้ได้
- [Boot 4.0 RC1 release notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0.0-RC1-Release-Notes): เปลี่ยนเป็น JUnit Jupiter 6.0
- [OpenSearch client matrix](https://github.com/opensearch-project/opensearch-java/blob/main/COMPATIBILITY.md)
- [OpenSearch Java client](https://docs.opensearch.org/latest/clients/java/): candidate 3.10.0 และ recommended transport
- [Client changelog](https://github.com/opensearch-project/opensearch-java/blob/main/CHANGELOG.md): ตรวจ Jackson 3 support เทียบ release ที่จะใช้
- [Spring Boot JSON](https://docs.spring.io/spring-boot/4.0/reference/features/json.html): Jackson 3 เป็น default

Links แบบ latest/main อาจเปลี่ยน ต้องบันทึก release/tag และวันที่เมื่อเลือก baseline จริง

## Baseline decision ของ Chapter 01

ผู้ใช้อนุมัติ Boot-managed JUnit 6 แทน JUnit 5 เมื่อเริ่ม Chapter 01 ดู [ADR 0003](decisions/0003-test-platform-selection.md) เพราะ SpringExtension 7 ต้องใช้ Jupiter 6+ ไม่ override API/engine/platform ลงเป็น 5

Pin exact versions ใน POM, Wrapper และ Compose แล้ว ตรวจ APIs ด้วย javap จาก resolved 3.10.0 JAR ไม่มี probe project แยก การยืนยัน baseline นี้ครอบคลุมเฉพาะ feature/platform ที่ทดสอบใน Chapter 01 ไม่รับประกัน vector/hybrid chapters หรือ OS อื่น

## Spring Data integration ที่ตรวจแล้ว

เลือก 3.1.4 จาก [tagged matrix](https://github.com/opensearch-project/spring-data-opensearch/blob/v3.1.4/README.md) และ [release](https://github.com/opensearch-project/spring-data-opensearch/releases/tag/v3.1.4) Resolve ร่วมกับ Boot 4.1.2 ได้ Spring Data Elasticsearch 6.1.2, Commons 4.1.2 และ opensearch-java 3.10.0

ใช้ core library พร้อม explicit ReactiveOpenSearchClient bean ไม่ใช้ starter/default ReactiveOpenSearchConfiguration Exclude opensearch-rest-high-level-client; ไม่มี Elasticsearch Java Client ใน resolved graph และ IT classpath ยังมี opensearch-rest-client 3.8.0 transitive แต่ configuration ของบทนี้ไม่สร้าง RestClient/RestClientTransport

ReactiveOpenSearchClient และ OpenSearchAsyncClient รับ OpenSearchTransport เดียวกันจริง ใช้ Apache HttpClient 5 + Jackson 3 mapper, pool/timeouts ชุดเดียว ปิด reactive wrapper destroy callback ให้ transport bean เป็น owner เดียว Configuration shutdown test และ IT shared-instance assertions ผ่าน

Metadata methods ของ reactive client ถูกครอบ Mono.defer มีแยก service tests สำหรับ lazy invocation/fresh retry/error/cancellation/deadline มี client spies ที่ call real methods ตรวจ selected wrapper พร้อม 8 real IT และ healthy/failure smoke ทุก route

ยังไม่พิสูจน์ repository startup/index creation, object converters, vector/hybrid parity หรือ platform อื่น Exact commands อยู่ใน [current report](../01-basic-connection/VERIFICATION.md) ผล official-only เดิมเก็บใน [archived report](../01-basic-connection/verification/official-only-2026-10-25.md)

## Trade-offs และ open decisions

ยังต้องเลือก license, embedding model/dataset provenance และ tested platforms Local precomputed embeddings ลด network/model startup cost แต่ไม่พิสูจน์ inference pipeline Optional inference ต้องแยก instructions และ resource requirements
