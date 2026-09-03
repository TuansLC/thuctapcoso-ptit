package com.ptit.courseregistration.domain;

import java.math.BigDecimal;

/**
 * Bang quy doi diem so (thang 10) sang diem chu va thang 4 -- DOAN.md muc 7.2.
 *
 * KHONG luu vao DB. Day la du lieu DAN XUAT, tinh lai moi lan hien thi tu score
 * va credits (muc 4 va muc 6). Vi vay khong co bang grades va khong co cot gpa.
 *
 * Thu tu khai bao la GIAM DAN theo nguong: of() tra ve muc dau tien co nguong
 * nho hon hoac bang diem, nen thu tu nay la mot phan cua logic, khong phai ngau nhien.
 */
public enum LetterGrade {

    A     ("A",  "8.5", "4.0"),
    B_PLUS("B+", "8.0", "3.5"),
    B     ("B",  "7.0", "3.0"),
    C_PLUS("C+", "6.5", "2.5"),
    C     ("C",  "5.5", "2.0"),
    D_PLUS("D+", "5.0", "1.5"),
    D     ("D",  "4.0", "1.0"),
    F     ("F",  "0.0", "0.0");

    private final String label;
    private final BigDecimal minScore;
    private final BigDecimal gradePoint;

    LetterGrade(String label, String minScore, String gradePoint) {
        this.label = label;
        this.minScore = new BigDecimal(minScore);
        this.gradePoint = new BigDecimal(gradePoint);
    }

    /**
     * @param score diem thang 10, co the null (chua co diem)
     * @return null neu chua co diem, nguoc lai la muc diem chu tuong ung
     */
    public static LetterGrade of(BigDecimal score) {
        if (score == null) {
            return null;
        }
        for (LetterGrade g : values()) {
            if (score.compareTo(g.minScore) >= 0) {
                return g;
            }
        }
        return F;
    }

    public String getLabel() {
        return label;
    }

    public BigDecimal getGradePoint() {
        return gradePoint;
    }

    /** Duoi 4.0 la truot. Cung la nguong tinh "da dat mon tien quyet" (muc 7.2). */
    public boolean isFail() {
        return this == F;
    }

    /** Mau Bootstrap de hien badge. Luon di kem chu, khong dung mau mot minh. */
    public String getBadgeClass() {
        if (this == F) {
            return "text-bg-danger";
        }
        if (this == D || this == D_PLUS) {
            return "text-bg-warning";
        }
        return "text-bg-success";
    }
}
