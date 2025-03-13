package org.niit_project.backend.entities;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "announcements")
public class Announcement {
    @Id
    private String id;

    private String announcementTitle, announcementDescription, announcementPhoto;

    private List<Object> audience;
    private LocalDateTime createdTime;

    private String color;

    public Announcement(){}

    public Announcement(String announcementTitle, String announcementDescription, String announcementPhoto, List<Object> audience, LocalDateTime createdTime) {
        this.announcementTitle = announcementTitle;
        this.announcementDescription = announcementDescription;
        this.announcementPhoto = announcementPhoto;
        this.audience = audience;
        this.createdTime = createdTime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAnnouncementTitle() {
        return announcementTitle;
    }

    public void setAnnouncementTitle(String announcementTitle) {
        this.announcementTitle = announcementTitle;
    }

    public String getAnnouncementDescription() {
        return announcementDescription;
    }

    public void setAnnouncementDescription(String announcementDescription) {
        this.announcementDescription = announcementDescription;
    }

    public String getAnnouncementPhoto() {
        return announcementPhoto;
    }

    public void setAnnouncementPhoto(String announcementPhoto) {
        this.announcementPhoto = announcementPhoto;
    }

    public List<Object> getAudience() {
        return audience;
    }

    public void setAudience(List<Object> audience) {
        this.audience = audience;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
