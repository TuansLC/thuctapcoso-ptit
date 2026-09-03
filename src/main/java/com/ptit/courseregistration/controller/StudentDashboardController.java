package com.ptit.courseregistration.controller;

import com.ptit.courseregistration.domain.Registration;
import com.ptit.courseregistration.domain.Semester;
import com.ptit.courseregistration.dto.SemesterResult;
import com.ptit.courseregistration.repository.RegistrationRepository;
import com.ptit.courseregistration.service.AppUserDetails;
import com.ptit.courseregistration.service.GradeService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Man hinh "Hoc phan cua toi": lich, diem, tong tin chi, GPA (DOAN.md muc 5).
 *
 * Moi con so o day la DU LIEU DAN XUAT, tinh tu score va credits luc hien thi.
 * Khong co bang nao luu diem chu hay GPA (muc 6).
 */
@Controller
public class StudentDashboardController {

    private final RegistrationRepository registrationRepository;
    private final GradeService gradeService;

    public StudentDashboardController(RegistrationRepository registrationRepository,
                                      GradeService gradeService) {
        this.registrationRepository = registrationRepository;
        this.gradeService = gradeService;
    }

    @GetMapping("/student/my-courses")
    @Transactional(readOnly = true)
    public String myCourses(@AuthenticationPrincipal AppUserDetails currentUser, Model model) {
        List<Registration> all = registrationRepository
                .findByStudentWithSectionAndCourse(currentUser.getId());

        // Nhom theo hoc ky, giu thu tu ky moi nhat truoc do query da sort san.
        Map<Semester, List<Registration>> grouped = new LinkedHashMap<>();
        for (Registration r : all) {
            grouped.computeIfAbsent(r.getSection().getSemester(), k -> new ArrayList<>()).add(r);
        }

        List<SemesterResult> results = new ArrayList<>();
        grouped.forEach((semester, registrations) ->
                results.add(new SemesterResult(semester, registrations,
                        gradeService.summarize(registrations))));

        model.addAttribute("results", results);
        // GPA tich luy tinh tren TOAN BO cac ky, khong phai trung binh cua cac GPA ky.
        model.addAttribute("overall", gradeService.summarize(all));
        model.addAttribute("gradeService", gradeService);
        return "student/my-courses";
    }
}
