package com.ptit.courseregistration.dto;

import com.ptit.courseregistration.domain.ClassSection;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Form quan ly lop hoc phan -- man hinh nhap cua DOAN.md muc 5.
 *
 * Moi lop hoc DUNG MOT buoi moi tuan (muc 14), nen lich chi gom thu + tiet bat dau
 * + so tiet, khong co khoang tuan hoc.
 */
public class ClassSectionForm {

    private Long id;

    @NotBlank(message = "Mã lớp không được để trống")
    @Size(max = 20, message = "Mã lớp tối đa 20 ký tự")
    private String code;

    @NotNull(message = "Phải chọn môn học")
    private Long courseId;

    @NotNull(message = "Phải chọn học kỳ")
    private Long semesterId;

    /** Text thuan, KHONG co vai giang vien trong he thong (muc 14). */
    @Size(max = 100, message = "Tên giảng viên tối đa 100 ký tự")
    private String lecturerName;

    @Size(max = 20, message = "Phòng tối đa 20 ký tự")
    private String room;

    @NotNull(message = "Phải chọn thứ")
    @Min(value = 2, message = "Thứ phải từ 2 đến 7")
    @Max(value = 7, message = "Thứ phải từ 2 đến 7")
    private Byte dayOfWeek;

    @NotNull(message = "Phải nhập tiết bắt đầu")
    @Min(value = 1, message = "Tiết bắt đầu từ 1 đến 12")
    @Max(value = 12, message = "Tiết bắt đầu từ 1 đến 12")
    private Byte startPeriod;

    @NotNull(message = "Phải nhập số tiết")
    @Min(value = 1, message = "Số tiết tối thiểu là 1")
    @Max(value = 6, message = "Số tiết tối đa là 6")
    private Byte periodCount;

    @NotNull(message = "Phải nhập sĩ số")
    @Min(value = 1, message = "Sĩ số tối thiểu là 1")
    @Max(value = 500, message = "Sĩ số tối đa là 500")
    private Integer capacity;

    /** Chi de hien thi, khong cho sua tay -- counter do he thong tu giu (muc 7.8). */
    private Integer registeredCount;

    public static ClassSectionForm from(ClassSection section) {
        ClassSectionForm f = new ClassSectionForm();
        f.id = section.getId();
        f.code = section.getCode();
        f.courseId = section.getCourse().getId();
        f.semesterId = section.getSemester().getId();
        f.lecturerName = section.getLecturerName();
        f.room = section.getRoom();
        f.dayOfWeek = section.getDayOfWeek();
        f.startPeriod = section.getStartPeriod();
        f.periodCount = section.getPeriodCount();
        f.capacity = section.getCapacity();
        f.registeredCount = section.getRegisteredCount();
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

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public Long getSemesterId() {
        return semesterId;
    }

    public void setSemesterId(Long semesterId) {
        this.semesterId = semesterId;
    }

    public String getLecturerName() {
        return lecturerName;
    }

    public void setLecturerName(String lecturerName) {
        this.lecturerName = lecturerName;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public Byte getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(Byte dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public Byte getStartPeriod() {
        return startPeriod;
    }

    public void setStartPeriod(Byte startPeriod) {
        this.startPeriod = startPeriod;
    }

    public Byte getPeriodCount() {
        return periodCount;
    }

    public void setPeriodCount(Byte periodCount) {
        this.periodCount = periodCount;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Integer getRegisteredCount() {
        return registeredCount;
    }

    public void setRegisteredCount(Integer registeredCount) {
        this.registeredCount = registeredCount;
    }
}
