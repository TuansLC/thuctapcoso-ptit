package com.ptit.courseregistration.service;

import com.ptit.courseregistration.domain.Role;
import com.ptit.courseregistration.domain.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Principal cua Spring Security, boc entity User cua ung dung.
 *
 * Ly do tu viet thay vi dung org.springframework.security.core.userdetails.User:
 * can mang theo ID de controller lay duoc studentId ma khong phai truy van lai
 * users theo username o moi request.
 *
 * Luu y: KHONG giu tham chieu toi entity con song trong session. Chi copy ra cac
 * gia tri nguyen thuy, vi doi tuong nay ton tai ngoai pham vi transaction.
 */
public class AppUserDetails implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final String fullName;
    private final Role role;

    public AppUserDetails(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.fullName = user.getFullName();
        this.role = user.getRole();
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Tien to ROLE_ la quy uoc cua Spring Security de hasRole("ADMIN") hoat dong.
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
