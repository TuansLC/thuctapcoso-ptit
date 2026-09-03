package com.ptit.courseregistration.service;

import com.ptit.courseregistration.domain.User;
import com.ptit.courseregistration.dto.RegisterForm;
import com.ptit.courseregistration.exception.BusinessException;
import com.ptit.courseregistration.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dang ky tai khoan sinh vien va nap thong tin dang nhap cho Spring Security.
 *
 * Nghiep vu nam o day, controller chi dieu phoi (DOAN.md muc 12).
 */
@Service
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Không tìm thấy tài khoản: " + username));
        return new AppUserDetails(user);
    }

    /**
     * Tao tai khoan sinh vien moi.
     *
     * Kiem tra trung o tang service de co thong bao than thien, con UNIQUE tren
     * users.username va users.code o tang DB la luoi an toan cho truong hop hai
     * request gui gan nhu cung luc (cung tinh than muc 7.6).
     */
    @Transactional
    public User registerStudent(RegisterForm form) {
        if (!form.isPasswordConfirmed()) {
            throw new BusinessException("Mật khẩu nhập lại không khớp");
        }
        if (userRepository.existsByUsername(form.getUsername())) {
            throw new BusinessException("Tên đăng nhập đã tồn tại");
        }
        if (userRepository.existsByCode(form.getCode())) {
            throw new BusinessException("Mã sinh viên đã tồn tại");
        }

        String email = (form.getEmail() == null || form.getEmail().isBlank())
                ? null
                : form.getEmail().trim();

        User student = User.newStudent(
                form.getUsername().trim(),
                passwordEncoder.encode(form.getPassword()),
                form.getFullName().trim(),
                form.getCode().trim(),
                email);

        return userRepository.save(student);
    }
}
