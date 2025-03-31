package org.niit_project.backend.entities;

import lombok.Data;
import org.niit_project.backend.enums.Colors;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "communities")
public class Community {

    @Id
    private String id;

    private String communityName;
    private String communityDescription;
    private String communityProfile;

    private Object creator;

    private LocalDateTime createdAt;

    private String color;
    private List<Object> members;

    @Transient
    private List<Object> channels;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCommunityName() {
        return communityName;
    }

    public void setCommunityName(String communityName) {
        this.communityName = communityName;
    }

    public String getCommunityDescription() {
        return communityDescription;
    }

    public void setCommunityDescription(String communityDescription) {
        this.communityDescription = communityDescription;
    }

    public String getCommunityProfile() {
        return communityProfile;
    }

    public void setCommunityProfile(String communityProfile) {
        this.communityProfile = communityProfile;
    }

    public String getColor() {
        return color;
    }

    public void setColor(Colors color) {
        this.color = color.name();
    }

    public List<Object> getMembers() {
        return members;
    }

    public void setMembers(List<Object> members) {
        this.members = members;
    }

    public List<Object> getChannels() {
        return channels == null? List.of() : channels;
    }

    public void setChannels(List<Object> channels) {
        this.channels = channels;
    }

    public Object getCreator() {
        return creator;
    }

    public void setCreator(Object creator) {
        this.creator = creator;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }


    /**
     * To get the latest channel based on time.
     * Call this method only when you're {@code 10 billion percent} sure that the channels are not stored as {@code String} Objects but {@code Channel} Objects.
     * @return Channel
     * @throws ClassCastException
     */
    @Transient
    public Channel getLatestChannel(){
        var channelsList = getChannels() == null? List.of(): getChannels();
        var channels = channelsList.stream().map(o -> (Channel)o).sorted((o1, o2) -> (o2.getLatestMessage() == null? o2.getCreatedAt() : o2.getLatestMessage().getTimestamp()).compareTo(o1.getLatestMessage() == null? o1.getCreatedAt(): o1.getLatestMessage().getTimestamp())).toList();

        if(channels.isEmpty()){
            return null;
        }
        return channels.get(0);
    }

    /**
     * To get the latest channel sort time based.
     * Call this method only when you're {@code 10 billion percent} sure that the channels are not stored as {@code String} Objects but {@code Channel} Objects.
     * @return Channel
     * @throws ClassCastException
     */
    @Transient
    public LocalDateTime getLatestSortTime(){
        var latestChannel = getLatestChannel();

        if(latestChannel == null){
            return null;
        }
        return latestChannel.getLatestMessage() == null? latestChannel.getCreatedAt() : latestChannel.getLatestMessage().getTimestamp();
    }


}
