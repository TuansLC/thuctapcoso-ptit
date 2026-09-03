package com.ptit.courseregistration.controller;

import com.ptit.courseregistration.dto.RegisterForm;
import com.ptit.courseregistration.exception.BusinessException;
import com.ptit.courseregistration.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Dang nhap / dang xuat do Spring Security xu ly, controller nay chi tra ve view
 * dang nhap va lo trinh dang ky tai khoan sinh vien.
 *
 * Controller chi dieu phoi, nghiep vu nam o AuthService (DOAN.md muc 12).
 */
@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String loginPage() {
        // Form POST /login do filter cua Spring Security nhan, khong co handler o day.
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("form", new RegisterForm());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterForm form,
                           BindingResult binding,
                           RedirectAttributes flash) {
        if (!form.isPasswordConfirmed()) {
            binding.rejectValue("confirmPassword", "password.mismatch",
                    "Mật khẩu nhập lại không khớp");
        }
        if (binding.hasErrors()) {
            // Tra lai form kem thong bao loi tren tung truong, giu nguyen gia tri da nhap.
            return "auth/register";
        }

        try {
            authService.registerStudent(form);
        } catch (BusinessException ex) {
            // Trung username / trung ma sinh vien: hien ngay tren form thay vi
            // day qua GlobalExceptionHandler, de nguoi dung khong mat du lieu da nhap.
            binding.reject("register.failed", ex.getMessage());
            return "auth/register";
        }

        flash.addFlashAttribute("successMessage",
                "Tạo tài khoản thành công. Đăng nhập để bắt đầu.");
        return "redirect:/login";
    }
}
