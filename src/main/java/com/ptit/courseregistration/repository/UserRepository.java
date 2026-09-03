package com.ptit.courseregistration.repository;

import com.ptit.courseregistration.domain.Role;
import com.ptit.courseregistration.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByCode(String code);

    /** Dung boi AdminBootstrap de biet da co tai khoan ADMIN nao chua. */
    boolean existsByRole(Role role);
}
