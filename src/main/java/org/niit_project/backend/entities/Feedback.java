package org.niit_project.backend.entities;

import lombok.Data;
import org.niit_project.backend.enums.FeedbackType;
import org.niit_project.backend.models.User;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "feedbacks")
public class Feedback {

    @Id
    private String id;

    private FeedbackType type;

    private String description;

    private String evidenceUrl;

    private Object perpetrator;
    //change to object

    private Object reporter;
    //change to object

    private Boolean anonymous;

    private LocalDateTime createdAt;

    public Feedback(String id, FeedbackType type, String description, String evidenceUrl, User perpetrator, Boolean isAnonymous, LocalDateTime createdAt) {
        this.id = id;
        this.type = type;
        this.description = description;
        this.evidenceUrl = evidenceUrl;
        this.perpetrator = perpetrator;
        this.anonymous = isAnonymous;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public FeedbackType getType() {
        return type;
    }

    public void setType(FeedbackType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEvidenceUrl() {
        return evidenceUrl;
    }

    public void setEvidenceUrl(String evidenceUrl) {
        this.evidenceUrl = evidenceUrl;
    }

    public Object getPerpetrator() {
        return perpetrator;
    }

    public void setPerpetrator(Object perpetrator) {
        this.perpetrator = perpetrator;
    }

    public Boolean getAnonymous() {
        return anonymous;
    }

    public Object getReporter() {
        return reporter;
    }

    public void setReporter(Object reporter) {
        this.reporter = reporter;
    }

    public void setAnonymous(Boolean anonymous) {
        this.anonymous = anonymous;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setImageUrl(String secureUrl) {
        this.evidenceUrl = secureUrl;
    }

}
