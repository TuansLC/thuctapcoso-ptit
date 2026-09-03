package com.ptit.courseregistration.service;

import com.ptit.courseregistration.domain.Course;
import com.ptit.courseregistration.dto.CourseForm;
import com.ptit.courseregistration.exception.BusinessException;
import com.ptit.courseregistration.exception.NotFoundException;
import com.ptit.courseregistration.repository.ClassSectionRepository;
import com.ptit.courseregistration.repository.CourseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Quan ly mon hoc -- chuc nang admin, DOAN.md muc 5.
 *
 * Day la man hinh NHAP sinh ra du lieu "mon hoc, tin chi, mon tien quyet" trong bang
 * truy vet o muc 6.
 */
@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final ClassSectionRepository sectionRepository;

    public CourseService(CourseRepository courseRepository,
                         ClassSectionRepository sectionRepository) {
        this.courseRepository = courseRepository;
        this.sectionRepository = sectionRepository;
    }

    @Transactional(readOnly = true)
    public List<Course> list() {
        return courseRepository.findAllWithPrerequisite();
    }

    @Transactional(readOnly = true)
    public Course get(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy môn học."));
    }

    /**
     * Danh sach mon co the chon lam tien quyet: moi mon TRU chinh no.
     * Chu trinh sau mot cap duoc chan rieng trong save().
     */
    @Transactional(readOnly = true)
    public List<Course> selectablePrerequisites(Long editingId) {
        return courseRepository.findAllWithPrerequisite().stream()
                .filter(c -> editingId == null || !c.getId().equals(editingId))
                .toList();
    }

    @Transactional
    public Course save(CourseForm form) {
        String code = form.getCode().trim();

        courseRepository.findByCode(code).ifPresent(existing -> {
            if (form.isNew() || !existing.getId().equals(form.getId())) {
                throw new BusinessException("Mã môn học " + code + " đã tồn tại.");
            }
        });

        Course prerequisite = resolvePrerequisite(form);

        if (form.isNew()) {
            return courseRepository.save(
                    new Course(code, form.getName().trim(), form.getCredits(), prerequisite));
        }

        Course course = get(form.getId());
        course.setCode(code);
        course.setName(form.getName().trim());
        course.setCredits(form.getCredits());
        course.setPrerequisite(prerequisite);
        return course;
    }

    /**
     * Chan mon tu lam tien quyet cua chinh no, va chan chu trinh dai hon (A -> B -> A).
     *
     * Khong co chan nay thi checkPrerequisitePassed() se khong bao gio thoa duoc,
     * va sinh vien khong bao gio dang ky duoc mon do -- mot loai bug im lang.
     */
    private Course resolvePrerequisite(CourseForm form) {
        if (form.getPrerequisiteId() == null) {
            return null;
        }
        if (form.getPrerequisiteId().equals(form.getId())) {
            throw new BusinessException("Môn học không thể là môn tiên quyết của chính nó.");
        }

        Course prerequisite = courseRepository.findById(form.getPrerequisiteId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy môn tiên quyết đã chọn."));

        // Di nguoc chuoi tien quyet; neu gap lai chinh mon dang sua thi thanh chu trinh.
        Course cursor = prerequisite;
        int guard = 0;
        while (cursor != null && guard++ < 50) {
            if (form.getId() != null && cursor.getId().equals(form.getId())) {
                throw new BusinessException(
                        "Chọn " + prerequisite.getName() + " làm tiên quyết sẽ tạo ra vòng lặp "
                                + "tiên quyết, khiến không sinh viên nào đăng ký được môn này.");
            }
            cursor = cursor.getPrerequisite();
        }
        return prerequisite;
    }

    /**
     * Xoa mon hoc.
     *
     * Chan thay vi xoa cung khi con du lieu tham chieu (muc 11). Xoa cung se keo theo
     * xoa lop hoc phan va xoa dang ky cua sinh vien -- mat du lieu ma khong ai yeu cau.
     */
    @Transactional
    public void delete(Long id) {
        Course course = get(id);

        if (sectionRepository.existsByCourseId(id)) {
            throw new BusinessException("Không xóa được môn " + course.getName()
                    + " vì đang có lớp học phần thuộc môn này. Xóa các lớp đó trước.");
        }
        if (courseRepository.existsByPrerequisiteId(id)) {
            throw new BusinessException("Không xóa được môn " + course.getName()
                    + " vì đang là môn tiên quyết của môn khác.");
        }
        courseRepository.delete(course);
    }
}
