# Verification standards

Root README เก็บ chapter lifecycle (`Planned`, `Implemented`, `Verified`) ส่วนรายงานใช้ gate statuses `PASS`, `FAIL`, `BLOCKED`, `NOT VERIFIED`, `N/A` สถานะ N/A ต้องมีเหตุผล ไม่แทน mandatory runtime gate ด้วย source inspection

| Gate | สิ่งที่รันและตรวจ |
|---|---|
| Environment | java -version, ./mvnw -version, docker version, docker compose version และ OS/CPU |
| Dependencies | effective POM, relevant dependency tree, exact image tag/digest และ plugins |
| Build/tests | ./mvnw clean verify; ตรวจ Surefire/Failsafe counts, failures, errors และ skips |
| Infrastructure | Compose up, ps และ API readiness ด้วย protocol/auth ที่ถูกต้อง |
| Application | รัน packaged JAR ตาม README แล้วตรวจ readiness และ process ownership |
| API | ทุก implemented endpoint มี success และ representative meaningful failure; assert HTTP status และ fields |
| OpenSearch data | ตรวจ mappings/data/query behavior จริง ห้ามใช้ mock เป็น integration proof |
| README | ทดลอง setup/run/examples/teardown หรือระบุคำสั่งที่ไม่ได้รัน |
| Cleanup | หยุด resources/processes ที่สร้าง ไม่ลบของผู้ใช้หรือ volumes โดยปริยาย |

สร้าง [VERIFICATION report](templates/chapter-verification.md) หลัง attempted verification ใช้ timezone ชัดเจน ระบุ commit/branch หรือ unborn HEAD, dirty/untracked state และขอบเขต source ที่ทดสอบ ไม่อ้าง commit ที่ยังไม่เกิด

เก็บ exact commands, exit codes, test counts และ output excerpts เท่าที่จำเป็น Redact credentials แต่คงหลักฐานที่ตรวจได้ Report locations ต้องระบุถ้าเป็น local ephemeral target ที่ไม่ได้ commit อย่า link ไปไฟล์ไม่มีจริงเพื่อให้ดูเหมือนมีหลักฐาน

Verified ต้องผ่าน mandatory objectives ของ Spring Data reactive และ official async พร้อม parity/feature coverage ตาม [dual-approach contract](dual-approach.md) รวมทั้งทุก applicable mandatory gate บน final relevant source state ถ้าแก้ behavior/config/tests หลังตรวจ ให้ rerun affected checks รวม smoke flow หลัง final runtime fix Historical report ไม่พิสูจน์ services ปัจจุบันกำลังรัน

FAIL คือรันแล้วไม่ผ่าน BLOCKED คือ precondition ทำให้ตรวจไม่ได้ NOT VERIFIED คือยังไม่รัน ทั้งสามไม่ใช่ PASS หากโค้ดมีแล้วแต่ตรวจไม่ครบ lifecycle คง Implemented และระบุข้อจำกัด อย่าแก้ report เก่าให้กลายเป็นผลใหม่โดยไม่ได้รัน

เมื่อ scope ขยายจากหนึ่งเป็นสองแนวทาง ให้คงผลเดิมเป็น historical evidence พร้อม timestamp/source ไม่ลบหรือแก้ expected/actual เก่าให้ดูเหมือนตรวจสอง path แล้ว Overall lifecycle เป็น Implemented จนกว่าจะผ่านข้อกำหนดใหม่ Scope amendment ต้องอยู่ทั้ง README และ VERIFICATION

Foundation checks เป็นคนละขอบเขตกับ chapter verification ไม่มี Maven/Docker checks ใน foundation จนกว่าจะมี chapter ที่ได้รับอนุมัติ
