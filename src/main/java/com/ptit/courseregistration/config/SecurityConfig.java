package com.ptit.courseregistration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 6 -- form login, phan quyen theo URL.
 *
 * CO Y khong inject AuthService vao day. Neu inject, se thanh vong phu thuoc:
 * SecurityConfig -> AuthService -> PasswordEncoder (bean do chinh SecurityConfig tao).
 * Spring Boot tu dong ghep UserDetailsService voi PasswordEncoder khi trong context
 * co dung mot bean moi loai, nen khong can khai bao AuthenticationProvider tay.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * BCrypt theo quy uoc DOAN.md muc 12.
     * BCrypt tu sinh salt rieng cho tung mat khau va nhung salt vao chuoi hash,
     * nen khong can cot salt rieng trong bang users.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/login",
                                "/register",
                                "/error",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/webjars/**"
                        ).permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/student/**").hasRole("STUDENT")
                        // Mac dinh dong: URL nao chua khai bao thi phai dang nhap.
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        // true = luon ve "/" sau khi dang nhap, HomeController dieu huong theo vai.
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                );

        // CSRF de BAT (mac dinh cua Spring Security). Ung dung nay render server-side
        // bang Thymeleaf va khong co REST API (muc 14), nen moi thao tac ghi deu di qua
        // form POST -- Thymeleaf tu chen hidden input _csrf khi form dung th:action.
        // Neu tat CSRF thi nut dang ky / rut mon co the bi kich hoat tu trang ngoai.

        return http.build();
    }
}
