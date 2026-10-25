# สรุปการปรับข้อกำหนดเป็นสองแนวทาง

บันทึกนี้เป็นผล requirements-only update ก่อน migration; รายการที่ระบุว่ายังไม่ implement สะท้อนเวลานั้น ปัจจุบัน Chapter 01 migrate แล้ว ดู [current verification](../01-basic-connection/VERIFICATION.md)

วันที่: 2026-10-25T09:16:14+07:00
Scope: requirements, documentation, agent workflow และ chapter coverage status ผู้ใช้ต้องการ Spring Data OpenSearch ร่วมกับ official async client

## เปลี่ยนข้อกำหนดอะไร

| ประเด็น | เดิม | ใหม่ |
|---|---|---|
| Learning approaches | Official async client เส้นทางเดียว | Spring Data OpenSearch reactive + official OpenSearchAsyncClient |
| Chapter organization | Standalone Maven project ต่อบท | คง standalone และมีสองแนวทางภายใน project เดียว |
| API conventions | Chapter 01 ใช้ /api/cluster/* | Target /api/spring-data/* และ /api/java-client/*; routes เดิมยังไม่เปลี่ยน |
| Data/mapping | Explicit client mapping | เปรียบเทียบ Spring Data mapping/converters กับ explicit client ด้วย equivalent fixtures |
| Reactive rules | Lazy future bridge สำหรับ direct client | ตรวจและ defer eager invocation ของทั้งสอง path ไม่ถือว่า Mono lazy เสมอ |
| Verification | Proof ของ client path เดียว | Unit/HTTP/IT/smoke แยกสองแนวทางพร้อม parity/coverage |
| Advanced features | Direct client support | ตรวจ native/reactive-client/fallback/unsupported แยก ไม่สมมติ vector/hybrid support |
| Chapter 01 overall | Verified ตาม scope เดิม | Implemented เพราะ requirement ใหม่ยังไม่ครบ; คง historical official-only 25 tests |

## สิ่งที่เพิ่มและปรับ

- เพิ่ม [dual-approach contract](dual-approach.md) รวม naming, routes, transport/lifecycle, coverage/parity และเป้าหมายครบ 20 บท
- เพิ่ม [ADR 0004](decisions/0004-two-learning-approaches.md); เปลี่ยน ADR 0002 เป็น Superseded เฉพาะ single-path choice โดยเก็บประวัติ
- ปรับ AGENTS, CONTRIBUTING, architecture, engineering/reactive/testing/verification/chapter standards และ troubleshooting
- ปรับ compatibility ให้ Spring Data 3.1.4 เป็น candidate แบบ Documented/Unresolved locally; ไม่อ้างว่า resolve หรือ runtime ผ่านแล้ว
- ปรับ plan/implement/review skills และ templates ให้ครอบคลุมสองแนวทาง พร้อมแยก historical evidence จาก requirement ใหม่
- ปรับ root roadmap, Chapter 01 README/report และ PR template ให้สะท้อน coverage ตามจริง

## Chapter 01 ที่ยังต้อง implement

1. Resolve Spring Data candidate กับ Boot 4.1.2 และตรวจ transitive dependencies/configuration
2. สร้าง Spring Data reactive wiring และ transport owner ที่ถูกต้อง
3. แยก services/controllers สอง prefixes และตัดสินใจ legacy-route policy
4. เพิ่ม unit/HTTP/real OpenSearch IT และ packaged smoke ของ Spring Data พร้อม cross-path parity
5. Full clean verify, dependency/API evidence และ smoke/error checks ใหม่ ก่อนกลับเป็น Verified

ไม่ได้เพิ่ม Spring Data dependency, Java classes, endpoints หรือ tests ในครั้งนี้ ไม่ได้เปลี่ยน code/config/Wrapper/Compose ของบทเดิม ไม่มี Chapter 02 และไม่มี commit/push

## Compatibility findings และข้อจำกัด

Official matrix ของ Spring Data OpenSearch v3.1.4 ระบุสาย 3.1.x กับ Boot 4.1.x, Spring Data Elasticsearch 6.1.x และ JDK 21+ ยังไม่ใช่หลักฐาน linkage/feature support ของ exact repository stack อ่าน [compatibility](compatibility.md)

Candidate reactive configuration ใช้ RestClient-based default และ ReactiveOpenSearchClient.info เริ่ม async invocation ก่อนคืน Mono ต้องตรวจ transport override และใช้ deferred invocation ตาม contract ไม่ประกาศว่าการ share Apache HttpClient 5 transport ใช้ได้แล้ว

## Verification ของการแก้ requirements

- `bash scripts/check-foundation.sh`: PASS พร้อม tooling regression tests 4 cases
- `quick_validate.py` ของ skill-creator: PASS ทั้ง plan-chapter, implement-chapter และ review-chapter
- Local Markdown file links/final newlines: PASS รวม Chapter 01 README/report
- Runnable-source fingerprint: ตรงกับ historical report `b6b397dc35a6375cee0545741720320753fa635376cd08276b9ff1a606356d62` ยืนยัน code/tests/POM/Compose/Wrapper ไม่เปลี่ยน
- ตรวจ roadmap และ report ว่า Chapter 01 เป็น Implemented / dual-path NOT VERIFIED ตาม requirement ใหม่
- ไม่รัน Maven/Docker หรือ hosted CI ซ้ำสำหรับ documentation-only change และไม่อ้างผล Spring Data runtime

## Files changed

- `.agents/skills/implement-chapter/SKILL.md`
- `.agents/skills/plan-chapter/SKILL.md`
- `.agents/skills/review-chapter/SKILL.md`
- `.github/pull_request_template.md`
- `01-basic-connection/README.md`
- `01-basic-connection/VERIFICATION.md`
- `AGENTS.md`
- `CONTRIBUTING.md`
- `README.md`
- `docs/architecture.md`
- `docs/chapter-contract.md`
- `docs/compatibility.md`
- `docs/decisions/0002-async-opensearch-client.md`
- `docs/decisions/0004-two-learning-approaches.md`
- `docs/dual-approach.md`
- `docs/engineering-standards.md`
- `docs/reactive-opensearch.md`
- `docs/reference-analysis.md`
- `docs/requirements-update.md`
- `docs/templates/chapter-plan.md`
- `docs/templates/chapter-readme.md`
- `docs/templates/chapter-review.md`
- `docs/templates/chapter-verification.md`
- `docs/testing-standards.md`
- `docs/troubleshooting.md`
- `docs/verification-standards.md`
