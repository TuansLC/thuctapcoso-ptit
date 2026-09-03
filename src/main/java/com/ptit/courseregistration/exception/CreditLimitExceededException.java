package com.ptit.courseregistration.exception;

/** Tang validate 5: vuot gioi han tin chi cua hoc ky -- DOAN.md muc 7.2. */
public class CreditLimitExceededException extends BusinessException {

    public CreditLimitExceededException(String message) {
        super(message);
    }
}
