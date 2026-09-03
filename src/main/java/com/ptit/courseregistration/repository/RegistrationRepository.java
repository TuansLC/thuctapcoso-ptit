package com.ptit.courseregistration.repository;

import com.ptit.courseregistration.domain.ClassSection;
import com.ptit.courseregistration.domain.Registration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    Optional<Registration> findByStudentIdAndSectionId(Long studentId, Long sectionId);

    /** Tang validate 2: chua dang ky mon nay (DOAN.md muc 7.1). */
    boolean existsByStudentIdAndSectionId(Long studentId, Long sectionId);

    /**
     * Tang validate 3: da dat mon tien quyet.
     * "Dat" = ton tai mot ban ghi dang ky cua sinh vien voi mon do va score >= 4.0.
     * Khong gioi han hoc ky: mon tien quyet co the hoc o bat ky ky nao truoc do.
     */
    @Query("""
            select count(r) > 0 from Registration r
             where r.student.id = :studentId
               and r.section.course.id = :courseId
               and r.score >= :passMark
            """)
    boolean hasPassedCourse(@Param("studentId") Long studentId,
                            @Param("courseId") Long courseId,
                            @Param("passMark") BigDecimal passMark);

    /**
     * Tang validate 4: lay cac lop sinh vien da dang ky trong ky de so trung lich.
     *
     * Co y KHONG viet dieu kien giao doan bang SQL: logic do da nam o
     * ClassSection.overlaps() va phai chi co MOT ban duy nhat trong he thong.
     * Mot sinh vien mot ky nhieu nhat khoang muoi lop nen so trong bo nho khong ton kem.
     */
    @Query("""
            select r.section from Registration r
             where r.student.id = :studentId
               and r.section.semester.id = :semesterId
            """)
    List<ClassSection> findSectionsByStudentAndSemester(@Param("studentId") Long studentId,
                                                        @Param("semesterId") Long semesterId);

    /** Tang validate 5: tong tin chi da dang ky trong ky. */
    @Query("""
            select coalesce(sum(r.section.course.credits), 0) from Registration r
             where r.student.id = :studentId
               and r.section.semester.id = :semesterId
            """)
    int sumRegisteredCredits(@Param("studentId") Long studentId,
                             @Param("semesterId") Long semesterId);

    /** Man hinh "Hoc phan cua toi" va tinh GPA. */
    @Query("""
            select r from Registration r
              join fetch r.section s
              join fetch s.course c
              join fetch s.semester sem
             where r.student.id = :studentId
             order by sem.id desc, c.code
            """)
    List<Registration> findByStudentWithSectionAndCourse(@Param("studentId") Long studentId);

    /** Man hinh Nhap diem: lay ca bang de nhap mot luot. */
    @Query("""
            select r from Registration r
              join fetch r.student
             where r.section.id = :sectionId
             order by r.student.code
            """)
    List<Registration> findBySectionWithStudent(@Param("sectionId") Long sectionId);

    /**
     * Xoa lop hoc phan phai xoa dang ky truoc vi khong dung ON DELETE CASCADE
     * (DOAN.md muc 4 va 7.2). Goi trong cung transaction voi lenh xoa lop.
     */
    @Modifying
    @Query("delete from Registration r where r.section.id = :sectionId")
    int deleteBySectionId(@Param("sectionId") Long sectionId);

    /**
     * Doi chieu counter registered_count voi so dong thuc te.
     * Muc 7.8 noi dem truc tiep cung an toan mien la nam trong transaction da lock
     * dong lop hoc phan; ham nay dung de kiem chung counter khong bi troi lech.
     */
    long countBySectionId(Long sectionId);
}
