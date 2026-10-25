# Troubleshooting

เอกสารนี้เป็นแนวทางวินิจฉัย Chapter 01 มี current verification สองแนวทางแล้ว รวม shared transport/metadata parity แต่ไม่ใช่หลักฐานของทุก feature หรือ platform

| อาการ | ตรวจและแก้ตามหลักฐาน |
|---|---|
| scripts หา Python ไม่พบ | ใช้ Python 3.9+ และ Bash; checker ไม่ต้อง pip install |
| verify-chapter แจ้ง chapter ไม่มี | ตรวจ chapter ที่มีจริง ไม่สร้าง future chapters เพียงเพื่อผ่าน discovery |
| Java version ไม่ตรง | เทียบ java -version กับ ./mvnw -version ตั้ง JAVA_HOME ของงานให้เป็น JDK 21 |
| No tests / engine linkage error | ตรวจ test platform, effective POM, Surefire/Failsafe และ JUnit API/engine/platform alignment |
| Docker connection refused | ตรวจ daemon/context/permissions ไม่ disable ITs เพื่อผ่าน |
| Image ไม่รองรับ CPU | ตรวจ manifest ของ exact tag เทียบ arm64/amd64 ไม่อ้างว่ารองรับจาก image ชื่อเดียวกัน |
| Container exit / memory/bootstrap error | อ่าน logs, heap และ Docker memory; ตรวจ kernel requirements ของ release |
| Port collision | ปรับ documented env ports ใช้ Compose project แยก; tests ใช้ random mappings |
| Spring Data beans/client ไปผิด cluster | ตรวจ OpenSearch implementation, auto-config, qualifiers, shared transport และ endpoint ของแต่ละ approach |
| Reactive method ยิง request ก่อน subscribe | ตรวจ method body; defer invocation แทนถือว่า Mono lazy เสมอ |
| TLS handshake failure | ตรวจ protocol, CA trust, hostname และ cert validity ไม่ปิด verification เป็นวิธีถาวร |
| HTTP 401/403 จาก OpenSearch | แยก credential กับ permission ใช้ app identity ที่เหมาะสม |
| Compose env มีค่าแต่ app ไม่มี | Compose .env ไม่ส่งเข้า host JVM โดยอัตโนมัติ อ่าน explicit env export/run instructions |
| Get พบแต่ search ไม่พบ | ตรวจ refresh policy/visibility อย่าเพิ่ม sleeps แบบเดา |
| Pool timeout / high memory | ตรวจ bounded concurrency, payload/page/bulk size และ retry amplification |
| Vector dimension error | ตรวจ model/version/preprocessing ให้ตรง fixtures/mapping/query |

อย่าสั่ง stop/delete containers หรือ volumes ที่ไม่ได้สร้างในงาน Cleanup ใช้ project/resource names ของบทและไม่ใส่ volumes removal โดยปริยาย

OpenSearch server ใช้ JVM ใน image แยกจาก JDK ของ app ตรวจ [Docker installation](https://docs.opensearch.org/latest/install-and-configure/install-opensearch/docker/) สำหรับ kernel/memory/security requirements ของ release ที่เลือก ไม่แก้ host settings โดยอัตโนมัติ
