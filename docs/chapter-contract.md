# Chapter contract

แต่ละบทใช้ชื่อ `NN-topic-name` ตาม root roadmap เป็น standalone Spring Boot project ไม่มี source/artifact/fixtures dependency ไปบทอื่น ไม่สร้าง empty future folders

## Two-approach contract

ทุก chapter ต้องมี Spring Data OpenSearch reactive และ official OpenSearchAsyncClient ตาม [ข้อกำหนดกลาง](dual-approach.md) ภายใน POM/Compose เดียวกัน ไม่มีสอง subprojects โดยปริยาย แยก prefixes/classes ตาม approach และใช้ shared contracts/fixtures ที่เทียบกันได้

README ต้องมี implementation status/feature coverage ของแต่ละ path, mapping/query/error/lifecycle comparison และ native/fallback limitations Tests และ smoke ต้องตรวจทั้งสองแนวทาง ไม่บังคับ repository API สำหรับ cluster metadata ที่ไม่มี repository-level abstraction

Chapter 01 เดิมอยู่ใน migration pending state routes เดิมยังไม่เปลี่ยน อ่าน scope amendment ก่อนแก้ code

## ไฟล์ที่ต้องมีเมื่อ implement

| Path | หน้าที่ |
|---|---|
| README.md | เอกสารภาษาไทยตาม template พร้อม actual project tree |
| VERIFICATION.md | ผลจริงหลัง attempted verification; เริ่ม NOT VERIFIED ได้แต่ห้าม fabricated results |
| pom.xml | Boot parent โดยตรง, Java 21, dependencies และ Surefire/Failsafe |
| mvnw, mvnw.cmd, .mvn/wrapper/maven-wrapper.properties | Wrapper ที่ pin distribution/checksum และรันจากบทได้ |
| docker-compose.yml | Local OpenSearch, readiness, scoped resources และ adjustable ports |
| .env.example | Non-secret configuration และ instructions การตั้ง credentials |
| requests/examples.http | ตัวอย่าง requests ตรง API จริง ไม่มี embedded secrets |
| scripts/smoke-test.sh | Assert status/fields และจบ nonzero เมื่อไม่ตรง expectation |
| src/main/java | Application และ packages ที่มีหน้าที่จริง |
| src/main/resources/application.yml | Typed/documented properties และ explicit defaults |
| src/test/java | Unit/HTTP tests และ real-server *IT |
| src/test/resources/fixtures | Deterministic test data ตามความจำเป็น |

Base package `com.example.learning.opensearch.<topic>` โดย Java identifier ไม่มี hyphen Application class อยู่ root package ใช้ config/controller/service/dto/exception ตามหน้าที่ เพิ่ม domain/query/support เมื่อจำเป็น ไม่บังคับ empty packages

## README requirements

ใช้ [template](templates/chapter-readme.md) ให้ครบ objectives, prerequisite concepts, exact versions, actual tree, class responsibilities, flow, fixtures/mappings, config/env, setup/readiness, run/test, API examples/assertions, cleanup, pitfalls, exercises และ sources Tree ต้องแสดง source/test files จริงครบ ไม่ใช้ ellipsis แทนไฟล์ใน README ที่ส่งมอบ

แยก illustrative response กับ observed output ระบุ dynamic fields ไม่แปะค่าที่แต่งขึ้นเป็นผลทดสอบ อธิบาย expected result ของแต่ละ exercise และแยก optional model/load tasks

## Infrastructure contract

Single-node default; no fixed container_name; localhost-bound host ports; configurable app/OpenSearch ports; Compose project per chapter; no destructive reset โดยปริยาย Tests ใช้ random ports ไม่อ้าง external running cluster และไม่ใช้ `latest` image tag

Seed/setup ทำซ้ำได้ พร้อม failure propagation และ resource ownership Readiness ตรวจ API/auth จริง Credentials least privilege ใน security chapter ไม่ใช้ admin เป็น application identity

## Completion

ตรวจ feature coverage/parity ของทั้งสอง approach แล้วทำ [full verification](verification-standards.md), ปรับ roadmap เฉพาะบทที่เปลี่ยน และหยุด ไม่ต้อง verify บทก่อนหน้าเพื่อเรียนบทนี้ แต่ความเปลี่ยนแปลง common standards ต้องประเมิน regression ที่เกี่ยวข้อง
