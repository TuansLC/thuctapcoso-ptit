package com.ptit.courseregistration.exception;

/** Tang validate 3: chua dat mon tien quyet (score >= 4.0) -- DOAN.md muc 7.2. */
public class PrerequisiteNotMetException extends BusinessException {

    public PrerequisiteNotMetException(String message) {
        super(message);
    }
}
