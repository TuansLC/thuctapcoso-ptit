package com.ptit.courseregistration.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test thuan cho dieu kien trung lich -- DOAN.md muc 7.2.
 *
 * Khong can Spring context, khong can DB, chay trong vai milli giay.
 *
 * VI SAO TEST NAY QUAN TRONG: cong thuc giao doan la
 *
 *     startA < endB && startB < endA,  voi end = start_period + period_count
 *
 * Neu ai do code end = start + count - 1 thi hai lop LIEN KE bi bao trung oan, ma
 * phan lon truong hop khac van dung nen bug rat kho thay. Hai ca dau tien duoi day
 * chinh la hai ca chan bug do.
 */
class ScheduleConflictTest {

    /** Chi can day/start/count cho phep so lich; course va semester khong lien quan. */
    private ClassSection section(int day, int start, int count) {
        return new ClassSection(
                "S" + day + start + count, null, null, null, null,
                (byte) day, (byte) start, (byte) count, 40);
    }

    @Test
    @DisplayName("end la bien phai MO: tiet 1-3 co end = 4")
    void end_la_bien_phai_mo() {
        assertThat(section(2, 1, 3).endPeriodExclusive()).isEqualTo(4);
        assertThat(section(2, 7, 3).endPeriodExclusive()).isEqualTo(10);
    }

    @Test
    @DisplayName("Hai lop LIEN KE cung thu thi KHONG trung (tiet 1-3 va tiet 4-6)")
    void lop_lien_ke_khong_trung() {
        ClassSection a = section(2, 1, 3);   // tiet 1-3, end = 4
        ClassSection b = section(2, 4, 3);   // tiet 4-6, start = 4

        // 1 < 7 && 4 < 4  ->  false
        assertThat(a.overlaps(b)).isFalse();
        assertThat(b.overlaps(a)).isFalse();
    }

    @Test
    @DisplayName("Hai lop CHONG mot tiet thi trung (tiet 1-3 va tiet 3-5)")
    void lop_chong_mot_tiet_thi_trung() {
        ClassSection a = section(2, 1, 3);   // tiet 1-3, end = 4
        ClassSection b = section(2, 3, 3);   // tiet 3-5, start = 3

        // 1 < 6 && 3 < 4  ->  true
        assertThat(a.overlaps(b)).isTrue();
        assertThat(b.overlaps(a)).isTrue();
    }

    @Test
    @DisplayName("Khac thu thi khong bao gio trung du tiet giong nhau")
    void khac_thu_khong_trung() {
        assertThat(section(2, 1, 3).overlaps(section(3, 1, 3))).isFalse();
    }

    @Test
    @DisplayName("So voi null tra ve false, khong nem NullPointerException")
    void so_voi_null() {
        assertThat(section(2, 1, 3).overlaps(null)).isFalse();
    }

    @ParameterizedTest(name = "Thu {0}: [{1},{2}) vs [{3},{4}) -> trung = {5}")
    @CsvSource({
            // day, startA, countA, startB, countB, kyVongTrung
            "2, 1, 3, 1, 3, true",    // trung hoan toan
            "2, 1, 3, 2, 1, true",    // B nam gon trong A
            "2, 2, 1, 1, 3, true",    // A nam gon trong B
            "2, 1, 3, 3, 1, true",    // chong dung tiet 3
            "2, 1, 3, 4, 1, false",   // lien ke ngay sau
            "2, 4, 3, 1, 3, false",   // lien ke ngay truoc
            "2, 1, 2, 5, 2, false",   // cach xa nhau
            "6, 10, 3, 12, 1, true",  // chong o cuoi ngay
            "7, 1, 12, 12, 1, true"   // ca ngay trum tiet cuoi
    })
    void bang_ca_bien(int day, int startA, int countA, int startB, int countB, boolean expected) {
        assertThat(section(day, startA, countA).overlaps(section(day, startB, countB)))
                .isEqualTo(expected);
    }
}
