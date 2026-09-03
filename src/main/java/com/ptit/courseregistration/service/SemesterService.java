package com.ptit.courseregistration.service;

import com.ptit.courseregistration.domain.Semester;
import com.ptit.courseregistration.dto.SemesterForm;
import com.ptit.courseregistration.exception.BusinessException;
import com.ptit.courseregistration.exception.NotFoundException;
import com.ptit.courseregistration.repository.ClassSectionRepository;
import com.ptit.courseregistration.repository.SemesterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Quan ly hoc ky -- chuc nang admin, DOAN.md muc 5.
 *
 * QUY TAC DA CHOT: TAI MOT THOI DIEM CHI CO MOT HOC KY active.
 * Kich hoat ky moi thi tu dong tat cac ky con lai. Ly do: man hinh "Lop dang mo" cua
 * sinh vien hien lop cua ky dang hoat dong; neu co hai ky active cung luc thi khong
 * xac dinh duoc hien ky nao, va gioi han tin chi cung tro nen nhap nhang.
 *
 * Ky "chua chot" = active con true. Do la luc admin sua duoc diem (muc 11).
 */
@Service
public class SemesterService {

    private final SemesterRepository semesterRepository;
    private final ClassSectionRepository sectionRepository;

    public SemesterService(SemesterRepository semesterRepository,
                           ClassSectionRepository sectionRepository) {
        this.semesterRepository = semesterRepository;
        this.sectionRepository = sectionRepository;
    }

    @Transactional(readOnly = true)
    public List<Semester> list() {
        return semesterRepository.findAllByOrderByIdDesc();
    }

    @Transactional(readOnly = true)
    public Semester get(Long id) {
        return semesterRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy học kỳ."));
    }

    @Transactional(readOnly = true)
    public Optional<Semester> findActive() {
        return semesterRepository.findByActiveTrue();
    }

    @Transactional
    public Semester save(SemesterForm form) {
        String name = form.getName().trim();

        if (!form.getRegOpenAt().isBefore(form.getRegCloseAt())) {
            throw new BusinessException(
                    "Thời điểm mở cổng phải trước thời điểm đóng cổng đăng ký.");
        }

        boolean duplicated = semesterRepository.findAllByOrderByIdDesc().stream()
                .anyMatch(s -> s.getName().equalsIgnoreCase(name)
                        && (form.isNew() || !s.getId().equals(form.getId())));
        if (duplicated) {
            throw new BusinessException("Đã có học kỳ tên " + name + ".");
        }

        Semester semester;
        if (form.isNew()) {
            semester = semesterRepository.save(new Semester(
                    name, form.getRegOpenAt(), form.getRegCloseAt(),
                    form.getMaxCredits(), form.isActive()));
        } else {
            semester = get(form.getId());
            semester.setName(name);
            semester.setRegOpenAt(form.getRegOpenAt());
            semester.setRegCloseAt(form.getRegCloseAt());
            semester.setMaxCredits(form.getMaxCredits());
            semester.setActive(form.isActive());
        }

        if (form.isActive()) {
            deactivateOthers(semester.getId());
        }
        return semester;
    }

    /** Dong ky hien tai va mo ky duoc chon. Dung o buoc 10 kich ban demo muc 9. */
    @Transactional
    public void activate(Long id) {
        Semester semester = get(id);
        semester.setActive(true);
        deactivateOthers(id);
    }

    @Transactional
    public void deactivate(Long id) {
        get(id).setActive(false);
    }

    private void deactivateOthers(Long keepId) {
        for (Semester other : semesterRepository.findAllByOrderByIdDesc()) {
            if (!other.getId().equals(keepId) && Boolean.TRUE.equals(other.getActive())) {
                other.setActive(false);
            }
        }
    }

    @Transactional
    public void delete(Long id) {
        Semester semester = get(id);
        if (sectionRepository.existsBySemesterId(id)) {
            throw new BusinessException("Không xóa được " + semester.getName()
                    + " vì đang có lớp học phần trong kỳ này. Xóa các lớp đó trước.");
        }
        semesterRepository.delete(semester);
    }
}
