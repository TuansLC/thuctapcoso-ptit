package com.ptit.courseregistration.dto;

import com.ptit.courseregistration.domain.ClassSection;

/**
 * Mot dong tren man hinh "Lop dang mo" cua sinh vien.
 *
 * @param section     lop hoc phan (da fetch san course va semester)
 * @param registered  sinh vien nay da dang ky lop nay chua
 * @param blockReason null nghia la dang ky duoc; khac null la ly do bi chan
 *
 * LUU Y: blockReason chi de HIEN THI. No duoc tinh ngoai pham vi lock nen co the cu
 * ngay khi vua tinh xong -- giua luc hien trang va luc bam nut, nguoi khac co the da
 * chiem cho. register() van kiem tra lai day du trong transaction co lock.
 */
public record OpenSectionView(
        ClassSection section,
        boolean registered,
        String blockReason
) {
    public boolean canRegister() {
        return !registered && blockReason == null;
    }
}
