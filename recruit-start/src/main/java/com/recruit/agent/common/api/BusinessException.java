package com.recruit.agent.common.api;

public class BusinessException extends RuntimeException {
    private final BusinessErrorCode code;

    public BusinessException(BusinessErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessErrorCode code() {
        return code;
    }
}
