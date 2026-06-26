package com.bankingproject.loanservice.client;

public class NotificationRequest {

    private Long userId;
    private String type;
    private String message;
    private String status;

    public NotificationRequest() {
    }

    public NotificationRequest(Long userId, String type, String message, String status) {
        this.userId = userId;
        this.type = type;
        this.message = message;
        this.status = status;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
