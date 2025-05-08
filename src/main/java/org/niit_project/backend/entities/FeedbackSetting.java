package org.niit_project.backend.entities;

import lombok.Data;
import org.niit_project.backend.enums.FeedbackRateLimit;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * FeedbackSetting stores the Feedback Rate Limit configuration per Faculty.
 */
@Data
@Document(collection = "feedback_settings")
public class FeedbackSetting {

    @Id
    private String id;

    private String facultyId;

    private FeedbackRateLimit rateLimit;

    public FeedbackSetting() {}

    public FeedbackSetting(String facultyId, FeedbackRateLimit rateLimit) {
        this.facultyId = facultyId;
        this.rateLimit = rateLimit;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFacultyId() {
        return facultyId;
    }

    public void setFacultyId(String facultyId) {
        this.facultyId = facultyId;
    }

    public FeedbackRateLimit getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(FeedbackRateLimit rateLimit) {
        this.rateLimit = rateLimit;
    }
}
