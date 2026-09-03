package com.ptit.courseregistration.controller;

import com.ptit.courseregistration.domain.Registration;
import com.ptit.courseregistration.dto.GradeEntryForm;
import com.ptit.courseregistration.exception.BusinessException;
import com.ptit.courseregistration.service.ClassSectionService;
import com.ptit.courseregistration.service.GradeService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Nhap diem: chon lop, nhap diem ca bang mot lan (DOAN.md muc 5).
 *
 * Day la man hinh NHAP sinh ra du lieu "diem so" trong bang truy vet o muc 6, va la
 * buoc 8 cua kich ban demo muc 9 -- diem nhap o day tro thanh dieu kien tien quyet
 * chan dang ky o ky sau.
 */
@Controller
@RequestMapping("/admin/grades")
public class AdminGradeController {

    private final GradeService gradeService;
    private final ClassSectionService sectionService;

    public AdminGradeController(GradeService gradeService, ClassSectionService sectionService) {
        this.gradeService = gradeService;
        this.sectionService = sectionService;
    }

    @GetMapping
    public String index(@RequestParam(required = false) Long sectionId, Model model) {
        model.addAttribute("sections", sectionService.list());

        if (sectionId == null) {
            model.addAttribute("form", new GradeEntryForm());
            return "admin/grades";
        }

        var section = sectionService.get(sectionId);
        List<Registration> registrations = gradeService.listForEntry(sectionId);

        GradeEntryForm form = new GradeEntryForm();
        form.setSectionId(sectionId);
        for (Registration r : registrations) {
            GradeEntryForm.Row row = new GradeEntryForm.Row();
            row.setRegistrationId(r.getId());
            row.setScore(r.getScore());
            form.getRows().add(row);
        }

        model.addAttribute("form", form);
        model.addAttribute("section", section);
        model.addAttribute("registrations", registrations);
        model.addAttribute("gradeService", gradeService);
        return "admin/grades";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("form") GradeEntryForm form,
                       BindingResult binding,
                       Model model,
                       RedirectAttributes flash) {
        if (form.getSectionId() == null) {
            flash.addFlashAttribute("errorMessage", "Chưa chọn lớp học phần.");
            return "redirect:/admin/grades";
        }
        if (binding.hasErrors()) {
            return reRender(form, model);
        }
        try {
            int changed = gradeService.saveScores(form.getSectionId(), form.toScoreMap());
            flash.addFlashAttribute("successMessage", changed == 0
                    ? "Không có điểm nào thay đổi."
                    : "Đã lưu điểm cho " + changed + " sinh viên.");
        } catch (BusinessException ex) {
            binding.reject("save.failed", ex.getMessage());
            return reRender(form, model);
        }
        return "redirect:/admin/grades?sectionId=" + form.getSectionId();
    }

    private String reRender(GradeEntryForm form, Model model) {
        model.addAttribute("sections", sectionService.list());
        model.addAttribute("section", sectionService.get(form.getSectionId()));
        model.addAttribute("registrations", gradeService.listForEntry(form.getSectionId()));
        model.addAttribute("gradeService", gradeService);
        return "admin/grades";
    }
}
