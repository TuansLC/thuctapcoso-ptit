package com.ptit.courseregistration.exception;

/** Tang validate 1 cua register(), va cung dung cho drop() -- DOAN.md muc 7.1. */
public class RegistrationClosedException extends BusinessException {

    public RegistrationClosedException(String message) {
        super(message);
    }
}
