package com.ptit.courseregistration.dto;

import java.math.BigDecimal;

/**
 * Tom tat ket qua hoc tap cho man hinh "Hoc phan cua toi".
 *
 * Toan bo cac so o day la DU LIEU DAN XUAT, tinh tu score va credits luc hien thi,
 * khong co cot nao tuong ung trong DB (DOAN.md muc 6).
 *
 * @param gpa            GPA thang 4, null khi chua co mon nao co diem
 * @param registeredCredits tong tin chi da dang ky (ke ca mon chua co diem)
 * @param gradedCredits  tin chi da co diem -- day la MAU SO cua GPA
 * @param passedCredits  tin chi da dat (diem tu 4.0)
 */
public record AcademicSummary(
        BigDecimal gpa,
        int registeredCredits,
        int gradedCredits,
        int passedCredits
) {
    public boolean hasGpa() {
        return gpa != null;
    }
}
