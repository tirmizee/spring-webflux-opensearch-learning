# Engineering standards

## Java และ API

ใช้ Java 21 compiler release 21, constructor injection, final dependencies และ records สำหรับ immutable DTOs เมื่อเหมาะสม ไม่เพิ่ม Lombok โดยปริยาย ใช้ descriptive names, explicit imports และ readable code ไม่สร้าง factories/interfaces/common modules โดยไม่มีหน้าที่จริง

Controllers คืน Mono/Flux และไม่ทำ client integration โดยตรง Services ไม่ผูกกับ HTTP ResponseEntity โดยไม่จำเป็น แยก vendor models จาก public DTOs ใช้ centralized error mapping และ input validation จำกัด size, query length และ whitelist fields/sort options ไม่เปิด arbitrary cluster administration ผ่าน public input

### Record formatting

record ที่มี 1–2 components ให้เขียน declaration ในบรรทัดเดียว เช่น:

```java
public record ConnectionSettings(String uri) {
}

public record ConnectionTimeouts(Duration connectTimeout, Duration responseTimeout) {
}
```

record ที่มี 3 components ขึ้นไป ให้ขึ้นบรรทัดใหม่หลัง `(` แยกหนึ่ง component ต่อบรรทัดและเยื้อง 4 spaces วาง `) {` ในบรรทัดของตัวเองโดยจัดแนวกับ `public record` เช่น:

```java
public record ClusterInfoResponse(
    String clusterName,
    String clusterUuid,
    String nodeName,
    String distribution,
    String version
) {
}
```

ใช้รูปแบบนี้กับ records ที่สร้างหรือแก้ใน scope ของงาน ไม่ reformat ไฟล์อื่นที่ไม่เกี่ยวข้องโดยอัตโนมัติ และไม่เปลี่ยนลำดับ ชื่อ หรือชนิดของ components เพื่อจัดรูปแบบ

### Blank line หลังเปิด type body

สำหรับ class หรือ record ที่มีสมาชิกใน body ให้เว้นบรรทัดว่างหนึ่งบรรทัดหลัง `{` ของ declaration ก่อน field, constructor, method, comment หรือ annotation ของสมาชิกแรก ใช้บรรทัดว่างที่ไม่มี spaces และไม่เพิ่มบรรทัดว่างคั่นระหว่าง annotation กับสมาชิกที่ annotation นั้นกำกับ

```java
public class OpenSearchConfiguration {

    @Bean(destroyMethod = "close")
    OpenSearchTransport openSearchTransport() {
        // ...
    }
}
```

```java
public record ConnectionSettings(String uri) {

    @AssertTrue(message = "URI must not be blank")
    public boolean isUriValid() {
        return uri != null && !uri.isBlank();
    }
}
```

Empty body เช่น DTO record ที่ไม่มีสมาชิกเพิ่มเติม ไม่ต้องเพิ่มบรรทัดว่าง ใช้กฎนี้กับไฟล์ที่สร้างหรือแก้ใน scope ของงาน

## OpenSearch behavior

- Explicit mappings สำหรับ fields ที่ใช้สอน อธิบาย text vs keyword, dates และ numeric precision ใช้ UTC/explicit timezone ใน data assertions
- กำหนด index/resource prefix แยกบทและ tests ไม่ลบ wildcard resources
- Empty search เป็น 200 พร้อม empty results; missing document เป็น 404; create conflict เป็น 409; invalid application input เป็น 400
- Timeout/dependency unavailable ใช้ contract เช่น 504/503 ตามสาเหตุ ห้ามแปลง upstream credential failure เป็น caller 401 โดยอัตโนมัติ
- Error payload ใช้ consistent ProblemDetail หรือ DTO ไม่เผย credentials, connection internals หรือ stack traces
- Search total ต้องอธิบาย relation/track_total_hits เมื่อใช้ ไม่ถือว่าค่า total เป็น exact ทุกกรณี
- Read-after-write ใน search ขึ้นกับ refresh; realtime get แตกต่างจาก search ไม่ refresh ทุก write ในตัวอย่าง production โดยไม่อธิบาย
- Bulk ต้องตรวจ item failures แม้ HTTP success; selective retry ต้องรักษา idempotency
- Setup/seed มี explicit command/lifecycle และ readiness gate ไม่ทำ detached subscribe ที่ startup

## สองแนวทางและ Spring Data conventions

ทำตาม [dual-approach contract](dual-approach.md) แยก SpringData* และ JavaClient* classes/routes แต่ใช้ equivalent behavior/DTOs ใช้ reactive repositories/operations/template สำหรับ document use cases; repository interfaces ที่ Spring Data implement ให้เป็น abstraction ที่มีหน้าที่จริง ไม่อยู่ในข้อห้าม single-implementation interfaces แบบไร้เหตุผล

Spring Data domain model อาจต้องมี annotations/converter metadata ต่างจาก direct client DTO ไม่บังคับใช้ Java class เดียวหาก semantics/serialization ไม่ตรง ต้องอธิบาย field names, IDs, dates, nulls และ concurrency metadata พร้อม parity assertions

ตรวจ auto-config และ dependency graph ว่าใช้ OpenSearch implementations จริง ไม่บังคับห้าม Spring Data Elasticsearch transitive interfaces เพราะ Spring Data OpenSearch สร้างบนโครงการนั้น หลีกเลี่ยง client/server Elasticsearch ที่ถูกตั้งขึ้นโดยอุบัติเหตุ

Spring Data อาจแปลง upstream exceptions เป็น DataAccessException ส่วน official path ใช้ client/transport exceptions ต้อง normalize ตาม cause/operation ที่ตรวจจริงให้เป็น HTTP contract เดียวกัน ไม่ blanket-map ทุก DataAccessException เป็น 503 และไม่เปิดเผย upstream credentials

Async integration ของทั้งสอง path ใช้ [reactive standards](reactive-opensearch.md)

## Configuration และ security

Pin exact versions และ image tags ตาม [compatibility](compatibility.md) ใช้ environment/config properties ไม่ hardcode secrets `.env.example` มีเฉพาะ non-secret defaults และ placeholders ต้องอธิบายการส่ง env เข้า application ด้วย เพราะ Spring Boot ไม่โหลด Compose .env โดยอัตโนมัติ

Compose bind localhost, ไม่มี fixed container_name และใช้ project-scoped resources Host ports configurable; tests ใช้ random mapped ports Security-disabled mode จำกัด local learning และระบุชัด Security chapter ใช้ trusted CA ไม่ copy trust-all TLS/hostname verification bypass มาเป็น default Credentials ของ app ต้องมีสิทธิ์น้อยกว่า bootstrap admin

Single-node replica policy ต้องตั้งใจและอธิบาย yellow health ไม่ได้แปลว่า application เสียเสมอ Readiness ต้องตรวจ API/auth ไม่ดูแค่ container Running

## Dataset และ advanced chapters

Fixtures มี stable IDs และ expected assertions ไม่พึ่งเวลาปัจจุบันหรือ external services Core exercises ต้องจบได้บน local setup Optional model inference, load tests และ advanced tuning แยกชัดเจน

Fixed vectors สอน distance/API ไม่ใช่หลักฐาน semantic quality Real embeddings ต้องมี model/version, dimension, preprocessing และ provenance/license เดียวกันระหว่าง document/query Hybrid search ใช้ appropriate normalization/rank pipeline และ judgments ไม่บวก raw scores ต่างสเกลโดยไม่มีเหตุผล
