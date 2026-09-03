package com.ptit.courseregistration.controller;

import com.ptit.courseregistration.domain.Role;
import com.ptit.courseregistration.service.AppUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Trang goc.
 *
 * Sau khi dang nhap, Spring Security luon dua nguoi dung ve "/" (xem
 * defaultSuccessUrl trong SecurityConfig), va cho nay dieu huong tiep theo vai.
 * Dat dieu huong o day thay vi viet AuthenticationSuccessHandler rieng cho don gian,
 * va de "/" luon la mot diem vao dung cho ca hai vai.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home(@AuthenticationPrincipal AppUserDetails currentUser) {
        if (currentUser == null) {
            // Khach chua dang nhap: trang gioi thieu voi hai nut dang nhap / dang ky.
            return "home";
        }
        if (currentUser.getRole() == Role.ADMIN) {
            return "redirect:/admin/courses";
        }
        return "redirect:/student/sections";
    }
}
