package com.ptit.courseregistration.service;

import com.ptit.courseregistration.domain.ClassSection;
import com.ptit.courseregistration.domain.LetterGrade;
import com.ptit.courseregistration.domain.Registration;
import com.ptit.courseregistration.dto.AcademicSummary;
import com.ptit.courseregistration.exception.BusinessException;
import com.ptit.courseregistration.exception.NotFoundException;
import com.ptit.courseregistration.repository.ClassSectionRepository;
import com.ptit.courseregistration.repository.RegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Nhap diem (chuc nang admin) va tinh diem chu / GPA (du lieu dan xuat).
 *
 * Khong co bang grades va khong co cot gpa: diem la cot score tren registrations,
 * con diem chu va GPA tinh lai luc hien thi (DOAN.md muc 4 va muc 6).
 */
@Service
public class GradeService {

    private static final BigDecimal MIN_SCORE = BigDecimal.ZERO;
    private static final BigDecimal MAX_SCORE = new BigDecimal("10.0");

    private final RegistrationRepository registrationRepository;
    private final ClassSectionRepository sectionRepository;

    public GradeService(RegistrationRepository registrationRepository,
                        ClassSectionRepository sectionRepository) {
        this.registrationRepository = registrationRepository;
        this.sectionRepository = sectionRepository;
    }

    // =========================================================================
    // Nhap diem -- man hinh admin
    // =========================================================================

    @Transactional(readOnly = true)
    public List<Registration> listForEntry(Long sectionId) {
        return registrationRepository.findBySectionWithStudent(sectionId);
    }

    /**
     * Nhap diem ca bang mot lan.
     *
     * @param scores anh xa registrationId -> diem; gia tri null nghia la XOA diem
     *               (tra ve trang thai chua co diem)
     *
     * Quy tac chot: chi cho sua diem khi hoc ky CHUA CHOT, tuc semester.active con true.
     * Khi admin dong ky (active = false) thi bang diem khoa lai, chi doc duoc.
     * Day la cach dien giai cau tra loi "cho sua khi ky chua chot" o muc 11.
     */
    @Transactional
    public int saveScores(Long sectionId, Map<Long, BigDecimal> scores) {
        ClassSection section = sectionRepository.findByIdWithCourseAndSemester(sectionId)
                .orElseThrow(() -> new NotFoundException("Lớp học phần không tồn tại."));

        if (!Boolean.TRUE.equals(section.getSemester().getActive())) {
            throw new BusinessException("Học kỳ " + section.getSemester().getName()
                    + " đã chốt nên không sửa được điểm. Mở lại học kỳ nếu cần chỉnh.");
        }

        List<Registration> registrations = registrationRepository.findBySectionWithStudent(sectionId);
        int changed = 0;

        for (Registration registration : registrations) {
            if (!scores.containsKey(registration.getId())) {
                continue;   // dong khong duoc gui len thi khong dung den
            }
            BigDecimal newScore = scores.get(registration.getId());
            validateScore(newScore);

            BigDecimal current = registration.getScore();
            boolean same = (current == null && newScore == null)
                    || (current != null && newScore != null && current.compareTo(newScore) == 0);
            if (!same) {
                registration.setScore(newScore);
                changed++;
            }
        }
        return changed;
    }

    private void validateScore(BigDecimal score) {
        if (score == null) {
            return;   // null = xoa diem, hop le
        }
        if (score.compareTo(MIN_SCORE) < 0 || score.compareTo(MAX_SCORE) > 0) {
            throw new BusinessException("Điểm phải nằm trong khoảng 0 đến 10, giá trị nhận được: "
                    + score.toPlainString());
        }
    }

    // =========================================================================
    // Du lieu dan xuat -- diem chu va GPA
    // =========================================================================

    public LetterGrade letterOf(BigDecimal score) {
        return LetterGrade.of(score);
    }

    /**
     * GPA thang 4 = trung binh co trong so tin chi.
     *
     *     GPA = tong(quy_doi_4 x credits) / tong(credits)
     *
     * BA QUY TAC DA CHOT (muc 7.2), khong de moi nguoi hieu mot kieu:
     *
     *  1. Mon co score = null (chua co diem) KHONG vao GPA -- bo khoi ca tu va mau.
     *  2. Mon F CO vao GPA: tu cong 0, mau van cong credits. Mon da hoc va truot thi
     *     phai keo GPA xuong; neu loai khoi mau thi truot lai thanh co loi.
     *  3. Lam tron 2 chu so thap phan khi hien thi.
     */
    public AcademicSummary summarize(Collection<Registration> registrations) {
        BigDecimal weightedSum = BigDecimal.ZERO;
        int gradedCredits = 0;
        int registeredCredits = 0;
        int passedCredits = 0;

        for (Registration r : registrations) {
            int credits = r.getSection().getCourse().getCredits();
            registeredCredits += credits;

            LetterGrade grade = LetterGrade.of(r.getScore());
            if (grade == null) {
                continue;   // quy tac 1
            }
            // quy tac 2: ke ca F cung vao tu va mau
            weightedSum = weightedSum.add(grade.getGradePoint().multiply(BigDecimal.valueOf(credits)));
            gradedCredits += credits;
            if (!grade.isFail()) {
                passedCredits += credits;
            }
        }

        BigDecimal gpa = null;
        if (gradedCredits > 0) {
            // quy tac 3
            gpa = weightedSum.divide(BigDecimal.valueOf(gradedCredits), 2, RoundingMode.HALF_UP);
        }
        return new AcademicSummary(gpa, registeredCredits, gradedCredits, passedCredits);
    }
}
