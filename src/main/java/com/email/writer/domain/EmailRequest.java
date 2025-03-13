package com.email.writer.domain;

import lombok.Data;

@Data
public class EmailRequest {
    private String emailContent;
    private String tone;
}
