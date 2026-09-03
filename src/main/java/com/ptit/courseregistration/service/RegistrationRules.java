package com.ptit.courseregistration.service;

import com.ptit.courseregistration.domain.ClassSection;
import com.ptit.courseregistration.domain.Course;
import com.ptit.courseregistration.domain.Semester;
import com.ptit.courseregistration.exception.AlreadyRegisteredException;
import com.ptit.courseregistration.exception.CreditLimitExceededException;
import com.ptit.courseregistration.exception.PrerequisiteNotMetException;
import com.ptit.courseregistration.exception.RegistrationClosedException;
import com.ptit.courseregistration.exception.ScheduleConflictException;
import com.ptit.courseregistration.repository.RegistrationRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * NAM TANG VALIDATE cua register() -- DOAN.md muc 7.1 va 7.2.
 *
 * Tach rieng khoi RegistrationService voi mot ly do cu the: cac quy tac nay con duoc
 * dung o cho khac (man hinh "Lop dang mo" muon bao truoc lop nao dang ky duoc). Neu
 * copy logic ra hai noi, hai ban se lech nhau va bug rat kho tim. O day chi co MOT ban.
 *
 * Moi ham nem exception rieng voi thong bao nguoi dung doc hieu duoc. Rang buoc tang DB
 * (chk_capacity, uk_student_section) la luoi an toan thu hai, khong phai thay the
 * (muc 7.6).
 */
@Component
public class RegistrationRules {

    /** Diem san de tinh la "da dat" mot mon (muc 7.2). */
    public static final BigDecimal PASS_MARK = new BigDecimal("4.0");

    private final RegistrationRepository registrationRepository;

    public RegistrationRules(RegistrationRepository registrationRepository) {
        this.registrationRepository = registrationRepository;
    }

    // =========================================================================
    // TANG 1 -- cong dang ky con mo
    // =========================================================================
    public void checkRegistrationOpen(Semester semester, LocalDateTime now) {
        if (!semester.isRegistrationOpen(now)) {
            throw new RegistrationClosedException(
                    "Cổng đăng ký của " + semester.getName() + " hiện đang đóng.");
        }
    }

    // =========================================================================
    // TANG 2 -- chua dang ky mon nay
    // =========================================================================
    public void checkNotAlreadyRegistered(Long studentId, ClassSection section) {
        if (registrationRepository.existsByStudentIdAndSectionId(studentId, section.getId())) {
            throw new AlreadyRegisteredException(
                    "Bạn đã đăng ký lớp " + section.getCode() + " rồi.");
        }
        // Da dang ky mot lop KHAC cua cung mon hoc thi cung khong cho dang ky them.
        boolean sameCourseRegistered = registrationRepository
                .findSectionsByStudentAndSemester(studentId, section.getSemester().getId())
                .stream()
                .anyMatch(s -> s.getCourse().getId().equals(section.getCourse().getId()));
        if (sameCourseRegistered) {
            throw new AlreadyRegisteredException(
                    "Bạn đã đăng ký một lớp khác của môn " + section.getCourse().getName()
                            + " trong học kỳ này.");
        }
    }

    // =========================================================================
    // TANG 3 -- da dat mon tien quyet
    // =========================================================================
    public void checkPrerequisitePassed(Long studentId, Course course) {
        Course prerequisite = course.getPrerequisite();
        if (prerequisite == null) {
            return;
        }
        boolean passed = registrationRepository.hasPassedCourse(
                studentId, prerequisite.getId(), PASS_MARK);
        if (!passed) {
            throw new PrerequisiteNotMetException(
                    "Môn " + course.getName() + " yêu cầu đã đạt môn tiên quyết "
                            + prerequisite.getName() + " (điểm từ 4.0). "
                            + "Bạn chưa học hoặc chưa đạt môn này.");
        }
    }

    // =========================================================================
    // TANG 4 -- khong trung thoi khoa bieu
    // =========================================================================
    public void checkNoScheduleConflict(Long studentId, ClassSection section) {
        List<ClassSection> registered = registrationRepository
                .findSectionsByStudentAndSemester(studentId, section.getSemester().getId());

        for (ClassSection other : registered) {
            if (other.getId().equals(section.getId())) {
                continue;
            }
            // So sanh bang ClassSection.overlaps() -- ban duy nhat cua dieu kien giao doan.
            if (section.overlaps(other)) {
                throw new ScheduleConflictException(
                        "Lớp " + section.getCode() + " trùng lịch với lớp " + other.getCode()
                                + " (" + describeSchedule(other) + ") mà bạn đã đăng ký.");
            }
        }
    }

    // =========================================================================
    // TANG 5 -- chua vuot gioi han tin chi cua hoc ky
    // =========================================================================
    public void checkCreditLimit(Long studentId, ClassSection section) {
        Semester semester = section.getSemester();
        int registered = registrationRepository.sumRegisteredCredits(studentId, semester.getId());
        int adding = section.getCourse().getCredits();
        int max = semester.getMaxCredits();

        if (registered + adding > max) {
            throw new CreditLimitExceededException(
                    "Vượt giới hạn tín chỉ: đã đăng ký " + registered + " tín chỉ, môn này "
                            + adding + " tín chỉ, giới hạn của kỳ là " + max + " tín chỉ.");
        }
    }

    // =========================================================================
    // Tien ich hien thi
    //
    // Chuoi nhan lich do ClassSection sinh ra, khong viet lai o day: chi mot ban duy
    // nhat trong toan he thong.
    // =========================================================================

    public static String describeSchedule(ClassSection section) {
        return section.getScheduleLabel();
    }
}
