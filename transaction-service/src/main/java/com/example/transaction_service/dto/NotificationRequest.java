package com.example.transaction_service.dto;

public class NotificationRequest {

    private Long userId;
    private String type;
    private String message;
    private String status;
    private String phoneNumber;

    public NotificationRequest() {
    }

    public NotificationRequest(Long userId, String type, String message, String status) {
        this(userId, type, message, status, null);
    }

    public NotificationRequest(Long userId, String type, String message, String status, String phoneNumber) {
        this.userId = userId;
        this.type = type;
        this.message = message;
        this.status = status;
        this.phoneNumber = phoneNumber;
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

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
