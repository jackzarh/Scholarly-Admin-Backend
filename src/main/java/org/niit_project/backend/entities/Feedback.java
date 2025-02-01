package org.niit_project.backend.entities;

import lombok.Data;
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

    private String reporterName;
    //change to object

    private Boolean isAnonymous;

    private LocalDateTime createdAt;

    public Feedback(String id, FeedbackType type, String description, String evidenceUrl, Member perpetrator, String reporterName, Boolean isAnonymous, LocalDateTime createdAt) {
        this.id = id;
        this.type = type;
        this.description = description;
        this.evidenceUrl = evidenceUrl;
        this.perpetrator = perpetrator;
        this.reporterName = reporterName;
        this.isAnonymous = isAnonymous;
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

    public String getReporterName() {
        return reporterName;
    }

    public void setReporterName(String reporterName) {
        this.reporterName = reporterName;
    }

    public Boolean getAnonymous() {
        return isAnonymous;
    }

    public void setAnonymous(Boolean anonymous) {
        isAnonymous = anonymous;
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
