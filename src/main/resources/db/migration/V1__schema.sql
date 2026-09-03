-- =============================================================================
-- V1: Schema goc - DUNG 5 BANG (DOAN.md muc 4)
--
-- Khong them bang nao ngoai 5 bang duoi day. Xem DOAN.md muc 14.
-- engine=InnoDB la BAT BUOC: chi InnoDB moi ho tro row-level lock,
-- ma toan bo lop ky thuat cua do an (SELECT ... FOR UPDATE, muc 7.1) dua vao no.
-- =============================================================================

create table users (
  id bigint auto_increment primary key,
  username  varchar(50)  not null unique,
  password  varchar(100) not null,          -- BCrypt
  full_name varchar(100) not null,
  code      varchar(20)  unique,            -- ma sinh vien; null voi tai khoan ADMIN
  email     varchar(100),
  role      varchar(20)  not null           -- ADMIN | STUDENT
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table courses (
  id bigint auto_increment primary key,
  code    varchar(20)  not null unique,
  name    varchar(150) not null,
  credits int not null,
  prerequisite_id bigint null,              -- tu tro, moi mon toi da 1 mon tien quyet
  foreign key (prerequisite_id) references courses(id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table semesters (
  id bigint auto_increment primary key,
  name         varchar(50) not null,        -- "Hoc ky 1 - 2026/2027"
  reg_open_at  datetime not null,
  reg_close_at datetime not null,
  max_credits  int not null default 24,
  active       boolean not null default false
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table class_sections (
  id bigint auto_increment primary key,
  code          varchar(20) not null unique,
  course_id     bigint not null,
  semester_id   bigint not null,
  lecturer_name varchar(100),               -- text, KHONG co vai giang vien
  room          varchar(20),
  day_of_week   tinyint not null,           -- 2..7
  start_period  tinyint not null,           -- tiet bat dau 1..12
  period_count  tinyint not null,
  capacity         int not null,
  registered_count int not null default 0,
  foreign key (course_id)   references courses(id),
  foreign key (semester_id) references semesters(id),
  -- Luoi an toan tang DB (muc 7.6). MySQL thuc su thi hanh CHECK tu 8.0.16.
  -- Chi chan duoc chieu TANG; chieu giam do drop() tu giu dung bang lock (muc 7.1).
  constraint chk_capacity check (registered_count <= capacity)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table registrations (
  id bigint auto_increment primary key,
  student_id    bigint not null,
  section_id    bigint not null,
  registered_at datetime not null,
  score         decimal(4,2) null,          -- null = chua co diem
  foreign key (student_id) references users(id),
  foreign key (section_id) references class_sections(id),
  -- Giai quyet race condition KHAC HAN voi row lock: bam nut hai lan, F5, mang lag.
  -- Hai race condition khac nhau can hai co che khac nhau (muc 7.6).
  unique key uk_student_section (student_id, section_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;
