package com.ptit.courseregistration.repository;

import com.ptit.courseregistration.domain.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByCode(String code);

    boolean existsByCode(String code);

    /**
     * Fetch join mon tien quyet: man hinh Quan ly mon hoc hien ten mon tien quyet,
     * ma open-in-view = false nen phai lay san, khong the lazy load luc render.
     */
    @Query("select c from Course c left join fetch c.prerequisite order by c.code")
    List<Course> findAllWithPrerequisite();

    /** Chan xoa mon dang la tien quyet cua mon khac (DOAN.md muc 11). */
    boolean existsByPrerequisiteId(Long prerequisiteId);
}
