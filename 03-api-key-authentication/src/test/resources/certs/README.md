# Unit-test CA fixture

`unit-ca.pem` เป็น self-signed public certificate สำหรับ unit tests เท่านั้น สร้างใหม่ในงาน refactoring วันที่ 2026-10-25 ด้วย Temurin 21 `keytool` ไม่ได้ copy มาจากระบบภายนอก ไม่มี private key/keystore ใน repository และห้ามติดตั้ง CA นี้ใน OS/JVM trust store หรือใช้กับ production

- Subject: `CN=Chapter 02 Unit Test CA - DO NOT TRUST`
- RSA 2048 / SHA256withRSA / CA=true
- Validity UTC: 2019-12-31 17:00:00 ถึง 2039-12-31 17:00:00
- Certificate DER SHA-256: `01F2788D9FF47A28D925119EB0FEE4384B21D125E63E942EDB52832CD47BA402`

คำสั่งที่ใช้สร้างครั้งเดียว (startdate เป็น local Asia/Bangkok จึงต่างจาก UTC 7 ชั่วโมง):

```bash
keytool -genkeypair -alias unit-ca -keyalg RSA -keysize 2048 \
  -sigalg SHA256withRSA -dname 'CN=Chapter 02 Unit Test CA - DO NOT TRUST' \
  -ext bc=ca:true -startdate '2020/01/01 00:00:00' -validity 7305 \
  -keystore /private/tmp/ch02-unit-fixture.p12 -storetype PKCS12 \
  -storepass test-fixture-only -noprompt
keytool -exportcert -rfc -alias unit-ca \
  -keystore /private/tmp/ch02-unit-fixture.p12 -storepass test-fixture-only \
  -file src/test/resources/certs/unit-ca.pem
```

`test-fixture-only` เป็น disposable keystore password ของการสร้าง fixture ไม่ใช่ application credential Private keystore ถูกลบหลัง export แล้ว การ generate ใหม่จะได้ key/serial/fingerprint ใหม่ ต้องอัปเดต provenance ด้วย Unit tests โหลด PEM ผ่าน classpath ไม่เรียก keytool/OpenSSL/Bash/Docker ระหว่าง tests ไม่ต้องเพิ่ม BouncyCastle

Fixture นี้ตรวจ CA parsing/context/configuration เท่านั้น Real TLS integration tests ยังคงสร้าง ephemeral CA/leaf/key ใหม่ด้วย chapter security scripts และ cleanup หลัง tests ไม่ใช้ fixture public CA นี้เชื่อมกับ server

Chapter 03 copies this public fixture from Chapter 02 at implementation time; no private key copied and no runtime dependency on Chapter 02.
