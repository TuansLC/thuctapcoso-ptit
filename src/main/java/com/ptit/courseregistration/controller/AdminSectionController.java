package com.ptit.courseregistration.controller;

import com.ptit.courseregistration.dto.ClassSectionForm;
import com.ptit.courseregistration.exception.BusinessException;
import com.ptit.courseregistration.service.ClassSectionService;
import com.ptit.courseregistration.service.CourseService;
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

/** Quan ly lop hoc phan: mon, ky, giang vien, phong, thu/tiet, si so (DOAN.md muc 5). */
@Controller
@RequestMapping("/admin/sections")
public class AdminSectionController {

    private final ClassSectionService sectionService;
    private final CourseService courseService;
    private final SemesterService semesterService;

    public AdminSectionController(ClassSectionService sectionService,
                                  CourseService courseService,
                                  SemesterService semesterService) {
        this.sectionService = sectionService;
        this.courseService = courseService;
        this.semesterService = semesterService;
    }

    @GetMapping
    public String index(Model model) {
        ClassSectionForm form = new ClassSectionForm();
        // Mac dinh tro vao ky dang mo de admin bot mot buoc chon.
        semesterService.findActive().ifPresent(s -> form.setSemesterId(s.getId()));
        return render(model, form);
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        return render(model, ClassSectionForm.from(sectionService.get(id)));
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("form") ClassSectionForm form,
                       BindingResult binding,
                       Model model,
                       RedirectAttributes flash) {
        if (binding.hasErrors()) {
            return renderList(model);
        }
        try {
            sectionService.save(form);
        } catch (BusinessException ex) {
            binding.reject("save.failed", ex.getMessage());
            return renderList(model);
        }
        flash.addFlashAttribute("successMessage",
                form.isNew() ? "Đã mở lớp học phần." : "Đã cập nhật lớp học phần.");
        return "redirect:/admin/sections";
    }

    /**
     * Xoa lop kem cac dang ky cua lop do, va noi ro da xoa bao nhieu ban ghi --
     * cau tra loi cho "dong cong ma lop thieu si so" o muc 11.
     */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        int removed = sectionService.delete(id);
        flash.addFlashAttribute("successMessage", removed == 0
                ? "Đã xóa lớp học phần."
                : "Đã xóa lớp học phần và " + removed + " bản ghi đăng ký kèm theo.");
        return "redirect:/admin/sections";
    }

    private String render(Model model, ClassSectionForm form) {
        model.addAttribute("form", form);
        return renderList(model);
    }

    private String renderList(Model model) {
        model.addAttribute("sections", sectionService.list());
        model.addAttribute("courses", courseService.list());
        model.addAttribute("semesters", semesterService.list());
        return "admin/sections";
    }
}
