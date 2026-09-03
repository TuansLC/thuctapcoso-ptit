package com.ptit.courseregistration.service;

import com.ptit.courseregistration.domain.ClassSection;
import com.ptit.courseregistration.domain.Course;
import com.ptit.courseregistration.domain.LetterGrade;
import com.ptit.courseregistration.domain.Registration;
import com.ptit.courseregistration.dto.AcademicSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test thuan cho diem chu va GPA -- DOAN.md muc 7.2.
 *
 * summarize() va letterOf() khong dung repository nao, nen truyen null vao constructor
 * la du. Khong can Spring context, khong can DB.
 */
class GradeServiceTest {

    private final GradeService gradeService = new GradeService(null, null);

    // =========================================================================
    // Diem chu -- kiem tra dung tai cac nguong, vi day la cho de sai dau <= va <
    // =========================================================================
    @ParameterizedTest(name = "score {0} -> {1}")
    @CsvSource({
            "10.0, A",
            "8.5,  A",
            "8.49, B+",
            "8.0,  B+",
            "7.99, B",
            "7.0,  B",
            "6.99, C+",
            "6.5,  C+",
            "6.49, C",
            "5.5,  C",
            "5.49, D+",
            "5.0,  D+",
            "4.99, D",
            "4.0,  D",
            "3.99, F",
            "0.0,  F"
    })
    void quy_doi_diem_chu(String score, String expected) {
        assertThat(gradeService.letterOf(new BigDecimal(score)).getLabel()).isEqualTo(expected);
    }

    @Test
    @DisplayName("Chua co diem thi khong co diem chu")
    void chua_co_diem() {
        assertThat(gradeService.letterOf(null)).isNull();
    }

    @Test
    @DisplayName("Nguong dat mon tien quyet la 4.0, tuc tu D tro len")
    void nguong_dat() {
        assertThat(LetterGrade.of(new BigDecimal("4.0")).isFail()).isFalse();
        assertThat(LetterGrade.of(new BigDecimal("3.99")).isFail()).isTrue();
    }

    // =========================================================================
    // GPA -- ba quy tac da chot o muc 7.2
    // =========================================================================

    @Test
    @DisplayName("GPA co trong so tin chi: 4TC diem A + 2TC diem C = 3.33")
    void gpa_co_trong_so_tin_chi() {
        var regs = List.of(
                registration(4, "9.0"),   // A  -> 4.0 x 4 = 16
                registration(2, "6.0")    // C  -> 2.0 x 2 = 4
        );
        AcademicSummary s = gradeService.summarize(regs);

        assertThat(s.gpa()).isEqualByComparingTo("3.33");   // 20 / 6
        assertThat(s.gradedCredits()).isEqualTo(6);
        assertThat(s.passedCredits()).isEqualTo(6);
    }

    @Test
    @DisplayName("QUY TAC 1: mon chua co diem bi loai khoi CA tu VA mau")
    void mon_chua_co_diem_khong_vao_gpa() {
        var regs = List.of(
                registration(3, "8.0"),   // B+ -> 3.5 x 3 = 10.5
                registration(3, null)     // chua co diem
        );
        AcademicSummary s = gradeService.summarize(regs);

        assertThat(s.gpa()).isEqualByComparingTo("3.50");   // 10.5 / 3, khong phai / 6
        assertThat(s.registeredCredits()).isEqualTo(6);     // van tinh vao tin chi da dang ky
        assertThat(s.gradedCredits()).isEqualTo(3);
    }

    @Test
    @DisplayName("QUY TAC 2: mon F vao ca tu (0 diem) va mau, nen keo GPA xuong")
    void mon_f_keo_gpa_xuong() {
        var regs = List.of(
                registration(3, "7.5"),   // B -> 3.0 x 3 = 9.0
                registration(3, "3.0")    // F -> 0.0 x 3 = 0
        );
        AcademicSummary s = gradeService.summarize(regs);

        // 9.0 / 6 = 1.50. Neu loai F khoi mau se ra 3.00, tuc truot lai thanh co loi.
        assertThat(s.gpa()).isEqualByComparingTo("1.50");
        assertThat(s.gradedCredits()).isEqualTo(6);
        assertThat(s.passedCredits()).isEqualTo(3);
    }

    @Test
    @DisplayName("Chua mon nao co diem thi GPA la null, khong phai 0.0")
    void chua_co_diem_nao_thi_gpa_null() {
        AcademicSummary s = gradeService.summarize(List.of(registration(3, null)));

        assertThat(s.gpa()).isNull();
        assertThat(s.hasGpa()).isFalse();
        assertThat(s.registeredCredits()).isEqualTo(3);
        assertThat(s.gradedCredits()).isZero();
    }

    @Test
    @DisplayName("QUY TAC 3: lam tron 2 chu so thap phan")
    void lam_tron_hai_chu_so() {
        // 3TC A (4.0) + 3TC B (3.0) + 3TC F (0) = (12 + 9 + 0) / 9 = 2.3333...
        var regs = List.of(
                registration(3, "9.0"),
                registration(3, "7.0"),
                registration(3, "1.0")
        );
        assertThat(gradeService.summarize(regs).gpa()).isEqualByComparingTo("2.33");
    }

    @Test
    @DisplayName("Danh sach rong thi khong bao loi chia cho 0")
    void danh_sach_rong() {
        AcademicSummary s = gradeService.summarize(List.of());

        assertThat(s.gpa()).isNull();
        assertThat(s.registeredCredits()).isZero();
    }

    // =========================================================================

    private Registration registration(int credits, String score) {
        Course course = new Course("C" + credits, "Mon " + credits, credits, null);
        ClassSection section = new ClassSection(
                "SEC", course, null, null, null, (byte) 2, (byte) 1, (byte) 3, 40);
        Registration r = Registration.of(null, section, LocalDateTime.now());
        if (score != null) {
            r.setScore(new BigDecimal(score));
        }
        return r;
    }
}
