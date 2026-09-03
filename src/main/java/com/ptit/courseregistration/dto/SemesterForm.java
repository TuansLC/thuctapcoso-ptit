package com.ptit.courseregistration.dto;

import com.ptit.courseregistration.domain.Semester;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * Form quan ly hoc ky -- man hinh nhap cua DOAN.md muc 5.
 *
 * regOpenAt / regCloseAt binding tu input type="datetime-local", nen pattern phai la
 * "yyyy-MM-dd'T'HH:mm" chu khong phai dinh dang co dau cach.
 */
public class SemesterForm {

    private Long id;

    @NotBlank(message = "Tên học kỳ không được để trống")
    @Size(max = 50, message = "Tên học kỳ tối đa 50 ký tự")
    private String name;

    @NotNull(message = "Thời điểm mở cổng đăng ký không được để trống")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime regOpenAt;

    @NotNull(message = "Thời điểm đóng cổng đăng ký không được để trống")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime regCloseAt;

    @NotNull(message = "Giới hạn tín chỉ không được để trống")
    @Min(value = 1, message = "Giới hạn tín chỉ tối thiểu là 1")
    @Max(value = 40, message = "Giới hạn tín chỉ tối đa là 40")
    private Integer maxCredits = 24;

    private boolean active;

    public static SemesterForm from(Semester semester) {
        SemesterForm f = new SemesterForm();
        f.id = semester.getId();
        f.name = semester.getName();
        f.regOpenAt = semester.getRegOpenAt();
        f.regCloseAt = semester.getRegCloseAt();
        f.maxCredits = semester.getMaxCredits();
        f.active = Boolean.TRUE.equals(semester.getActive());
        return f;
    }

    public boolean isNew() {
        return id == null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
