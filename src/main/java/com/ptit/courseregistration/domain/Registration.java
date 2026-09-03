package com.ptit.courseregistration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mot ban ghi dang ky cua mot sinh vien vao mot lop hoc phan.
 * Diem la cot score ngay tren bang nay, khong co bang grades (DOAN.md muc 4).
 *
 * UNIQUE(student_id, section_id) chan race condition KHAC voi row lock tren
 * class_sections: bam nut hai lan, F5, mang lag (muc 7.6).
 */
@Entity
@Table(
        name = "registrations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_student_section",
                columnNames = {"student_id", "section_id"}
        )
)
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private ClassSection section;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt;

    /** null = chua co diem. Thang 10, do Admin nhap o man hinh Nhap diem. */
    @Column(precision = 4, scale = 2)
    private BigDecimal score;

    protected Registration() {
        // JPA
    }

    public static Registration of(User student, ClassSection section, LocalDateTime at) {
        Registration r = new Registration();
        r.student = student;
        r.section = section;
        r.registeredAt = at;
        r.score = null;
        return r;
    }

    public boolean hasScore() {
        return score != null;
    }

    public Long getId() {
        return id;
    }

    public User getStudent() {
        return student;
    }

    public ClassSection getSection() {
        return section;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }
}
