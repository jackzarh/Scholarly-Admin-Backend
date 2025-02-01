package org.niit_project.backend.dto;

import java.time.LocalDateTime;

public class FeedbackDTO {
    private String id;
    private String type;
    private String description;
    private LocalDateTime createdAt;

    public FeedbackDTO(String id, String type, String description, LocalDateTime createdAt) {
        this.id = id;
        this.type = type;
        this.description = description;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
