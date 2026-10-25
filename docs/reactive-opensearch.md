# Async OpenSearch กับ Spring WebFlux

ใช้ Spring Data OpenSearch reactive และ official OpenSearchAsyncClient สอง path ตาม [ADR 0004](decisions/0004-two-learning-approaches.md) Official path ใช้ Apache HttpClient 5 async transport Async I/O ไม่ทำให้ serialization, callbacks หรือ CPU-heavy work หายไป

## Spring Data reactive path

ใช้ reactive repository/operations/template หรือ ReactiveOpenSearchClient ตาม feature ที่ตรวจพบจริง ห้ามเรียก synchronous repository/ElasticsearchOperations ใน event loop แล้วครอบ Mono ภายหลัง ตรวจว่าตัว transport ส่ง async request จริง

Candidate 3.1.4 มีบาง methods เช่น ReactiveOpenSearchClient.info ที่เรียก performRequestAsync ก่อนคืน Mono จึงใช้ `Mono.defer(() -> springDataClient.info())` เมื่ออยากได้ cold operation นี่เป็นข้อกำหนด application ไม่ใช่คำรับรองว่า reactive library ทุก method lazy อยู่แล้ว

ทดสอบทั้งสอง path ว่าไม่มี I/O ก่อน subscribe และ invocation ใหม่เมื่อ retry การ close reactive client ไม่จำเป็นต้องเป็น owner ของ transport ต้องตรวจ configuration/source และกำหนด close owner เดียว ไม่ close ทุก bean ที่อ้าง transport เดียวกัน

Default configuration ของ Spring Data candidate ใช้ RestClient-based transport การย้ายไป Apache HttpClient 5 ต้องตรวจ compatibility/transport options ก่อน ไม่ copy configuration เดิมมาบังคับใช้โดยไม่ทดสอบ ดู [sources และ coverage policy](dual-approach.md)

## Official client lazy bridge

Client invocation ต้องอยู่ภายใน deferred publisher จึงไม่เกิด request ตอน assembly และ retry ต้องสร้าง future ใหม่ บาง client methods throw IOException ก่อนคืน future ต้องส่งเป็น Reactor error เช่นเดียวกับ future ที่ complete exceptionally ตรวจ signature ของ exact client version ก่อนเขียนโค้ด

รูปแบบต่อไปนี้เป็นแนวทาง ไม่ใช่ compiled implementation ของ repository:

```java
return Mono.defer(() -> {
    try {
        return Mono.fromFuture(asyncClient.search(request, Product.class));
    } catch (IOException exception) {
        return Mono.error(exception);
    }
});
```

`Mono.fromFuture(asyncClient.search(...))` ที่อยู่นอก defer อาจเริ่มงานก่อน subscribe; การ retry publisher ที่เก็บ future เดิมจะไม่ได้ยิง request ใหม่ ตรวจ synchronous runtime exceptions และ CompletionException wrapping เมื่อออกแบบ error mapping ไม่ unwrap ทุก Throwable จนเสียสาเหตุ

## Threads และ resource lifecycle

ห้าม block/join/get, manual subscribe หรือ sync OpenSearch I/O ใน controller/service pipeline ไม่ใช้ supplyAsync ครอบ sync client แล้วเรียกว่า async transport ไม่ใส่ boundedElastic รอบ async calls โดยไม่มีเหตุผล Ancillary blocking file/model work ต้องแยก scheduler อย่างมีขอบเขตเมื่อจำเป็น

Transport singleton มีเจ้าของ lifecycle ชัดเจน ปิดครั้งเดียวผ่าน Spring shutdown ไม่สร้างต่อ request Callbacks อาจทำงานบน transport threads อย่าทำ CPU-heavy processing ขนาดใหญ่บน callback โดยไม่ประเมินผล

## Cancellation และ deadlines

Reactor bridge อาจ propagate cancellation ไป CompletableFuture ตาม overload/version ไม่รับประกัน HTTP request abort หรือ server-side write rollback แยก unit assertion ว่า future ถูก cancel ออกจาก transport integration evidence

กำหนด pool acquisition, connect/response timeout และ Reactor overall deadline ให้สัมพันธ์กัน Deadline ไม่เท่ากับยกเลิก operation สำเร็จ Retry ใช้ capped attempts, backoff/jitter และ error filter คิดรวม transport-level retries ด้วย หลีกเลี่ยง retry writes ที่ outcome ไม่ชัดโดยไม่มี idempotency

## Backpressure และ memory

Flux จาก search hits ไม่ใช่ streaming OpenSearch response จำกัด response page size, bulk batch count/bytes, flatMap concurrency และ buffers ไม่ collectAll ผลลัพธ์ไม่จำกัด Pagination ต้องควบคุมจำนวนหน้าที่ in-flight และปิด PIT/scroll ตาม lifecycle รวมทั้ง error/cancel

Bulk HTTP 200 อาจมี item failures ทดสอบ partial success และ retry เฉพาะส่วนที่ปลอดภัย ไม่ retry ทั้ง batch โดยอัตโนมัติ

## หลักฐานที่ต้องมี

StepVerifier/Mockito แยกสอง approach พิสูจน์ no invocation ก่อน subscribe, fresh call เมื่อ retry, sync throw, async failure และ cancellation ของ bridge Real-server tests พิสูจน์ mapping/query/transport behavior ที่เกี่ยวข้อง การตรวจชื่อ thread อย่างเดียวไม่พิสูจน์ว่าไม่มี blocking

แหล่งอ้างอิง: [client source](https://github.com/opensearch-project/opensearch-java/blob/main/java-client/src/main/java/org/opensearch/client/opensearch/OpenSearchAsyncClient.java), [Java transport](https://docs.opensearch.org/latest/clients/java/), [Reactor Mono API](https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Mono.html) ตรวจเทียบ resolved versions เสมอ
