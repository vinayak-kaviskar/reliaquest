package com.reliaquest.api.exception;

import java.util.List;

public class RequestValidationException extends RuntimeException {

    private final List<String> validationErrors;

    public RequestValidationException(String message, List<String> validationErrors) {
        super(message);
        this.validationErrors = validationErrors;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }
}
