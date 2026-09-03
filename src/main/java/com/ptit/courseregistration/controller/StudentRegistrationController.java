package com.ptit.courseregistration.controller;

import com.ptit.courseregistration.domain.Semester;
import com.ptit.courseregistration.service.AppUserDetails;
import com.ptit.courseregistration.service.CourseService;
import com.ptit.courseregistration.service.RegistrationService;
import com.ptit.courseregistration.service.SemesterService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Man hinh "Lop dang mo" cua sinh vien: danh sach, loc, nut dang ky / rut
 * (DOAN.md muc 5).
 *
 * Controller chi dieu phoi. Toan bo nghiep vu -- ke ca lock va 5 tang validate --
 * nam trong RegistrationService (muc 12).
 */
@Controller
@RequestMapping("/student/sections")
public class StudentRegistrationController {

    private final RegistrationService registrationService;
    private final SemesterService semesterService;
    private final CourseService courseService;

    public StudentRegistrationController(RegistrationService registrationService,
                                         SemesterService semesterService,
                                         CourseService courseService) {
        this.registrationService = registrationService;
        this.semesterService = semesterService;
        this.courseService = courseService;
    }

    /** So lop hien tren mot trang. */
    private static final int PAGE_SIZE = 10;

    @GetMapping
    public String index(@AuthenticationPrincipal AppUserDetails currentUser,
                        @RequestParam(required = false) Long courseId,
                        @RequestParam(required = false, defaultValue = "false") boolean onlyAvailable,
                        @RequestParam(required = false, defaultValue = "0") int page,
                        Model model) {

        Optional<Semester> active = semesterService.findActive();
        model.addAttribute("activeSemester", active.orElse(null));
        model.addAttribute("courses", courseService.list());
        model.addAttribute("selectedCourseId", courseId);
        model.addAttribute("onlyAvailable", onlyAvailable);
        model.addAttribute("now", LocalDateTime.now());

        // page am tu URL do nguoi dung sua tay: dua ve 0 thay vi de PageRequest nem loi.
        int safePage = Math.max(0, page);
        Pageable pageable = PageRequest.of(safePage, PAGE_SIZE,
                Sort.by("course.code", "code"));

        // Khong co ky nao dang mo thi khong co gi de hien; template noi ro dieu do.
        active.ifPresent(semester -> model.addAttribute("rows",
                registrationService.listOpenSections(
                        currentUser.getId(), semester.getId(), courseId, onlyAvailable, pageable)));

        return "student/sections";
    }

    @PostMapping("/{sectionId}/register")
    public String register(@AuthenticationPrincipal AppUserDetails currentUser,
                           @PathVariable Long sectionId,
                           RedirectAttributes flash) {
        // Loi nghiep vu (lop day, trung lich, thieu tien quyet...) do
        // GlobalExceptionHandler bat va chuyen thanh flash message.
        registrationService.register(currentUser.getId(), sectionId);
        flash.addFlashAttribute("successMessage", "Đăng ký thành công.");
        return "redirect:/student/sections";
    }

    @PostMapping("/{sectionId}/drop")
    public String drop(@AuthenticationPrincipal AppUserDetails currentUser,
                       @PathVariable Long sectionId,
                       RedirectAttributes flash) {
        registrationService.drop(currentUser.getId(), sectionId);
        flash.addFlashAttribute("successMessage", "Đã rút lớp.");
        return "redirect:/student/sections";
    }
}
