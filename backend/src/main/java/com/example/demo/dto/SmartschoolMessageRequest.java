package com.example.demo.dto;

public class SmartschoolMessageRequest {

    private String platformUrl;
    private String subject;
    private String body;
    private String recipientSub;

    public SmartschoolMessageRequest() {
    }

    public String getPlatformUrl() {
        return platformUrl;
    }

    public void setPlatformUrl(String platformUrl) {
        this.platformUrl = platformUrl;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getRecipientSub() {
        return recipientSub;
    }

    public void setRecipientSub(String recipientSub) {
        this.recipientSub = recipientSub;
    }
}