package org.niit_project.backend.entities;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "channels")
public class Channel {

    @Id
    private String id;

    private String channelName;

    private String channelDescription;

    private String channelProfile;

    private Object creator;

    private LocalDateTime createdAt;

    private List<Object> members;

    private ChannelType channelType;

    private Chat latestMessage;

    @Transient
    private Integer unreadMessages;

    public Chat getLatestMessage() {
        return latestMessage;
    }

    public void setLatestMessage(Chat latestMessage) {
        this.latestMessage = latestMessage;
    }

    public Integer getUnreadMessages() {
        return unreadMessages == null? 0: unreadMessages;
    }

    public void setUnreadMessages(Integer unreadMessages) {
        this.unreadMessages = unreadMessages;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getChannelDescription() {
        return channelDescription;
    }

    public void setChannelDescription(String channelDescription) {
        this.channelDescription = channelDescription;
    }

    public List<Object> getMembers() {
        return members;
    }

    public void setMembers(List<Object> members) {
        this.members = members;
    }

    public ChannelType getChannelType() {
        return channelType;
    }

    public void setChannelType(ChannelType channelType) {
        this.channelType = channelType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Object getCreator() {
        return creator;
    }

    public void setCreator(Object creator) {
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
}
