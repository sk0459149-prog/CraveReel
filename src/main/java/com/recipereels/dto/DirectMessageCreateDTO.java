package com.recipereels.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class DirectMessageCreateDTO {

    @NotNull(message = "Recipient ID is required")
    private Long recipientId;

    private String subject;

    @NotBlank(message = "Message content is required")
    private String content;

    public DirectMessageCreateDTO() {
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Long recipientId) {
        this.recipientId = recipientId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
