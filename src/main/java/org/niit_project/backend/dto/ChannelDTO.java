package org.niit_project.backend.dto;

import lombok.Data;
import org.niit_project.backend.entities.ChannelType;
import org.niit_project.backend.entities.Chat;
import org.niit_project.backend.entities.Student;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "channels")
public class ChannelDTO {

    @Id
    private String id;

    private String channelName;

    private String channelProfile;

    private String creator;

    private LocalDateTime createdAt;

    private List<Student> members;

    private ChannelType channelType;

    private Chat latestMessage;

    private Integer unreadMessages;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Chat getLatestMessage() {
        return latestMessage;
    }

    public void setLatestMessage(Chat latestMessage) {
        this.latestMessage = latestMessage;
    }

    public ChannelType getChannelType() {
        return channelType;
    }

    public void setChannelType(ChannelType channelType) {
        this.channelType = channelType;
    }

    public List<Student> getMembers() {
        return members;
    }

    public void setMembers(List<Student> members) {
        this.members = members;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getChannelProfile() {
        return channelProfile;
    }

    public void setChannelProfile(String channelProfile) {
        this.channelProfile = channelProfile;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public Integer getUnreadMessages() {
        return unreadMessages;
    }

    public void setUnreadMessages(Integer unreadMessages) {
        this.unreadMessages = unreadMessages;
    }
}
