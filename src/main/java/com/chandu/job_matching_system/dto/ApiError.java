package com.chandu.job_matching_system.dto;

import java.time.Instant;
import java.util.Map;

public class ApiError {

    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private Map<String, String> validationErrors;

    public ApiError() {
        this.timestamp = Instant.now();
    }

    public ApiError(
            int status,
            String error,
            String message,
            String path,
            Map<String, String> validationErrors) {

        this.timestamp = Instant.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.validationErrors = validationErrors;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getValidationErrors() {
        return validationErrors;
    }
}