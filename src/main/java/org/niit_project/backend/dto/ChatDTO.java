package org.niit_project.backend.dto;

import lombok.Data;
import org.niit_project.backend.entities.Admin;
import org.niit_project.backend.entities.AttachmentType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "chats")
public class ChatDTO {
    @Id
    private String id;

    private String senderId;
    private String senderProfile;

    private String message;
    private String attachment;

    private AttachmentType attachmentType;

    private LocalDateTime timestamp;
    private List<Admin> readReceipt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Admin> getReadReceipt() {
        return readReceipt;
    }

    public void setReadReceipt(List<Admin> readReceipt) {
        this.readReceipt = readReceipt;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public AttachmentType getAttachmentType() {
        return attachmentType;
    }

    public void setAttachmentType(AttachmentType attachmentType) {
        this.attachmentType = attachmentType;
    }

    public String getAttachment() {
        return attachment;
    }

    public void setAttachment(String attachment) {
        this.attachment = attachment;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSenderProfile() {
        return senderProfile;
    }

    public void setSenderProfile(String senderProfile) {
        this.senderProfile = senderProfile;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }
}
