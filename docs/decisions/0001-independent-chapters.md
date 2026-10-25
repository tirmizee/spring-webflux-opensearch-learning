# ADR 0001: Independent chapters

Status: Accepted — foundation plan approved 2026-10-25

## Decision

แต่ละ chapter เป็น Maven/Spring Boot project ของตัวเอง มี Wrapper, Compose, fixtures และ tests ไม่มี root parent/aggregator หรือ shared runtime module เริ่มบทใดก็ได้โดยไม่ต้อง build/seed บทก่อน

## Consequences

Configuration ซ้ำมากขึ้นแต่ผู้เรียนเห็น dependencies ครบ ใช้ [chapter contract](../chapter-contract.md) และ templates ดูแลความสอดคล้อง Root scripts เป็น convenience เท่านั้น ผู้เรียนยัง `./mvnw clean verify` จากบทได้

การอนุมัติ decision นี้ไม่ใช่คำสั่งสร้างทุก chapter ลำดับ roadmap เป็น learning progression ไม่มี runtime dependency
