package com.ptit.courseregistration.exception;

/**
 * Lop cha cho moi loi NGHIEP VU -- loi ma nguoi dung can doc hieu duoc,
 * khong phai loi ky thuat.
 *
 * GlobalExceptionHandler bat dung lop nay va chuyen thanh flash message,
 * nen moi message truyen vao day deu se hien ra man hinh cho nguoi dung doc.
 * Khong bao gio nhet chi tiet ky thuat (ten bang, cau SQL, stack trace) vao message.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
