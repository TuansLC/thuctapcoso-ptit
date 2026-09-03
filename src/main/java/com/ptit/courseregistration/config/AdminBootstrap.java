package com.ptit.courseregistration.config;

import com.ptit.courseregistration.domain.Role;
import com.ptit.courseregistration.domain.User;
import com.ptit.courseregistration.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Tao tai khoan ADMIN dau tien luc khoi dong, neu trong bang users chua co ADMIN nao.
 *
 * VI SAO KHONG SEED ADMIN BANG FLYWAY (lech nhe so voi DOAN.md muc 6):
 *
 *  1. Hash BCrypt phai do chinh PasswordEncoder cua ung dung sinh ra. Dan mot chuoi
 *     hash cheo vao file .sql la dat cuoc rang thuat toan va tham so khong bao gio doi.
 *  2. Quan trong hon: migration nam trong git. Nhet hash cua mot mat khau da biet vao
 *     do la dua mat khau dung chung vao lich su repository VINH VIEN -- xoa o commit sau
 *     cung khong go duoc khoi lich su.
 *
 * Moi loai du lieu khac (mon hoc, hoc ky, lop hoc phan) van seed bang Flyway nhu muc 6,
 * vi chung khong chua bi mat va deu co man hinh nhap tuong ung.
 *
 * Cach dat mat khau admin co dinh cho may ca nhan: tao file
 * src/main/resources/application-local.yml (da nam trong .gitignore) voi noi dung
 *
 *   app:
 *     admin:
 *       password: matkhaucuaban
 *
 * roi chay voi --spring.profiles.active=local. Neu khong dat, ung dung sinh mat khau
 * ngau nhien va in ra log MOT LAN duy nhat luc tao.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final String adminUsername;
    private final String adminFullName;
    private final String configuredPassword;

    public AdminBootstrap(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          @Value("${app.admin.username:admin}") String adminUsername,
                          @Value("${app.admin.full-name:Quan tri he thong}") String adminFullName,
                          @Value("${app.admin.password:}") String configuredPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminFullName = adminFullName;
        this.configuredPassword = configuredPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            log.info("Da co tai khoan ADMIN, khong tao moi.");
            return;
        }

        boolean generated = configuredPassword == null || configuredPassword.isBlank();
        String rawPassword = generated ? randomPassword() : configuredPassword;

        User admin = User.newAdmin(adminUsername, passwordEncoder.encode(rawPassword), adminFullName);
        userRepository.save(admin);

        if (generated) {
            log.warn("""

                    ================================================================
                     DA TAO TAI KHOAN ADMIN DAU TIEN
                       username: {}
                       password: {}
                     Mat khau nay duoc sinh ngau nhien va CHI HIEN THI MOT LAN.
                     Ghi lai ngay. Muon dat mat khau co dinh, xem huong dan trong
                     AdminBootstrap.java (dung application-local.yml).
                    ================================================================
                    """, adminUsername, rawPassword);
        } else {
            log.warn("Da tao tai khoan ADMIN '{}' voi mat khau lay tu cau hinh app.admin.password. "
                    + "Bao dam file cau hinh do KHONG duoc commit vao git.", adminUsername);
        }
    }

    /** 24 byte tu SecureRandom, ma hoa base64 khong padding -> 32 ky tu URL-safe. */
    private String randomPassword() {
        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
