package com.ptit.courseregistration.exception;

/**
 * Tang validate 2 cua register() -- DOAN.md muc 7.1.
 *
 * Day la lop kiem tra o tang SERVICE de co thong bao than thien.
 * Luoi an toan thu hai la UNIQUE(student_id, section_id) o tang DB, chan truong hop
 * bam nut hai lan / F5 / mang lag ma service khong kip thay (muc 7.6).
 */
public class AlreadyRegisteredException extends BusinessException {

    public AlreadyRegisteredException(String message) {
        super(message);
    }
}
