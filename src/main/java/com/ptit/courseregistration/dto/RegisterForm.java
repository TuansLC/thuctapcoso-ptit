package com.ptit.courseregistration.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Form dang ky tai khoan sinh vien.
 *
 * Day la MAN HINH NHAP sinh ra du lieu "tai khoan dang nhap" trong bang truy vet
 * o DOAN.md muc 6. Vi vay khong seed tai khoan sinh vien bang Flyway --
 * moi tai khoan phai di qua day.
 *
 * Dung class thay vi record vi Thymeleaf can setter de binding lai gia tri
 * khi form co loi validate.
 */
public class RegisterForm {

    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(min = 3, max = 50, message = "Tên đăng nhập từ 3 đến 50 ký tự")
    @Pattern(regexp = "^[a-zA-Z0-9_.]+$",
            message = "Tên đăng nhập chỉ gồm chữ, số, dấu gạch dưới và dấu chấm")
    private String username;

    /**
     * Gioi han 72 ky tu khong phai con so tuy y: BCrypt chi bam 72 byte dau tien
     * va am tham bo phan con lai. Neu cho nhap dai hon, hai mat khau khac nhau
     * tu ky tu 73 tro di se dang nhap duoc lan nhau.
     */
    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, max = 72, message = "Mật khẩu từ 6 đến 72 ký tự")
    private String password;

    @NotBlank(message = "Vui lòng nhập lại mật khẩu")
    private String confirmPassword;

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
    private String fullName;

    @NotBlank(message = "Mã sinh viên không được để trống")
    @Size(max = 20, message = "Mã sinh viên tối đa 20 ký tự")
    private String code;

    @Email(message = "Email không đúng định dạng")
    @Size(max = 100, message = "Email tối đa 100 ký tự")
    private String email;

    public boolean isPasswordConfirmed() {
        return password != null && password.equals(confirmPassword);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
