# Sơ đồ cho báo cáo

Ba sơ đồ dưới đây sinh trực tiếp từ code đang chạy, không phải vẽ tay:
ERD lấy đúng theo `src/main/resources/db/migration/V1__schema.sql`, use case theo mục 5
của `DOAN.md`, sequence theo `RegistrationService.register()`.

**Cách xuất ảnh để dán vào báo cáo**

- Mermaid (ERD, sequence): GitHub render sẵn khi xem file này. Trong VS Code dùng
  extension *Markdown Preview Mermaid Support*. Muốn ảnh PNG/SVG thì dán vào
  [mermaid.live](https://mermaid.live) rồi Export.
- PlantUML (use case): dán vào [plantuml.com/plantuml](https://www.plantuml.com/plantuml)
  hoặc dùng extension *PlantUML* trong VS Code.

Nếu sau này sửa schema thì sửa cả file này — sơ đồ lệch với code còn tệ hơn không có sơ đồ.

---

## 1. ERD — đúng 5 bảng

```mermaid
erDiagram
    users {
        bigint id PK
        varchar username UK "not null"
        varchar password "BCrypt, not null"
        varchar full_name "not null"
        varchar code UK "mã SV, null với ADMIN"
        varchar email
        varchar role "ADMIN | STUDENT"
    }

    courses {
        bigint id PK
        varchar code UK "not null"
        varchar name "not null"
        int credits "not null"
        bigint prerequisite_id FK "tự trỏ, tối đa 1"
    }

    semesters {
        bigint id PK
        varchar name "not null"
        datetime reg_open_at "not null"
        datetime reg_close_at "not null"
        int max_credits "default 24"
        boolean active "default false"
    }

    class_sections {
        bigint id PK
        varchar code UK "not null"
        bigint course_id FK "not null"
        bigint semester_id FK "not null"
        varchar lecturer_name "text, không có vai GV"
        varchar room
        tinyint day_of_week "2..7"
        tinyint start_period "1..12"
        tinyint period_count "not null"
        int capacity "not null"
        int registered_count "counter dư thừa có chủ đích"
    }

    registrations {
        bigint id PK
        bigint student_id FK "not null"
        bigint section_id FK "not null"
        datetime registered_at "not null"
        decimal score "null = chưa có điểm"
    }

    courses      |o--o{ courses        : "là tiên quyết của"
    courses      ||--o{ class_sections : "được mở thành"
    semesters    ||--o{ class_sections : "chứa"
    users        ||--o{ registrations  : "sinh viên đăng ký"
    class_sections ||--o{ registrations : "có bản ghi"
```

### Ràng buộc không vẽ được trên ERD

Ba thứ này là phần quan trọng nhất của thiết kế nhưng ký hiệu ERD không diễn tả được,
nên phải nêu bằng lời trong báo cáo.

| Ràng buộc | Bảng | Vai trò |
|---|---|---|
| `CHECK (registered_count <= capacity)` | `class_sections` | Lưới an toàn tầng DB, chặn vượt sĩ số theo chiều tăng |
| `UNIQUE (student_id, section_id)` | `registrations` | Chặn một race condition **khác** với row lock: bấm nút hai lần, F5, mạng lag |
| Không dùng `ON DELETE CASCADE` ở mọi khoá ngoại | tất cả | Xoá dữ liệu sinh viên phải là hành động tường minh trong code |

Hai điểm thiết kế nên nói kèm:

- `registered_count` **dư thừa có chủ đích**. Về lý thuyết tính được bằng
  `count(*)` trên `registrations`, vẫn lưu vì đọc nhanh khi hiển thị hàng trăm lớp và
  vì cần một chỗ cụ thể để gắn `CHECK`. Đánh đổi là phải tự giữ đồng bộ, và đó là lý do
  cả `register()` lẫn `drop()` đều phải chạy trong transaction đã lock dòng.
- **Không có bảng `grades`, không có cột `gpa`.** Điểm là cột `score` trên
  `registrations`; điểm chữ và GPA là dữ liệu dẫn xuất, tính lại mỗi lần hiển thị.

---

## 2. Sơ đồ use case

```plantuml
@startuml
left to right direction
skinparam packageStyle rectangle
skinparam shadowing false

actor "Sinh viên" as SV
actor "Admin" as AD

rectangle "Website đăng ký học phần trực tuyến" {

  package "AUTH" {
    usecase "Đăng ký tài khoản sinh viên" as UC_REG
    usecase "Đăng nhập / Đăng xuất" as UC_LOGIN
  }

  package "ADMIN" {
    usecase "Quản lý môn học" as UC_COURSE
    usecase "Quản lý học kỳ" as UC_SEM
    usecase "Quản lý lớp học phần" as UC_SEC
    usecase "Nhập điểm" as UC_GRADE
  }

  package "SINH VIÊN" {
    usecase "Xem lớp đang mở" as UC_LIST
    usecase "Đăng ký học phần" as UC_ENROLL
    usecase "Rút học phần" as UC_DROP
    usecase "Xem học phần của tôi" as UC_MY
  }

  package "Kiểm tra khi đăng ký" {
    usecase "Cổng đăng ký còn mở" as V1
    usecase "Chưa đăng ký môn này" as V2
    usecase "Đã đạt môn tiên quyết" as V3
    usecase "Không trùng thời khóa biểu" as V4
    usecase "Chưa vượt giới hạn tín chỉ" as V5
    usecase "Lớp còn chỗ" as V6
  }
}

SV --> UC_REG
SV --> UC_LOGIN
SV --> UC_LIST
SV --> UC_ENROLL
SV --> UC_DROP
SV --> UC_MY

AD --> UC_LOGIN
AD --> UC_COURSE
AD --> UC_SEM
AD --> UC_SEC
AD --> UC_GRADE

UC_ENROLL ..> V1 : <<include>>
UC_ENROLL ..> V2 : <<include>>
UC_ENROLL ..> V3 : <<include>>
UC_ENROLL ..> V4 : <<include>>
UC_ENROLL ..> V5 : <<include>>
UC_ENROLL ..> V6 : <<include>>

UC_ENROLL ..> UC_LIST : <<extend>>
UC_DROP   ..> UC_LIST : <<extend>>

note bottom of UC_GRADE
  Điểm nhập ở đây trở thành
  điều kiện tiên quyết chặn
  đăng ký ở học kỳ sau.
end note

note bottom of V3
  Đạt = tồn tại bản ghi đăng ký
  môn tiên quyết với score >= 4.0
end note
@enduml
```

Ghi chú khi trình bày: `Đăng ký học phần` và `Rút học phần` vẽ tách khỏi
`Xem lớp đang mở` cho rõ nghiệp vụ, nhưng trên giao diện chúng nằm cùng một màn hình
(hai nút trên mỗi dòng của danh sách lớp), đúng như mục 5 mô tả.

Sáu use case trong nhóm *Kiểm tra khi đăng ký* không phải chức năng riêng — chúng là
các bước bắt buộc bên trong `register()`, vẽ ra để thấy vì sao chức năng này là trọng
tâm kỹ thuật của đồ án.

---

## 3. Sequence — `register()` và chỗ đặt lock

Đây là sơ đồ nên đưa vào phần trọng tâm của báo cáo. Điểm cần chỉ vào khi bảo vệ là
**dòng `SELECT ... FOR UPDATE` ở bước 2**: từ đó trở đi không transaction nào khác
đọc-ghi được dòng lớp học phần này cho tới khi transaction hiện tại kết thúc.

```mermaid
sequenceDiagram
    autonumber
    actor SV as Sinh viên
    participant C as StudentRegistrationController
    participant S as RegistrationService
    participant R as RegistrationRules
    participant DB as MySQL (InnoDB)

    SV->>C: POST /student/sections/{id}/register
    C->>S: register(studentId, sectionId)

    rect rgb(235, 245, 255)
    note over S,DB: Bắt đầu @Transactional
    S->>DB: SELECT ... FOR UPDATE (khoá dòng class_sections)
    DB-->>S: bản ghi lớp, ĐÃ COMMIT mới nhất<br/>(locking read, không phải snapshot MVCC)

    S->>R: 1. cổng đăng ký còn mở?
    S->>R: 2. chưa đăng ký môn này?
    S->>R: 3. đã đạt môn tiên quyết?
    S->>R: 4. không trùng thời khóa biểu?
    S->>R: 5. chưa vượt giới hạn tín chỉ?
    R-->>S: qua hết 5 tầng

    alt registered_count >= capacity
        S-->>C: ClassFullException
    else còn chỗ
        S->>DB: UPDATE registered_count = registered_count + 1
        S->>DB: INSERT INTO registrations
    end
    note over S,DB: Commit — lock được nhả ở đây
    end

    C-->>SV: redirect + flash message
```

Ba câu trả lời sẵn cho câu hỏi có thể bị hỏi từ sơ đồ này:

- **Sao lock trước rồi mới validate, không làm ngược lại?** Làm ngược được và lock sẽ
  giữ ngắn hơn, nhưng phải kiểm tra lại sĩ số lần nữa sau khi lock, tức thêm một vòng
  truy vấn. Nhóm chọn thứ tự này vì dễ đọc và dễ chứng minh đúng; đánh đổi là thời gian
  giữ lock dài hơn mức tối thiểu.
- **Khoá thế thì hệ thống chậm không?** Lock ở mức **dòng**, và khoá đúng dòng lớp học
  phần đang bị tranh chấp. Hai sinh viên đăng ký hai lớp khác nhau không chờ nhau chút
  nào; chỉ người tranh cùng một lớp mới xếp hàng.
- **Trong lúc giữ lock có làm gì chậm không?** Không. Không gửi email, không gọi API
  ngoài, không `Thread.sleep`. Transaction giữ lock phải ngắn.
