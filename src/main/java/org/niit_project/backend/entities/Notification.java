package org.niit_project.backend.entities;

import lombok.Data;
import org.niit_project.backend.enums.NotificationCategory;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "notifications")
@Data
public class Notification {

    @Id
    private String id;

    private String title, content, target, image;

    private List<String> recipients, readReceipt;

    @Transient
    private boolean read;

    private LocalDateTime timestamp;

    private NotificationCategory category;


    public Notification(){}

    public Notification(String id, NotificationCategory category, LocalDateTime timestamp, String target, String content, String title) {
        this.id = id;
        this.category = category;
        this.timestamp = timestamp;
        this.target = target;
        this.content = content;
        this.title = title;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getReadReceipt() {
        return readReceipt;
    }

    public void setReadReceipt(List<String> readReceipt) {
        this.readReceipt = readReceipt;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getRecipients() {
        return recipients == null? List.of(): recipients;
    }

    public void setRecipients(List<String> recipients) {
        this.recipients = recipients;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public NotificationCategory getCategory() {
        return category == null? NotificationCategory.other: category;
    }

    public void setCategory(NotificationCategory category) {
        this.category = category;
    }


    public com.google.firebase.messaging.Notification toFirebaseNotification(){
        return com.google.firebase.messaging.Notification.builder()
                .setTitle(title)
                .setBody(content)
                .setImage(image)
                .build();
    }
}
