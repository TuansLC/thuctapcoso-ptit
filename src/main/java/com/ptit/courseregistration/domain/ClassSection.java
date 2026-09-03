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

import java.util.Objects;

/**
 * Lop hoc phan. Moi lop hoc DUNG MOT buoi moi tuan, nen lich nam luon tren bang nay,
 * khong co bang lich rieng (DOAN.md muc 4).
 *
 * Day la dong du lieu bi tranh chap, va la doi tuong duoc lock trong register()
 * (muc 7.7). Quan he course/semester de LAZY co chu y: findByIdForUpdate chi duoc
 * lock dong class_sections, khong keo theo dong courses/semesters vao pham vi lock.
 */
@Entity
@Table(name = "class_sections")
public class ClassSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    /** Text thuan. KHONG co vai giang vien trong he thong (muc 14). */
    @Column(name = "lecturer_name", length = 100)
    private String lecturerName;

    @Column(length = 20)
    private String room;

    /** 2 = Thu Hai ... 7 = Thu Bay. Kieu Byte de khop cot tinyint cua MySQL. */
    @Column(name = "day_of_week", nullable = false)
    private Byte dayOfWeek;

    /** Tiet bat dau, 1..12. */
    @Column(name = "start_period", nullable = false)
    private Byte startPeriod;

    /** So tiet lien tiep. */
    @Column(name = "period_count", nullable = false)
    private Byte periodCount;

    @Column(nullable = false)
    private Integer capacity;

    /**
     * Counter du thua CO CHU Y (muc 7.8): tinh duoc bang count(*) tren registrations
     * nhung van luu de doc nhanh va de co cho gan CHECK. Doi lai phai tu giu dong bo,
     * va moi thay doi phai nam trong transaction da lock dong nay.
     */
    @Column(name = "registered_count", nullable = false)
    private Integer registeredCount = 0;

    protected ClassSection() {
        // JPA
    }

    public ClassSection(String code, Course course, Semester semester, String lecturerName,
                        String room, Byte dayOfWeek, Byte startPeriod, Byte periodCount,
                        Integer capacity) {
        this.code = code;
        this.course = course;
        this.semester = semester;
        this.lecturerName = lecturerName;
        this.room = room;
        this.dayOfWeek = dayOfWeek;
        this.startPeriod = startPeriod;
        this.periodCount = periodCount;
        this.capacity = capacity;
        this.registeredCount = 0;
    }

    // ---------------------------------------------------------------------
    // Logic lich hoc -- DOAN.md muc 7.2
    // Dat o day de chi co MOT ban duy nhat trong toan he thong. Neu copy logic nay
    // ra nhieu cho, hai ban se lech nhau va bug rat kho tim.
    // ---------------------------------------------------------------------

    /**
     * Bien phai MO: khoang tiet la [startPeriod, startPeriod + periodCount).
     * Lop tiet 1-3 co start = 1, end = 4. TUYET DOI khong tru 1 o day --
     * neu tru, hai lop lien ke se bi bao trung oan (muc 7.2).
     */
    public int endPeriodExclusive() {
        return startPeriod + periodCount;
    }

    /**
     * Trung lich khi cung thu VA khoang tiet giao nhau.
     * Dieu kien giao cua hai doan: startA < endB && startB < endA.
     */
    public boolean overlaps(ClassSection other) {
        if (other == null || !Objects.equals(this.dayOfWeek, other.dayOfWeek)) {
            return false;
        }
        return this.startPeriod < other.endPeriodExclusive()
                && other.startPeriod < this.endPeriodExclusive();
    }

    public boolean isFull() {
        return registeredCount >= capacity;
    }

    public int availableSeats() {
        return capacity - registeredCount;
    }

    /**
     * Nhan lich de hien thi, vi du "Thứ 2, tiết 1-3".
     *
     * Dat o entity de template goi truc tiep duoc va de chi co MOT cho sinh ra chuoi
     * nay. Luu y tiet cuoi HIEN THI la endPeriodExclusive() - 1, con logic so trung
     * lich van dung bien phai mo.
     */
    public String getScheduleLabel() {
        if (dayOfWeek == null || startPeriod == null || periodCount == null) {
            return "";
        }
        return getDayLabel() + ", tiết " + startPeriod + "-" + (endPeriodExclusive() - 1);
    }

    public String getDayLabel() {
        if (dayOfWeek == null) {
            return "Thứ ?";
        }
        return switch (dayOfWeek.intValue()) {
            case 2 -> "Thứ 2";
            case 3 -> "Thứ 3";
            case 4 -> "Thứ 4";
            case 5 -> "Thứ 5";
            case 6 -> "Thứ 6";
            case 7 -> "Thứ 7";
            default -> "Thứ ?";
        };
    }

    /** Chi goi trong transaction da lock dong nay bang findByIdForUpdate. */
    public void increaseRegisteredCount() {
        this.registeredCount = this.registeredCount + 1;
    }

    /** Chi goi trong transaction da lock dong nay bang findByIdForUpdate. */
    public void decreaseRegisteredCount() {
        if (this.registeredCount > 0) {
            this.registeredCount = this.registeredCount - 1;
        }
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public Semester getSemester() {
        return semester;
    }

    public void setSemester(Semester semester) {
        this.semester = semester;
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
}
