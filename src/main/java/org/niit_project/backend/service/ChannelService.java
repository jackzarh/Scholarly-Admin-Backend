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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
            /// We get the last message of the chat and if it's empty we set it to null
            /// And also set the unread messages of the chat.
            channel.setLatestMessage(chatService.getLastChat(channel.getId()).orElse(null));
            channel.setUnreadMessages(chatService.getUnseenChatsCount(channel.getId(), adminId).orElse(0));
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
        gottenChannel.setMembers(List.of(gottenChannel.getMembers(), member.getId()));
        channelRepository.save(gottenChannel);

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

        /// To update the websocket that a new chat has been added
        var chatsResponse = new ApiResponse("User was added", createdChat);
        messagingTemplate.convertAndSend("/chats/" + channelId, chatsResponse);


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
        var members = gottenChannel.getMembers();
        members.remove(userId);

        // We save to the database
        gottenChannel.setMembers(members);
        channelRepository.save(gottenChannel);

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


        return true;

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

            /// To update the websocket that a new chat has been added
            var chatsResponse = new ApiResponse("Channel Created", createdChat);
            messagingTemplate.convertAndSend("/chats/" + createdChannel.getId(), chatsResponse);

            createdChannel.setLatestMessage(createdChat);
            return Optional.of(createdChannel);
        } catch (Exception e) {
            return Optional.empty();
        }

    }

    public Optional<Channel> updateChannel(String channelId,Channel channel){
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

        return Optional.of(channelRepository.save(gottenChannel));
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

        return Optional.of(savedChannel);
    }
}
