package com.ptit.courseregistration.service;

import com.ptit.courseregistration.domain.ClassSection;
import com.ptit.courseregistration.domain.Course;
import com.ptit.courseregistration.domain.Semester;
import com.ptit.courseregistration.dto.ClassSectionForm;
import com.ptit.courseregistration.exception.BusinessException;
import com.ptit.courseregistration.exception.NotFoundException;
import com.ptit.courseregistration.repository.ClassSectionRepository;
import com.ptit.courseregistration.repository.CourseRepository;
import com.ptit.courseregistration.repository.RegistrationRepository;
import com.ptit.courseregistration.repository.SemesterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Quan ly lop hoc phan -- chuc nang admin, DOAN.md muc 5.
 *
 * Day la man hinh NHAP sinh ra du lieu "lop mo, giang vien, phong, thu/tiet, si so"
 * trong bang truy vet o muc 6.
 */
@Service
public class ClassSectionService {

    private static final Logger log = LoggerFactory.getLogger(ClassSectionService.class);

    /** Mot ngay hoc toi da 12 tiet, khop chu thich cot start_period o muc 4. */
    private static final int LAST_PERIOD_OF_DAY = 12;

    private final ClassSectionRepository sectionRepository;
    private final CourseRepository courseRepository;
    private final SemesterRepository semesterRepository;
    private final RegistrationRepository registrationRepository;

    public ClassSectionService(ClassSectionRepository sectionRepository,
                               CourseRepository courseRepository,
                               SemesterRepository semesterRepository,
                               RegistrationRepository registrationRepository) {
        this.sectionRepository = sectionRepository;
        this.courseRepository = courseRepository;
        this.semesterRepository = semesterRepository;
        this.registrationRepository = registrationRepository;
    }

    @Transactional(readOnly = true)
    public List<ClassSection> list() {
        return sectionRepository.findAllWithCourseAndSemester();
    }

    @Transactional(readOnly = true)
    public List<ClassSection> listBySemester(Long semesterId) {
        return sectionRepository.findBySemesterWithCourse(semesterId);
    }

    @Transactional(readOnly = true)
    public ClassSection get(Long id) {
        return sectionRepository.findByIdWithCourseAndSemester(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy lớp học phần."));
    }

    @Transactional
    public ClassSection save(ClassSectionForm form) {
        String code = form.getCode().trim();

        sectionRepository.findAllWithCourseAndSemester().stream()
                .filter(s -> s.getCode().equalsIgnoreCase(code))
                .findFirst()
                .ifPresent(existing -> {
                    if (form.isNew() || !existing.getId().equals(form.getId())) {
                        throw new BusinessException("Mã lớp " + code + " đã tồn tại.");
                    }
                });

        // Khoang tiet phai nam trong mot ngay. end la bien phai MO nen tiet cuoi la
        // start + count - 1 (muc 7.2).
        int lastPeriod = form.getStartPeriod() + form.getPeriodCount() - 1;
        if (lastPeriod > LAST_PERIOD_OF_DAY) {
            throw new BusinessException("Lớp bắt đầu từ tiết " + form.getStartPeriod()
                    + " và học " + form.getPeriodCount() + " tiết sẽ kết thúc ở tiết "
                    + lastPeriod + ", vượt quá tiết " + LAST_PERIOD_OF_DAY + " của một ngày.");
        }

        Course course = courseRepository.findById(form.getCourseId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy môn học đã chọn."));
        Semester semester = semesterRepository.findById(form.getSemesterId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy học kỳ đã chọn."));

        if (form.isNew()) {
            return sectionRepository.save(new ClassSection(
                    code, course, semester,
                    trimToNull(form.getLecturerName()), trimToNull(form.getRoom()),
                    form.getDayOfWeek(), form.getStartPeriod(), form.getPeriodCount(),
                    form.getCapacity()));
        }

        ClassSection section = get(form.getId());
        int registered = section.getRegisteredCount();

        // Muc 7.2: chan ha si so xuong duoi so da dang ky. Neu de lot xuong DB thi
        // vi pham chk_capacity va nguoi dung nhan mot loi SQL tho.
        if (form.getCapacity() < registered) {
            throw new BusinessException("Không hạ sĩ số xuống " + form.getCapacity()
                    + " được vì lớp đã có " + registered
                    + " sinh viên đăng ký. Sĩ số phải từ " + registered + " trở lên.");
        }

        // Doi thu/tiet cua lop DA co sinh vien se lam thoi khoa bieu cua ho trung nhau
        // ma khong ai kiem tra lai. Chan o day thay vi de du lieu sai am tham.
        boolean scheduleChanged =
                !Objects.equals(section.getDayOfWeek(), form.getDayOfWeek())
                        || !Objects.equals(section.getStartPeriod(), form.getStartPeriod())
                        || !Objects.equals(section.getPeriodCount(), form.getPeriodCount());
        if (scheduleChanged && registered > 0) {
            throw new BusinessException("Lớp đã có " + registered
                    + " sinh viên đăng ký nên không đổi được thứ/tiết, vì thời khóa biểu của họ"
                    + " có thể trở thành trùng lịch. Hãy mở một lớp mới hoặc xóa lớp này.");
        }

        section.setCode(code);
        section.setCourse(course);
        section.setSemester(semester);
        section.setLecturerName(trimToNull(form.getLecturerName()));
        section.setRoom(trimToNull(form.getRoom()));
        section.setDayOfWeek(form.getDayOfWeek());
        section.setStartPeriod(form.getStartPeriod());
        section.setPeriodCount(form.getPeriodCount());
        section.setCapacity(form.getCapacity());
        return section;
    }

    /**
     * Xoa lop hoc phan, xoa luon cac dang ky cua lop do.
     *
     * PHAI xoa registrations TRUOC roi moi xoa lop, vi khoa ngoai KHONG dung
     * ON DELETE CASCADE (muc 4). Ca hai lenh nam trong CUNG MOT transaction nen
     * khong co trang thai trung gian nao bi nhin thay.
     *
     * Day la cau tra loi cho "dong cong ma lop thieu si so" o muc 11.
     */
    @Transactional
    public int delete(Long id) {
        ClassSection section = get(id);
        int removed = registrationRepository.deleteBySectionId(id);
        sectionRepository.delete(section);

        log.info("Da xoa lop {} kem {} ban ghi dang ky", section.getCode(), removed);
        return removed;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
