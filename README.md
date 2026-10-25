# spring-webflux-opensearch-learning

Repository สำหรับเรียน Java 21, Spring Boot 4.x, Spring WebFlux และ OpenSearch 3.x แบบ hands-on ผ่าน **Spring Data OpenSearch reactive** และ **official OpenSearchAsyncClient** สองแนวทาง แต่ละ chapter เป็น Maven project อิสระ มี configuration, fixtures และ tests ของตัวเอง

**สถานะปัจจุบัน: Chapter 01 ผ่าน verification สองแนวทางแล้ว** Spring Data reactive และ official async client ผ่าน 49 tests พร้อม real OpenSearch/packaged-app smoke และ parity checks ผลเดิม 25 tests เก็บเป็น historical report ผู้ใช้อนุมัติ Boot-managed JUnit 6 แล้ว ดู [compatibility](docs/compatibility.md) และ [Chapter 01 verification](01-basic-connection/VERIFICATION.md) สำหรับผลจริง

## Learning roadmap

`Planned` = ยังไม่ implement · `Implemented` = มี implementation แต่ยัง verify ไม่ครบ · `Verified` = ทุก mandatory gate ผ่านพร้อมหลักฐานของ source state ที่ระบุ

| # | Chapter | เรียนรู้และพิสูจน์ | Status |
|---|---|---|---|
| 01 | [01-basic-connection](01-basic-connection/) | สองแนวทางของ cluster info/health, shared lifecycle, failure และ parity | Verified |
| 02 | `02-security-and-tls` | TLS, authentication, least privilege และ forbidden operations | Planned |
| 03 | `03-index-mappings-and-settings` | Explicit mappings, settings และ invalid document types | Planned |
| 04 | `04-document-crud` | Create/get/update/delete, conflicts และ refresh visibility | Planned |
| 05 | `05-full-text-search` | Match, multi-match, phrase, BM25 และ boosting | Planned |
| 06 | `06-filtering-and-bool-queries` | Term/terms, range, exists และ query/filter context | Planned |
| 07 | `07-analyzers-and-thai-search` | Analyzers, normalizers, Thai text และ token assertions | Planned |
| 08 | `08-sorting-and-pagination` | Stable sort, search_after, PIT และ cleanup | Planned |
| 09 | `09-aggregations-and-facets` | Buckets, metrics, facets และ composite pagination | Planned |
| 10 | `10-autocomplete-and-fuzzy-search` | Prefix, edge n-gram, completion และ fuzziness | Planned |
| 11 | `11-bulk-ingestion` | Batching, bounded concurrency, item failures และ selective retry | Planned |
| 12 | `12-errors-timeouts-and-retries` | Timeouts, retry budget, cancellation และ error mapping | Planned |
| 13 | `13-concurrency-and-partial-updates` | Partial update, upsert และ optimistic concurrency | Planned |
| 14 | `14-aliases-and-reindexing` | Versioned indices, reindex และ controlled alias cutover | Planned |
| 15 | `15-nested-queries-and-field-collapse` | Nested vs object, inner_hits และ grouped results | Planned |
| 16 | `16-testcontainers-and-integration-testing` | Container lifecycle, dynamic config, isolation และ fixtures | Planned |
| 17 | `17-observability-and-performance` | Metrics, pool behavior, slow queries และ bounded load | Planned |
| 18 | `18-vector-search-fundamentals` | knn_vector, distance, dimensions และ exact/approximate neighbors | Planned |
| 19 | `19-semantic-search-and-embeddings` | Embedding provenance, model metadata และ semantic retrieval | Planned |
| 20 | `20-hybrid-search-and-relevance` | BM25 + vector, search pipeline และ relevance evaluation | Planned |

ทุกบทต้องมีสองแนวทางและ coverage comparison ตาม [dual-approach contract และ roadmap coverage](docs/dual-approach.md) Advanced features ต้องระบุ abstraction gaps/fallback ตามหลักฐาน ไม่สมมติ Spring Data native support

Tests มีตั้งแต่บทแรก Chapter 16 เจาะลึก testing ทุกบทมี setup ของตัวเอง ไม่ต้องรันบทก่อนหน้า ลำดับข้างต้นเป็นคำแนะนำด้านความรู้

บท 18 ใช้ fixed vectors สอน API/คณิตศาสตร์ บท 19–20 ใช้ real precomputed embeddings ที่ระบุ model และ license; default path ไม่ต้องใช้ paid APIs การรัน model และ advanced load/tuning เป็น optional exercises ผล hybrid search ต้องประเมินจาก judgments ไม่รับประกันว่าดีกว่าเสมอ

## เริ่มใช้งาน foundation

ต้องมี Bash และ Python 3.9+ สำหรับ foundation checks ส่วน Chapter 01 ต้องใช้ Java 21, Maven Wrapper และ Docker Compose ตาม [README ของบท](01-basic-connection/README.md)

```bash
bash scripts/check-foundation.sh
```

คำสั่งนี้ตรวจไฟล์ส่วนกลาง, relative file links, roadmap และ skill metadata ไม่รัน Java หรือ Docker อ่าน README ของบทก่อน แล้วรัน full Maven verification จาก repository root:

```bash
bash scripts/verify-chapter.sh 01-basic-connection
```

## เอกสารและ workflow

- [Contributing](CONTRIBUTING.md) และ [Codex instructions](AGENTS.md)
- [ข้อกำหนดสองแนวทาง](docs/dual-approach.md) และ [สรุปการเปลี่ยนข้อกำหนด](docs/requirements-update.md)
- [Architecture](docs/architecture.md), [chapter contract](docs/chapter-contract.md) และ [compatibility](docs/compatibility.md)
- [Engineering](docs/engineering-standards.md), [reactive integration](docs/reactive-opensearch.md), [testing](docs/testing-standards.md) และ [verification](docs/verification-standards.md)
- [Troubleshooting](docs/troubleshooting.md) และ [reference analysis](docs/reference-analysis.md)
- [Foundation verification](docs/foundation-verification.md)

ใช้ `$plan-chapter`, `$implement-chapter` หรือ `$review-chapter` ตามขอบเขตงาน ดูไฟล์ใน [.agents/skills](.agents/skills/) Templates อยู่ใน [docs/templates](docs/templates/)

ยังไม่เลือก repository license และยังไม่คัดลอก source code หรือ datasets จาก reference ต้องตรวจสิทธิ์ก่อน reuse
