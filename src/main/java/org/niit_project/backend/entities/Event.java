package org.niit_project.backend.entities;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "events")
public class Event {
    @Id
    private String id;
    private String eventTitle;
    private String eventDescription;
    private List<Object> audience;
    private String keyInformation;
    private LocalDateTime createdTime;
    private LocalDateTime designatedTime;
    private String eventPhoto;

    public Event(String id, String eventTitle, String eventDescription, List<Object> audience, String keyInformation, LocalDateTime createdTime, LocalDateTime designatedTime, String eventPhoto) {
        this.id = id;
        this.eventTitle = eventTitle;
        this.eventDescription = eventDescription;
        this.audience = audience;
        this.keyInformation = keyInformation;
        this.createdTime = createdTime;
        this.designatedTime = designatedTime;
        this.eventPhoto = eventPhoto;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEventTitle() {
        return eventTitle;
    }

    public void setEventTitle(String eventTitle) {
        this.eventTitle = eventTitle;
    }

    public String getEventDescription() {
        return eventDescription;
    }

    public void setEventDescription(String eventDescription) {
        this.eventDescription = eventDescription;
    }

    public List<Object> getAudience() {
        return audience;
    }

    public void setAudience(List<Object> audience) {
        this.audience = audience;
    }

    public String getKeyInformation() {
        return keyInformation;
    }

    public void setKeyInformation(String keyInformation) {
        this.keyInformation = keyInformation;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public LocalDateTime getDesignatedTime() {
        return designatedTime;
    }

    public void setDesignatedTime(LocalDateTime designatedTime) {
        this.designatedTime = designatedTime;
    }

    public String getEventPhoto() {
        return eventPhoto;
    }

    public void setEventPhoto(String eventPhoto) {
        this.eventPhoto = eventPhoto;
    }

    public Event() {
        this.createdTime = LocalDateTime.now();
    }

    public Event(String eventTitle, String eventDescription, List<Object> audience, String keyInformation, LocalDateTime designatedTime, String eventPhoto) {
        this.eventTitle = eventTitle;
        this.eventDescription = eventDescription;
        this.audience = audience;
        this.keyInformation = keyInformation;
        this.createdTime = LocalDateTime.now();
        this.designatedTime = designatedTime;
        this.eventPhoto = eventPhoto;
    }
}
