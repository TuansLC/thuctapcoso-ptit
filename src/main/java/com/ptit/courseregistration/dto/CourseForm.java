package com.ptit.courseregistration.dto;

import com.ptit.courseregistration.domain.Course;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Form quan ly mon hoc -- man hinh nhap cua DOAN.md muc 5. */
public class CourseForm {

    private Long id;

    @NotBlank(message = "Mã môn học không được để trống")
    @Size(max = 20, message = "Mã môn học tối đa 20 ký tự")
    private String code;

    @NotBlank(message = "Tên môn học không được để trống")
    @Size(max = 150, message = "Tên môn học tối đa 150 ký tự")
    private String name;

    @NotNull(message = "Số tín chỉ không được để trống")
    @Min(value = 1, message = "Số tín chỉ tối thiểu là 1")
    @Max(value = 10, message = "Số tín chỉ tối đa là 10")
    private Integer credits;

    /** Toi da MOT mon tien quyet (muc 14). Null nghia la khong co. */
    private Long prerequisiteId;

    public static CourseForm from(Course course) {
        CourseForm f = new CourseForm();
        f.id = course.getId();
        f.code = course.getCode();
        f.name = course.getName();
        f.credits = course.getCredits();
        f.prerequisiteId = course.getPrerequisite() == null ? null : course.getPrerequisite().getId();
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCredits() {
        return credits;
    }

    public void setCredits(Integer credits) {
        this.credits = credits;
    }

    public Long getPrerequisiteId() {
        return prerequisiteId;
    }

    public void setPrerequisiteId(Long prerequisiteId) {
        this.prerequisiteId = prerequisiteId;
    }
}
