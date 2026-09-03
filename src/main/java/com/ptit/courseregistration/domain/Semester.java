package com.ptit.courseregistration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "semesters")
public class Semester {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "reg_open_at", nullable = false)
    private LocalDateTime regOpenAt;

    @Column(name = "reg_close_at", nullable = false)
    private LocalDateTime regCloseAt;

    /** Gioi han tin chi mot sinh vien duoc dang ky trong ky nay. */
    @Column(name = "max_credits", nullable = false)
    private Integer maxCredits;

    @Column(nullable = false)
    private Boolean active;

    protected Semester() {
        // JPA
    }

    public Semester(String name, LocalDateTime regOpenAt, LocalDateTime regCloseAt,
                    Integer maxCredits, Boolean active) {
        this.name = name;
        this.regOpenAt = regOpenAt;
        this.regCloseAt = regCloseAt;
        this.maxCredits = maxCredits;
        this.active = active;
    }

    /**
     * Cong dang ky con mo hay khong. Dung o tang validate thu nhat cua register()
     * va o drop() (DOAN.md muc 7.1).
     */
    public boolean isRegistrationOpen(LocalDateTime at) {
        return Boolean.TRUE.equals(active)
                && !at.isBefore(regOpenAt)
                && !at.isAfter(regCloseAt);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getRegOpenAt() {
        return regOpenAt;
    }

    public void setRegOpenAt(LocalDateTime regOpenAt) {
        this.regOpenAt = regOpenAt;
    }

    public LocalDateTime getRegCloseAt() {
        return regCloseAt;
    }

    public void setRegCloseAt(LocalDateTime regCloseAt) {
        this.regCloseAt = regCloseAt;
    }

    public Integer getMaxCredits() {
        return maxCredits;
    }

    public void setMaxCredits(Integer maxCredits) {
        this.maxCredits = maxCredits;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
