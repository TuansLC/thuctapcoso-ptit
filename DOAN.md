# Đồ án Thực tập cơ sở — Website đăng ký học phần trực tuyến

> File này là bản đặc tả chốt của đồ án, đồng thời dùng làm prompt cho AI assistant.
> Cách dùng: mở file này trong ngữ cảnh chat, hoặc dán toàn bộ nội dung vào đầu
> cuộc hội thoại, rồi nêu việc cần làm.

---

## 1. Bối cảnh

| Mục | Nội dung |
|---|---|
| Môn học | Thực tập cơ sở — Học viện Công nghệ Bưu chính Viễn thông (PTIT) |
| Quy mô nhóm | 3 thành viên |
| Thời lượng | 1 học kỳ |
| Mục tiêu | Một web app hoàn chỉnh, chạy được, ít chức năng nhưng có **một** phần kỹ thuật đủ sâu để trình bày và bảo vệ |

Định hướng xuyên suốt: **đổi "làm nhiều chức năng" thành "làm một chức năng thật kỹ"**.
Chức năng đó là `register()` — đăng ký học phần với xử lý tranh chấp đồng thời.

---

## 2. Thông tin đăng ký đề tài

**Tên đề tài**

```
Xây dựng website đăng ký học phần trực tuyến
```

**Chức năng chính**

```
- Quản lý tài khoản, môn học & học kỳ
- Đăng ký - rút học phần, kiểm tra ràng buộc & xử lý tranh chấp chỗ đồng thời
- Quản lý lớp học phần, nhập điểm & tra cứu kết quả học tập (GPA)
```

**Ngôn ngữ sử dụng**

```
FE: Thymeleaf, Bootstrap 5
BE: Java Spring Boot 3.5
Database: MySQL 8
```

---

## 3. Stack và lý do chọn phiên bản

```
Java 21 LTS
Spring Boot 3.5.16
Spring Data JPA
Spring Security 6
Thymeleaf + thymeleaf-extras-springsecurity6
Flyway (migration)
MySQL 8
Bootstrap 5 (+ HTMX nếu cần ajax nhẹ)
Docker Compose cho MySQL
Maven
```

**Vì sao Spring Boot 3.5.x mà không phải 4.1.x** (kiểm chứng tháng 8/2026):

Bản Thymeleaf mới nhất là 3.1.5.RELEASE (21/4/2026), và [trang download của Thymeleaf](https://www.thymeleaf.org/download.html)
chỉ cung cấp hai artifact tích hợp Spring: `thymeleaf-spring5` và `thymeleaf-spring6`.
Không có `thymeleaf-spring7`. Spring Boot 4.x lại được xây trên Spring Framework 7,
nên chưa có module tích hợp Thymeleaf chính thức.

Nhánh 3.5 hết hỗ trợ OSS miễn phí từ 30/6/2026 (bản cuối là 3.5.16, ra 25/6/2026),
theo [dữ liệu vòng đời Spring Boot](https://endoflife.date/spring-boot). Với đồ án học phần
điều này không ảnh hưởng, và tài liệu cho Spring Boot 3.x nhiều hơn 4.x rất nhiều.

Đưa lập luận này vào báo cáo — nó cho thấy phiên bản được chọn có căn cứ.

*(Thông tin từ các nguồn trên đã được diễn giải lại để tuân thủ quy định về bản quyền nội dung.)*

---

## 4. Cơ sở dữ liệu — đúng 5 bảng

Không thêm bảng nếu không có lý do bắt buộc. Không gộp xuống 4 bảng
(gộp học kỳ vào lớp học phần sẽ gây dư thừa dữ liệu, vi phạm chuẩn hoá).

```sql
create table users (
  id bigint auto_increment primary key,
  username  varchar(50)  not null unique,
  password  varchar(100) not null,          -- BCrypt
  full_name varchar(100) not null,
  code      varchar(20)  unique,            -- mã sinh viên
  email     varchar(100),
  role      varchar(20)  not null           -- ADMIN | STUDENT
);

create table courses (
  id bigint auto_increment primary key,
  code    varchar(20)  not null unique,
  name    varchar(150) not null,
  credits int not null,
  prerequisite_id bigint null,              -- tự trỏ, mỗi môn tối đa 1 môn tiên quyết
  foreign key (prerequisite_id) references courses(id)
);

create table semesters (
  id bigint auto_increment primary key,
  name         varchar(50) not null,        -- "Học kỳ 1 - 2026/2027"
  reg_open_at  datetime not null,
  reg_close_at datetime not null,
  max_credits  int not null default 24,
  active       boolean not null default false
);

create table class_sections (
  id bigint auto_increment primary key,
  code          varchar(20) not null unique,
  course_id     bigint not null,
  semester_id   bigint not null,
  lecturer_name varchar(100),               -- text, KHÔNG có vai giảng viên
  room          varchar(20),
  day_of_week   tinyint not null,           -- 2..7
  start_period  tinyint not null,           -- tiết bắt đầu 1..12
  period_count  tinyint not null,
  capacity         int not null,
  registered_count int not null default 0,
  foreign key (course_id)   references courses(id),
  foreign key (semester_id) references semesters(id),
  constraint chk_capacity check (registered_count <= capacity)
);

create table registrations (
  id bigint auto_increment primary key,
  student_id    bigint not null,
  section_id    bigint not null,
  registered_at datetime not null,
  score         decimal(4,2) null,          -- null = chưa có điểm
  foreign key (student_id) references users(id),
  foreign key (section_id) references class_sections(id),
  unique key uk_student_section (student_id, section_id)
);
```

Ghi chú thiết kế:

- Mỗi lớp học một buổi mỗi tuần → lịch nằm luôn trên `class_sections`, không cần bảng lịch riêng.
- Điểm là cột `score` trên `registrations`, không có bảng `grades`.
- Điểm chữ và GPA **không lưu**, tính lúc hiển thị từ `score` và `credits`.
- `registered_count` là counter dư thừa có chủ đích (xem mục 7.6).

---

## 5. Phạm vi chức năng — đúng 6 chức năng

```
AUTH
  Đăng ký tài khoản sinh viên / Đăng nhập / Đăng xuất

ADMIN
  [nhập]     Quản lý môn học          - mã, tên, tín chỉ, chọn môn tiên quyết
  [nhập]     Quản lý học kỳ           - tên kỳ, mở/đóng cổng đăng ký, giới hạn tín chỉ
  [nhập]     Quản lý lớp học phần     - môn, kỳ, giảng viên, phòng, thứ/tiết, sĩ số
  [nhập]     Nhập điểm                - chọn lớp, nhập điểm cả bảng một lần

SINH VIÊN
  [xem+nhập] Lớp đang mở              - danh sách + lọc + nút đăng ký / rút
  [xem]      Học phần của tôi         - lịch, điểm, tổng tín chỉ, GPA
```

Bốn màn hình admin dùng chung một khuôn form, viết cái đầu xong ba cái sau là lặp pattern.

---

## 6. Nguyên tắc truy vết dữ liệu (quan trọng khi bảo vệ)

Mọi màn hình **xem** phải truy được về một màn hình **nhập** trong cùng hệ thống.
Không dữ liệu nào từ ngoài rơi vào.

| Dữ liệu được xem | Ai nhập | Ở đâu |
|---|---|---|
| Tài khoản đăng nhập | Sinh viên | Trang đăng ký tài khoản |
| Môn học, tín chỉ, môn tiên quyết | Admin | Quản lý môn học |
| Học kỳ, thời gian mở cổng, giới hạn tín chỉ | Admin | Quản lý học kỳ |
| Lớp mở, giảng viên, phòng, thứ/tiết, sĩ số | Admin | Quản lý lớp học phần |
| Đăng ký học phần | Sinh viên | Lớp đang mở |
| Điểm số | Admin | Nhập điểm |
| Điểm chữ, GPA, tỉ lệ lấp đầy | không ai — hệ thống tính | dữ liệu dẫn xuất |

Seed dữ liệu bằng Flyway để danh sách trông đầy là được, nhưng chỉ seed những loại
**có màn hình nhập tương ứng**, và khi demo phải demo được đường sinh ra ít nhất
một bản ghi mỗi loại.

---

## 7. Lõi kỹ thuật — trọng tâm của toàn bộ đồ án

### 7.1 Hàm chính

```java
@Transactional
public void register(Long studentId, Long sectionId) {
    ClassSection sec = sectionRepo.findByIdForUpdate(sectionId)   // SELECT ... FOR UPDATE
            .orElseThrow(() -> new NotFoundException("Lớp không tồn tại"));

    checkRegistrationOpen(sec.getSemester());        // cổng đăng ký còn mở
    checkNotAlreadyRegistered(studentId, sec);       // chưa đăng ký môn này
    checkPrerequisitePassed(studentId, sec.getCourse());  // đã đạt môn tiên quyết
    checkNoScheduleConflict(studentId, sec);         // không trùng thời khóa biểu
    checkCreditLimit(studentId, sec);                // chưa vượt giới hạn tín chỉ

    if (sec.getRegisteredCount() >= sec.getCapacity())
        throw new ClassFullException("Lớp đã đủ sĩ số");

    sec.setRegisteredCount(sec.getRegisteredCount() + 1);
    registrationRepo.save(Registration.of(studentId, sec));
}
```

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select c from ClassSection c where c.id = :id")
Optional<ClassSection> findByIdForUpdate(@Param("id") Long id);
```

### 7.2 Quy tắc nghiệp vụ

- Trùng thời khóa biểu: cùng `day_of_week` **và** khoảng tiết giao nhau.
  Điều kiện giao của hai đoạn: `startA < endB && startB < endA`.
- Môn tiên quyết đạt = tồn tại `registrations` của sinh viên với môn đó và `score >= 4.0`.
- Giới hạn tín chỉ: tổng `credits` các môn đã đăng ký trong kỳ + môn mới `<= semesters.max_credits`.
- Không cho rút môn khi `score` khác null, hoặc khi cổng đăng ký đã đóng.
- Thang điểm chữ: A ≥ 8.5, B+ ≥ 8.0, B ≥ 7.0, C+ ≥ 6.5, C ≥ 5.5, D+ ≥ 5.0, D ≥ 4.0, dưới 4.0 là F.
- GPA thang 4 tính theo trung bình có trọng số tín chỉ.

### 7.3 Vì sao không viết naive

Đoạn code sai mà phần lớn đồ án viết:

```java
if (section.getRegisteredCount() < section.getCapacity()) {
    section.setRegisteredCount(section.getRegisteredCount() + 1);
    registrationRepo.save(...);
}
```

Dòng thời gian gây lỗi (lớp sĩ số 40, đang có 39):

```
t1   A đọc count = 39
t2                      B đọc count = 39
t3   A: 39 < 40 → qua
t4                      B: 39 < 40 → qua
t5   A ghi count = 40
t6                      B ghi count = 40      ← ghi đè
→ 41 sinh viên trong lớp, count = 40
```

Đây là lỗi **lost update**. Chỉ xuất hiện khi có tải, test một mình không bao giờ thấy.

### 7.4 Bác bỏ hai giải pháp sai

**`synchronized`** chỉ có tác dụng trong một JVM. Hai instance sau load balancer là hỏng.
Nó khoá theo tiến trình chứ không theo dữ liệu, nên người đăng ký hai lớp khác nhau
vẫn phải chờ nhau vô ích.

**Nâng isolation level** không giải quyết được. MySQL InnoDB mặc định là REPEATABLE READ,
và REPEATABLE READ *không* ngăn lost update ở kiểu đọc-rồi-ghi tại tầng ứng dụng, vì
`SELECT` thường đọc theo snapshot MVCC nên không thấy thay đổi transaction khác vừa commit.
Chỉ SERIALIZABLE mới ngăn được, nhưng nó thêm lock vào mọi SELECT trong toàn hệ thống.

Mấu chốt: `SELECT ... FOR UPDATE` là **locking read**, đọc bản mới nhất đã commit
chứ không đọc snapshot. Đó là lý do nó giải quyết được vấn đề.

### 7.5 So sánh bốn phương án

| Phương án | Ưu | Nhược | Kết luận |
|---|---|---|---|
| `synchronized` | dễ viết | chết khi nhiều instance | loại |
| Optimistic lock `@Version` | không giữ lock, tốt khi ít tranh chấp | ngày mở cổng tranh chấp cao → retry liên tục | không phù hợp |
| Pessimistic lock `FOR UPDATE` | đúng chắc chắn, kiểm soát được | giữ lock suốt transaction, cần đề phòng deadlock | **chọn** |
| `UPDATE` có điều kiện, một câu | nhanh nhất, lock ngắn nhất | không chèn được logic phức tạp ở giữa | biết và nêu được |

Phương án bốn (compare-and-swap ở tầng DB):

```sql
UPDATE class_sections
   SET registered_count = registered_count + 1
 WHERE id = ? AND registered_count < capacity;
```

Số dòng ảnh hưởng bằng 1 là chiếm được chỗ, bằng 0 là lớp đã đầy. Không cần lock
tường minh vì câu `UPDATE` tự giữ exclusive lock và đánh giá điều kiện trên dữ liệu
mới nhất. Chọn phương án 3 vì còn 5 tầng kiểm tra phải chạy trong cùng transaction,
nhưng **phải nêu được rằng biết phương án 4 và biết khi nào nó tốt hơn**.

### 7.6 Phòng thủ nhiều lớp

Validate ở tầng service để có thông báo lỗi thân thiện. Ràng buộc ở tầng DB để dữ liệu
không bao giờ sai kể cả khi code có bug.

```sql
constraint chk_capacity check (registered_count <= capacity)
unique key uk_student_section (student_id, section_id)
```

- MySQL chỉ thực sự thi hành `CHECK` từ 8.0.16 trở đi; các bản trước nhận cú pháp rồi bỏ qua.
- `UNIQUE(student_id, section_id)` giải quyết **race condition khác hẳn**: bấm nút hai lần,
  F5, mạng lag. Lock trên lớp học phần không phải thứ chặn ca này.
  Hai race condition khác nhau cần hai cơ chế khác nhau.

### 7.7 Lock đúng đối tượng

Lock `class_sections` chứ không lock `registrations`, vì thứ bị tranh chấp là **sức chứa
của lớp**. Lock `registrations` vô nghĩa — mỗi sinh viên tạo một dòng riêng, không ai tranh với ai.

Hệ quả tốt: lock ở mức dòng nên hai sinh viên đăng ký hai lớp khác nhau không chờ nhau
chút nào. Chỉ người tranh cùng một lớp mới xếp hàng. Đây là câu trả lời cho
"khoá thế thì hệ thống chậm à".

### 7.8 `registered_count` là quyết định thiết kế

Cột này về lý thuyết dư thừa, tính được bằng `SELECT COUNT(*) FROM registrations WHERE section_id = ?`.
Vẫn lưu vì hai lý do: đọc nhanh khi hiển thị hàng trăm lớp, và cần chỗ cụ thể để gắn `CHECK`.
Đánh đổi là phải tự giữ đồng bộ.

Nếu bị hỏi "sao không đếm trực tiếp": đếm trực tiếp *cũng* an toàn miễn là nằm trong
transaction đã lock dòng lớp học phần. Nhóm chọn counter vì hiệu năng đọc.

### 7.9 Thứ tự lock và deadlock

Nếu làm đăng ký nhiều môn một lượt, phải lock theo **id tăng dần**:

```java
sectionIds.stream().sorted().forEach(id -> sectionRepo.findByIdForUpdate(id));
```

Hai transaction lock cùng hai dòng theo thứ tự ngược nhau sẽ deadlock.
Nguyên tắc kèm theo: transaction giữ lock phải ngắn — không gửi email, không gọi
API ngoài trong lúc đang giữ lock.

---

## 8. Test đồng thời — bằng chứng của đồ án

```java
@Test
void chi_dung_capacity_sinh_vien_dang_ky_thanh_cong() throws Exception {
    var pool  = Executors.newFixedThreadPool(32);
    var latch = new CountDownLatch(1);
    var ok    = new AtomicInteger();

    for (int i = 0; i < 200; i++) {
        long sv = studentIds.get(i);
        pool.submit(() -> {
            latch.await();                      // chặn tất cả ở một vạch
            try { service.register(sv, sectionId); ok.incrementAndGet(); }
            catch (Exception ignored) {}
            return null;
        });
    }
    latch.countDown();                          // thả cùng lúc
    pool.shutdown();
    pool.awaitTermination(60, TimeUnit.SECONDS);

    assertThat(ok.get()).isEqualTo(40);
    assertThat(sectionRepo.findById(sectionId).orElseThrow()
            .getRegisteredCount()).isEqualTo(40);
}
```

`CountDownLatch` là chi tiết bắt buộc: nếu thả 200 thread ra tự chạy, chúng khởi động
lệch nhau vài chục milli giây và **không thực sự đồng thời** — test sẽ xanh dù code sai.

Bảng số liệu đưa vào báo cáo:

```
Lớp sĩ số 40, 200 yêu cầu đồng thời

Chưa có lock:   200 thành công,  registered_count = 200,  sĩ số thực 200   SAI
Có lock:         40 thành công,  registered_count =  40,  sĩ số thực  40   ĐÚNG
```

---

## 9. Kịch bản demo (đúng chuỗi phụ thuộc dữ liệu)

Demo theo thứ tự này thì không ai hỏi được "dữ liệu ở đâu ra", vì họ thấy nó sinh ra ngay trước mắt.

```
1.  Đăng ký 3 tài khoản sinh viên A, B, C
2.  Admin tạo môn "Lập trình Java" (3 TC)
           tạo môn "Lập trình Web" (3 TC, tiên quyết = Lập trình Java)
3.  Admin tạo Học kỳ 1, mở cổng đăng ký
4.  Admin mở lớp Lập trình Java — Thứ 2 tiết 1-3, sĩ số 2
5.  SV A  đăng ký → được
6.  SV B  đăng ký → được, lớp đầy
7.  SV C  đăng ký → bị chặn "Lớp đã đủ sĩ số"
8.  Admin nhập điểm: A = 7.5, B = 3.0
9.  SV A  xem điểm + GPA                    ← do bước 8 sinh ra
10. Admin đóng kỳ 1, tạo Học kỳ 2, mở lớp Lập trình Web
11. SV A  đăng ký Lập trình Web → được (đã đạt tiên quyết)
12. SV B  đăng ký Lập trình Web → bị chặn (tiên quyết chưa đạt, 3.0 < 4.0)
13. SV C  đăng ký Lập trình Web → bị chặn (chưa học tiên quyết)
14. SV A  đăng ký lớp trùng Thứ 2 tiết 2   → bị chặn trùng lịch
15. Chạy test 200 thread vào lớp sĩ số 40
```

Vòng lặp bước 8 → 12 là phần đẹp nhất: **điểm do hệ thống nhập ở kỳ 1 trở thành điều
kiện chặn đăng ký ở kỳ 2**. Dữ liệu chảy trọn vòng bên trong hệ thống.

---

## 10. Ba điểm nói trong 3 phút bảo vệ

1. Chỉ ra lỗi lost update bằng dòng thời gian (mục 7.3)
2. Vì sao nâng isolation level không giải quyết được, và locking read khác snapshot read ra sao (mục 7.4)
3. Bảng số liệu trước/sau khi có lock (mục 8)

Kết bằng hai câu:

- **Nối vào thực tế:** hệ thống đăng ký học phần của các trường sập vào ngày mở cổng
  chính vì bài toán này cộng với tải. Chia đợt đăng ký theo khóa là giải pháp *tổ chức*,
  lock là giải pháp *kỹ thuật*. Hai cái bổ trợ nhau.
- **Nói ra giới hạn:** pessimistic lock khiến yêu cầu vào cùng một lớp bị xếp hàng, nên
  throughput trên một lớp bị chặn bởi thời gian giữ transaction. Ở quy mô hàng chục nghìn
  sinh viên, giải pháp thật là hàng đợi hoặc giữ chỗ trên Redis trước khi ghi DB.
  Nhóm không làm vì vượt phạm vi, nhưng biết đó là hướng đi tiếp.

---

## 11. Câu hỏi thầy có thể hỏi và cách trả lời

| Câu hỏi | Trả lời |
|---|---|
| Muốn xem điểm thì phải làm gì trước? | Chỉ vào kịch bản mục 9 — điểm do admin nhập ở bước 8 |
| Sinh viên rút môn sau khi có điểm? | Chặn rút khi `score` khác null, và khi cổng đăng ký đã đóng |
| Xoá môn đang có lớp mở? | Không xoá cứng, chỉ ẩn; hoặc chặn nếu còn lớp tham chiếu |
| Đóng cổng mà lớp thiếu sĩ số? | Admin xoá lớp, hệ thống xoá đăng ký kèm theo và thông báo |
| Nhập sai điểm? | Cho sửa khi kỳ chưa chốt |
| Sao không có bảng GPA? | GPA là dữ liệu dẫn xuất, tính từ `score` và `credits` khi hiển thị |
| Sao không dùng Spring Boot mới nhất? | Thymeleaf chưa có module tích hợp Spring Framework 7 (mục 3) |
| Sao không dùng `synchronized`? | Mục 7.4 |
| Sao không nâng isolation level? | Mục 7.4 |
| Khoá thế hệ thống chậm không? | Lock ở mức dòng, chỉ người tranh cùng lớp mới xếp hàng (mục 7.7) |

---

## 12. Cấu trúc project

```
src/main/java/com/ptit/courseregistration/
├── CourseRegistrationApplication.java
├── config/          SecurityConfig, WebConfig
├── domain/          User, Course, Semester, ClassSection, Registration, Role enum
├── repository/      5 repository, có findByIdForUpdate
├── service/         AuthService, CourseService, SemesterService,
│                    ClassSectionService, RegistrationService, GradeService
├── controller/      AuthController, AdminCourseController, AdminSemesterController,
│                    AdminSectionController, AdminGradeController,
│                    StudentRegistrationController, StudentDashboardController
├── dto/
└── exception/        GlobalExceptionHandler, ClassFullException,
                     PrerequisiteNotMetException, ScheduleConflictException,
                     CreditLimitExceededException

src/main/resources/
├── templates/
│   ├── layout.html
│   ├── fragments/   header, nav, alerts, pagination
│   ├── auth/        login, register
│   ├── admin/       courses, semesters, sections, grades
│   └── student/     sections, my-courses
├── static/
├── db/migration/    V1__schema.sql, V2__seed.sql
└── application.yml

src/test/java/.../RegistrationConcurrencyTest.java

docker-compose.yml
pom.xml
```

Quy ước:

- Dùng Flyway, **không** dùng `spring.jpa.hibernate.ddl-auto=update`.
- Mật khẩu băm BCrypt.
- Nghiệp vụ nằm ở service, controller chỉ điều phối.
- Lỗi nghiệp vụ ném exception riêng, `GlobalExceptionHandler` chuyển thành flash message.
- Thymeleaf dùng layout + fragment, không lặp HTML.

---

## 13. Chia việc 3 người

```
Người 1   Spring Security + auth
          RegistrationService: lock + 5 tầng validate + rút môn
          RegistrationConcurrencyTest
          → phần khó nhất, cần người mạnh nhất

Người 2   4 màn hình admin (môn học, học kỳ, lớp học phần, nhập điểm)
          Tính điểm chữ, GPA
          Migration Flyway + dữ liệu seed

Người 3   2 màn hình sinh viên, layout Thymeleaf toàn bộ, Bootstrap
          Lọc và phân trang danh sách lớp
          ERD, sơ đồ use case, báo cáo, slide
```

Chốt `ClassSection` và `Registration` trong tuần đầu rồi mới code song song.

---

## 14. Ranh giới phạm vi — KHÔNG làm những thứ sau

Danh sách này để chống phình phạm vi. Nếu muốn thêm, phải là quyết định có ý thức.

- Không có thanh toán, ví, đặt cọc
- Không có vai giảng viên (giảng viên là cột text trên lớp học phần)
- Không có hàng đợi chờ lớp (hết chỗ thì báo lỗi)
- Không có bảng audit log toàn hệ thống
- Không có khoa, ngành, chương trình khung
- Không có đợt đăng ký chia theo khóa
- Không có lịch nhiều buổi mỗi tuần, không có khoảng tuần học
- Không có nhiều môn tiên quyết cho một môn
- Không có REST API + SPA — dùng Thymeleaf render server-side
- Không có WebSocket
- Không quá 5 bảng, không quá 6 chức năng

Làm sau nếu còn thời gian, theo thứ tự ưu tiên:

1. Lưới thời khóa biểu dạng bảng tuần
2. Xuất PDF phiếu đăng ký, xuất Excel bảng điểm
3. Thống kê tỉ lệ lấp đầy lớp cho admin
4. Import danh sách môn học từ Excel

---

## 15. Cách dùng file này làm prompt

Mở phiên chat mới trên máy khác, đưa file này vào ngữ cảnh rồi nói việc cần làm. Ví dụ:

```
Đọc DOAN.md. Dựng khung project theo mục 12: pom.xml, docker-compose.yml,
migration Flyway với 5 bảng ở mục 4, entity JPA, SecurityConfig.
```

```
Đọc DOAN.md. Viết RegistrationService theo mục 7.1 và 7.2, đầy đủ 5 tầng validate,
kèm exception riêng cho từng loại lỗi.
```

```
Đọc DOAN.md. Viết RegistrationConcurrencyTest theo mục 8.
```

```
Đọc DOAN.md. Làm màn hình "Lớp đang mở" cho sinh viên: danh sách, lọc theo môn,
nút đăng ký và rút, dùng Thymeleaf + Bootstrap 5 theo quy ước mục 12.
```

Nhắc AI tuân thủ mục 14 nếu thấy nó tự thêm chức năng ngoài phạm vi.
