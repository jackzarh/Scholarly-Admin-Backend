package org.niit_project.backend.service;

import org.niit_project.backend.dto.ApiResponse;
import org.niit_project.backend.entities.*;
import org.niit_project.backend.enums.AttachmentType;
import org.niit_project.backend.enums.Colors;
import org.niit_project.backend.enums.MessageType;
import org.niit_project.backend.enums.NotificationCategory;
import org.niit_project.backend.models.Delete;
import org.niit_project.backend.models.User;
import org.niit_project.backend.repository.ChannelRepository;
import org.niit_project.backend.repository.ChatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
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
    private StudentService studentService;

    @Autowired
    private SideChatService chatService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private DirectMessageService directMessageService;

    @Autowired
    private MongoTemplate  mongoTemplate;

    public Optional<List<Channel>> getAllChannels(){
        try{
            return Optional.of(channelRepository.findAll());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public List<Channel> getUserChannels(String userId){

        var matchAggregation = Aggregation.match(Criteria.where("members").in(userId));
        var sortAggregation = Aggregation.sort(Sort.by(Sort.Direction.DESC, "createdAt"));
        var aggregation = Aggregation.newAggregation(matchAggregation, sortAggregation);

        var results = mongoTemplate.aggregate(aggregation, "channels", Channel.class).getMappedResults();
        var formed = results.stream().peek(channel -> {
            var membersAggregation = Aggregation.newAggregation(Aggregation.match(Criteria.where("_id").in(channel.getMembers())));
            var studentMembers = mongoTemplate.aggregate(membersAggregation, "students", Student.class).getMappedResults();
            var adminMembers = mongoTemplate.aggregate(membersAggregation, "admins", Admin.class).getMappedResults();

            //Then collate all members
            var allMembers = new ArrayList<User>();
            allMembers.addAll(studentMembers.stream().map(User::fromStudent).toList());
            allMembers.addAll(adminMembers.stream().map(User::fromAdmin).toList());
            channel.setMembers(Arrays.asList(allMembers.toArray()));
            var creator = allMembers.stream().filter(member -> member.getId().equals(channel.getCreator())).findFirst().orElse(allMembers.isEmpty()? null: allMembers.get(0));
            channel.setCreator(creator);

            /// We get the last message of the chat and if it's empty we set it to null
            /// And also set the unread messages of the chat.
            channel.setLatestMessage(chatService.getLastChat(channel.getId()).orElse(null));
            channel.setUnreadMessages(chatService.getUnseenChatsCount(channel.getId(), userId));
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

        var gottenAdminCreator = adminService.getAdmin(channel.getCreator().toString());

        if(gottenAdminCreator.isEmpty()){
            var student = studentService.getCompactStudent(channel.getCreator().toString()).get();
            channel.setCreator(User.fromStudent(student));
        }
        else{
            channel.setCreator(User.fromAdmin(gottenAdminCreator.get()));
        }

        // Aggregation stage to match the members base on their id
        var membersMatch = Aggregation.match(Criteria.where("_id").in(members));
        var aggregations = Aggregation.newAggregation(membersMatch);
        var studentMembers = mongoTemplate.aggregate(aggregations, "students", Student.class).getMappedResults();
        var adminMembers = mongoTemplate.aggregate(aggregations, "admins", Admin.class).getMappedResults();

        //Then collate all members
        var allMembers = new ArrayList<>(studentMembers.stream().map(User::fromStudent).toList());
        allMembers.addAll(adminMembers.stream().map(User::fromAdmin).toList());

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

    public User addMember(String userId, String channelId) throws Exception{
        // We first have to know whether such person is an
        // Admin or a member

        var admin = adminService.getAdmin(userId);
        var student = studentService.getCompactStudent(userId);
        if(admin.isEmpty() && student.isEmpty()){
            throw new Exception("User or Admin doesn't exist");
        }

        // We're using getCompactChannel because it has the id as a String
        var channel = getCompactChannel(channelId);
        if(channel.isEmpty()){
            throw new Exception("Channel doesn't exist");
        }
        var gottenChannel = channel.get();

        // Since the channel exists (Let's get the community)
        var community = mongoTemplate.findOne(Query.query(Criteria.where("_id").is(gottenChannel.getCommunityId())), Community.class, "communities");
        if(community == null){
            throw new Exception("Community does not exist");
        }

        // We check and make sure the user is a part of the channel's community
        if(!community.getMembers().contains(userId)){
            throw new Exception("This admin/student is not part of this channel's community");
        }

        // We make sure the admin/student isn't already a member of the channel
        if(gottenChannel.getMembers().contains(userId)){
            throw new Exception("This user/admin is already a member of this channel");
        }

        var member = admin.map(User::fromAdmin).orElseGet(() -> User.fromStudent(student.get()));

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
        chat.setDmId(channelId);
        chat.setSenderProfile(member.getProfile());
        chat.setSenderId(userId);
        chat.setTimestamp(LocalDateTime.now());
        chat.setMessageType(MessageType.member);
        chat.setMessage(member.getFirstName() + " joined channel from community");
        chat.setReadReceipt(List.of());
        var createdChat = chatRepository.save(chat);
        // ...And send this chat to the channel's chat websocket
        var chatsResponse = new ApiResponse("User was added", createdChat);
        messagingTemplate.convertAndSend("/chats/" + channelId, chatsResponse);

        // Adding the user to the channel's respective DM (If the DM exists).
        try{
            directMessageService.addMember(member.getId(), channelId);
        }
        catch (Exception e) {
            // `DM doesn't exist` is the message usually thrown
            // when the channel hasn't created a DM yet.
            if(!e.getMessage().equals("DM doesn't exist")){
                throw e;
            }
        }

        // Then, We update each channel's DM by adding this member
        // We're going to broadcast the information to all the members
        // var membersId = savedChannel.getMembers().stream().map(Object::toString).toList();
        // var fetchedChannel = getOneChannel(channelId).get();
        // for(String memberId : membersId) {
        //  fetchedChannel.setUnreadMessages(chatService.getUnseenChatsCount(channelId, memberId));
        //  fetchedChannel.setLatestMessage(createdChat);
        //  var updatedChannelResponse = new ApiResponse("Member Added", fetchedChannel);
        //
        //
        //  messagingTemplate.convertAndSend("/channels/" + memberId, updatedChannelResponse);
        // }


        // Lastly, we send push notifications to all the members indicating that this member was added
        var notification = new Notification();
        notification.setCategory(NotificationCategory.channel);
        notification.setTitle("New Member");
        notification.setContent(member.getFirstName() +" joined channel '" + savedChannel.getChannelName() + "'");
        notification.setTarget(savedChannel.getId());
        notification.setRecipients(savedChannel.getMembers().stream().map(Object::toString).toList());
        notificationService.sendPushNotification(notification, false);

        return member;
    }

    public boolean removeMember(String userId, String channelId) throws Exception{

        // We first have to know whether such person is an
        // Admin or a member

        var admin = adminService.getAdmin(userId);
        var student = studentService.getCompactStudent(userId);
        if(admin.isEmpty() && student.isEmpty()){
            throw new Exception("User or Admin doesn't exist");
        }

        // We're using getCompactChannel because it has the id as a String
        var channel = getCompactChannel(channelId);
        if(channel.isEmpty()){
            throw new Exception("Channel doesn't exist");
        }
        var gottenChannel = channel.get();


        // We make sure the admin/student is  a member of the channel
        if(!gottenChannel.getMembers().contains(userId)){
            throw new Exception("This user/admin is not a member of this channel");
        }

        var member = admin.map(User::fromAdmin).orElseGet(() -> User.fromStudent(student.get()));

        /// We then update the members/add the member
        /// And save it in the repository
        var members = gottenChannel.getMembers().stream().map(Object::toString).toList();
        var newMembers = new ArrayList<Object>(members);
        newMembers.remove(member.getId());
        gottenChannel.setMembers(newMembers);
        var savedChannel = channelRepository.save(gottenChannel);

        // If the user/admin was added. We want to send a chat indicating
        // That the added was added
        var chat = new Chat();
        chat.setDmId(channelId);
        chat.setSenderProfile(member.getProfile());
        chat.setSenderId(userId);
        chat.setTimestamp(LocalDateTime.now());
        chat.setMessageType(MessageType.member);
        chat.setMessage(member.getFirstName() + " left channel");
        chat.setReadReceipt(List.of());
        var createdChat = chatRepository.save(chat);
        // ...And send this chat to the channel's chat websocket
        var chatsResponse = new ApiResponse("User left", createdChat);
        messagingTemplate.convertAndSend("/chats/" + channelId, chatsResponse);

        // We send a message to the channel and dms websocket of the user who was removed
        // Indicating that he's been removed
        var delete = new Delete();
        delete.setId(channelId); // Bearing in mind that DMs and Channels have the same id
        delete.setDeleted(true);
        messagingTemplate.convertAndSend("/channels/"+userId, delete);
        messagingTemplate.convertAndSend("/dms/"+userId, delete);



        // Adding the user to the channel's respective DM (If the DM exists).
        try{
            directMessageService.removeMember(member.getId(), channelId);
        }
        catch (Exception e) {
            // `DM doesn't exist` is the message usually thrown
            // when the channel hasn't created a DM yet.
            if(!e.getMessage().equals("DM doesn't exist")){
                throw e;
            }
        }

        return true;

    }

    public Notification sendInvitation(String email, String channelId) throws Exception{
        var channelExists = channelRepository.findById(channelId);

        var admin = adminService.getAdminByEmail(email);
        var student = studentService.getStudentEmail(email);

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
        notification.setRecipients(List.of(id));
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
            addMember(invitation.getRecipients().get(0), invitation.getTarget());
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
    public Optional<User> getCreator(String channelId){
        var channel = getOneChannel(channelId);
        if(channel.isEmpty()){
            return Optional.empty();
        }

        var gottenChannel = channel.get();

        return Optional.of((User) gottenChannel.getCreator());
    }


    public Channel createChannel(String creatorId, Channel channel, String communityId) throws Exception{
        // To verify first of all that the community exists:
        var communityExists = mongoTemplate.exists(Query.query(Criteria.where("_id").is(communityId)), Community.class, "communities");
        if(!communityExists){
            throw new Exception("The target community for this channel doesn't exist");
        }

        // This way, both students and admins can create channels
        var admin  = adminService.getAdmin(creatorId);
        var student = studentService.getCompactStudent(creatorId);
        if(admin.isEmpty() && student.isEmpty()){
            throw new Exception("Admin or Student not found");
        }

        var creator = admin.map(User::fromAdmin).orElseGet(() -> User.fromStudent(student.get()));

        channel.setId(null);
        channel.setCommunityId(communityId);
        channel.setCreator(creatorId);
        channel.setMembers(List.of(creatorId));
        channel.setCreatedAt(LocalDateTime.now());
        channel.setColor(Colors.getRandomColor().name());

        var createdChannel = channelRepository.save(channel);

        // If the channel was created. We want to send a chat indicating
        // That the channel was created
        var chat = new Chat();
        chat.setDmId(createdChannel.getId());
        chat.setSenderProfile(creator.getProfile());
        chat.setSenderId(creatorId);
        chat.setTimestamp(LocalDateTime.now());
        chat.setMessageType(MessageType.create);
        chat.setMessage(creator.getFirstName() + (" created Channel '") + channel.getChannelName() + "'.");
        chat.setReadReceipt(List.of(creatorId));
        var createdChat = chatRepository.save(chat);

        var notification = new Notification();
        notification.setCategory(NotificationCategory.channel);
        notification.setTitle("Channel Created");
        notification.setContent("You created channel '" + channel.getChannelName() + "'");
        notification.setTarget(createdChannel.getId());
        notification.setRecipients(List.of(creatorId));
        notificationService.sendNotification(notification);


        /// To update the websocket that a new chat has been added
        var chatsResponse = new ApiResponse("Channel Created", createdChat);
        messagingTemplate.convertAndSend("/chats/" + createdChannel.getId(), chatsResponse);


        var fetchedChannel = getOneChannel(createdChannel.getId()).get();
        fetchedChannel.setLatestMessage(createdChat);
        fetchedChannel.setUnreadMessages(0);
        var createdChannelResponse = new ApiResponse("Channel Created Successfully", fetchedChannel);
        messagingTemplate.convertAndSend("/channels/" + creatorId, createdChannelResponse);
        return fetchedChannel;

    }

    public Channel updateChannel(String channelId, Channel channel) throws Exception{
        // Only Channel name and Channel Description are edited;

        /// Intentionally using the repository's findById method instead
        // of the getOneChannel because the getOneChannel transforms
        // the members list from strings to Members. So we have to use
        // The one that brings the exact form it is in the database which is the good ol
        // findById Method.
        var gottenChannelExists = channelRepository.findById(channelId);

        if(gottenChannelExists.isEmpty()){
            throw new Exception("Channel does not exist");
        }

        var gottenChannel = gottenChannelExists.get();
        gottenChannel.setChannelName(channel.getChannelName());
        gottenChannel.setChannelDescription(channel.getChannelDescription());
        gottenChannel.setChannelType(channel.getChannelType());

        var creator = getCreator(channelId).get();

        var savedChannel = channelRepository.save(gottenChannel);
        var fetchedChannel = getOneChannel(channelId).get();

        var chat = new Chat();
        chat.setDmId(savedChannel.getId());
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
        var notification = new Notification();
        notification.setCategory(NotificationCategory.channel);
        notification.setTitle("Channel Updated");
        notification.setContent("Channel '" + channel.getChannelName() + "' was updated.");
        notification.setTarget(savedChannel.getId());
        notification.setRecipients(membersId);
        notificationService.sendNotification(notification);

        return fetchedChannel;
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
        chat.setDmId(channelId);
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

        var notification = new Notification();
        notification.setCategory(NotificationCategory.channel);
        notification.setTitle("Channel Profile Updated");
        notification.setContent("Channel '" + channel.getChannelName() + "', was updated");
        notification.setTarget(savedChannel.getId());
        notification.setRecipients(members);
        try {
            notificationService.sendNotification(notification);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Optional.of(fetchedChannel);
    }
}
