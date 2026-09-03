-- =============================================================================
-- V2: Du lieu seed cho moi truong phat trien
--
-- NGUYEN TAC TRUY VET (DOAN.md muc 6): chi seed nhung loai du lieu CO man hinh
-- nhap tuong ung. Vi vay o day chi co mon hoc, hoc ky, lop hoc phan -- ba loai
-- deu do Admin nhap duoc qua UI.
--
-- KHONG seed tai khoan sinh vien: theo muc 6, tai khoan phai sinh ra tu trang
-- dang ky tai khoan. Demo muc 9 buoc 1 la dang ky A, B, C truc tiep tren UI.
--
-- KHONG seed tai khoan ADMIN o day: hash BCrypt phai do chinh encoder cua ung dung
-- sinh ra, va nhet hash vao migration da commit la dua mat khau dung chung vao git.
-- Admin duoc tao boi config/AdminBootstrap.java luc khoi dong.
--
-- Muon demo muc 9 tu dau tren DB sach: docker compose down -v roi up lai.
-- =============================================================================

-- ---------- Mon hoc ----------
insert into courses (code, name, credits, prerequisite_id) values
  ('INT1339', 'Lap trinh Java',      3, null),
  ('INT1340', 'Lap trinh Web',       3, null),
  ('INT1313', 'Co so du lieu',       3, null),
  ('BAS1201', 'Toan roi rac',        3, null);

-- Mon tien quyet gan sau bang code de khong phu thuoc gia tri auto_increment.
-- Lap trinh Web yeu cau da dat Lap trinh Java -- day la cap mon phuc vu demo
-- muc 9 buoc 11..13.
update courses c
   join courses p on p.code = 'INT1339'
   set c.prerequisite_id = p.id
 where c.code = 'INT1340';

-- ---------- Hoc ky ----------
-- Dung moc thoi gian tuong doi de cong dang ky luon dang mo bat ke chay ngay nao.
-- Admin sua lai duoc o man hinh Quan ly hoc ky.
insert into semesters (name, reg_open_at, reg_close_at, max_credits, active) values
  ('Hoc ky 1 - 2026/2027',
   date_sub(now(), interval 7 day),
   date_add(now(), interval 60 day),
   24,
   true);

-- ---------- Lop hoc phan ----------
-- day_of_week: 2 = Thu Hai ... 7 = Thu Bay
-- Khoang tiet la [start_period, start_period + period_count) -- bien phai MO (muc 7.2)
insert into class_sections
  (code, course_id, semester_id, lecturer_name, room, day_of_week, start_period, period_count, capacity, registered_count)
select 'JAVA-01', c.id, s.id, 'Nguyen Van A', 'A101', 2, 1, 3, 40, 0
  from courses c join semesters s on s.name = 'Hoc ky 1 - 2026/2027'
 where c.code = 'INT1339';

insert into class_sections
  (code, course_id, semester_id, lecturer_name, room, day_of_week, start_period, period_count, capacity, registered_count)
select 'JAVA-02', c.id, s.id, 'Nguyen Van A', 'A102', 4, 4, 3, 40, 0
  from courses c join semesters s on s.name = 'Hoc ky 1 - 2026/2027'
 where c.code = 'INT1339';

insert into class_sections
  (code, course_id, semester_id, lecturer_name, room, day_of_week, start_period, period_count, capacity, registered_count)
select 'WEB-01', c.id, s.id, 'Tran Thi B', 'B201', 3, 1, 3, 35, 0
  from courses c join semesters s on s.name = 'Hoc ky 1 - 2026/2027'
 where c.code = 'INT1340';

insert into class_sections
  (code, course_id, semester_id, lecturer_name, room, day_of_week, start_period, period_count, capacity, registered_count)
select 'DB-01', c.id, s.id, 'Le Van C', 'B202', 5, 7, 3, 30, 0
  from courses c join semesters s on s.name = 'Hoc ky 1 - 2026/2027'
 where c.code = 'INT1313';

-- JAVA-01 (Thu 2, tiet 1-3) va WEB-01 (Thu 3, tiet 1-3) khac thu nen KHONG trung.
-- JAVA-02 (Thu 4, tiet 4-6) de test ca bien tiet lien ke voi mot lop Thu 4 tiet 1-3
-- ma Admin tu tao khi demo.
