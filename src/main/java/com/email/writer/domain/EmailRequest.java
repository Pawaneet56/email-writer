package com.email.writer.domain;


import lombok.Data;


public class EmailRequest {
    private String emailContent;
    private String tone;

    public String getEmailContent() {
        return this.emailContent;
    }

    public String getTone() {
        return this.tone;
    }
}
