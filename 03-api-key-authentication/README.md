# Chapter 03 — API Key Authentication

สถานะ: **Verified** สำหรับ local starter API Key/configuration/JVM trust/lifecycle scope ดู [VERIFICATION.md](VERIFICATION.md)

บทนี้ใช้ `spring-data-opensearch-starter` กับ Spring WebFlux เพื่อเรียน native OpenSearch API Key: การสร้าง, กำหนดสิทธิ์, ใช้งาน, expiry, revoke และ rotation เป็น standalone Maven project ไม่ต้อง build หรือรันบทอื่น

## เริ่มใช้งานทีละขั้น

ใช้ **สอง terminals** โดยทั้งคู่ต้องอยู่ใน `03-api-key-authentication` คำสั่ง `java -jar` จะรันค้างไว้เพื่อให้ Terminal B เรียก API ได้ ส่วน `smoke-test.sh` มีหน้าที่ตรวจแอปเท่านั้น ไม่เริ่มแอปหรือเปลี่ยน API Key ให้

ต้องมี JDK 21 (`java`, `keytool`), Bash, OpenSSL, Docker/Compose, cURL และ jq ตั้ง JAVA_HOME ให้ใช้ JDK 21 ก่อน build

### 1. Terminal A — เตรียม OpenSearch ครั้งแรก

```bash
# จาก repository root
cd 03-api-key-authentication
cp .env.example .env
bash scripts/prepare-security.sh
docker compose config --quiet
docker compose up -d opensearch
bash scripts/initialize-security.sh
bash scripts/create-api-key.sh monitor config/api-keys/monitor.json
```

ถ้าเคยเตรียมแล้ว **ไม่รัน prepare-security.sh ซ้ำ** เพราะ script ปฏิเสธการทับ certificates เดิม ใช้ `docker compose up -d opensearch` และ `bash scripts/initialize-security.sh` เพื่อเปิดระบบเดิม หาก label `monitor` มีอยู่แล้ว ให้ใช้ fresh label ตามขั้นตอนถัดไป ไม่ลบไฟล์เดิมเพื่อบังคับสร้างทับ

`.env` เป็นของ Docker Compose ไม่ได้ export เข้า host JVM ตัวอย่างนี้ใช้ OpenSearch `https://localhost:9203` ถ้าเปลี่ยน port ต้องกำหนด OPENSEARCH_URI ให้ทั้ง application และ administration scripts

### 2. Terminal A — build และเริ่มแอปด้วย key ใหม่

สร้าง key ใหม่เพื่อไม่เผลอใช้ key จากครั้งก่อนที่หมดอายุแล้ว ค่าเริ่มต้นของ script คือ **3600 วินาที (1 ชั่วโมง)**

```bash
./mvnw -B -ntp clean verify -Dmaven.compiler.showDeprecation=true
MONITOR_LABEL="monitor_$(date +%Y%m%d%H%M%S)"
bash scripts/create-api-key.sh "$MONITOR_LABEL" config/api-keys/monitor.json
java -Djavax.net.ssl.trustStore=certs/generated/public/truststore.p12 \
  -Djavax.net.ssl.trustStoreType=PKCS12 -Djavax.net.ssl.trustStorePassword=changeit \
  -jar target/api-key-authentication.jar \
  --server.port=8083 \
  --opensearch.api-key-file="certs/generated/secrets/$MONITOR_LABEL.token"
```

รอข้อความ `Started ApiKeyApplication` แล้วปล่อย Terminal A ไว้ หากขึ้น `Port 8083 was already in use` ให้หยุดแอปเดิมใน terminal ที่รันด้วย Ctrl-C ก่อน อย่าไปตรวจ process เก่าที่ port เดิมแล้วคิดว่าเป็นแอปที่เพิ่งเริ่ม

Full Maven verification ต้องมี Docker พร้อม ITs ใช้ isolated OpenSearch และ random ports ของตัวเอง ไม่ใช้ Compose instance ไม่ skip เมื่อ tools ขาด

### 3. Terminal B — ตรวจกรณีใช้งานสำเร็จ

เปิด terminal ใหม่แล้วเข้า directory ของบท:

```bash
cd /Volumes/ExternalSSD/Projects/spring-webflux-opensearch-learning/03-api-key-authentication
curl -sS http://127.0.0.1:8083/api/spring-data/cluster/info | jq
curl -sS http://127.0.0.1:8083/api/spring-data/cluster/health | jq
APP_BASE_URL=http://127.0.0.1:8083 bash scripts/smoke-test.sh healthy
```

เปลี่ยน absolute path ใน `cd` ให้ตรง checkout ของคุณ ผลที่คาดหวัง:

```text
PASS healthy info HTTP 200
PASS healthy health HTTP 200
```

### 4. Terminal A — เปลี่ยนเป็น info-only key

**กด Ctrl-C เพื่อหยุดแอปที่รันจากขั้นตอน 2 ก่อน** แล้วใช้ Terminal A เดิมสร้าง key ใหม่และเริ่มแอป:

```bash
INFO_LABEL="info_$(date +%Y%m%d%H%M%S)"
bash scripts/create-api-key.sh "$INFO_LABEL" config/api-keys/info-only.json
java -Djavax.net.ssl.trustStore=certs/generated/public/truststore.p12 \
  -Djavax.net.ssl.trustStoreType=PKCS12 -Djavax.net.ssl.trustStorePassword=changeit \
  -jar target/api-key-authentication.jar \
  --server.port=8083 \
  --opensearch.api-key-file="certs/generated/secrets/$INFO_LABEL.token"
```

รอ `Started ApiKeyApplication` อีกครั้ง Key ถูกอ่านตอน startup เปลี่ยนไฟล์หรือสร้าง key ใหม่ขณะที่แอปเก่ารันอยู่ไม่ได้เปลี่ยน credential ของแอปนั้น

### 5. Terminal B — ตรวจ insufficient permissions

```bash
APP_BASE_URL=http://127.0.0.1:8083 bash scripts/smoke-test.sh insufficient-permissions
```

ผลที่ถูกต้องคือ:

```text
PASS insufficient-permissions info HTTP 200
PASS insufficient-permissions health HTTP 502
```

`info-only.json` ให้ `cluster:monitor/main` ดังนั้น info ต้องสำเร็จ ส่วน health ถูกปฏิเสธด้วย `OPENSEARCH_AUTHORIZATION_FAILED` ถ้า info เป็น 502 ด้วย แสดงว่ายังไม่ใช่ผลสำเร็จของ exercise นี้

### ถ้าเจอ FAIL info: HTTP 502 expected 200

ดู application error code โดยไม่พิมพ์ token:

```bash
curl -sS http://127.0.0.1:8083/api/spring-data/cluster/info \
  | jq '{status, code, instance}'
```

| code | ตรวจและแก้ |
|---|---|
| OPENSEARCH_AUTHENTICATION_FAILED | Key ไม่ถูกยอมรับ อาจหมดอายุ/revoked/ไม่ใช่ของ cluster นี้ กลับขั้นตอน 4 สร้าง key ใหม่และ restart แอป |
| OPENSEARCH_AUTHORIZATION_FAILED | แอปใช้ key ที่ไม่มีสิทธิ์ info ตรวจว่า create ด้วย info-only.json และส่งไฟล์ของ label เดียวกัน |
| OPENSEARCH_TLS_FAILURE | ตรวจ JVM truststore path/type/password และ hostname ใช้คำสั่ง java ครบทุกบรรทัด โดย -D อยู่ก่อน -jar |
| OPENSEARCH_UNAVAILABLE / OPENSEARCH_TIMEOUT | ตรวจ docker compose ps, OpenSearch readiness และ URI |
| ไม่มี code ตามตาราง | ตรวจ port/JAR ว่าเป็นแอปบทนี้ ไม่ใช่ process เก่าหรือแอปอื่น |

ตรวจ native API ด้วย key file ที่เพิ่งสร้างจาก **Terminal A หลังหยุดแอป** ได้ โดย token ไม่อยู่ใน argv หรือ console:

```bash
curl -sS --cacert certs/generated/public/ca.pem \
  --config "certs/generated/secrets/$INFO_LABEL.curl" \
  -o /dev/null -w 'OpenSearch info HTTP %{http_code}\n' https://localhost:9203/
```

Native info ต้องเป็น 200 ถ้าเป็น 401 ให้สร้าง fresh key ก่อน ถ้า native 200 แต่ application ยัง 502 ให้ตรวจ code/TLS และยืนยันว่าเริ่มแอปด้วย key file เดียวกัน ไม่แสดงเนื้อหา `.token` หรือ `.curl` เพื่อวินิจฉัย

## Configuration ชุดเดียว

มี prefix `opensearch` ชุดเดียว ไม่มี application CA property และไม่มี URI/timeout ซ้ำ:

```yaml
opensearch:
  uris: ${OPENSEARCH_URI:https://localhost:9203}
  api-key-file: ${OPENSEARCH_API_KEY_FILE:certs/generated/secrets/monitor.token}
  connection-timeout: ${OPENSEARCH_CONNECT_TIMEOUT:1s}
  socket-timeout: ${OPENSEARCH_RESPONSE_TIMEOUT:3s}
  request-timeout: ${OPENSEARCH_REQUEST_TIMEOUT:5s}
```

`uris`, `connection-timeout`, `socket-timeout` เป็น built-in properties ของ starter ส่วน `api-key-file` และ `request-timeout` เป็น additional properties ของบทใต้ prefix เดียวกัน ไม่ใช่ built-in API Key support ไม่มี effective settings record หรือ custom pool properties อีกต่อไป Pool ใช้ default ของ low-level client รุ่นที่ resolve (total 30 / per route 10) ไม่ใช่ application concurrency limit

บทนี้รับ HTTPS URI หนึ่งค่า มี host และไม่มี credentials/path/query/fragment รวมถึง **ไม่มี `/` ต่อท้าย URI** เพราะ starter/low-level builder รุ่นนี้ parse รูปแบบนั้นไม่ผ่าน ปฏิเสธ username/password และ path-prefix เพื่อไม่ fallback เป็น Basic authentication Timeouts ต้องอยู่ระหว่าง 1ms–60s และ request-timeout >= socket-timeout

Starter 3.1.4 map connection-timeout ไปยัง connection establishment และ pool acquisition ส่วน customizer map socket-timeout ไปเป็น response timeout เพราะ implementation ของ starter รุ่นนี้ยังไม่ได้ apply ค่านี้เอง Response timeout ไม่ใช่ total deadline ส่วน Reactor request-timeout คุม operation ต่อ subscription การ cancel ไม่รับประกันว่า I/O/server work หยุดจริง

## Starter auto-configuration + customizer ทำงานอย่างไร

Application ใช้ `@SpringBootApplication` ปกติ **ไม่มี excludeName** แล้วให้ starter สร้าง RestClientBuilder → RestClient → RestClientTransport → ReactiveOpenSearchClient รวมถึง template/converter อัตโนมัติ ไม่ประกาศ client/transport/template beans เอง

`ApiKeyConfiguration` ประกาศเพียง `RestClientBuilderCustomizer` หนึ่ง bean:

1. อ่าน token file ครั้งเดียวตอน startup ไม่อ่านใน Reactor request pipeline
2. เพิ่ม `Authorization: ApiKey <token>` ผ่าน default headers
3. ปิด HTTP-layer redirects และ automatic retries เพื่อไม่ส่ง credential ไป redirect host; low-level RestClient ยังมี retry/failover semantics ของ library จึงไม่อ้างว่าปิด retry ทุกชั้น
4. ตั้ง response timeout จาก built-in socket-timeout

ดู implementation ที่ [ApiKeyConfiguration](src/main/java/com/example/learning/opensearch/apikey/config/ApiKeyConfiguration.java) และ additional properties ที่ [ApiKeyConnectionProperties](src/main/java/com/example/learning/opensearch/apikey/config/ApiKeyConnectionProperties.java)

Customizer เป็น callback ที่ starter เรียก ไม่ได้ build client หรือแทน callback chain ของ starter ตัว service ยังใช้ ReactiveOpenSearchClient ผ่าน Mono.defer ไม่มี synchronous OpenSearch calls ใน request flow

**Trade-off ของ exact release:** starter 3.1.4 ใช้ default RestClientTransport/RestClientBuilder ที่ deprecated แล้ว บทนี้เลือก default path เพื่อโฟกัส API Key ไม่ได้หมายความว่าต้องสร้าง transport เองจึงจะใช้ API Key ได้ Application ไม่ suppress deprecation warnings และ verification ต้องรายงาน warnings ตามจริง หากต้องการฝึก ApacheHttpClient5Transport ที่สร้างเองให้ดู Chapter 02 แทน โดยบทนี้ไม่มี runtime dependency ไปบทนั้น

Spring จัดการ shutdown ตาม lifecycle ของ auto-configured beans Reactive wrapper รุ่นนี้ close เป็น no-op ส่วน transport และ RestClient อาจเรียก close underlying REST client ซ้ำตาม default bean lifecycle จึงไม่อ้างว่ามี single close owner ที่เราออกแบบเอง Tests ตรวจ context shutdown แล้วใช้งานต่อไม่ได้ ไม่เพิ่ม lifecycle post-processor เพื่อเปลี่ยนพฤติกรรมของ starter

## HTTPS ใช้ JVM default trust

Application ไม่มี CA loader/custom trust manager ใช้ JVM default SSLContext ของ low-level client พร้อม hostname verification ไม่มี trust-all Production ที่ certificate chain เชื่อถือได้โดย JVM และ SAN ตรง hostname กำหนด HTTPS URI กับ key file แล้วรัน `java -jar target/api-key-authentication.jar` ได้ ไม่ต้องใช้ local truststore options

Docker ของบทใช้ private CA จึงเตรียมความเชื่อถือภายนอก application ผ่าน JVM options ใน quick start Truststore มี public certificates เท่านั้น ไม่มี private key/API Key; `changeit` เป็น demo integrity password ไม่ใช่ API credential Custom truststore แทน default JVM trust anchors ไม่ merge public roots อัตโนมัติ ต้องตั้งก่อนเริ่ม JVM และ restart หลังเปลี่ยน truststore

ถ้ามี generated material จากรุ่นก่อนที่ไม่มี truststore ให้ใช้ CA เดิม ไม่ regenerate certificates/data volume:

```bash
keytool -importcert -noprompt -alias chapter03 -file certs/generated/public/ca.pem \
  -keystore certs/generated/public/truststore.p12 -storetype PKCS12 -storepass changeit
```

Certificate creation เป็น infrastructure prerequisite; การสอน TLS เชิงลึกอยู่ Chapter 02 แต่บทนี้รันได้เอง

## API contracts และ least privilege

| GET endpoint | 200 response fields |
|---|---|
| /api/spring-data/cluster/info | clusterName, clusterUuid, nodeName, distribution, version |
| /api/spring-data/cluster/health | clusterName, status, numberOfNodes, numberOfDataNodes, timedOut |

Single-node yellow เป็นข้อมูลที่ส่งกลับได้ ไม่ใช่ connection failure

| Failure | Application status/code |
|---|---|
| Invalid/expired/revoked key; native 401 | 502 OPENSEARCH_AUTHENTICATION_FAILED |
| Info-only key เรียก health; native 403 | 502 OPENSEARCH_AUTHORIZATION_FAILED |
| Untrusted certificate หรือ hostname mismatch | 502 OPENSEARCH_TLS_FAILURE |
| Connection unavailable | 503 OPENSEARCH_UNAVAILABLE |
| Deadline/transport timeout | 504 OPENSEARCH_TIMEOUT |

ส่ง native token ผ่าน `Authorization: ApiKey <token>` ไม่ Base64 encode และไม่ใช้ Elasticsearch endpoints Monitor key มีเฉพาะ cluster info/health; info-only key อ่านได้เฉพาะ info ทั้งคู่ไม่มี index/Security API administration permissions

Invalid-key exercise ต้องใช้ token ที่ผ่าน local format validation แต่ server ไม่ยอมรับ ไฟล์ว่าง/หลายบรรทัด fail ตอน startup ซึ่งเป็นคนละกรณี

## API Key lifecycle

Admin ใช้ POST/GET `/_plugins/_security/api/apitokens` และ DELETE `/_plugins/_security/api/apitokens/{id}` ผ่าน mTLS; application key ไม่มีสิทธิ์จัดการ keys

```bash
bash scripts/create-api-key.sh rotation_a config/api-keys/monitor.json
bash scripts/list-api-keys.sh
bash scripts/create-api-key.sh rotation_b config/api-keys/monitor.json
# เริ่ม/restart application ด้วย rotation_b.token และตรวจ success ก่อน revoke A
bash scripts/revoke-api-key.sh rotation_a
# A ต้องถูกปฏิเสธ แต่ B ยังใช้ได้
bash scripts/create-api-key.sh short config/api-keys/monitor.json 20
# เริ่ม application ด้วย short.token ตรวจ success ก่อน แล้วตรวจ failure หลัง expiry
```

Scripts ไม่พิมพ์ token ใช้ protected cURL config แทน secret ใน argv, `umask 077`, unique server names และไม่ overwrite local labels Token คืนครั้งเดียว; list คืน metadata เท่านั้น หากสร้างสำเร็จแต่รับ response/เขียนไฟล์ล้มเหลว ไม่ retry อัตโนมัติ ให้ตรวจ metadata และ revoke key ที่เกี่ยวข้องก่อนเลือก label ใหม่

API Key อ่านตอน startup และมี String/header copies ใน heap ไม่รับประกันล้างทุกสำเนา ห้ามเปิด wire logging หรือเผย heap dumps โดยไม่มีการควบคุม ปิด redirects/automatic retries ไม่ส่ง key ไป redirect host CA/admin private keys อยู่ใต้ parent 0700; mounted cert files 0644 Compose node ไม่ mount admin private key ส่วน bootstrap container ใช้ชั่วคราว Docker access/host owner ยังอ่าน secrets ได้ ไม่เหมาะกับ shared untrusted host

## Tests และ cleanup

Required gates: unit/reactive/HTTP/configuration tests, real OpenSearch ITs, native least privilege และ denied index/admin operations, invalid/expired/revoked keys, cached-key rotation, untrusted trust/hostname mismatch และ transport shutdown พร้อม auto-configured client/transport/template/converter origins Packaged smoke ต้องใช้ JAR ล่าสุดและตรวจ response fields/error codes จริง ไม่มี cross-approach parity เพราะบทนี้มี starter path เดียว

Tests เท่านั้นที่สร้าง isolated trust contexts เพื่อเลือก trusted/untrusted fixtures; ไม่รวม test CA loader/cert fixtures ใน packaged application ไม่ skip required gates เมื่อ tools ขาด แยก PASS, FAIL และ NOT VERIFIED ใน [VERIFICATION.md](VERIFICATION.md)

หยุด JAR ที่คุณเริ่มด้วย Ctrl-C และ revoke labels ที่ไม่ใช้แล้ว จาก directory ของบท:

```bash
docker compose down
```

ไม่ใช้ `down -v` เป็น default และไม่ regenerate CA ทับ existing data volume ไม่ครอบคลุม production deployment, multi-node revocation, AWS SigV4, hot reload หรือ document/vector search

## Architecture และขอบเขต

```mermaid
flowchart LR
  A["GET /api/spring-data/cluster/info or health"] --> B["ClusterConnectionService"]
  B --> C["Mono.defer ReactiveOpenSearchClient"]
  C --> D["Starter-managed RestClientTransport"]
  D --> E["HTTPS + API Key + OpenSearch Security"]
```

ใช้ starter แนวทางเดียว ไม่มี core-only project หรือ official async application path แม้ starter มี core และ OpenSearch Java Client เป็น transitive dependencies ซึ่งยังจำเป็นต่อการทำงาน

Baseline: Java 21, Spring Boot 4.1.2, spring-data-opensearch-starter 3.1.4, underlying opensearch-java 3.10.0, OpenSearch 3.9.0, Maven 3.9.11/Wrapper 3.3.4 และ Boot-managed JUnit 6/Mockito/Testcontainers 2.0.5 Compatibility ที่ยืนยันได้จำกัดตามผลจริงใน verification report

Starter สร้าง client/transport/data infrastructure เองทั้งหมด โดย application เพิ่ม API Key ผ่าน RestClientBuilderCustomizer Tests ตรวจ bean factory origins จริง ไม่มี custom transport factory หรือ client beans ดู trade-off ของ deprecated default transport ในหัวข้อด้านบน

Metadata APIs ใช้ reactive-client tier ไม่ใช่ document repository/mapping coverage ทุก operation defer invocation และมี overall deadline Client/transport lifecycle ให้ Spring starter จัดการตามค่า default ไม่มี application-managed close owner

```text
03-api-key-authentication/
├── pom.xml / mvnw / mvnw.cmd / .mvn/
├── src/main/java/ / src/main/resources/application.yml
├── src/test/java/ / src/test/resources/
├── docker-compose.yml / .env.example / .gitignore
├── config/opensearch.yml / config/security/*.yml
├── config/api-keys/{monitor,info-only}.json
├── scripts/{prepare-security,initialize-security,create-api-key,list-api-keys,revoke-api-key,smoke-test}.sh
├── requests/examples.http
├── README.md / VERIFICATION.md
└── verification/  # historical evidence เท่านั้น
```


## Sources

- [OpenSearch API Keys](https://docs.opensearch.org/latest/security/access-control/api-keys/) — native feature introduced 3.7
- [Create](https://docs.opensearch.org/latest/security/api/api-keys/create/), [List](https://docs.opensearch.org/latest/security/api/api-keys/list/), [Revoke](https://docs.opensearch.org/latest/security/api/api-keys/revoke/)
- [Starter 3.1.4 client configuration](https://github.com/opensearch-project/spring-data-opensearch/blob/v3.1.4/spring-data-opensearch-starter/src/main/java/org/opensearch/spring/boot/autoconfigure/OpenSearchClientConfigurations.java)
- [Starter 3.1.4 data configuration](https://github.com/opensearch-project/spring-data-opensearch/blob/v3.1.4/spring-data-opensearch-starter/src/main/java/org/opensearch/spring/boot/autoconfigure/data/OpenSearchDataConfiguration.java)

Latest pages เปลี่ยนได้ Runtime evidence ยืนยันเฉพาะ exact versions/environment ในรายงาน
