package org.niit_project.backend.service;

import org.niit_project.backend.dto.ApiResponse;
import org.niit_project.backend.entities.*;
import org.niit_project.backend.repository.ChannelRepository;
import org.niit_project.backend.repository.ChatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ChannelService {

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserService userService;

    @Autowired
    private SideChatService chatService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MongoTemplate  mongoTemplate;

    public Optional<List<Channel>> getAllChannels(){
        try{
            return Optional.of(channelRepository.findAll());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public List<Channel> getAdminChannels(String adminId){

        var matchAggregation = Aggregation.match(Criteria.where("members").in(adminId));
        var sortAggregation = Aggregation.sort(Sort.by(Sort.Direction.DESC, "createdAt"));
        var aggregation = Aggregation.newAggregation(matchAggregation, sortAggregation);

        var results = mongoTemplate.aggregate(aggregation, "channels", Channel.class).getMappedResults();
        var formed = results.stream().peek(channel -> {
            var membersAggregation = Aggregation.newAggregation(Aggregation.match(Criteria.where("_id").in(channel.getMembers())));
            var studentMembers = mongoTemplate.aggregate(membersAggregation, "users", Student.class).getMappedResults();
            var adminMembers = mongoTemplate.aggregate(membersAggregation, "admins", Admin.class).getMappedResults();

            //Then collate all members
            var allMembers = new ArrayList<>(studentMembers.stream().map(Member::fromStudent).toList());
            allMembers.addAll(adminMembers.stream().map(Member::fromAdmin).toList());
            channel.setMembers(Arrays.asList(allMembers.toArray()));
            var creator = allMembers.stream().filter(member -> member.getId().equals(channel.getCreator())).findFirst().orElse(allMembers.isEmpty()? null: allMembers.get(0));
            channel.setCreator(creator);

            /// We get the last message of the chat and if it's empty we set it to null
            /// And also set the unread messages of the chat.
            channel.setLatestMessage(chatService.getLastChat(channel.getId()).orElse(null));
            channel.setUnreadMessages(chatService.getUnseenChatsCount(channel.getId(), adminId));
        }).toList();
        var sorted = formed.stream().sorted((channel1, channel2) -> (channel2.getLatestMessage() != null ? channel2.getLatestMessage().getTimestamp() : channel2.getCreatedAt()).compareTo((channel1.getLatestMessage() != null? channel1.getLatestMessage().getTimestamp(): channel1.getCreatedAt()))).toList();

        return sorted;
    }

    // WARNING: Should be used if robust details concerning channel is needed !!
    public Optional<Channel> getOneChannel(String id) {
        var channelExists = channelRepository.existsById(id);

        if(!channelExists){
            return Optional.empty();
        }


        var channel = channelRepository.findById(id).get();
        var members = channel.getMembers();

        var gottenCreator = adminService.getAdmin(channel.getCreator().toString());

        if(gottenCreator.isEmpty()){
            var student = userService.getCompactStudent(channel.getCreator().toString()).get();
            channel.setCreator(Member.fromStudent(student));
        }
        else{
            channel.setCreator(Member.fromAdmin(gottenCreator.get()));
        }

        // Aggregation stage to match the members base on their id
        var membersMatch = Aggregation.match(Criteria.where("_id").in(members));
        var aggregations = Aggregation.newAggregation(membersMatch);
        var studentMembers = mongoTemplate.aggregate(aggregations, "users", Student.class).getMappedResults();
        var adminMembers = mongoTemplate.aggregate(aggregations, "admins", Admin.class).getMappedResults();

        //Then collate all members
        var allMembers = new ArrayList<>(studentMembers.stream().map(Member::fromStudent).toList());
        allMembers.addAll(adminMembers.stream().map(Member::fromAdmin).toList());

        channel.setMembers(allMembers.stream().map(member -> (Object) member).toList());

        return Optional.of(channel);

    }

    public Optional<Channel> getCompactChannel(String id) {
        var channelExists = channelRepository.existsById(id);

        if(!channelExists){
            return Optional.empty();
        }

        var channel = channelRepository.findById(id).get();
        return Optional.of(channel);

    }

    public Member addMember(String userId, String channelId) throws Exception{
        // We first have to know whether such person is an
        // Admin or a member

        var admin = adminService.getAdmin(userId);
        var student = userService.getCompactStudent(userId);

        // We're using getCompactChannel because it has the id as a String
        var channel = getCompactChannel(channelId);

        if(admin.isEmpty() && student.isEmpty()){
            throw new Exception("User or Admin doesn't exist");
        }

        if(channel.isEmpty()){
            throw new Exception("Channel doesn't exist");
        }

        var gottenChannel = channel.get();

        // We make sure the admin/student isn't already a member of the channel
        if(gottenChannel.getMembers().contains(userId)){
            throw new Exception("This user/admin is already a member of this channel");
        }

        var member = admin.map(Member::fromAdmin).orElseGet(() -> Member.fromStudent(student.get()));

        /// We then update the members/add the member
        /// And save it in the repository
        var members = gottenChannel.getMembers().stream().map(Object::toString).toList();
        var newMembers = new ArrayList<Object>(members);
        newMembers.add(member.getId());
        gottenChannel.setMembers(newMembers);
        var savedChannel = channelRepository.save(gottenChannel);

        // If the user/admin was added. We want to send a chat indicating
        // That the added was added
        var chat = new Chat();
        chat.setChannelId(channelId);
        chat.setSenderProfile(member.getProfile());
        chat.setSenderId(userId);
        chat.setTimestamp(LocalDateTime.now());
        chat.setMessageType(MessageType.member);
        chat.setMessage(member.getFirstName() + " joined channel");
        chat.setReadReceipt(List.of());
        var createdChat = chatRepository.save(chat);

        var notification = new Notification();
        notification.setCategory(NotificationCategory.channel);
        notification.setTitle("Joined Channel");
        notification.setContent("You were added to channel '" + savedChannel.getChannelName() + "'");
        notification.setTarget(savedChannel.getId());
        notification.setUserId(userId);
        notificationService.sendNotification(notification);

        /// To update the websocket that a new chat has been added
        var chatsResponse = new ApiResponse("User was added", createdChat);
        messagingTemplate.convertAndSend("/chats/" + channelId, chatsResponse);

        // We're going to broadcast the information to all the members
        var membersId = savedChannel.getMembers().stream().map(Object::toString).toList();
        var fetchedChannel = getOneChannel(channelId).get();
        for(String memberId : membersId){
            fetchedChannel.setUnreadMessages(chatService.getUnseenChatsCount(channelId, memberId));
            fetchedChannel.setLatestMessage(createdChat);
            var updatedChannelResponse = new ApiResponse("Member Added", fetchedChannel);


            messagingTemplate.convertAndSend("/channels/" + memberId, updatedChannelResponse);
        }

        return member;
    }

    public boolean removeMember(String userId, String channelId) throws Exception{
        // We first have to know whether such person is an
        // Admin or a member

        var admin = adminService.getAdmin(userId);
        var student = userService.getCompactStudent(userId);

        // We're using getCompactChannel because it has the id as a String
        var channel = getCompactChannel(channelId);

        if(admin.isEmpty() && student.isEmpty()){
            throw new Exception("User or Admin doesn't exist");
        }

        if(channel.isEmpty()){
            throw new Exception("Channel doesn't exist");
        }

        var gottenChannel = channel.get();

        // We make sure the admin/student is a member of the channel
        if(!gottenChannel.getMembers().contains(userId)){
            throw new Exception("This user/admin is not a member of this channel");
        }

        // We remove the member
        var members = new ArrayList<Object>(gottenChannel.getMembers().stream().map(Object::toString).toList());
        members.remove(userId);

        // We save to the database
        gottenChannel.setMembers(members);
        var savedChannel = channelRepository.save(gottenChannel);

        var notification = new Notification();
        notification.setCategory(NotificationCategory.channel);
        notification.setTitle("Left Channel");
        notification.setContent("You were removed from channel '" + savedChannel.getChannelName() + "'");
        notification.setTarget(savedChannel.getId());
        notification.setUserId(userId);
        notificationService.sendNotification(notification);

        var member = admin.map(Member::fromAdmin).orElseGet(() -> Member.fromStudent(student.get()));

        // If the user/admin was removed or left. We want to send a chat indicating
        // That the victim was removed or left
        var chat = new Chat();
        chat.setChannelId(channelId);
        chat.setSenderProfile(member.getProfile());
        chat.setSenderId(userId);
        chat.setTimestamp(LocalDateTime.now());
        chat.setMessageType(MessageType.member);
        chat.setMessage(member.getFirstName() + " left channel");
        chat.setReadReceipt(List.of());
        var createdChat = chatRepository.save(chat);

        /// To update the websocket that a new chat has been added
        var chatsResponse = new ApiResponse("User was removed", createdChat);
        messagingTemplate.convertAndSend("/chats/" + channelId, chatsResponse);

        // We're going to broadcast the information to all the members
        var membersId = savedChannel.getMembers().stream().map(Object::toString).toList();
        var fetchedChannel = getOneChannel(channelId).get();

        for(String memberId : membersId){
            fetchedChannel.setUnreadMessages(chatService.getUnseenChatsCount(channelId, memberId));
            fetchedChannel.setLatestMessage(createdChat);
            var updatedChannelResponse = new ApiResponse("Member Removed", fetchedChannel);

            messagingTemplate.convertAndSend("/channels/" + channelId, updatedChannelResponse);
        }


        return true;

    }

    public Notification sendInvitation(String email, String channelId) throws Exception{
        var channelExists = channelRepository.findById(channelId);

        var admin = adminService.getAdminByEmail(email);
        var student = userService.getStudentEmail(email);

        if(admin.isEmpty() && student.isEmpty()){
            throw new Exception("User or Admin doesn't exist");
        }
        var id = admin.isEmpty()? student.get().getId(): admin.get().getId();

        if(channelExists.isEmpty()){
            throw new Exception("Channel doesn't exist");
        }
        var channel = channelExists.get();

        if(channel.getMembers().contains(id)){
            throw new Exception("User is already a member");
        }

        var notification = new Notification();
        notification.setUserId(id);
        notification.setCategory(NotificationCategory.invitation);
        notification.setTarget(channelId);
        notification.setTitle("Channel Invitation");
        notification.setContent("You were invited to join '" + channel.getChannelName() + "'");
        return notificationService.sendNotification(notification);
    }

    public Notification respondToInvitation(String invitationId, boolean accepted) throws Exception{
        var invitation = notificationService.getNotification(invitationId);
        invitation.setRead(true);
        if(accepted){
            addMember(invitation.getUserId(), invitation.getTarget());
            invitation.setTitle("Accepted Invitation");
            invitation.setContent("You accepted an invitation");
        }

        if(!accepted){
            invitation.setTitle("Rejected Invitation");
            invitation.setContent("You rejected an invitation");
        }

        return notificationService.updateNotification(invitationId, invitation);
    }

    // WARNING: Must not be called in getOneChannel !!
    public Optional<Member> getCreator(String channelId){
        var channel = getOneChannel(channelId);
        if(channel.isEmpty()){
            return Optional.empty();
        }

        var gottenChannel = channel.get();

        return Optional.of((Member) gottenChannel.getCreator());
    }


    public Optional<Channel> createChannel(String creatorId, Channel channel){
        var creator = adminService.getAdmin(creatorId).get();
        channel.setId(null);
        channel.setCreator(creatorId);
        channel.setMembers(List.of(creatorId));
        channel.setCreatedAt(LocalDateTime.now());

        try{
            var createdChannel = channelRepository.save(channel);

            // If the channel was created. We want to send a chat indicating
            // That the channel was created
            var chat = new Chat();
            chat.setChannelId(createdChannel.getId());
            chat.setSenderProfile(creator.getProfile());
            chat.setSenderId(creatorId);
            chat.setTimestamp(LocalDateTime.now());
            chat.setMessageType(MessageType.create);
            chat.setMessage(creator.getFirstName() + " created Channel '" + channel.getChannelName() + "'.");
            chat.setReadReceipt(List.of(creatorId));
            var createdChat = chatRepository.save(chat);

            var notification = new Notification();
            notification.setCategory(NotificationCategory.channel);
            notification.setTitle("Channel Created");
            notification.setContent("You created channel '" + channel.getChannelName() + "'");
            notification.setTarget(createdChannel.getId());
            notification.setUserId(creatorId);
            notificationService.sendNotification(notification);


            /// To update the websocket that a new chat has been added
            var chatsResponse = new ApiResponse("Channel Created", createdChat);
            messagingTemplate.convertAndSend("/chats/" + createdChannel.getId(), chatsResponse);


            var fetchedChannel = getOneChannel(channel.getId()).get();
            fetchedChannel.setLatestMessage(createdChat);
            fetchedChannel.setUnreadMessages(0);
            var createdChannelResponse = new ApiResponse("Channel Created Successfully", fetchedChannel);
            messagingTemplate.convertAndSend("/channels/" + creatorId, createdChannelResponse);
            return Optional.of(fetchedChannel);
        } catch (Exception e) {
            return Optional.empty();
        }

    }

    public Optional<Channel> updateChannel(String channelId, Channel channel){
        // Only Channel name and Channel Description are edited;

        /// Intentionally using the repository's findById method instead
        // of the getOneChannel because the getOneChannel transforms
        // the members list from strings to Members. So we have to use
        // The one that brings the exact form it is in the database which is the good ol
        // findById Method.
        var gottenChannelExists = channelRepository.findById(channelId);

        if(gottenChannelExists.isEmpty()){
            return Optional.empty();
        }



        var gottenChannel = gottenChannelExists.get();
        gottenChannel.setChannelName(channel.getChannelName());
        gottenChannel.setChannelDescription(channel.getChannelDescription());
        gottenChannel.setChannelType(channel.getChannelType());

        var creator = getCreator(channelId).get();

        var savedChannel = channelRepository.save(gottenChannel);
        var fetchedChannel = getOneChannel(channelId).get();

        var chat = new Chat();
        chat.setChannelId(savedChannel.getId());
        chat.setSenderProfile(creator.getProfile());
        chat.setSenderId(creator.getId());
        chat.setTimestamp(LocalDateTime.now());
        chat.setMessageType(MessageType.update);
        chat.setMessage(creator.getFirstName() + " updated channel '" + channel.getChannelName() + "'.");
        chat.setReadReceipt(List.of(creator.getId()));
        var createdChat = chatRepository.save(chat);

        /// To update the websocket that a new chat has been added
        var chatsResponse = new ApiResponse("Channel Updated", createdChat);
        messagingTemplate.convertAndSend("/chats/" + savedChannel.getId(), chatsResponse);



        // We're going to broadcast the information to all the members
        var membersId = savedChannel.getMembers().stream().map(Object::toString).toList();
        for(String memberId : membersId){
            fetchedChannel.setUnreadMessages(chatService.getUnseenChatsCount(channelId, memberId));
            fetchedChannel.setLatestMessage(createdChat);
            var updatedChannelResponse = new ApiResponse("Channel Updated", fetchedChannel);
            messagingTemplate.convertAndSend("/channels/" + memberId, updatedChannelResponse);
        }

        for(String memberId: membersId){
            var notification = new Notification();
            notification.setCategory(NotificationCategory.channel);
            notification.setTitle("Channel Updated");
            notification.setContent((Objects.equals(memberId, gottenChannel.getCreator().toString())? "You updated channel '":"Someone updated channel '") + channel.getChannelName() + "'");
            notification.setTarget(savedChannel.getId());
            notification.setUserId(memberId);
            try {
                notificationService.sendNotification(notification);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return Optional.of(fetchedChannel);
    }

    public Optional<Channel> updateChannelProfile(String channelId, String url){
        /// Intentionally using the repository's findById method instead
        // of the getOneChannel because the getOneChannel transforms
        // the members list from strings to Members. So we have to use
        // The one that brings the exact form it is in the database which is the good ol
        // findById Method.
        var gottenChannel = channelRepository.findById(channelId);

        var gottenCreator = getCreator(channelId);

        if(gottenChannel.isEmpty() || gottenCreator.isEmpty()){
            return Optional.empty();
        }

        var channel = gottenChannel.get();
        var creator = gottenCreator.get();
        channel.setChannelProfile(url);
        var savedChannel = channelRepository.save(channel);

        // Once channel photo changed,
        // We send a chat indicating that channel photo has changed
        var chat = new Chat();
        chat.setChannelId(channelId);
        chat.setSenderProfile(creator.getProfile());
        chat.setSenderId(creator.getId());
        chat.setTimestamp(LocalDateTime.now());
        chat.setMessageType(MessageType.update);
        chat.setAttachment(url);
        chat.setAttachmentType(AttachmentType.image);
        chat.setMessage("Channel's photo was changed");
        chat.setReadReceipt(List.of(creator.getId()));
        var savedChat = chatRepository.save(chat);

        /// To update the websocket that a new chat has been added
        var chatsResponse = new ApiResponse("Channel Photo Updated", savedChat);
        messagingTemplate.convertAndSend("/chats/" + channelId, chatsResponse);

        // We're going to broadcast the information to all the members
        var members = channel.getMembers().stream().map(Object::toString).toList();
        var fetchedChannel = getOneChannel(channelId).get();
        for(String member : members){
            fetchedChannel.setUnreadMessages(chatService.getUnseenChatsCount(channelId, member));
            fetchedChannel.setLatestMessage(savedChat);
            var updatedChannelResponse = new ApiResponse("Channel Photo Updated", fetchedChannel);
            messagingTemplate.convertAndSend("/channels/" + member, updatedChannelResponse);
        }

        for(String member: members){
            var notification = new Notification();
            notification.setCategory(NotificationCategory.channel);
            notification.setTitle("Channel Profile Updated");
            notification.setContent((Objects.equals(member, channel.getCreator().toString())? "You updated channel '":"Someone updated channel '") + channel.getChannelName() + "'");
            notification.setTarget(savedChannel.getId());
            notification.setUserId(member);
            try {
                notificationService.sendNotification(notification);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return Optional.of(fetchedChannel);
    }
}
