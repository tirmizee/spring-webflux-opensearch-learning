# ADR 0003: Test platform selection

Status: Accepted — ผู้ใช้อนุมัติ Boot-managed JUnit 6 สำหรับ Chapter 01 เมื่อ 2026-10-25

## Context และหลักฐาน

Requirement เดิมระบุ JUnit 5 แต่ Spring Framework 7 SpringExtension ต้องใช้ JUnit Jupiter 6.0+ จึงไม่ override ลงเป็น 5

แหล่งอ้างอิง: [SpringExtension API](https://docs.spring.io/spring-framework/docs/7.0.10/javadoc-api/org/springframework/test/context/junit/jupiter/SpringExtension.html), [official issue 35900](https://github.com/spring-projects/spring-framework/issues/35900)

## Decision

ผู้ใช้เลือก “ใช้ Boot-managed JUnit 6 (แนะนำ)” ในการเริ่ม Chapter 01 ใช้ Spring Boot 4.1.2 BOM ซึ่ง resolve JUnit Jupiter 6.0.3 พร้อม Mockito และ Testcontainers ตาม BOM ไม่ pin API/engine/platform คนละสาย

Chapter 01 ใช้ @WebFluxTest และ @SpringBootTest/SpringExtension จริง ไม่หลบ compatibility ด้วยการเลี่ยง integration support ส่วน tests ยังคง Jupiter programming model

## Consequences และขอบเขต

ข้อกำหนด test platform ของ Chapter 01 เปลี่ยนจาก JUnit 5 เป็น Boot-managed JUnit 6 การเลือก platform ของบทใหม่ต้องตรวจ requirement และ compatibility อีกครั้ง การ resolve ไม่ใช่ runtime verification ดูผลจริงที่ [Chapter 01 report](../../01-basic-connection/VERIFICATION.md)
