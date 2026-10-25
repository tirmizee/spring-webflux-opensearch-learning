# การร่วมพัฒนา

อ่าน [AGENTS.md](AGENTS.md) และ [chapter contract](docs/chapter-contract.md) ก่อนเริ่ม ทุกบทเป็น standalone project ไม่มี root Maven reactor

## Workflow

1. ตรวจ working tree และ scope ที่ผู้ใช้อนุมัติ ใช้ `plan-chapter` เมื่อเป็นคำขอออกแบบ
2. ระบุ objectives, API contracts สอง prefixes, feature coverage/parity, index resources, exact versions และ assertions ของทั้งสองแนวทาง ตาม [plan template](docs/templates/chapter-plan.md)
3. เมื่อได้รับคำสั่ง implement ให้ทำบทนั้นพร้อม tests และ README ตาม [engineering standards](docs/engineering-standards.md)
4. รัน `bash scripts/check-foundation.sh` และ `bash scripts/verify-chapter.sh NN-topic-name` สำหรับบทที่มีจริง จากนั้นทำ packaged application/Compose smoke checks ตาม [verification standards](docs/verification-standards.md)
5. บันทึกผลจริงใน `VERIFICATION.md` แล้วเปลี่ยน roadmap ตามหลักฐาน ใช้ [review template](docs/templates/chapter-review.md) สำหรับ review

อ่าน [dual-approach contract](docs/dual-approach.md) ทุกบทต้องมี Spring Data reactive และ official async path หากมี feature gap ให้ระบุ native/client/fallback/unsupported ตามจริง ห้ามนับ fallback เป็น native coverage

ไม่ต้องขออนุมัติซ้ำสำหรับการแก้ไขภายใน scope เดิม แต่ไม่เริ่มบทถัดไป ไม่ commit/push เอง และไม่แก้ completed chapters อื่นโดยไม่มีคำสั่ง หากจำเป็นต้องสร้าง branch ให้ใช้ `codex/` prefix เว้นแต่ผู้ใช้ระบุชื่ออื่น

## Dependency updates

[compatibility.md](docs/compatibility.md) เป็นแหล่ง baseline ส่วน root README เป็นแหล่ง chapter status เมื่อเปลี่ยน version ให้ตรวจ official release/API, resolved dependency tree และ rerun affected tests อย่าอัปเดตทุกบทพร้อมกันโดยปริยาย

แยก `Documented`, `Resolved`, `Runtime verified` และผลเก่าที่ stale ห้ามถือว่า external compatibility matrix แทนผลทดสอบของ repository

## เอกสารและ checks

ใช้ภาษาไทย คง technical terms เป็น English Relative links ต้องชี้ไฟล์จริง; future chapter ใช้ inline code แทน link ตรวจ file tree หลัง rename/add/delete และไม่เก็บ secrets, downloaded models หรือ logs จำนวนมากใน Git

Foundation checker ใช้ Python standard library ไม่ตรวจ external URLs, Markdown anchors, Java correctness หรือ GitHub-hosted workflow execution ทบทวนสิ่งเหล่านี้ตามประเภทการเปลี่ยนแปลง
