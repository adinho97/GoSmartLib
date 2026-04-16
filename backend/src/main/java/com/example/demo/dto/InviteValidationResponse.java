package com.example.demo.dto;

public class InviteValidationResponse {

    private boolean valid;
    private String schoolId;
    private String message;

    public InviteValidationResponse() {
    }

    public InviteValidationResponse(boolean valid, String schoolId) {
        this.valid = valid;
        this.schoolId = schoolId;
    }

    public InviteValidationResponse(boolean valid, String message, String schoolId) {
        this.valid = valid;
        this.message = message;
        this.schoolId = schoolId;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(String schoolId) {
        this.schoolId = schoolId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
