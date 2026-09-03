package com.ptit.courseregistration.dto;

import com.ptit.courseregistration.domain.Registration;
import com.ptit.courseregistration.domain.Semester;

import java.util.List;

/**
 * Ket qua hoc tap cua mot hoc ky tren man hinh "Hoc phan cua toi".
 *
 * @param semester      hoc ky
 * @param registrations cac lop da dang ky trong ky, da fetch san course
 * @param summary       tin chi va GPA CUA RIENG KY NAY (du lieu dan xuat)
 */
public record SemesterResult(
        Semester semester,
        List<Registration> registrations,
        AcademicSummary summary
) {
}
