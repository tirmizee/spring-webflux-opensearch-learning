# ADR 0002: Official async OpenSearch client

Status: Superseded เฉพาะ single-path choice โดย [ADR 0004](0004-two-learning-approaches.md) เมื่อ 2026-10-25

เนื้อหาด้านล่างคือ decision เดิมเพื่อเก็บประวัติ Native async I/O ยังคงใช้ แต่ข้อกำหนดปัจจุบันมีสองแนวทาง

## Decision

ใช้ OpenSearchAsyncClient และ Apache HttpClient 5 async transport ใน WebFlux request flow Bridge เป็น lazy Mono และสร้าง future ใหม่ต่อ subscription/retry ไม่มี synchronous client path หรือ Spring Data Elasticsearch dependency ใน baseline

## Consequences

ต้องเขียน configuration/mapping และ error translation เอง Async I/O ไม่รับประกัน cancellation ที่ server หรือ bounded memory จึงต้องกำหนด timeouts, concurrency และ lifecycle พร้อม tests ตาม [reactive standards](../reactive-opensearch.md)

หากจำเป็นต้องสอน blocking legacy integration ภายหลัง ให้เป็น explicit optional exercise ที่มีเหตุผลและ scope ไม่เปลี่ยน default architecture
