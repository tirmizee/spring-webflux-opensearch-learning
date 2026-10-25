# Learning roadmap

`Planned` = ยังไม่ implement · `Implemented` = มี implementation แต่ยัง verify ไม่ครบ · `Verified` = ทุก mandatory gate ผ่านพร้อมหลักฐานของ source state ที่ระบุ

| # | Chapter | เรียนรู้และพิสูจน์ | Status |
|---|---|---|---|
| 01 | [01-basic-connection](../01-basic-connection/) | สองแนวทางของ cluster info/health, shared lifecycle, failure และ parity | Verified |
| 02 | [02-security-and-tls](../02-security-and-tls/) | TLS, authentication, least privilege และ forbidden operations | Verified |
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

ทุกบทต้องมีสองแนวทางและ coverage comparison ตาม [dual-approach contract และ roadmap coverage](dual-approach.md) Advanced features ต้องระบุ abstraction gaps/fallback ตามหลักฐาน ไม่สมมติ Spring Data native support

Tests มีตั้งแต่บทแรก Chapter 16 เจาะลึก testing ทุกบทมี setup ของตัวเอง ไม่ต้องรันบทก่อนหน้า ลำดับข้างต้นเป็นคำแนะนำด้านความรู้

บท 18 ใช้ fixed vectors สอน API/คณิตศาสตร์ บท 19–20 ใช้ real precomputed embeddings ที่ระบุ model และ license; default path ไม่ต้องใช้ paid APIs การรัน model และ advanced load/tuning เป็น optional exercises ผล hybrid search ต้องประเมินจาก judgments ไม่รับประกันว่าดีกว่าเสมอ

## Coverage เป้าหมายของสองแนวทาง

ตารางนี้แสดง design targets ไม่ใช่คำยืนยัน API support หรือ implementation status สถานะบทอยู่ในตาราง roadmap ด้านบน

| Chapter | Spring Data OpenSearch target | Official Java Client target / comparison |
|---|---|---|
| 01 Connection | Reactive client metadata/cluster APIs; ตรวจ wiring | Async info/health และ error/lifecycle parity |
| 02 Security | Reactive configuration, trusted TLS และ app identity | Transport TLS/auth ด้วยสิทธิ์เดียวกัน |
| 03 Mappings | Mapping annotations, converter และ index operations | Explicit mappings/settings และเปรียบเทียบ generated mapping |
| 04 CRUD | Reactive repository และ operations | Async CRUD, ID/conflict/refresh parity |
| 05 Full-text | Operations/query abstractions ที่รองรับ | Typed match/multi_match/phrase และ relevance assertions |
| 06 Filtering | Reactive query/filter composition | Bool/term/range boundaries บน fixtures เดียวกัน |
| 07 Analyzers | Index/settings integration และ query behavior | Explicit analyzer setup/_analyze; ระบุ low-level boundary |
| 08 Pagination | Reactive sorting/page/cursor support ที่ตรวจได้ | search_after/PIT, tie-breaker และ cleanup |
| 09 Aggregations | Supported aggregation containers/conversion | Typed aggregations, buckets/counts และ pagination |
| 10 Autocomplete | Supported query abstractions; ระบุ gaps | Prefix/ngram/completion/fuzzy queries |
| 11 Bulk | Reactive saveAll/bulk behavior และ partial errors | Explicit batch/item errors และ concurrency bounds |
| 12 Resilience | Spring Data exception translation/deadlines | Client exception mapping, fresh retry และ shared contract |
| 13 Concurrency | Entity/version metadata และ partial updates | seq_no/primary_term, conflict/upsert semantics |
| 14 Lifecycle | Available index/alias/reindex abstractions | Explicit admin requests และ safe cutover |
| 15 Nested/grouping | Mapping/query/inner_hits conversion | Nested/collapse requests และ grouping parity |
| 16 Testing | Reactive repository/template/client tests | Async adapter tests และ cross-path full integration |
| 17 Observability | Abstraction/converter overhead และ metrics | Transport metrics กับ bounded workload เดียวกัน |
| 18 Vector | ตรวจ vector mapping/query support ก่อน implement | Exact/ANN baseline และ known-neighbor assertions |
| 19 Semantic | Embedding-field mapping/retrieval ที่รองรับ | Ingest/search requests พร้อม model provenance |
| 20 Hybrid | ตรวจ hybrid/pipeline support; explicit gap policy | Hybrid query/pipeline และ relevance judgments |
