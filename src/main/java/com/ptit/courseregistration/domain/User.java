package com.ptit.courseregistration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /** Luon la hash BCrypt, khong bao gio la mat khau tho. */
    @Column(nullable = false, length = 100)
    private String password;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    /** Ma sinh vien. Null voi tai khoan ADMIN -- MySQL cho phep nhieu null trong unique index. */
    @Column(unique = true, length = 20)
    private String code;

    @Column(length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    protected User() {
        // JPA
    }

    public static User newStudent(String username, String encodedPassword,
                                  String fullName, String code, String email) {
        User u = new User();
        u.username = username;
        u.password = encodedPassword;
        u.fullName = fullName;
        u.code = code;
        u.email = email;
        u.role = Role.STUDENT;
        return u;
    }

    public static User newAdmin(String username, String encodedPassword, String fullName) {
        User u = new User();
        u.username = username;
        u.password = encodedPassword;
        u.fullName = fullName;
        u.code = null;
        u.role = Role.ADMIN;
        return u;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }
}
