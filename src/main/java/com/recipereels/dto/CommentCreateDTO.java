package com.recipereels.dto;

import jakarta.validation.constraints.NotBlank;

public class CommentCreateDTO {

    @NotBlank(message = "Comment text cannot be empty")
    private String content;

    private Long parentId;

    public CommentCreateDTO() {
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }
}
