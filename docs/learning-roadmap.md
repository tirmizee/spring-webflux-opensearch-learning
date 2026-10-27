# Learning roadmap

`Planned` = ยังไม่ implement · `Implemented` = มี implementation แต่ยัง verify ไม่ครบ · `Verified` = ทุก mandatory gate ผ่านพร้อมหลักฐานของ source state ที่ระบุ

| # | Chapter | เรียนรู้และพิสูจน์ | Status |
|---|---|---|---|
| 01 | [01-basic-connection](../01-basic-connection/) | สองแนวทางของ cluster info/health, shared lifecycle, failure และ parity | Verified |
| 02 | [02-security-and-tls](../02-security-and-tls/) | TLS, authentication, least privilege และ forbidden operations | Verified |
| 03 | [03-api-key-authentication](../03-api-key-authentication/) | API Key lifecycle ผ่าน starter auto-configuration + customizer และ JVM trust | Verified |
| 04 | [04-index-mappings-and-settings](../04-index-mappings-and-settings/) | Starter-only: explicit mappings, converter, dynamic/final settings และ invalid document types | Verified |
| 05 | [05-document-crud](../05-document-crud/) | Starter-only: reactive repository/operations CRUD, atomic create conflict และ refresh visibility | Verified |
| 06 | `06-full-text-search` | Match, multi-match, phrase, BM25 และ boosting | Planned |
| 07 | `07-filtering-and-bool-queries` | Term/terms, range, exists และ query/filter context | Planned |
| 08 | `08-analyzers-and-thai-search` | Analyzers, normalizers, Thai text และ token assertions | Planned |
| 09 | `09-sorting-and-pagination` | Stable sort, search_after, PIT และ cleanup | Planned |
| 10 | `10-aggregations-and-facets` | Buckets, metrics, facets และ composite pagination | Planned |
| 11 | `11-autocomplete-and-fuzzy-search` | Prefix, edge n-gram, completion และ fuzziness | Planned |
| 12 | `12-bulk-ingestion` | Batching, bounded concurrency, item failures และ selective retry | Planned |
| 13 | `13-errors-timeouts-and-retries` | Timeouts, retry budget, cancellation และ error mapping | Planned |
| 14 | `14-concurrency-and-partial-updates` | Partial update, upsert และ optimistic concurrency | Planned |
| 15 | `15-aliases-and-reindexing` | Versioned indices, reindex และ controlled alias cutover | Planned |
| 16 | `16-nested-queries-and-field-collapse` | Nested vs object, inner_hits และ grouped results | Planned |
| 17 | `17-testcontainers-and-integration-testing` | Container lifecycle, dynamic config, isolation และ fixtures | Planned |
| 18 | `18-observability-and-performance` | Metrics, pool behavior, slow queries และ bounded load | Planned |
| 19 | `19-vector-search-fundamentals` | knn_vector, distance, dimensions และ exact/approximate neighbors | Planned |
| 20 | `20-semantic-search-and-embeddings` | Embedding provenance, model metadata และ semantic retrieval | Planned |
| 21 | `21-hybrid-search-and-relevance` | BM25 + vector, search pipeline และ relevance evaluation | Planned |

บททั่วไปมีสองแนวทางและ coverage comparison ตาม [dual-approach contract และ roadmap coverage](dual-approach.md) Chapter 03–05 มี approved starter-only exceptions ใน AGENTS.md Advanced features ต้องระบุ abstraction gaps/fallback ตามหลักฐาน ไม่สมมติ Spring Data native support

Tests มีตั้งแต่บทแรก Chapter 17 เจาะลึก testing ทุกบทมี setup ของตัวเอง ไม่ต้องรันบทก่อนหน้า ลำดับข้างต้นเป็นคำแนะนำด้านความรู้

บท 19 ใช้ fixed vectors สอน API/คณิตศาสตร์ บท 20–21 ใช้ real precomputed embeddings ที่ระบุ model และ license; default path ไม่ต้องใช้ paid APIs การรัน model และ advanced load/tuning เป็น optional exercises ผล hybrid search ต้องประเมินจาก judgments ไม่รับประกันว่าดีกว่าเสมอ

## Coverage เป้าหมายของสองแนวทาง

ตารางนี้แสดง design targets ไม่ใช่คำยืนยัน API support หรือ implementation status สถานะบทอยู่ในตาราง roadmap ด้านบน

| Chapter | Spring Data OpenSearch target | Official Java Client target / comparison |
|---|---|---|
| 01 Connection | Reactive client metadata/cluster APIs; ตรวจ wiring | Async info/health และ error/lifecycle parity |
| 02 Security | Reactive configuration, trusted TLS และ app identity | Transport TLS/auth ด้วยสิทธิ์เดียวกัน |
| 03 API Key | Starter: auto client/transport/template/converter + API Key customizer | ไม่มี second path ตาม approved exception |
| 04 Mappings | Starter: annotations/converter/index operations; dynamic settings ใช้ reactive-client tier | ไม่มี second path; ตรวจ generated mapping/settings และ native server failures |
| 05 CRUD | Starter: repository save/find, operations delete, reactive-client create-only และ refresh visibility | ไม่มี second path ตาม approved exception |
| 06 Full-text | Operations/query abstractions ที่รองรับ | Typed match/multi_match/phrase และ relevance assertions |
| 07 Filtering | Reactive query/filter composition | Bool/term/range boundaries บน fixtures เดียวกัน |
| 08 Analyzers | Index/settings integration และ query behavior | Explicit analyzer setup/_analyze; ระบุ low-level boundary |
| 09 Pagination | Reactive sorting/page/cursor support ที่ตรวจได้ | search_after/PIT, tie-breaker และ cleanup |
| 10 Aggregations | Supported aggregation containers/conversion | Typed aggregations, buckets/counts และ pagination |
| 11 Autocomplete | Supported query abstractions; ระบุ gaps | Prefix/ngram/completion/fuzzy queries |
| 12 Bulk | Reactive saveAll/bulk behavior และ partial errors | Explicit batch/item errors และ concurrency bounds |
| 13 Resilience | Spring Data exception translation/deadlines | Client exception mapping, fresh retry และ shared contract |
| 14 Concurrency | Entity/version metadata และ partial updates | seq_no/primary_term, conflict/upsert semantics |
| 15 Lifecycle | Available index/alias/reindex abstractions | Explicit admin requests และ safe cutover |
| 16 Nested/grouping | Mapping/query/inner_hits conversion | Nested/collapse requests และ grouping parity |
| 17 Testing | Reactive repository/template/client tests | Async adapter tests และ cross-path full integration |
| 18 Observability | Abstraction/converter overhead และ metrics | Transport metrics กับ bounded workload เดียวกัน |
| 19 Vector | ตรวจ vector mapping/query support ก่อน implement | Exact/ANN baseline และ known-neighbor assertions |
| 20 Semantic | Embedding-field mapping/retrieval ที่รองรับ | Ingest/search requests พร้อม model provenance |
| 21 Hybrid | ตรวจ hybrid/pipeline support; explicit gap policy | Hybrid query/pipeline และ relevance judgments |
