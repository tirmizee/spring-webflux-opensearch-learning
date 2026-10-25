# Reference analysis

อ้างอิง [spring-webflux-elasticsearch-learning](https://github.com/tirmizee/spring-webflux-elasticsearch-learning) จากการอ่านใน planning phase วันที่ 2026-10-25 ไม่ได้ clone/copy source หรือรัน tests ของ repository ต้นทาง

อ่าน root README, chapter tree, Chapter 01 pom/service, Chapter 05 README/verification และ .kiro/steering workflow/structure/standards

| Convention | แนวทางที่เลือก |
|---|---|
| Chapter อยู่ root และเป็น independent Maven project | คงไว้ ไม่มี root aggregator |
| Thai README, English terms/code | คงไว้ |
| README + Compose + VERIFICATION แยกบท | คงไว้ เพิ่ม Wrapper และ reusable templates |
| Planned/Implemented/Verified | คงไว้ ไม่อ้าง Verified ของ reference เป็นผลของเรา |
| Spring Data + synchronous official client สองเส้นทาง | ข้อกำหนดล่าสุดคงสองแนวทาง โดยใช้ Spring Data OpenSearch reactive และ official OpenSearchAsyncClient |
| .kiro/steering | ใช้ AGENTS.md และ repository-local Codex skills |
| ก่อนหน้าต้อง completed | ไม่ใช้เป็น gate เพราะบทต้อง independent |
| Elasticsearch API key workflow | ออกแบบใหม่ตาม OpenSearch Security/version |

ตัวอย่าง [JavaClientConnectionService](https://github.com/tirmizee/spring-webflux-elasticsearch-learning/blob/main/01-basic-connection/src/main/java/com/example/learning/basicconnection/service/JavaClientConnectionService.java) ใช้ synchronous client ที่ offload ไป boundedElastic เป็นการแยก blocking work ไม่ใช่ native async I/O โครงการนี้ใช้ nonblocking paths ทั้งสอง approach และ lazy invocation โดยเก็บ [single-path decision เดิม](decisions/0002-async-opensearch-client.md) เป็นประวัติที่ถูกแทนด้วย [ADR 0004](decisions/0004-two-learning-approaches.md)

Sources: [README](https://github.com/tirmizee/spring-webflux-elasticsearch-learning/blob/main/README.md), [structure](https://github.com/tirmizee/spring-webflux-elasticsearch-learning/blob/main/.kiro/steering/structure.md), [verification](https://github.com/tirmizee/spring-webflux-elasticsearch-learning/blob/main/.kiro/steering/verification-standards.md)

การใช้แนวคิด/conventions ไม่ใช่การอนุญาตให้ copy code ต้องตรวจ license/provenance ก่อน reuse ทั้ง code และ datasets
