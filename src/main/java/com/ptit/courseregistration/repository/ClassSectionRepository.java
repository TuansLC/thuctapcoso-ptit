package com.ptit.courseregistration.repository;

import com.ptit.courseregistration.domain.ClassSection;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClassSectionRepository extends JpaRepository<ClassSection, Long> {

    /**
     * TRAI TIM KY THUAT CUA DO AN -- DOAN.md muc 7.1.
     *
     * PESSIMISTIC_WRITE sinh ra "select ... for update", tuc la mot LOCKING READ:
     * doc ban moi nhat da commit chu KHONG doc snapshot MVCC. Day chinh la ly do
     * nang isolation level khong giai quyet duoc bai toan (muc 7.4).
     *
     * CO Y khong join fetch course/semester o day. Neu join fetch, cau "for update"
     * se lock luon dong tuong ung trong courses va semesters -- nhung dong ma moi
     * sinh vien dang ky bat ky lop nao cua cung mon deu can. Lam vay bien lock dong
     * thanh diem tranh chap chung va co the gay deadlock. Chi lock dung doi tuong bi
     * tranh chap la suc chua cua lop (muc 7.7).
     *
     * Goi ham nay xong, sec.getCourse() / sec.getSemester() se lazy load binh thuong
     * vi ta dang o trong @Transactional.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ClassSection c where c.id = :id")
    Optional<ClassSection> findByIdForUpdate(@Param("id") Long id);

    /** Man hinh danh sach: lay san course va semester vi open-in-view = false. */
    @Query("""
            select s from ClassSection s
              join fetch s.course c
              join fetch s.semester sem
             where sem.id = :semesterId
             order by c.code, s.code
            """)
    List<ClassSection> findBySemesterWithCourse(@Param("semesterId") Long semesterId);

    /**
     * Man hinh "Lop dang mo" cua sinh vien: co phan trang va hai bo loc.
     *
     * join fetch s.course O DAY LA AN TOAN voi phan trang, vi course la quan he
     * MOT-chieu (@ManyToOne). Neu fetch mot COLLECTION thi Hibernate buoc phai keo het
     * ve bo nho roi cat trang trong bo nho -- vua sai y nghia phan trang vua ton bo nho.
     * Voi quan he don tri thi LIMIT/OFFSET van chay duoi DB nhu binh thuong.
     *
     * countQuery viet rieng va KHONG co join fetch: dem thi khong can nap du lieu.
     *
     * @param courseId      null = khong loc theo mon
     * @param onlyAvailable true = chi lay lop con cho
     */
    @Query(value = """
            select s from ClassSection s
              join fetch s.course c
             where s.semester.id = :semesterId
               and (:courseId is null or c.id = :courseId)
               and (:onlyAvailable = false or s.registeredCount < s.capacity)
            """,
            countQuery = """
            select count(s) from ClassSection s
             where s.semester.id = :semesterId
               and (:courseId is null or s.course.id = :courseId)
               and (:onlyAvailable = false or s.registeredCount < s.capacity)
            """)
    Page<ClassSection> findOpenSections(@Param("semesterId") Long semesterId,
                                        @Param("courseId") Long courseId,
                                        @Param("onlyAvailable") boolean onlyAvailable,
                                        Pageable pageable);

    @Query("""
            select s from ClassSection s
              join fetch s.course
              join fetch s.semester
             where s.id = :id
            """)
    Optional<ClassSection> findByIdWithCourseAndSemester(@Param("id") Long id);

    /** Man hinh quan ly lop hoc phan cua admin: liet ke moi ky. */
    @Query("""
            select s from ClassSection s
              join fetch s.course c
              join fetch s.semester sem
             order by sem.id desc, c.code, s.code
            """)
    List<ClassSection> findAllWithCourseAndSemester();

    boolean existsByCode(String code);

    /** Chan xoa mon hoc khi con lop hoc phan tham chieu (DOAN.md muc 11). */
    boolean existsByCourseId(Long courseId);

    boolean existsBySemesterId(Long semesterId);
}
