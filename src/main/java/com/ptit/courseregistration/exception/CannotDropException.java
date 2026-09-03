package com.ptit.courseregistration.exception;

/**
 * Khong rut duoc mon: da co diem, hoac cong dang ky da dong (DOAN.md muc 7.2).
 */
public class CannotDropException extends BusinessException {

    public CannotDropException(String message) {
        super(message);
    }
}
