package com.ptit.courseregistration.service;

import com.ptit.courseregistration.domain.ClassSection;
import com.ptit.courseregistration.domain.Registration;
import com.ptit.courseregistration.domain.User;
import com.ptit.courseregistration.dto.OpenSectionView;
import com.ptit.courseregistration.exception.BusinessException;
import com.ptit.courseregistration.exception.CannotDropException;
import com.ptit.courseregistration.exception.ClassFullException;
import com.ptit.courseregistration.exception.NotFoundException;
import com.ptit.courseregistration.repository.ClassSectionRepository;
import com.ptit.courseregistration.repository.RegistrationRepository;
import com.ptit.courseregistration.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * =============================================================================
 * TRONG TAM KY THUAT CUA TOAN BO DO AN -- DOAN.md muc 7.
 * =============================================================================
 *
 * Bai toan: nhieu sinh vien bam dang ky cung mot lop trong cung mot khoanh khac.
 * Neu viet naive (doc registered_count, so sanh, roi ghi lai) thi gap loi LOST UPDATE:
 *
 *     t1   A doc count = 39
 *     t2                      B doc count = 39
 *     t3   A: 39 < 40 -> qua
 *     t4                      B: 39 < 40 -> qua
 *     t5   A ghi count = 40
 *     t6                      B ghi count = 40      <- ghi de
 *     => 41 sinh vien trong lop nhung count = 40
 *
 * Loi nay KHONG bao gio thay khi test mot minh. Chi xuat hien khi co tai.
 *
 * Cach giai: findByIdForUpdate() sinh ra "SELECT ... FOR UPDATE" -- mot LOCKING READ.
 * Locking read doc ban moi nhat DA COMMIT, khong doc snapshot MVCC. Do la ly do
 * nang isolation level khong giai quyet duoc: REPEATABLE READ cua InnoDB van doc
 * snapshot nen khong thay thay doi transaction khac vua commit (muc 7.4).
 *
 * Lock o muc DONG, va lock dung doi tuong bi tranh chap la suc chua cua lop hoc phan
 * (muc 7.7). Hai sinh vien dang ky hai lop KHAC nhau khong cho nhau chut nao --
 * day la cau tra loi cho "khoa the thi he thong cham a".
 */
@Service
public class RegistrationService {

    private final ClassSectionRepository sectionRepository;
    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final RegistrationRules rules;

    public RegistrationService(ClassSectionRepository sectionRepository,
                               RegistrationRepository registrationRepository,
                               UserRepository userRepository,
                               RegistrationRules rules) {
        this.sectionRepository = sectionRepository;
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
        this.rules = rules;
    }

    /**
     * Dang ky mot lop hoc phan.
     *
     * THU TU CAC BUOC LA CO CHU Y:
     *
     *  1. Lock dong lop hoc phan TRUOC. Tu day tro di, khong transaction nao khac
     *     doc-ghi duoc dong nay cho den khi transaction hien tai ket thuc.
     *  2. Chay 5 tang validate nghiep vu.
     *  3. Kiem tra si so -- doc registered_count SAU khi da lock nen day la so
     *     moi nhat da commit, khong phai snabshot cu.
     *  4. Tang counter va luu ban ghi dang ky.
     *
     * DANH DOI phai neu duoc khi bao ve: 5 tang validate chay TRONG luc dang giu lock,
     * nen thoi gian giu lock dai hon muc toi thieu. Doi lai la code de doc va de chung
     * minh dung. Neu can toi uu, co the validate truoc roi moi lock va kiem tra lai
     * si so, nhung khi do phai cham nhan mot vong truy van nua.
     *
     * KHONG lam viec gi cham trong transaction nay: khong gui email, khong goi API
     * ngoai (muc 7.9).
     */
    @Transactional
    public void register(Long studentId, Long sectionId) {
        // (1) SELECT ... FOR UPDATE -- co y KHONG join fetch de chi lock dong class_sections
        ClassSection section = sectionRepository.findByIdForUpdate(sectionId)
                .orElseThrow(() -> new NotFoundException("Lớp học phần không tồn tại."));

        LocalDateTime now = LocalDateTime.now();

        // (2) Nam tang validate nghiep vu
        rules.checkRegistrationOpen(section.getSemester(), now);   // cổng đăng ký còn mở
        rules.checkNotAlreadyRegistered(studentId, section);       // chưa đăng ký môn này
        rules.checkPrerequisitePassed(studentId, section.getCourse()); // đã đạt tiên quyết
        rules.checkNoScheduleConflict(studentId, section);         // không trùng thời khóa biểu
        rules.checkCreditLimit(studentId, section);                // chưa vượt giới hạn tín chỉ

        // (3) Si so -- KHONG co hang doi cho lop, het cho la bao loi (muc 14)
        if (section.isFull()) {
            throw new ClassFullException("Lớp " + section.getCode()
                    + " đã đủ sĩ số (" + section.getCapacity() + " chỗ).");
        }

        // (4) Chiem cho
        section.increaseRegisteredCount();

        // getReferenceById tra ve proxy, khong sinh them mot cau SELECT users.
        // Giu transaction ngan la co y: dang trong pham vi giu lock.
        User student = userRepository.getReferenceById(studentId);
        registrationRepository.save(Registration.of(student, section, now));
    }

    /**
     * Rut mot lop hoc phan.
     *
     * CUNG PHAI LOCK. drop() giam registered_count, tuc cung la doc-roi-ghi, nen cung
     * bi lost update. Bo lock o day thi counter troi lech dan xuong duoi so thuc, va
     * chk_capacity KHONG bat duoc vi rang buoc do chi chan chieu tang.
     */
    @Transactional
    public void drop(Long studentId, Long sectionId) {
        ClassSection section = sectionRepository.findByIdForUpdate(sectionId)
                .orElseThrow(() -> new NotFoundException("Lớp học phần không tồn tại."));

        Registration registration = registrationRepository
                .findByStudentIdAndSectionId(studentId, sectionId)
                .orElseThrow(() -> new NotFoundException("Bạn chưa đăng ký lớp này."));

        // Da co diem thi khong rut duoc -- muc 7.2
        if (registration.hasScore()) {
            throw new CannotDropException("Lớp " + section.getCode()
                    + " đã có điểm nên không thể rút.");
        }
        // Cong dang ky da dong thi khong rut duoc -- muc 7.2
        if (!section.getSemester().isRegistrationOpen(LocalDateTime.now())) {
            throw new CannotDropException("Cổng đăng ký của "
                    + section.getSemester().getName() + " đã đóng nên không thể rút lớp.");
        }

        section.decreaseRegisteredCount();
        registrationRepository.delete(registration);
    }

    /**
     * Cho biet vi sao sinh vien KHONG dang ky duoc lop nay, de man hinh "Lop dang mo"
     * giai thich thay vi chi lam mo cai nut.
     *
     * LUU Y BAN CHAT: ket qua nay CHI MANG TINH THAM KHAO va co the cu ngay khi vua
     * tinh xong -- giua luc hien trang va luc bam nut, nguoi khac co the da chiem cho.
     * Vi vay register() van kiem tra lai day du trong pham vi lock. Day khong phai
     * kiem tra trung lap vo ich: mot cai de hien thi, mot cai de bao dam dung.
     *
     * Dung lai dung cac ham cua RegistrationRules nen khong co ban logic thu hai.
     */
    @Transactional(readOnly = true)
    public Optional<String> whyCannotRegister(Long studentId, ClassSection section) {
        try {
            LocalDateTime now = LocalDateTime.now();
            rules.checkRegistrationOpen(section.getSemester(), now);
            rules.checkNotAlreadyRegistered(studentId, section);
            rules.checkPrerequisitePassed(studentId, section.getCourse());
            rules.checkNoScheduleConflict(studentId, section);
            rules.checkCreditLimit(studentId, section);
            if (section.isFull()) {
                return Optional.of("Lớp đã đủ sĩ số");
            }
            return Optional.empty();
        } catch (BusinessException ex) {
            return Optional.of(ex.getMessage());
        }
    }

    /**
     * Mot trang cua danh sach lop dang mo, kem trang thai doi voi sinh vien nay.
     *
     * Loc va phan trang lam DUOI DB, khong keo het ve roi cat trong bo nho.
     * Con phan tinh "vi sao khong dang ky duoc" thi lam trong bo nho nhung CHI TREN
     * CAC DONG CUA TRANG HIEN TAI -- moi dong can vai truy van nho, nen so dong phai
     * co gioi han. Day chinh la ly do thuc te de phai phan trang o man hinh nay.
     *
     * @param courseIdFilter null la khong loc theo mon
     */
    @Transactional(readOnly = true)
    public Page<OpenSectionView> listOpenSections(Long studentId, Long semesterId,
                                                  Long courseIdFilter, boolean onlyAvailable,
                                                  Pageable pageable) {
        Page<ClassSection> page = sectionRepository.findOpenSections(
                semesterId, courseIdFilter, onlyAvailable, pageable);

        Set<Long> registeredSectionIds = registrationRepository
                .findSectionsByStudentAndSemester(studentId, semesterId).stream()
                .map(ClassSection::getId)
                .collect(Collectors.toSet());

        return page.map(section -> {
            boolean registered = registeredSectionIds.contains(section.getId());
            String reason = registered ? null : whyCannotRegister(studentId, section).orElse(null);
            return new OpenSectionView(section, registered, reason);
        });
    }

    /**
     * Doi chieu counter registered_count voi so dong thuc te trong registrations.
     * Dung trong test de chung minh counter khong bi troi lech (muc 7.8).
     */
    @Transactional(readOnly = true)
    public boolean isCounterConsistent(Long sectionId) {
        ClassSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new NotFoundException("Lớp học phần không tồn tại."));
        long actual = registrationRepository.countBySectionId(sectionId);
        return actual == section.getRegisteredCount();
    }
}
