package org.niit_project.backend.entities;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "direct messages")
public class DirectMessage {
    @Id
    private String id;

    private String name,profile,color;

    private List<String> recipients;

    @Transient
    private Chat latestMessage;

    private LocalDateTime time;

    /**
     * We're making the community {@code transient} because, we don't
     * want it to be stored in the database. Rather, we want it to be
     * available in websockets or response bodies if and only if the {@code DirectMessage.fromCommunity()} constructor was called.
     */
    @Transient
    private Community community;

    @Transient
    private Integer unreadMessages;

    /**
     * If you're creating a DM from a Community's channel.
     * WARNING: If the DM needs to have it's {@code latestMessage}, it has to be assigned manually by a setter.
     * @param channel the channel of a community you're creating the DM from
     */
    public static DirectMessage fromChannel(Channel channel){
        var community = channel.getCommunity();
        var dm = new DirectMessage();
        dm.setId(channel.getId());
        dm.setName(channel.getChannelName());
        dm.setProfile(channel.getChannelProfile());
        dm.setColor(channel.getColor());
        dm.setTime(channel.getLatestMessage() == null? channel.getCreatedAt(): channel.getLatestMessage().getTimestamp());
        dm.setUnreadMessages(channel.getUnreadMessages() == null? 0: channel.getUnreadMessages());
        dm.setCommunity(community);

        // This way we're checking to always return a List of the recipient's ids whether,
        // the channel has it as a List of String or Members.
        dm.setRecipients(channel.getMembers().stream().map(member -> member instanceof Member? ((Member)member).getId(): member.toString()).toList());
        return dm;
    }

    /**
     * If you're creating a DM from a User (Whether student or Admin).
     *
     * @param member the student/admin you're creating a DM from
     * @param yourId the id of the student/admin that is initiating the DM
     */
    public static DirectMessage fromMember(Member member, String yourId){
        var dm = new DirectMessage();
        dm.setName(member.getFirstName() + " " + member.getLastName());
        dm.setProfile(member.getProfile());
        dm.setColor(member.getColor().name());
        dm.setRecipients(List.of(yourId, member.getId()));

        return dm;

    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<String> recipients) {
        this.recipients = recipients;
    }

    public Community getCommunity() {
        return community;
    }

    public void setCommunity(Community community) {
        this.community = community;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Chat getLatestMessage() {
        return latestMessage;
    }

    public void setLatestMessage(Chat latestMessage) {
        this.latestMessage = latestMessage;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public Integer getUnreadMessages() {
        return unreadMessages;
    }

    public void setUnreadMessages(Integer unreadMessages) {
        this.unreadMessages = unreadMessages;
    }
}
