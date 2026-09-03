package com.ptit.courseregistration.service;

import com.ptit.courseregistration.domain.ClassSection;
import com.ptit.courseregistration.domain.Course;
import com.ptit.courseregistration.domain.Registration;
import com.ptit.courseregistration.domain.Semester;
import com.ptit.courseregistration.domain.User;
import com.ptit.courseregistration.repository.ClassSectionRepository;
import com.ptit.courseregistration.repository.CourseRepository;
import com.ptit.courseregistration.repository.RegistrationRepository;
import com.ptit.courseregistration.repository.SemesterRepository;
import com.ptit.courseregistration.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * =============================================================================
 * BANG CHUNG CUA DO AN -- DOAN.md muc 8.
 * =============================================================================
 *
 * HAI DIEU KIEN BAT BUOC VE MOI TRUONG (muc 8.1), neu sai thi ket qua vo gia tri
 * ma test VAN XANH -- tuc la con te hon la do:
 *
 *  1. KHONG co @Transactional tren lop test nay. Neu co, du lieu setup nam trong
 *     transaction chua commit, 200 thread o transaction khac se khong thay no, va
 *     hanh vi lock khong phan anh thuc te chut nao.
 *
 *  2. Chay tren MySQL THAT qua Testcontainers, khong phai H2. H2 xu ly
 *     "SELECT ... FOR UPDATE" va CHECK khac InnoDB, nen chung minh tren H2 la
 *     chung minh ve H2 chu khong phai ve he thong se chay.
 *
 * Mot chi tiet nua: CountDownLatch la bat buoc. Neu tha 200 thread ra tu chay,
 * chung khoi dong lech nhau vai chuc milli giay va KHONG thuc su dong thoi --
 * test se xanh du code sai.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class RegistrationConcurrencyTest {

    private static final Logger log = LoggerFactory.getLogger(RegistrationConcurrencyTest.class);

    /**
     * MySQL that, khong phai H2. static de ca lop dung chung mot container.
     * {@code @ServiceConnection} tu tro spring.datasource vao container nay,
     * nen application-test.yml khong khai bao datasource.
     */
    @Container
    @ServiceConnection
    static MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    private static final int CAPACITY = 40;
    private static final int ATTEMPTS = 200;
    private static final int THREADS  = 32;

    @Autowired private RegistrationService registrationService;
    @Autowired private ClassSectionRepository sectionRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private SemesterRepository semesterRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RegistrationRepository registrationRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    // =========================================================================
    // TEST CHINH: co lock -> dung bang capacity
    // =========================================================================
    @Test
    @DisplayName("200 yeu cau dong thoi vao lop si so 40: dung 40 thanh cong")
    void chi_dung_capacity_sinh_vien_dang_ky_thanh_cong() throws Exception {
        Long sectionId = createSection("LOCK");
        List<Long> studentIds = createStudents("lock", ATTEMPTS);

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch startLine = new CountDownLatch(1);
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        for (int i = 0; i < ATTEMPTS; i++) {
            long studentId = studentIds.get(i);
            pool.submit(() -> {
                try {
                    startLine.await();                       // chan tat ca o mot vach
                    registrationService.register(studentId, sectionId);
                    ok.incrementAndGet();
                } catch (Exception expected) {
                    // Lop day la ket qua DUNG voi phan lon thread, khong phai su co
                    rejected.incrementAndGet();
                }
            });
        }

        startLine.countDown();                               // tha cung luc
        pool.shutdown();
        assertThat(pool.awaitTermination(120, TimeUnit.SECONDS))
                .as("200 yeu cau phai xu ly xong trong 120 giay")
                .isTrue();

        int counter = sectionRepository.findById(sectionId).orElseThrow().getRegisteredCount();
        long actualRows = registrationRepository.countBySectionId(sectionId);

        log.info("""

                ================= CO LOCK =================
                 So yeu cau dong thoi : {}
                 Si so lop            : {}
                 Dang ky thanh cong   : {}
                 Bi tu choi           : {}
                 registered_count     : {}
                 So dong registrations: {}
                ===========================================
                """, ATTEMPTS, CAPACITY, ok.get(), rejected.get(), counter, actualRows);

        assertThat(ok.get())
                .as("chi duoc dung %d sinh vien vao lop", CAPACITY)
                .isEqualTo(CAPACITY);
        assertThat(counter)
                .as("counter registered_count phai bang si so")
                .isEqualTo(CAPACITY);
        assertThat(actualRows)
                .as("so ban ghi dang ky THUC TE phai bang si so, khong duoc vuot")
                .isEqualTo(CAPACITY);
        assertThat(registrationService.isCounterConsistent(sectionId))
                .as("counter khong duoc troi lech so voi so dong thuc te")
                .isTrue();
    }

    // =========================================================================
    // TEST DOI CHUNG: khong lock -> lost update
    // =========================================================================
    @Test
    @DisplayName("Khong lock: 200 yeu cau vao lop si so 40 lot qua het -> lost update")
    void khong_lock_thi_lost_update() throws Exception {
        Long sectionId = createSection("NAIVE");
        List<Long> studentIds = createStudents("naive", ATTEMPTS);

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch startLine = new CountDownLatch(1);
        AtomicInteger ok = new AtomicInteger();

        for (int i = 0; i < ATTEMPTS; i++) {
            long studentId = studentIds.get(i);
            pool.submit(() -> {
                try {
                    startLine.await();
                    registerTheWrongWay(studentId, sectionId);
                    ok.incrementAndGet();
                } catch (Exception ignored) {
                    // khong quan tam: dang do muc do sai, khong phai do do dung
                }
            });
        }

        startLine.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(180, TimeUnit.SECONDS)).isTrue();

        int counter = sectionRepository.findById(sectionId).orElseThrow().getRegisteredCount();
        long actualRows = registrationRepository.countBySectionId(sectionId);

        log.info("""

                ================ KHONG LOCK ===============
                 So yeu cau dong thoi : {}
                 Si so lop            : {}
                 Dang ky thanh cong   : {}
                 registered_count     : {}   <- he thong TUONG
                 So dong registrations: {}   <- si so THUC
                 Chenh lech           : {}
                ===========================================
                """, ATTEMPTS, CAPACITY, ok.get(), counter, actualRows, actualRows - counter);

        // Dau vet ro rang nhat cua lost update: counter dem THIEU so voi so dong thuc te,
        // vi nhieu transaction cung doc mot gia tri cu roi ghi de len nhau.
        assertThat(actualRows)
                .as("counter dem thieu hon so dong thuc te -- dung la lost update")
                .isGreaterThan(counter);

        // He qua nguy hiem: chk_capacity KHONG bat duoc, vi rang buoc do so counter voi
        // capacity, ma counter lai la con so da bi dem thieu. Rang buoc tang DB van thay
        // moi thu binh thuong trong khi du lieu da sai.
        assertThat(counter)
                .as("counter van nam duoi capacity nen chk_capacity khong he bao loi")
                .isLessThanOrEqualTo(CAPACITY);
    }

    /**
     * Ban SAI co tinh, chi ton tai trong test de lay so lieu "truoc khi co lock"
     * cho bang o muc 8. KHONG dat ham nay vao code chinh.
     *
     * Khac biet duy nhat so voi register(): dung findById (doc theo snapshot MVCC)
     * thay vi findByIdForUpdate (locking read). Chi mot chu khac nhau, va do la
     * toan bo su khac biet giua dung va sai.
     *
     * KHONG dat Thread.sleep o day. Da thu va ket qua nguoc voi mong doi: sleep giu
     * lock ghi tren dong lop hoc phan lau hon, khien phan lon thread chet vi
     * innodb_lock_wait_timeout thay vi lot qua duoc -- tuc la che mat chinh cai bug
     * can chi ra. Cua so tranh chap thuc su nam giua luc DOC va luc GHI, va no da
     * du rong san.
     */
    private void registerTheWrongWay(Long studentId, Long sectionId) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            ClassSection section = sectionRepository.findById(sectionId).orElseThrow();

            if (section.getRegisteredCount() >= section.getCapacity()) {
                throw new IllegalStateException("Lop day");
            }

            section.increaseRegisteredCount();
            User student = userRepository.getReferenceById(studentId);
            registrationRepository.save(Registration.of(student, section, LocalDateTime.now()));
        });
    }

    // =========================================================================
    // Du lieu cho test. Moi test tu tao du lieu rieng voi ma khong trung nhau,
    // nen khong can xoa du lieu seed cua Flyway.
    // =========================================================================

    private Long createSection(String tag) {
        Semester semester = semesterRepository.save(new Semester(
                "Ky test " + tag,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30),
                24,
                true));
        Course course = courseRepository.save(new Course("TEST" + tag, "Mon test " + tag, 3, null));
        ClassSection section = sectionRepository.save(new ClassSection(
                "SEC-" + tag, course, semester, "GV Test", "P101",
                (byte) 2, (byte) 1, (byte) 3, CAPACITY));
        return section.getId();
    }

    private List<Long> createStudents(String prefix, int count) {
        List<User> students = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            students.add(User.newStudent(
                    prefix + "_sv" + i,
                    "{noop}khong-dung-de-dang-nhap",
                    "Sinh vien " + prefix + " " + i,
                    prefix + "_code" + i,
                    null));
        }
        return userRepository.saveAll(students).stream().map(User::getId).toList();
    }
}
