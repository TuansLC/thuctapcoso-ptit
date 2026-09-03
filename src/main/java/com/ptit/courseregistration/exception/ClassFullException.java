package com.ptit.courseregistration.exception;

/**
 * Lop da du si so.
 *
 * Nem sau khi da lock dong lop hoc phan bang findByIdForUpdate, nen so
 * registered_count doc duoc la so MOI NHAT DA COMMIT, khong phai snapshot.
 * Do la ly do khong xay ra lost update (DOAN.md muc 7.1 va 7.4).
 *
 * KHONG co hang doi cho lop: het cho thi bao loi (muc 14).
 */
public class ClassFullException extends BusinessException {

    public ClassFullException(String message) {
        super(message);
    }
}
