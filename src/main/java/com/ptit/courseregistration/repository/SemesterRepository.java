package com.ptit.courseregistration.repository;

import com.ptit.courseregistration.domain.Semester;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SemesterRepository extends JpaRepository<Semester, Long> {

    Optional<Semester> findByActiveTrue();

    List<Semester> findAllByOrderByIdDesc();

    boolean existsByName(String name);
}
