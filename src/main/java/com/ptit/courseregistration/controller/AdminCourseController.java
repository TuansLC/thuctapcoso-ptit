package com.ptit.courseregistration.controller;

import com.ptit.courseregistration.dto.CourseForm;
import com.ptit.courseregistration.exception.BusinessException;
import com.ptit.courseregistration.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Quan ly mon hoc. Controller chi dieu phoi, nghiep vu o CourseService (DOAN.md muc 12).
 *
 * Phan quyen do SecurityConfig lo: /admin/** yeu cau vai ADMIN.
 */
@Controller
@RequestMapping("/admin/courses")
public class AdminCourseController {

    private final CourseService courseService;

    public AdminCourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public String index(Model model) {
        return render(model, new CourseForm());
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        return render(model, CourseForm.from(courseService.get(id)));
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("form") CourseForm form,
                       BindingResult binding,
                       Model model,
                       RedirectAttributes flash) {
        if (binding.hasErrors()) {
            return renderWithForm(model);
        }
        try {
            courseService.save(form);
        } catch (BusinessException ex) {
            // Hien loi ngay tren form, giu lai gia tri da nhap.
            binding.reject("save.failed", ex.getMessage());
            return renderWithForm(model);
        }
        flash.addFlashAttribute("successMessage",
                form.isNew() ? "Đã thêm môn học." : "Đã cập nhật môn học.");
        return "redirect:/admin/courses";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        courseService.delete(id);
        flash.addFlashAttribute("successMessage", "Đã xóa môn học.");
        return "redirect:/admin/courses";
    }

    private String render(Model model, CourseForm form) {
        model.addAttribute("form", form);
        return renderWithForm(model);
    }

    /** Form da nam trong model (do @ModelAttribute hoac render()), chi bo sung danh sach. */
    private String renderWithForm(Model model) {
        CourseForm form = (CourseForm) model.getAttribute("form");
        Long editingId = form == null ? null : form.getId();

        model.addAttribute("courses", courseService.list());
        model.addAttribute("prerequisiteOptions", courseService.selectablePrerequisites(editingId));
        return "admin/courses";
    }
}
