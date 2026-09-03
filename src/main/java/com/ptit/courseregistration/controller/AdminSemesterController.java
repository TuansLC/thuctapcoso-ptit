package com.ptit.courseregistration.controller;

import com.ptit.courseregistration.dto.SemesterForm;
import com.ptit.courseregistration.exception.BusinessException;
import com.ptit.courseregistration.service.SemesterService;
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

import java.time.LocalDateTime;

/** Quan ly hoc ky: ten ky, mo/dong cong dang ky, gioi han tin chi (DOAN.md muc 5). */
@Controller
@RequestMapping("/admin/semesters")
public class AdminSemesterController {

    private final SemesterService semesterService;

    public AdminSemesterController(SemesterService semesterService) {
        this.semesterService = semesterService;
    }

    @GetMapping
    public String index(Model model) {
        SemesterForm form = new SemesterForm();
        // Goi y mot khoang thoi gian hop ly de admin khong phai tu go tu dau.
        form.setRegOpenAt(LocalDateTime.now().withSecond(0).withNano(0));
        form.setRegCloseAt(LocalDateTime.now().plusDays(30).withSecond(0).withNano(0));
        return render(model, form);
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        return render(model, SemesterForm.from(semesterService.get(id)));
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("form") SemesterForm form,
                       BindingResult binding,
                       Model model,
                       RedirectAttributes flash) {
        if (binding.hasErrors()) {
            return renderList(model);
        }
        try {
            semesterService.save(form);
        } catch (BusinessException ex) {
            binding.reject("save.failed", ex.getMessage());
            return renderList(model);
        }
        flash.addFlashAttribute("successMessage",
                form.isNew() ? "Đã thêm học kỳ." : "Đã cập nhật học kỳ.");
        return "redirect:/admin/semesters";
    }

    /** Mo ky nay va tu dong dong cac ky khac -- chi mot ky active (SemesterService). */
    @PostMapping("/{id}/activate")
    public String activate(@PathVariable Long id, RedirectAttributes flash) {
        semesterService.activate(id);
        flash.addFlashAttribute("successMessage",
                "Đã mở học kỳ này và đóng các học kỳ còn lại.");
        return "redirect:/admin/semesters";
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(@PathVariable Long id, RedirectAttributes flash) {
        semesterService.deactivate(id);
        flash.addFlashAttribute("successMessage",
                "Đã chốt học kỳ. Từ giờ không sửa được điểm của kỳ này.");
        return "redirect:/admin/semesters";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        semesterService.delete(id);
        flash.addFlashAttribute("successMessage", "Đã xóa học kỳ.");
        return "redirect:/admin/semesters";
    }

    private String render(Model model, SemesterForm form) {
        model.addAttribute("form", form);
        return renderList(model);
    }

    private String renderList(Model model) {
        model.addAttribute("semesters", semesterService.list());
        model.addAttribute("now", LocalDateTime.now());
        return "admin/semesters";
    }
}
