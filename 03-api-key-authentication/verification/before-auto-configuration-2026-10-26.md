> Historical evidence ของ custom transport ก่อนเปลี่ยนเป็น starter auto-configuration + customizer

# Verification — README walkthrough และ smoke diagnostics

Overall: **Verified** สำหรับ local starter-only scope วันที่ 2026-10-26, Asia/Bangkok ผลนี้ไม่ยืนยันสาเหตุของ HTTP 502 จาก process เดิมที่ผู้ใช้รายงาน เพราะเมื่อเริ่มตรวจไม่มี application ฟัง port 8083 แล้ว

## Scope และ source

เปลี่ยนเฉพาะ README, smoke-test.sh, Docker Compose และ verification evidence ของ Chapter 03 ไม่แก้ Java implementation, tests, dependencies หรือ Chapter 01/02 ไม่ commit/push รักษา uncommitted work เดิม

- README แยก Terminal A/B, หยุด process เดิมก่อนเปลี่ยน key, fresh labels, อายุ key เริ่มต้น 3600 วินาที และ explicit CLI key-file override
- smoke-test.sh เพิ่ม known-code-only diagnostics เมื่อ status ผิด ไม่พิมพ์ raw response/upstream messages หรือ secrets คง behavioral assertions เดิม
- Compose แก้ leading space หน้า top-level name ที่ทำให้ parse ไม่ผ่าน
- Historical report ก่อนเปลี่ยนเก็บที่ [รายงานก่อนแก้](before-readme-troubleshooting-2026-10-26.md)
- Base HEAD/branch ยังคง `acba0e394aff6f3205dd685e237beb80e8ef2e38` / `codex/chapter-02-transport-refactoring`; Chapter 03 ยัง untracked จึงใช้ fingerprint รวมไฟล์ใหม่ ไม่อ้าง base commit แทน source ใหม่

Environment: macOS 27.0.1 arm64, Temurin 21.0.12.1, Maven 3.9.11, Docker 29.8.2/Compose 5.6.0 ตาม environment ที่ตรวจในงานก่อนหน้า Baseline Boot 4.1.2, starter 3.1.4, SDK 3.10.0, OpenSearch 3.9.0 ไม่เปลี่ยน

Fingerprint ใช้ `git ls-files --cached --others --exclude-standard -- 03-api-key-authentication` deduplicate/sort chapter-relative paths, exclude Markdown/ignored generated files แล้ว SHA-256 ของ `path + NUL + raw bytes + NUL`:

| Item | SHA-256 |
|---|---|
| 48 source/config/script/test/wrapper files | 85ee7a026a4386e9180a500e046ce8a2fa63f7cf2079134159cda6c28a14a798 |
| target/api-key-authentication.jar | 9e082bee276dd5ee5e204b433e8933c0f70c6c09cf45dd5d9059c5abbb63863f |

## VERIFY — PASS

รันจาก directory ของบท:

```bash
env JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home \
  ./mvnw -B -ntp clean verify \
  -Dmaven.compiler.showDeprecation=true -Dmaven.compiler.failOnWarning=true
```

Exit 0, BUILD SUCCESS, finished `2026-10-26T06:30:16+07:00`, elapsed 01:43 min; log `/private/tmp/ch03-readme-verify.log` ตรวจ Surefire/Failsafe XML จริง:

| Suite | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| Unit/reactive/HTTP/configuration | 55 | 0 | 0 | 0 |
| Real OpenSearch IT | 10 | 0 | 0 | 0 |

ไม่มี compiler deprecation warnings; runtime Mockito agent/Netty macOS DNS fallback diagnostics ยังคงเป็นข้อจำกัดตาม historical report ไม่ suppress เพื่อซ่อน warning

รัน `docker compose config --quiet`, `docker compose up -d opensearch` และ `bash scripts/initialize-security.sh` จริง หลังแก้ indentation Compose config ผ่าน Bootstrap final exit 0 พร้อม authenticated HTTPS readiness; log `/private/tmp/ch03-readme-bootstrap-final.log` Initial readiness attempt ใน sandbox ติดต่อ localhost ไม่ได้และ exit 1 จึงรันใหม่ด้วย approved network access ไม่ถือ attempt แรกว่าผ่าน ไม่ regenerate CA หรือ data volume

### Packaged smoke และการทำตาม README

รัน temporary local driver (ไม่ได้เพิ่ม Python framework ลง repository):

```bash
python3 /private/tmp/ch03-readme-smoke.py
```

Exit 0; log `/private/tmp/ch03-readme-smoke-suite.log` เปิด Java 21 packaged JAR ตาม hash ข้างบนด้วย JVM truststore options ก่อน -jar และ `--server.port=<random-port> --opensearch.api-key-file=<fresh-key-file>` หลัง JAR ตามรูปแบบ README จงใจตั้ง OPENSEARCH_API_KEY_FILE เป็น missing stale path เพื่อยืนยันว่า explicit CLI input เลือก key ที่ตั้งใจจริง

ผ่าน 13 runtime checkpoints: healthy, permissions, invalid, unrelated truststore, no local truststore, unavailable, rotation A/B ก่อน revoke, revoked A, B หลัง revoke A, expiry ก่อน/หลัง และ trusted-but-wrong-SAN ทุก checkpoint ใช้ `APP_BASE_URL=... bash scripts/smoke-test.sh <mode>` ตรวจ info/health status/fields/error contract จริง

เพิ่ม negative diagnostic checks อีก 2 กรณี: เรียกโหมด insufficient-permissions กับแอปที่ใช้ invalid key และแอปที่ trust ผิด ทั้งคู่ได้ `FAIL info: HTTP 502 expected 200` และ exit 1 ตามคาด แต่แสดง known code/คำแนะนำแยกสาเหตุ:

```text
FAIL info: HTTP 502 expected 200; code=OPENSEARCH_AUTHENTICATION_FAILED
FAIL info: HTTP 502 expected 200; code=OPENSEARCH_TLS_FAILURE
```

นี่คือ **PASS ของ negative assertions** ไม่ใช่การนับ smoke failure ว่าผ่าน exercise Logs `/private/tmp/ch03-readme-diagnostic-invalid.log` และ `/private/tmp/ch03-readme-diagnostic-ca.log` ไม่มี token output

การทดสอบ permissions ด้วย fresh info-only key ได้ info 200 และ health 502 AUTHORIZATION_FAILED ตาม README จึงไม่ลด expected status เพื่อให้ผ่านข้อผิดพลาดที่ผู้ใช้รายงาน

Command variations: ใช้ random ports กับ unique labels แทน literal 8083/date labels และใช้ PKI เดิม จึงไม่รัน cp .env.example .env หรือ prepare-security.sh ซ้ำ ไม่อ้างว่าได้กด Ctrl-C/สลับ terminals จริง Driver หยุด owned processes และเริ่ม fresh JVMs แทน จับ lifecycle เดียวกับขั้นตอนคู่มือ

## CHECK / REVIEW — PASS

- `bash -n` smoke-test.sh และ Bash command blocks ทั้ง 10 blocks ใน README ผ่าน
- Markdown local link targets และ git diff --check ผ่าน
- อ่านแบบ read-only ตาม review-chapter หลังแก้: terminal order, environment/CLI precedence, key lifetime, error code classification, whitelist diagnostics, expected permissions และ resource ownership ไม่พบ actionable findings ค้าง
- ตรวจ issued tokens ที่มี paired id files กับ intended repository files และ current verification logs ไม่พบ leakage; ไม่พิมพ์ secret values
- Driver หยุด owned JVMs/hostname fixture และ revoke test keys แล้ว จากนั้น docker compose down exit 0 เก็บ volumes/PKI/demo keys เดิม ไม่แก้ resources ของบทอื่น

**NOT VERIFIED:** สาเหตุเฉพาะของ original user process, hosted CI, Windows/WSL/remote Docker และ production public-CA/multi-node/load behavior Historical full chapter evidence อยู่ในรายงานก่อนแก้ ไม่ใช้ยืนยันว่าแอปปัจจุบันกำลังรัน
