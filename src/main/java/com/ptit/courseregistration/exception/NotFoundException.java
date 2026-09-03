package com.ptit.courseregistration.exception;

/** Khong tim thay ban ghi. Tra ve trang 404 chu khong phai flash message. */
public class NotFoundException extends BusinessException {

    public NotFoundException(String message) {
        super(message);
    }
}
