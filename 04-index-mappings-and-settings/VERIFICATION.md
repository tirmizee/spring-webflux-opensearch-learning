# Chapter 04 verification

สถานะ: **Verified** สำหรับ approved **starter-only / local HTTP / no authentication or TLS** scope ตาม AGENTS.md และ [contract exception](../docs/dual-approach.md#chapter-04-approved-starter-only-scope) ไม่อ้าง two-path parity หรือผลของ Chapter 01–03 แทนบทนี้

ตรวจวันที่ 2026-10-26, Asia/Bangkok (+07:00) Final Maven เสร็จ `08:27:17+07:00`; packaged examples/smoke และ cleanup เสร็จก่อน `08:31:03+07:00` เป็น historical execution evidence ไม่ได้หมายความว่า app ยังเปิดอยู่

## Source state และ artifacts

Branch `codex/chapter-03-api-key-authentication`, base HEAD `2de0641e01428df6629c2cce6c7d20ebbe3de81c` Chapter 04 เป็น **untracked** ทั้งบท พร้อม foundation docs/CI ที่แก้แต่ยังไม่ commit ไม่ใช้ HEAD เพียงอย่างเดียวอ้างถึง source ที่ทดสอบ ไม่มี commit/push ในงานนี้

ระหว่างทำงานพบ external formatting edit ที่ `03-api-key-authentication/src/main/java/com/example/learning/opensearch/apikey/exception/ApiExceptionHandler.java` เก็บไว้ ไม่แก้หรือรวมไว้ใน Chapter 04 fingerprint และไม่อ้าง verification ของไฟล์นั้น

Source fingerprint รวม **33 files** ทั้งที่ยังไม่ track: ทุก file ใน Chapter 04 ยกเว้น `target/`, local `.env` และ report นี้ (หลีกเลี่ยง self-reference) รวม source/tests/fixtures/Compose/POM/Wrapper/README/dotfiles เรียง path แบบ lexical แล้วสร้าง manifest รูปแบบ `SHA256(file bytes) + "  " + chapter-relative path + "\n"` ใช้ UTF-8/LF จากนั้น SHA-256 ของ manifest:

`1bc864f06e2da5081770a8e4e8942565b7d87f56a61c256cc0f7366034c13395`

Root context fingerprint (`AGENTS.md`, root README, compatibility, dual-approach, roadmap และ explicit CI matrix; algorithm เดียวกัน ใช้ repo-relative paths): `6effc13ba304cbc2b4c508259eac4a1e51b91bbca83b923da1d0f50d72015513`

Packaged JAR SHA-256: `e984dbff61b56624492034a678cd2218a2fb5dfc1b7b4b24549efe721c792ff6`; executable bit ของ `mvnw` คงไว้ Report/roadmap status updates เป็น evidence metadata ไม่มี behavior changes หลัง final Maven/smoke

## REVIEW

ใช้ `review-chapter` แบบ read-only เทียบ approved plan กับ source/config/tests/docs ตรวจ reactive operations/client tier, starter ownership, sanitized errors, mapping/converter, index namespace, CI และ documentation ก่อน final VERIFY

พบ P2 unknown PATCH fields อาจถูก ignore และ P2 smoke cleanup name mismatch รายงานก่อนแก้แล้ว: เปิด fail-on-unknown-properties พร้อม real HTTP IT/smoke case; track indexName จาก create response ใน cleanup REVIEW ซ้ำไม่พบ unresolved findings Compiler check พบ P3 deprecated Jackson test `asText()` เปลี่ยนเป็น `asString()` ตรวจซ้ำ read-only แล้วรัน full VERIFY ใหม่โดยคง assertions เดิม

## Environment / dependencies — PASS

| Component | Observed |
|---|---|
| OS / CPU | macOS 27.0.1 build 26A434 / Darwin arm64 |
| Application Java | Temurin 21.0.12.1+1 LTS, compiler release 21 |
| Maven / Wrapper | 3.9.11 / 3.3.4, distribution SHA-256 pin ใน wrapper properties |
| Docker client / Engine / Compose | 29.8.2 / 29.8.2 / v5.6.0 |
| Boot / Framework / Reactor | 4.1.2 / 7.0.10 / 3.8.8 |
| Spring Data starter / core / abstraction | 3.1.4 / 3.1.4 / Spring Data Elasticsearch 6.1.2 |
| OpenSearch Java SDK / REST client | 3.10.0 / 3.8.0 |
| HttpClient5 / HttpCore5 | 5.6.5 / 5.4.4 |
| Jackson | tools.jackson 3.1.7; SDK transitive runtime com.fasterxml.jackson 2.21.7 ยังอยู่ใน graph |
| JUnit / Mockito / Testcontainers | Boot-managed 6.0.3 / 5.23.0 / 2.0.5 |
| Compiler / Surefire / Failsafe plugins | 3.15.0 / 3.5.6 / 3.5.6 |
| Smoke tools | Bash, /usr/bin/curl, jq-1.7.1-apple |

Image `opensearchproject/opensearch:3.9.0`, inspected local architecture arm64, RepoDigest `opensearchproject/opensearch@sha256:adfa61f85025d06b4aeb562e7e74fde7e31c437039c93c3862c17e9acebd6c7c` เป็น digest ที่ inspect จริง ไม่อ้างว่า server/client ต้องใช้ minor version เดียวกัน

Actual commands (รันจาก chapter directory เว้นแต่ระบุ):

```bash
/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/bin/java -version
./mvnw -version
uname -sm
sw_vers
docker version --format '{{.Client.Version}} / {{.Server.Version}}'
docker compose version
docker image inspect opensearchproject/opensearch:3.9.0 --format '{{json .RepoDigests}} {{.Architecture}}'
jq --version
env JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw -B -ntp help:effective-pom -Doutput=target/effective-pom.xml dependency:tree -DoutputFile=target/dependency-tree.txt
```

Final commands exit 0 Dependencies log `/private/tmp/ch04-dependencies.log`; effective POM/tree อยู่ local ephemeral `target/` ไม่ commit พบ exclusions มีผล: ไม่มี Elasticsearch Java client/Rest5Client และไม่มี OpenSearch high-level client ใช้ starter-generated REST transport ไม่เพิ่ม library/framework นอก approved baseline

## Build/tests — PASS

```bash
env JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw -B -ntp clean verify -Dmaven.compiler.showDeprecation=true
```

Exit **0**, BUILD SUCCESS, elapsed **16.135s** Final log `/private/tmp/ch04-final-verify.log` ตรวจ actual Surefire/Failsafe XML:

| Suite | Tests | Failures | Errors | Skipped |
|---|---|---|---|---|
| ConfigurationTest | 18 | 0 | 0 | 0 |
| ProductControllerTest | 4 | 0 | 0 | 0 |
| ApiExceptionHandlerTest | 5 | 0 | 0 | 0 |
| ProductServicesTest | 9 | 0 | 0 | 0 |
| IndexMappingsIT | 6 | 0 | 0 | 0 |
| Total | 42 | 0 | 0 | 0 |

Unit/HTTP 36, IT 6 ใช้ pinned real OpenSearch container ที่ tests สร้างเอง random mapped port และ unique `ch04-it-*` ไม่พึ่ง Compose ผู้ใช้ ไม่มี skip/Assumptions ปิด required gates XML อยู่ `target/surefire-reports/` และ `target/failsafe-reports/` เป็น local ephemeral artifacts

Compiler เหลือ **1 deprecation warning**: `OpenSearchConfiguration.java` callback parameter `RestClientBuilder` เป็น deprecated API ของ exact starter release ไม่ suppress Default `RestClientTransport` deprecated เช่นกัน แต่ tests ใช้ชื่อ class เป็น string จึงไม่เพิ่ม compiler warning Mockito dynamic-agent/JVM warnings และ Netty macOS native DNS provider load warning ปรากฏระหว่าง tests; Netty fallback system resolver แล้ว tests ผ่าน ไม่ได้พิสูจน์ external DNS/native resolver ทุกกรณี

## Coverage ต่อ real OpenSearch — PASS

- Starter factory origins/client transport identity และ `ReactiveOpenSearchTemplate`; native shutdown หลัง context close ทำให้ request ใหม่ fail ตาม lifecycle
- Explicit create พร้อม mapping/settings, strict + `_class`, name text/raw keyword, category/long/integer/boolean/date; ID ไม่เก็บซ้ำใน `_source`
- Converter round-trip รวม UTC Instant/price; HTTP save/get และ native persisted source; explicit refresh + exact name.raw search พบ ID p-001
- Numeric strings `"10"`, `"abc"` ถูก server ปฏิเสธ 400 mapper_parsing_exception; unknown field ได้ strict_dynamic_mapping_exception; bad document ไม่ถูกบันทึก
- เปลี่ยน field type เดิมได้ 400; shard change ถูกปฏิเสธทั้ง open และ closed index (closed case ตรวจ final-setting rejection)
- Dynamic replicas/refresh เปลี่ยนแล้วอ่านกลับจริง, reset defaults; invalid values/unknown PATCH fields ได้ 400 และไม่เปลี่ยน settings
- ไม่มี startup/implicit business-index creation; app pre-create write ได้ 409 INDEX_NOT_READY, native write ได้ 404 และ HEAD ยืนยัน index ไม่เกิด
- Lifecycle duplicate/missing index/missing sample, unavailable endpoint ได้ sanitized 503; lazy/fresh retry/sync errors/cancellation/deadline ตรวจด้วย unit tests ไม่อ้างว่า cancellation พิสูจน์ HTTP abort

## Compose / packaged application / API / README — PASS

```bash
cp .env.example .env
docker compose config --quiet
docker compose up -d --wait opensearch
docker compose ps
curl -fsS 'http://localhost:9204/_cluster/health?wait_for_status=yellow&timeout=3s'
/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/bin/java -jar target/index-mappings-and-settings.jar
bash scripts/smoke-test.sh
```

คำสั่งที่จบทำงาน exit 0 Compose healthy bind `127.0.0.1:9204`; cluster_name learning-ch04, health green, timed_out=false, nodes=1 Packaged PID **65362** startup port **8084** ใช้ final JAR จริง log `/private/tmp/ch04-app.log` รัน foreground ในอีก terminal/session ตาม README readiness ยืนยันผ่าน smoke HTTP ไม่ถือ startup log แทน backend connectivity

Smoke `/private/tmp/ch04-smoke.log`: **31 HTTP status + payload assertions ผ่าน** ครบ 7 method/path contracts มี success/failure ของทุก endpoint, native data/mapping/type/setting failures และ explicit refresh/search คำสั่ง `bash -n scripts/smoke-test.sh` exit 0

รัน README fenced API block + native examples block แบบตรงข้อความผ่าน temporary Bash file `/private/tmp/ch04-readme-examples.sh` exit 0 log `/private/tmp/ch04-readme-examples.log`: pre-create409, create201, mapping/settings/save/get/dynamic settings200, numeric/unknown/type/shards400, term searchพบ p-001 Delete block `/private/tmp/ch04-readme-delete.sh` exit 0 ตอบ204 ตามเอกสาร Expected failure examples ใช้ cURL status และตรวจเทียบกับ actual errors ร่วมกับ smoke assertions ไม่ถือ shell exit0 อย่างเดียวพิสูจน์ HTTP400

Additional safety verification:

- เมื่อ README สร้าง index ไว้แล้ว `bash scripts/smoke-test.sh` exit **1 ตาม expected guard** (HTTP200 index exists) ตรวจ native p-001 ยังคง found=true/priceSatang=129900 จากนั้น explicit DELETE ของข้อมูลที่งานนี้สร้างได้204 และ native404 Log `/private/tmp/ch04-preexisting-smoke.log`
- เปิด final JAR ชั่วคราว `--server.port=8085 --learning.index-name=ch04-smoke-other` PID **65673** แล้ว `APP_URL=http://localhost:8085 bash scripts/smoke-test.sh` exit **1 ตาม expected mismatch** ตรวจ native ch04-smoke-other404 ยืนยัน trap cleanup index ที่เพิ่งสร้าง Log `/private/tmp/ch04-other-app.log` และ `/private/tmp/ch04-mismatch-smoke.log`

## Cleanup — PASS

ส่ง Ctrl+C ให้ owned packaged sessions ทั้ง PID65362/65673 process exit130 ตาม signal logs มี Graceful shutdown complete ทั้งคู่ แล้วรัน:

```bash
docker compose down
docker compose ps
docker volume inspect os-learning-ch04_opensearch-data --format '{{.Name}}'
docker ps --format '{{.Names}} {{.Image}}'
```

Exit0 Chapter04 container/network ถูก remove, compose ps ว่าง, volume `os-learning-ch04_opensearch-data` ยังอยู่ ไม่ใช้ `-v` Testcontainers cleanup เอง เหลือ user-owned `os-learning-ch03-opensearch-1` ตามเดิม ไม่หยุด/แก้ cluster นั้น ไม่มี product smoke index ค้างก่อนหยุด Compose

## Historical attempts / limits

| Attempt | Outcome ที่สังเกตจริง |
|---|---|
| Restricted unit run `/private/tmp/ch04-unit.log` | FAIL:31 tests,13 Mockito attach errors จาก sandbox; ไม่ใช้เป็น final evidence |
| First unrestricted unit `/private/tmp/ch04-unit-unrestricted.log` | FAIL:31 tests,1 cancellation test ยกเลิกก่อน downstream subscribe; แก้ให้ยืนยัน subscribed ก่อน cancel |
| Initial integration diagnostic | FAIL:assert คำว่า final ขณะ open index แต่ server ตอบ non-dynamic; แก้ semantic assertion และเพิ่ม closed-index test ไม่ลบ coverage Log diagnostic ถูก rerun ทับแล้ว |
| Intermediate final run | PASS tests แต่พบ deprecated Jackson asText ใน tests; เปลี่ยน asString และ full rerun ได้ผล final42/0/0/0 ข้างบน |
| Shell orchestration ของ mismatch guard ครั้งแรก | FAIL จาก zsh reserved variable `status`; แก้เฉพาะ temporary command เป็น http_code แล้ว rerun ผ่าน ไม่ใช่ defect ของ Bash smoke script |
| Docker info จาก restricted multi-command ครั้งแรก | permission denied; rerun exact inspect/version commands นอก sandboxผ่าน ไม่ใช้ค่าที่อ่านไม่สำเร็จแทน evidence |

**NOT VERIFIED**: Windows/native Bash replacement, Linux/amd64 runtime, remote DNS/cluster, optional custom-port example19204, optional add-new-field exercise และ GitHub-hosted CI run ของ source นี้ ยังไม่ได้ push **N/A**: auth/TLS/API Key และ two-path parity ตาม approved scope ทุก mandatory gate ของบทผ่านบน platform/source ที่ระบุ ไม่รับประกัน write rollback/HTTP I/O abort เมื่อ Reactor timeout หรือ performance/load ของ production

## Source manifest

Inline manifest นี้เก็บหลักฐานของ files ใหม่โดยไม่ต้อง track/commit ก่อน ไม่รวม report นี้/target/local env:

```text
53e0cca722f4d32d61e0eccdf70aeb56512929cba7967097a080ab0e66a94671  .env.example
b93aeef4611294db24c8e8cd25c9bbcb3d7acf3ad65511051c0f3dd878b6affc  .gitignore
bb0b8f16b2661c3676ed5a6a15f5b11d864460c4e77cfb50c583f54708f944e7  .mvn/wrapper/maven-wrapper.properties
6e3e14a561450ee88c25678e7fdd2a68bbcb300bcf672a8f246e7d3bc9a4d5fd  README.md
d70642e6ad1cef08563414f0239648f8b1929b76841c31bbfdb01750593040aa  docker-compose.yml
2f2160b085c649895ab808b431a44978d95d6f62d2d02bc6a2fdf5f0b0e5e578  fixtures/product.json
cae96cef89ebea3531221f4ae17c23cf8edf67d00eae8306d4186ae1bbed4d02  mvnw
46eedb8419bd14fe70d5bb2916d7b6f51806e51b39d5b76a42610384ca929c1c  mvnw.cmd
24fe166f03273d910314c16ff79507fe3edd0bfaaa66f323b0ca551a98f6e09b  pom.xml
e5f1f607fbd32a1bc4d3e5650eda2a385f6036baa67756f742eb6c3036595317  scripts/smoke-test.sh
8d83470ba19b18a2ceddff1b9f3e37b6ca556a38de0c20b472d27d699593ec5b  src/main/java/com/example/learning/opensearch/mappings/MappingsApplication.java
2b31f560b10a77550323f2f22a5147bb57791c7b224d81bdf3203ac0f1bb4b58  src/main/java/com/example/learning/opensearch/mappings/config/LearningProperties.java
944ac8ce37517cb6a68f088ec48f3d71dfc7da31fcc5fc438548d947f87b7c52  src/main/java/com/example/learning/opensearch/mappings/config/OpenSearchConfiguration.java
bcef308bb257cad4b283b92555a21e044e6dd0c7c997a44fa3a2a5afc8d3c5b2  src/main/java/com/example/learning/opensearch/mappings/controller/ProductIndexController.java
50dc3eb25414a847bf71f03a7e3762467a76a5637a385ecd3b2f322fa515e7ce  src/main/java/com/example/learning/opensearch/mappings/controller/ProductSampleController.java
42a792e1bdca0e7a7ff521c99d74e5d1885aded541565c7de12dd3c09c9be3aa  src/main/java/com/example/learning/opensearch/mappings/domain/ProductDocument.java
479558dddbe4ebc93a7e5e854bed0b530d539c4f0e5a0bd0883ac725c9aae433  src/main/java/com/example/learning/opensearch/mappings/dto/CreateProductSampleRequest.java
eecf7999fecd08ac88a9b3cc28425d1d067d821611076f04ecb66ce3ca920d77  src/main/java/com/example/learning/opensearch/mappings/dto/IndexMappingResponse.java
cc71b10b65e3f7cef0a450a38bb1a34c124628629105e27f8a6a6943d456729d  src/main/java/com/example/learning/opensearch/mappings/dto/IndexOperationResponse.java
eebf36bebfc71668216a15e45d6c2ba93be752c33f8a732557ae6100e41c3c75  src/main/java/com/example/learning/opensearch/mappings/dto/IndexSettingsResponse.java
50077542e2da41a9fd7d06edf9e0417b81c50c08d9e0c2e9680d1eb5210fc8da  src/main/java/com/example/learning/opensearch/mappings/dto/ProductResponse.java
0fa28cdc45a98e11696a3e1af94d8d1db38074185f9d5ac146c86c905d51eec2  src/main/java/com/example/learning/opensearch/mappings/dto/UpdateIndexSettingsRequest.java
d7a5896cdab76b7246e9b1c2f34d35a8b7ce20469c3bbf2c97978b880df99196  src/main/java/com/example/learning/opensearch/mappings/exception/ApiExceptionHandler.java
29711d6ceb905ecb10cf9e5413484270959e2dc50d4bcba733852dc03dd010e1  src/main/java/com/example/learning/opensearch/mappings/exception/ChapterException.java
1ee805f0b1ed6900019ff9114407035bcf969d65a104e22e43c11d4c7cd61c5a  src/main/java/com/example/learning/opensearch/mappings/service/ProductIndexService.java
335ce9369dc92a69e5c8bd405699df94592aaa66d01d383b798075926f77073f  src/main/java/com/example/learning/opensearch/mappings/service/ProductSampleService.java
948f395042032179376c26a5dd34a1ded986a4f5e707a250408442ad23e2f6ca  src/main/resources/application.yml
2219b010653337eccc9186566f8adee79cdae7a9ebb0e71446bd4664ccea1cd4  src/main/resources/opensearch/product-index-settings.json
0d08e891222c57debf75d40ef33cd04c9f407d88b068145eeae353f3f468e3c8  src/test/java/com/example/learning/opensearch/mappings/config/ConfigurationTest.java
feb43c8e48e82f83982eec73d640b7b603e56755948fb7697f26d28597834853  src/test/java/com/example/learning/opensearch/mappings/controller/ProductControllerTest.java
2c3a77988110e6421f53d0f2fb701aeb54c8370eced7f7ea91c053582f33f8ee  src/test/java/com/example/learning/opensearch/mappings/exception/ApiExceptionHandlerTest.java
d5bd593afa043fcc34f72924c236f1f046faed7a12ae33ec3a42878906eb03cc  src/test/java/com/example/learning/opensearch/mappings/integration/IndexMappingsIT.java
875de408ce210e858be70d7d3f9390c1401c32a61e627c19cfb78b8cf6911bfd  src/test/java/com/example/learning/opensearch/mappings/service/ProductServicesTest.java
```

Root context manifest:

```text
bc88439f17ac4ccdd10a11e4ba3e10265b679844d9c1cedd1fa237797365eaf0  .github/workflows/chapter-verification.yml
c9ee5c77bfd33f7f5eb470b258da9e7e3e5e39aebab75d0f528797b79e42b8a7  AGENTS.md
0b076cb86133179fb844e153d292fac3eb1f16e37d4cf3a5de33505262b5e4de  README.md
3f5cf893b697cd5377436c69da40450e4ec00a1092da2d02457c0caa6d80baf0  docs/compatibility.md
af345624eb09111d3a279fd44983be895468714d952d0fc8f72f55de7233f8f3  docs/dual-approach.md
630c60172f1a5c0eac5e3068b48049337ae2135e0540d18226cdafab6c6d8326  docs/learning-roadmap.md
```

## Publication source amendment — 2026-10-26

ผู้ใช้อนุญาต commit/push และเปิด PR หลัง implementation สร้าง branch `codex/chapter-04-index-mappings-and-settings` จาก `origin/main` ที่ `9f6a300` ซึ่ง merge Chapter 03 แล้ว ข้อความ branch/dirty/no-push ด้านบนเป็นสถานะขณะ verification เดิม

พบ edit หลัง verification เพียง file เดียว: `src/main/java/com/example/learning/opensearch/mappings/service/ProductSampleService.java` เปลี่ยน return type ของ save จาก `Mono<ProductResponse>` เป็น `Mono<ProductResponse >` (เพิ่ม whitespace หนึ่งตัว) เก็บ edit นี้ไว้ ตรวจ diff และ whitespace-free bytes เทียบ source เดิมแล้วเหมือนกัน ไม่มี Java token/behavior เปลี่ยน อีก 32 files ตรงกับ original manifest ไม่อ้างว่ารัน full tests/smoke ใหม่หลัง formatting นี้

PASS: `env JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw -B -ntp compile` exit 0, BUILD SUCCESS เวลา `2026-10-26T17:56:13+07:00`; compiler รายงาน Nothing to compile, log `/private/tmp/ch04-publication-compile.log` เป็น incremental check ไม่ใช่ fresh full build ผล 42 tests/smoke ด้านบนเป็น execution ของ source เดิมที่มี tokens เดียวกัน

Current publication fingerprint (33 files, algorithm/exclusions เดิม): `8898a7633e442184eaa8059e4706bdd4aeb8963680d313a4c8689be2495e7bf2`

Current hash ของ ProductSampleService.java: `8c97d0dc0460559d0358c3cd8dd51352fbd143cd7cbfbf6546721e355a26233d` แทน original manifest entry `335ce9369dc92a69e5c8bd405699df94592aaa66d01d383b798075926f77073f` รายการอื่นและ root context fingerprint คงเดิม Chapter 03 formatting edit อยู่นอก commit/PR นี้


## README walkthrough amendment — 2026-10-26

บันทึกเวลา `2026-10-26T20:37:07+07:00` (Asia/Bangkok) บน branch `codex/chapter-04-index-mappings-and-settings`, HEAD `243426eb80f7a6d96907f3f3cb9a86e28ee7062b` พร้อม uncommitted README edits และ Chapter 03 edit เดิมที่ไม่ได้แตะในงานนี้ รอบนี้แก้เฉพาะเอกสารและเพิ่มหลักฐาน ไม่เปลี่ยน chapter lifecycle status และไม่แทนที่ historical manifests ด้านบน

README SHA-256 ที่ตรวจ: `03f4af27dec84e4efe605da1173bdf7127a4e498d37f86a341430095f54e0c5f` Java source/configuration/tests ไม่เปลี่ยนในรอบนี้

### Commands และผลจริง

รันจาก directory ของบทด้วย JDK 21; คำสั่ง shell มี exit `0` เว้นแต่ระบุไว้:

```bash
env OPENSEARCH_PORT=19204 docker compose -p ch04-readme-check config --quiet
env OPENSEARCH_PORT=19204 docker compose -p ch04-readme-check up -d --wait opensearch
curl -fsS --max-time 5 'http://localhost:19204/_cluster/health?wait_for_status=yellow&timeout=3s'
env JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw -B -ntp clean verify -Dmaven.compiler.showDeprecation=true
/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/bin/java -jar target/index-mappings-and-settings.jar --server.port=18084 --opensearch.uris=http://localhost:19204 --learning.index-name=ch04-readme-20261026
python3 /private/tmp/ch04-readme-walkthrough.py
kill -TERM 71761
env OPENSEARCH_PORT=19204 docker compose -p ch04-readme-check down
```

- **PASS environment/infrastructure:** Compose project แยก `ch04-readme-check` ใช้ volume ใหม่และ OpenSearch port `19204`; health เป็น `green`, `timed_out=false`, nodes `1` ไม่ใช้หรือลบ index/volume เดิมของผู้เรียน
- **PASS build/tests:** รัน Maven นอก sandbox ได้ `BUILD SUCCESS`, 36 unit/HTTP tests + 6 integration tests, failures/errors/skipped ทั้งหมด `0` ตรวจ XML reports จริงแล้ว Deprecated REST transport warnings ยังคงเปิดเผย ไม่มีการแก้ flags เพื่อข้าม tests
- **Sandbox attempt:** Maven รอบแรกภายใน sandbox exit `1` เพราะ Mockito/Byte Buddy self-attach ไม่สำเร็จ (36 tests, errors `13`, failures/skipped `0`, ยังไม่ถึง ITs) รันคำสั่งเดิมนอก sandbox แล้วผ่าน ไม่ถือรอบแรกเป็น product regression Log เดิม `/private/tmp/ch04-readme-latest-verify.log`; รอบผ่าน `/private/tmp/ch04-readme-latest-verify-unrestricted.log`
- **PASS packaged app:** PID `71761` เริ่มที่ port `18084` โดยใช้ `ch04-readme-20261026`; startup log `/private/tmp/ch04-readme-latest-app.log` เป็น long-running process จึงตรวจ startup/HTTP และ graceful shutdown แยกจาก shell exit ตอนเปิด
- **PASS README examples:** runner อ่าน Bash blocks จาก README Step 5–11 ทั้ง 17 blocks แล้วรันคำสั่งจริงตามลำดับ เปลี่ยนเฉพาะ URLs/index เป็นค่าของระบบแยกและตัด `cd` ที่ซ้ำกับ working directory ออก ตรวจ exit/status/payload ของ missing index, create, mapping/settings, save/get, dynamic settings/restore, invalid documents, field type/final settings, refresh, search และ DELETE
- **PASS new search examples:** `match name=keyboard` → `["p-001"]`; `term name.raw=Mechanical Keyboard` → `["p-001"]`; `term name.raw=keyboard` → `[]` Runner assert ผลจริง ไม่ใช้ mock
- **PASS smoke:** Bash block สุดท้ายเรียก `bash scripts/smoke-test.sh` พร้อม `APP_URL=http://localhost:18084`, `OPENSEARCH_URI=http://localhost:19204`, `LEARNING_INDEX_NAME=ch04-readme-20261026` ครบ assertions และ cleanup ของ script Log `/private/tmp/ch04-readme-walkthrough.log` ลงท้าย `PASS all 17 README Bash blocks (Steps 5–11) including match/term comparisons and smoke`
- **PASS cleanup:** script ลบ owned index แล้ว ส่ง TERM เฉพาะ owned app; process exit `143` ตาม SIGTERM และ log ระบุ `Graceful shutdown complete` จากนั้น Compose down เฉพาะ project ที่สร้าง exit `0` ไม่ใส่ `-v` จึงเก็บ volume สำหรับรอบตรวจนี้ไว้
- **PASS document checks:** step order, สารบัญ anchors/local source links, Bash syntax, JSON examples/request bodies และ `git diff --check`

### ขอบเขตที่ไม่ได้รันซ้ำ

ไม่ได้ copy `.env.example` ทับ `.env` หรือใช้ default app/OpenSearch ports `8084/9204` ในรอบนี้ ใช้ equivalent CLI/env overrides เพื่อแยก resources ไม่ได้ทดสอบ troubleshooting ทุกกรณี, restart/resume ด้วยข้อมูลเดิม, optional lowercase keyword/add-field exercises หรือ platform อื่น การเปรียบเทียบ text/keyword เป็น native OpenSearch experiment ไม่ได้เพิ่ม application search endpoint

Logs และ runner ใน `/private/tmp` เป็น local ephemeral evidence ไม่ได้ commit และอาจถูกล้างภายหลัง
