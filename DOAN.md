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
- `users.code` là `unique` nhưng cho phép null, vì tài khoản ADMIN không có mã sinh viên.
  MySQL cho phép nhiều dòng null trong một unique index nên điều này hợp lệ.
- **Không dùng `ON DELETE CASCADE`** ở bất kỳ khoá ngoại nào. Hệ quả: xoá lớp học phần
  đang có sinh viên sẽ nổ lỗi khoá ngoại, nên `ClassSectionService` phải xoá
  `registrations` của lớp trước rồi mới xoá lớp, trong cùng một transaction. Chọn cách này
  thay vì cascade để việc xoá dữ liệu sinh viên luôn là hành động tường minh trong code,
  đọc code là thấy, không bị DB âm thầm làm hộ.

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

**Rút môn cũng phải lock.** `drop()` giảm `registered_count`, tức cũng là đọc-rồi-ghi,
nên cũng bị lost update. Bỏ lock ở đây thì counter trôi lệch dần xuống dưới số thực,
và `chk_capacity` không bắt được vì nó chỉ chặn chiều tăng.

```java
@Transactional
public void drop(Long studentId, Long sectionId) {
    ClassSection sec = sectionRepo.findByIdForUpdate(sectionId)   // lock y như register()
            .orElseThrow(() -> new NotFoundException("Lớp không tồn tại"));

    Registration reg = registrationRepo
            .findByStudentIdAndSectionId(studentId, sectionId)
            .orElseThrow(() -> new NotFoundException("Bạn chưa đăng ký lớp này"));

    checkRegistrationOpen(sec.getSemester());        // cổng đăng ký còn mở
    if (reg.getScore() != null)
        throw new CannotDropException("Lớp đã có điểm, không rút được");

    sec.setRegisteredCount(sec.getRegisteredCount() - 1);
    registrationRepo.delete(reg);
}
```

### 7.2 Quy tắc nghiệp vụ

- Môn tiên quyết đạt = tồn tại `registrations` của sinh viên với môn đó và `score >= 4.0`.
- Giới hạn tín chỉ: tổng `credits` các môn đã đăng ký trong kỳ + môn mới `<= semesters.max_credits`.
- Không cho rút môn khi `score` khác null, hoặc khi cổng đăng ký đã đóng.
- Admin **không được hạ `capacity` xuống dưới `registered_count` hiện tại**. Chặn ở
  `ClassSectionService` với thông báo rõ ràng, vì nếu để lọt xuống DB thì vi phạm
  `chk_capacity` và người dùng nhận lỗi SQL thô.
- Admin xoá lớp học phần: xoá `registrations` của lớp đó trước rồi mới xoá lớp, làm trong
  cùng một transaction ở service (xem ghi chú mục 4 về việc không dùng `ON DELETE CASCADE`).

**Trùng thời khóa biểu**

Hai lớp trùng khi cùng `day_of_week` **và** khoảng tiết giao nhau. Điều kiện giao của
hai đoạn là `startA < endB && startB < endA`, trong đó:

```
end = start_period + period_count        (biên phải MỞ)
```

Định nghĩa `end` phải ghi rõ vì đây là chỗ dễ sai off-by-one nhất cả đồ án. Nếu ai code
`end = start_period + period_count - 1` thì hai lớp liền kề bị báo trùng oan, mà phần lớn
trường hợp khác vẫn đúng nên bug rất khó thấy. Hai ca kiểm chứng bắt buộc có trong test:

```
tiết 1-3 (start=1, end=4)  vs  tiết 4-6 (start=4, end=7)
  → 1 < 7 && 4 < 4  →  false  →  KHÔNG trùng   (liền kề, phải cho đăng ký)

tiết 1-3 (start=1, end=4)  vs  tiết 3-5 (start=3, end=6)
  → 1 < 6 && 3 < 4  →  true   →  TRÙNG         (chồng tiết 3)
```

**Điểm chữ và GPA**

Thang điểm chữ theo `score` (thang 10), và bảng quy đổi sang thang 4 để tính GPA:

| Điểm chữ | Điều kiện `score` | Quy đổi thang 4 |
|---|---|---|
| A  | ≥ 8.5 | 4.0 |
| B+ | ≥ 8.0 | 3.5 |
| B  | ≥ 7.0 | 3.0 |
| C+ | ≥ 6.5 | 2.5 |
| C  | ≥ 5.5 | 2.0 |
| D+ | ≥ 5.0 | 1.5 |
| D  | ≥ 4.0 | 1.0 |
| F  | < 4.0 | 0.0 |

```
GPA = Σ(quy_đổi_4 × credits) / Σ(credits)
```

Ba quy tắc phải chốt, không để mỗi người hiểu một kiểu:

- Môn có `score` null (chưa có điểm) **không** vào GPA, bỏ khỏi cả tử và mẫu.
- Môn F **có** vào GPA: tử cộng 0, mẫu vẫn cộng `credits`. Môn đã học và trượt thì phải
  kéo GPA xuống, nếu loại khỏi mẫu thì trượt lại thành có lợi.
- Hiển thị GPA làm tròn 2 chữ số thập phân. Không lưu vào DB (mục 4).

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

### 8.1 Hai điều kiện bắt buộc về môi trường chạy

Toàn bộ sức nặng của đồ án nằm ở test này, nên nếu chạy sai môi trường thì kết quả vô giá
trị — và tệ hơn là nó vẫn xanh, làm nhóm tin là code đúng.

**Không được đánh `@Transactional` lên test.** Nếu có, dữ liệu setup nằm trong transaction
chưa commit của test, 200 thread chạy ở transaction khác sẽ không thấy nó, và hành vi lock
không phản ánh thực tế chút nào. Test sẽ xanh hoặc đỏ vì một lý do hoàn toàn khác với lý do
nhóm nghĩ. Dọn dữ liệu bằng `@BeforeEach`/`@AfterEach` xoá tay, không dùng rollback tự động.

**Phải chạy trên MySQL thật, không phải H2.** H2 xử lý `SELECT ... FOR UPDATE` và `CHECK`
khác MySQL InnoDB. Chứng minh chống lost update trên H2 là chứng minh về H2, không phải về
hệ thống sẽ chạy. Dùng Testcontainers, hoặc trỏ thẳng vào MySQL trong `docker-compose.yml`
qua một profile test riêng. Cả hai cách đều không phát sinh chi phí vì đã có Docker.

Thêm hai điểm nhỏ nhưng dễ vướng:

- Test cần 200 tài khoản sinh viên. Seed chúng trong phần setup của test, **không** nhét vào
  `V2__seed.sql` — migration không nên chứa 200 user rác.
- Connection pool phải đủ lớn cho 32 thread, nếu không thread sẽ chờ ở pool thay vì chờ ở
  lock, và bài đo mất ý nghĩa. Đặt `spring.datasource.hikari.maximum-pool-size` ≥ 32 trong
  cấu hình test.

### 8.2 Test

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

### 8.3 Số liệu ĐO THẬT

Đây là kết quả chạy thật trên MySQL 8.0.46 qua Testcontainers, không phải số minh hoạ.
Bảng này mới là bảng đưa vào báo cáo.

```
Lớp sĩ số 40, 200 yêu cầu đồng thời, pool 32 thread, thả cùng lúc bằng CountDownLatch

                    thành công   registered_count   số dòng registrations   kết luận
Có lock                    40                 40                      40   ĐÚNG
Không lock                 11                  5                      11   SAI
```

Cột "có lock" **giống nhau tuyệt đối ở mọi lần chạy** — đó chính là điều cần chứng minh.
Cột "không lock" thì dao động theo tải của máy (hai lần chạy liên tiếp cho 10 và 11 lượt
thành công, counter 5 cả hai lần). Vì vậy test không assert vào con số cụ thể, chỉ assert
vào bất biến số học luôn đúng.

Cách đọc cột "không lock": hệ thống **tưởng** trong lớp có 5 sinh viên, nhưng thực tế
có 10 bản ghi đăng ký. Counter đếm thiếu 5 vì nhiều transaction cùng đọc một giá trị cũ
rồi ghi đè lên nhau — đúng định nghĩa lost update.

Ba điều rút ra từ số liệu thật, cả ba đều nên nói khi bảo vệ:

- **`chk_capacity` không hề báo lỗi** trong lần chạy sai. Ràng buộc đó so `registered_count`
  với `capacity`, mà `registered_count` lại chính là con số đã bị đếm thiếu. Ràng buộc
  tầng DB nhìn thấy mọi thứ bình thường trong khi dữ liệu đã sai. Đây là lý do không thể
  chỉ dựa vào `CHECK` để thay cho lock.
- **Bản không lock còn sinh deadlock thật** (MySQL error 1213, SQLState 40001). Nó không
  chỉ cho ra số sai mà còn không ổn định.
- Con số "không lock" **thấp hơn** trực giác ban đầu (10 chứ không phải 200) vì phần lớn
  thread chết giữa đường do tranh chấp ghi. Điều này không làm nhẹ vấn đề: 10 bản ghi
  thật trên một counter báo 5 nghĩa là nếu sĩ số lớp là 5 thì đã có 10 người vào lớp.

Ghi chú kỹ thuật khi viết bản đối chứng: **không** chèn `Thread.sleep` giữa đọc và ghi
để "làm rõ" race condition. Đã thử và kết quả ngược lại — sleep giữ lock ghi trên dòng
lớp học phần lâu hơn, khiến gần hết thread chết vì `innodb_lock_wait_timeout` thay vì lọt
qua được, tức là che mất chính cái bug cần chỉ ra.

Assertion đúng cho bản không lock là hai điều kiện luôn đúng về mặt số học, không phụ
thuộc tốc độ máy:

```java
assertThat(actualRows).isGreaterThan(counter);      // counter đếm thiếu
assertThat(counter).isLessThanOrEqualTo(CAPACITY);  // nên chk_capacity không bắt được
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
| Đóng cổng mà lớp thiếu sĩ số? | Admin xoá lớp; service xoá `registrations` của lớp trước rồi xoá lớp, cùng transaction (mục 4) |
| Sao không dùng `ON DELETE CASCADE`? | Để việc xoá dữ liệu sinh viên là hành động tường minh trong code, không để DB âm thầm làm (mục 4) |
| Hạ sĩ số lớp xuống dưới số đã đăng ký? | Chặn ở `ClassSectionService`, nếu không sẽ vi phạm `chk_capacity` và lỗi SQL lọt ra UI (mục 7.2) |
| Rút môn có bị lost update không? | Có, vì cũng là đọc-rồi-ghi trên `registered_count`. `drop()` lock y như `register()` (mục 7.1) |
| Nhập sai điểm? | Cho sửa khi kỳ chưa chốt |
| Môn trượt có tính vào GPA không? | Có, tử cộng 0 và mẫu vẫn cộng tín chỉ. Loại khỏi mẫu thì trượt lại thành có lợi (mục 7.2) |
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
├── config/          SecurityConfig, AdminBootstrap
│                    (không có WebConfig: xem mục 12.1)
├── domain/          User, Course, Semester, ClassSection, Registration,
│                    Role enum, LetterGrade enum
├── repository/      5 repository, có findByIdForUpdate
├── service/         AuthService, AppUserDetails, RegistrationRules,
│                    RegistrationService, CourseService, SemesterService,
│                    ClassSectionService, GradeService
├── controller/      AuthController, HomeController, AdminCourseController,
│                    AdminSemesterController, AdminSectionController,
│                    AdminGradeController, StudentRegistrationController,
│                    StudentDashboardController
├── dto/             RegisterForm, CourseForm, SemesterForm, ClassSectionForm,
│                    GradeEntryForm, OpenSectionView, SemesterResult, AcademicSummary
└── exception/       GlobalExceptionHandler, BusinessException (lớp cha),
                     NotFoundException, RegistrationClosedException,
                     AlreadyRegisteredException, PrerequisiteNotMetException,
                     ScheduleConflictException, CreditLimitExceededException,
                     ClassFullException, CannotDropException

src/main/resources/
├── templates/
│   ├── layout.html
│   ├── fragments/   header (gồm cả nav), alerts, pagination
│   ├── auth/        login, register
│   ├── admin/       courses, semesters, sections, grades
│   └── student/     sections, my-courses
├── static/
├── db/migration/    V1__schema.sql, V2__seed.sql
└── application.yml

src/test/java/.../RegistrationConcurrencyTest.java   ← 2 test: có lock và không lock
src/test/java/.../ScheduleConflictTest.java         ← ca biên tiết học ở mục 7.2
src/test/java/.../GradeServiceTest.java             ← ngưỡng điểm chữ và 3 quy tắc GPA
src/test/resources/application-test.yml             ← pool ≥ 32; datasource do Testcontainers cấp

docs/DIAGRAMS.md                                    ← ERD, use case, sequence cho báo cáo

docker-compose.yml
start-dev.cmd                                       ← chạy với JDK 21 (xem mục 12.2)
pom.xml
```

Quy ước:

- Dùng Flyway, **không** dùng `spring.jpa.hibernate.ddl-auto=update`. Đang đặt `validate`
  để Hibernate báo ngay nếu entity lệch schema Flyway.
- Mật khẩu băm BCrypt.
- Nghiệp vụ nằm ở service, controller chỉ điều phối.
- Lỗi nghiệp vụ ném exception riêng, `GlobalExceptionHandler` chuyển thành flash message.
- Thymeleaf dùng layout + fragment, không lặp HTML.
- Test đồng thời chạy trên MySQL thật và không có `@Transactional` (mục 8.1).
- Không dùng Lombok: getter/setter viết tay để sinh viên đọc code không cần biết thêm
  công cụ sinh mã.

### 12.1 Những chỗ code lệch nhẹ so với đặc tả, và lý do

Ghi lại để người sau không tưởng là làm sai.

| Chỗ lệch | Lý do |
|---|---|
| Thêm `BusinessException` làm lớp cha của mọi lỗi nghiệp vụ | `GlobalExceptionHandler` bắt một lớp là đủ, không phải liệt kê từng loại |
| Thêm `RegistrationClosedException`, `AlreadyRegisteredException` | Để 5 tầng validate có 5 thông báo riêng, đúng tinh thần "exception riêng cho từng loại lỗi" |
| Tách `RegistrationRules` khỏi `RegistrationService` | 5 hàm validate còn được màn hình "Lớp đang mở" dùng để báo trước lý do bị chặn. Tách ra để chỉ có MỘT bản logic |
| `AdminBootstrap` tạo admin lúc khởi động thay vì seed bằng Flyway | Hash BCrypt phải do chính encoder của ứng dụng sinh. Quan trọng hơn: nhét hash của một mật khẩu đã biết vào migration là đưa mật khẩu dùng chung vào lịch sử git vĩnh viễn. Mật khẩu sinh ngẫu nhiên và in ra log một lần |
| `LetterGrade` là enum trong `domain/` | Bảng quy đổi điểm chữ sang thang 4 cần một chỗ duy nhất; enum là chỗ tự nhiên nhất |
| `getScheduleLabel()` nằm trên `ClassSection` | Template gọi trực tiếp được, và chuỗi "Thứ 2, tiết 1-3" chỉ sinh ra ở một nơi |
| Chỉ MỘT học kỳ `active` tại một thời điểm | Màn hình "Lớp đang mở" phải biết hiển thị kỳ nào; hai kỳ active cùng lúc thì giới hạn tín chỉ cũng nhập nhằng |
| Chặn đổi thứ/tiết của lớp đã có sinh viên | Đổi lịch sẽ làm thời khóa biểu của họ trùng nhau mà không ai kiểm tra lại |
| **Không có `WebConfig`** dù mục 12 từng liệt kê | `WebConfig` chỉ cần khi phải đăng ký interceptor, formatter hay view resolver riêng. Project này không cần thứ nào trong số đó, nên tạo file rỗng chỉ để khớp danh sách là thêm code rác |
| Fragment `nav` gộp vào `header.html` | Navbar Bootstrap là một khối liền, tách hai file chỉ làm khó đọc |

### 12.2 Môi trường chạy trên máy hiện tại

- **Java 21 là bắt buộc.** Nếu `JAVA_HOME` đang trỏ JDK cũ, `mvn` báo
  *release version 21 not supported*. Dùng `start-dev.cmd` để chạy tạm, nhưng cách đúng
  là sửa `JAVA_HOME` hệ thống.
- **MySQL chạy ở cổng 3307**, không phải 3306, để không tranh cổng với MySQL của project
  khác trên cùng máy. Cả `docker-compose.yml` và `application.yml` đọc biến `DB_PORT`
  nên đổi một chỗ là đổi cả hai.
- Bootstrap 5 đóng gói trong jar qua webjar, không lấy từ CDN: lúc bảo vệ không phụ thuộc
  mạng. Đổi phiên bản phải sửa cả `pom.xml` và `templates/layout.html`.

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
          ERD, sơ đồ use case (đã có bản nguồn ở docs/DIAGRAMS.md), báo cáo, slide
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
- **Không tích hợp AI/LLM.** Đã cân nhắc và loại (8/2026): môn học không yêu cầu, và thêm
  vào thì làm loãng đúng phần trọng tâm là mục 7. Ba phương án từng xét: tự động xếp thời
  khoá biểu bằng backtracking (khả thi nhất, 0 chi phí, 0 bên thứ ba, nhưng tốn 1–1,5 tuần
  và phải refactor vào lõi đã chạy đúng), tìm kiếm ngôn ngữ tự nhiên qua LLM (cần API key,
  phụ thuộc mạng lúc demo, dữ liệu ra ngoài), chatbot RAG tư vấn quy chế (cần bảng thứ 6 để
  lưu hội thoại, và câu trả lời không truy được về màn hình nhập nào nên phá mục 6). Nếu về
  sau có người muốn mở lại, đọc đoạn này trước.

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
