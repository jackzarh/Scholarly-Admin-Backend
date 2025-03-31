package org.niit_project.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import io.github.cdimascio.dotenv.Dotenv;
import org.niit_project.backend.dto.ApiResponse;
import org.niit_project.backend.entities.*;
import org.niit_project.backend.enums.*;
import org.niit_project.backend.models.Delete;
import org.niit_project.backend.models.User;
import org.niit_project.backend.repository.CommunitiesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class CommunityService {

    @Autowired
    private CommunitiesRepository communitiesRepository;

    @Autowired
    private DirectMessageService directMessageService;

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
    private MongoTemplate mongoTemplate;

    @Autowired
    private ChannelService channelService;

    public Optional<List<Community>> getAllCommunities(){
        try{
            return Optional.of(communitiesRepository.findAll());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public List<Community> getUserCommunities(String userId){

        var matchAggregation = Aggregation.match(Criteria.where("members").in(userId));
        var sortAggregation = Aggregation.sort(Sort.by(Sort.Direction.DESC, "createdAt"));
        var aggregation = Aggregation.newAggregation(matchAggregation, sortAggregation);

        var results = mongoTemplate.aggregate(aggregation, "communities", Community.class).getMappedResults();
        var formed = results.stream().peek(community -> {
            var membersAggregation = Aggregation.newAggregation(Aggregation.match(Criteria.where("_id").in(community.getMembers())));
            var studentMembers = mongoTemplate.aggregate(membersAggregation, "students", Student.class).getMappedResults();
            var adminMembers = mongoTemplate.aggregate(membersAggregation, "admins", Admin.class).getMappedResults();

            //Then collate all members
            var allMembers = new ArrayList<User>();
            allMembers.addAll(studentMembers.stream().map(User::fromStudent).toList());
            allMembers.addAll(adminMembers.stream().map(User::fromAdmin).toList());
            community.setMembers(Arrays.asList(allMembers.toArray()));
            var creator = allMembers.stream().filter(member -> member.getId().equals(community.getCreator())).findFirst().orElse(allMembers.isEmpty()? null: allMembers.get(0));
            community.setCreator(creator);

            /// We get the last message of the chat and if it's empty we set it to null
            /// And also set the unread messages of the chat.

            var channelsMatchAggregation = Aggregation.match(Criteria.where("communityId").in(community.getId()));
            var channelResults = mongoTemplate.aggregate(Aggregation.newAggregation(channelsMatchAggregation), "channels", Channel.class).getMappedResults();
            var channelsFormed = channelResults.stream().peek(channel -> {
                // Set the channel's members to the members id of the community who are included of the channel
                channel.setMembers(List.of(allMembers.stream().filter(member -> channel.getMembers().contains(member.getId())).toList()));

                // Set the latest message and unread message of each channel
                channel.setLatestMessage(chatService.getLastChat(community.getId()).orElse(null));
                channel.setUnreadMessages(chatService.getUnseenChatsCount(community.getId(), userId));

            });
            community.setChannels(channelsFormed.map(channel -> (Object) channel).toList());
        }).toList();

        return formed.stream().sorted((channel1, channel2) -> (channel2.getLatestSortTime()).compareTo(channel1.getLatestSortTime())).toList();
    }

    // WARNING: Should be used if robust details concerning a community is needed !!
    public Optional<Community> getOneCommunity(String id) {
        var communityExists = communitiesRepository.existsById(id);

        if(!communityExists){
            return Optional.empty();
        }


        var community = communitiesRepository.findById(id).get();
        var members = community.getMembers();

        var gottenAdminCreator = adminService.getAdmin(community.getCreator().toString());

        if(gottenAdminCreator.isEmpty()){
            var student = studentService.getCompactStudent(community.getCreator().toString()).get();
            community.setCreator(User.fromStudent(student));
        }
        else{
            community.setCreator(User.fromAdmin(gottenAdminCreator.get()));
        }

        // Aggregation stage to match the members base on their id
        var membersMatch = Aggregation.match(Criteria.where("_id").in(members));
        var aggregations = Aggregation.newAggregation(membersMatch);
        var studentMembers = mongoTemplate.aggregate(aggregations, "students", Student.class).getMappedResults();
        var adminMembers = mongoTemplate.aggregate(aggregations, "admins", Admin.class).getMappedResults();

        //Then collate all members
        var allMembers = new ArrayList<>(studentMembers.stream().map(User::fromStudent).toList());
        allMembers.addAll(adminMembers.stream().map(User::fromAdmin).toList());
        community.setMembers(new ArrayList<>(allMembers));

        var channelsMatchAggregation = Aggregation.match(Criteria.where("communityId").in(community.getId()));
        var channelResults = mongoTemplate.aggregate(Aggregation.newAggregation(channelsMatchAggregation), "channels", Channel.class).getMappedResults();
        var channelsFormed = channelResults.stream().peek(channel -> {
            // Set the channel's members to the members id of the community who are included of the channel
            channel.setMembers(new ArrayList<>(allMembers.stream().filter(member -> channel.getMembers().contains(member.getId())).toList()));
        });
        community.setChannels(new ArrayList<>(channelsFormed.toList()));


        return Optional.of(community);

    }

    public Optional<Community> getCompactCommunity(String id) {
        var communityExists = communitiesRepository.existsById(id);

        if(!communityExists){
            return Optional.empty();
        }

        var community = communitiesRepository.findById(id).get();
        return Optional.of(community);

    }

    public User addMember(String userId, String communityId) throws Exception{
        // We first have to know whether such person is an
        // Admin or a member

        var admin = adminService.getAdmin(userId);
        var student = studentService.getCompactStudent(userId);
        if(admin.isEmpty() && student.isEmpty()){
            throw new Exception("User or Admin doesn't exist");
        }


        var community = getCompactCommunity(communityId);
        if(community.isEmpty()){
            throw new Exception("Community doesn't exist");
        }

        var gottenCommunity = community.get();

        // We make sure the admin/student isn't already a member of the community
        if(gottenCommunity.getMembers().contains(userId)){
            throw new Exception("This user/admin is already a member of this community");
        }

        var member = admin.map(User::fromAdmin).orElseGet(() -> User.fromStudent(student.get()));

        /// We then update the members/add the member
        /// And save it in the repository
        var members = gottenCommunity.getMembers().stream().map(Object::toString).toList();
        var newMembers = new ArrayList<Object>(members);
        newMembers.add(member.getId());
        gottenCommunity.setMembers(newMembers);
        var savedCommunity = communitiesRepository.save(gottenCommunity);


        var notification = new Notification();
        notification.setCategory(NotificationCategory.community);
        notification.setTitle("Joined Community");
        notification.setContent("You were added to community '" + savedCommunity.getCommunityName() + "'");
        notification.setTarget(savedCommunity.getId());
        notification.setRecipients(List.of(userId));
        notificationService.sendPushNotification(notification);

        var fetchedCommunity = getOneCommunity(communityId).get();
        var fetchedCommunityChannels = fetchedCommunity.getChannels().stream().map(o -> (Channel)o).toList();



        // If the user/admin was added. We want to send and update chat websocket
        // of all the main channels indicating
        // That the user was added
        var mainChannels = fetchedCommunityChannels.stream().filter(channel -> channel.getChannelType() != ChannelType.other).toList();

        // We send a chat to every Main (Announcement, Discussion or Project) Channel in the community
        // ...that a new member has joined.
        // And also add the user to thw channels and it's respective DMs

        for(var channel: mainChannels){
            var chat = new Chat();
            chat.setSenderProfile(member.getProfile());
            chat.setSenderId(userId);
            chat.setTimestamp(LocalDateTime.now());
            chat.setMessageType(MessageType.member);
            chat.setMessage(member.getFirstName() + " joined community");
            chat.setReadReceipt(List.of());
            chat.setDmId(channel.getId());
            var createdChat = mongoTemplate.save(chat, "chats");

            var chatResponse = new ApiResponse("Member joined", createdChat);
            messagingTemplate.convertAndSend("/chats/"+channel.getId(), chatResponse);

            channelService.addMember(userId, channel.getId());
        }

        // We're going to broadcast the information to the Discussion DM
        //var discussionsChannel = mainChannels.stream().filter(channel -> channel.getChannelType() == ChannelType.discussions).toList().get(0);
        //discussionsChannel.setMembers(newMembers);
        //var dm = DirectMessage.fromChannel(discussionsChannel);
        //dm.setLatestMessage(discussionUserJoinedChat);
        //directMessageService.updateDirectMessage(discussionsChannel.getId(), dm);


        return member;
    }

    public User removeMember(String userId, String communityId) throws Exception{
        var admin = adminService.getAdmin(userId);
        var student = studentService.getCompactStudent(userId);
        if(admin.isEmpty() && student.isEmpty()){
            throw new Exception("User or Admin doesn't exist");
        }


        var community = getCompactCommunity(communityId);
        if(community.isEmpty()){
            throw new Exception("Community doesn't exist");
        }

        var gottenCommunity = community.get();

        // We make sure the admin/student is actually a member of the community
        if(!gottenCommunity.getMembers().contains(userId)){
            throw new Exception("This user/admin is not a member of this community");
        }

        var member = admin.map(User::fromAdmin).orElseGet(() -> User.fromStudent(student.get()));


        // We're intentionally getting the full community before we
        // save it. Do not remove
        var fetchedCommunity = getOneCommunity(communityId).get();
        var fetchedCommunityChannels = fetchedCommunity.getChannels().stream().map(o -> (Channel)o).toList();

        /// We then update the members/add the member
        /// And save it in the repository
        var members = gottenCommunity.getMembers().stream().map(Object::toString).toList();
        var newMembers = new ArrayList<Object>(members);
        newMembers.remove(member.getId());
        gottenCommunity.setMembers(newMembers);
        var savedCommunity = communitiesRepository.save(gottenCommunity);


        var notification = new Notification();
        notification.setCategory(NotificationCategory.community);
        notification.setTitle("Left Community");
        notification.setContent("You left community '" + savedCommunity.getCommunityName() + "'");
        notification.setTarget(savedCommunity.getId());
        notification.setRecipients(List.of(userId));
        notificationService.sendPushNotification(notification);


       // We send a chat to every Channel the user is in Channel in the community
        // ...that a member left.
        // And also add the remove the user from the channels and it's respective DMs
        for(var channel: fetchedCommunityChannels){
            var isMemberOfChannel = channel.getMembers().stream().anyMatch(o -> ((User)o).getId().equals(userId));
            if(!isMemberOfChannel){
                continue;
            }

            var chat = new Chat();
            chat.setSenderProfile(member.getProfile());
            chat.setSenderId(userId);
            chat.setTimestamp(LocalDateTime.now());
            chat.setMessageType(MessageType.member);
            chat.setMessage(member.getFirstName() + " left community");
            chat.setReadReceipt(List.of());
            chat.setDmId(channel.getId());
            var createdChat = mongoTemplate.save(chat, "chats");
            var chatResponse = new ApiResponse("Member left", createdChat);
            messagingTemplate.convertAndSend("/chats/"+channel.getId(), chatResponse);

            channelService.removeMember(userId, channel.getId());
        }

        // We send a message to the communities websocket of the user who was removed
        // Indicating that he's been removed
        var delete = new Delete();
        delete.setId(communityId);
        delete.setDeleted(true);
        messagingTemplate.convertAndSend("/communities/"+userId, delete);
        return member;

    }

    public Notification sendInvitation(String email, String communityId) throws Exception{
        var communityExists = communitiesRepository.findById(communityId);
        if(communityExists.isEmpty()){
            throw new Exception("Community doesn't exist");
        }

        var admin = adminService.getAdminByEmail(email);
        var student = studentService.getStudentEmail(email);

        if(admin.isEmpty() && student.isEmpty()){
            throw new Exception("User or Admin doesn't exist");
        }
        var id = admin.isEmpty()? student.get().getId(): admin.get().getId();


        var notification = getNotification(communityId, communityExists.get(), id);
        return notificationService.sendPushNotification(notification);
    }

    private static Notification getNotification(String communityId, Community community, String id) throws Exception {

        if(community.getMembers().contains(id)){
            throw new Exception("User is already a member");
        }

        var notification = new Notification();
        notification.setRecipients(List.of(id));
        notification.setCategory(NotificationCategory.invitation);
        notification.setTarget(communityId);
        notification.setTitle("Community Invitation");
        notification.setContent("You were invited to join '" + community.getCommunityName() + "'");
        return notification;
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
        var channel = getOneCommunity(channelId);
        if(channel.isEmpty()){
            return Optional.empty();
        }

        var gottenChannel = channel.get();

        return Optional.of((User) gottenChannel.getCreator());
    }

    public Optional<Channel> getChannel(String communityId, ChannelType channelType){
        var matchAggregation = Aggregation.match(new Criteria().andOperator(Criteria.where("communityId").in(communityId), Criteria.where("channelType").in(channelType.name())));
        var aggregation = Aggregation.newAggregation(matchAggregation);

        var results = mongoTemplate.aggregate(aggregation,"channels", Channel.class).getMappedResults();

        if(results.isEmpty()){
            return Optional.empty();
        }

        return Optional.of(results.get(0));
    }


    public Community createCommunity(String creatorId, Community community) throws Exception{
        // This way, both students and admins can create channels
        User creator = null;
        var admin  = adminService.getAdmin(creatorId);
        if(admin.isEmpty()){
            var student = studentService.getCompactStudent(creatorId);
            if(student.isEmpty()){
                throw new Exception("Admin or Student not found");
            }
            creator = User.fromStudent(student.get());
        }
        else{
            creator = User.fromAdmin(admin.get());
        }

        community.setId(null);
        community.setCreator(creatorId);
        community.setMembers(List.of(creatorId));
        community.setCreatedAt(LocalDateTime.now());
        community.setColor(Colors.getRandomColor());
        var createdCommunity = communitiesRepository.save(community);

        // We create the Announcement, Discussions and Project channels under the community
        for(int i = 0; i<3; i++){
            var channelType = i==0? ChannelType.announcements : i==1? ChannelType.discussions : ChannelType.project;

            var channel = new Channel();
            channel.setCommunityId(createdCommunity.getId());
            channel.setCreatedAt(createdCommunity.getCreatedAt());
            channel.setChannelType(channelType);
            channel.setColor(Colors.getRandomColor().name());
            channel.setMembers(List.of(creatorId));
            channel.setChannelDescription(community.getCommunityDescription());
            channel.setChannelName(channelType.name().toUpperCase().charAt(0)+channelType.name().substring(1));
            var createdChannel = mongoTemplate.save(channel, "channels");
            var dmId = createdChannel.getId();

            // Once the channel is created, we  send a chat indicating
            // That the community was created.
            // So to do this, we just set the dmID of the already initalized Chat Object
            // to the id of the channel created.
            var chat = new Chat();
            chat.setSenderProfile(creator.getProfile());
            chat.setSenderId(creatorId);
            chat.setTimestamp(LocalDateTime.now());
            chat.setMessageType(MessageType.create);
            chat.setMessage(creator.getFirstName() + " created Community '" + community.getCommunityName() + "'.");
            chat.setReadReceipt(List.of(creatorId));
            chat.setDmId(dmId);
            chat.setDmId(dmId);


            System.out.println("\n\n============================");
            System.out.println("Created Channel Id: " + dmId);
            System.out.println("Chat's DM Id before save: " + chat.getDmId());
            var savedChat = mongoTemplate.save(chat, "chats");
            System.out.println("Chat's DM Id after save: " + savedChat.getDmId());

            // Then we automatically create a DM only for the Discussion Channel.
            if(channelType == ChannelType.discussions){
                var dm = new DirectMessage();
                dm.setId(createdChannel.getId());
                dm.setRecipients(createdChannel.getMembers());
                System.out.println("DM ID before saving: "+ dm.getId());
                directMessageService.createDirectMessage(dm);
                System.out.println("DM ID after saving: "+dm.getId());

            }
        }


        // We only send a notification indicating the community was created.
        // We're not sending notifications per-channel created because we don't want to flood user's notifications.
        var notification = new Notification();
        notification.setCategory(NotificationCategory.community);
        notification.setTitle("Community Created");
        notification.setContent("You created community '" + community.getCommunityName() + "'");
        notification.setTarget(createdCommunity.getId());
        notification.setRecipients(List.of(creatorId));
        notificationService.sendPushNotification(notification);


        return createdCommunity;

    }

    public Community updateCommunity(String communityId, Community community) throws Exception{
        var gottenCommunityExists = communitiesRepository.findById(communityId);
        if(gottenCommunityExists.isEmpty()){
            throw new Exception("Community does not exist");
        }
        var gottenCommunity = gottenCommunityExists.get();

        if(community.getCommunityName() == null){
            throw new Exception("Community name cannot be null");
        }
        if(community.getCommunityDescription() == null){
            throw new Exception("Community description cannot be null");
        }

        gottenCommunity.setCommunityName(community.getCommunityName());
        gottenCommunity.setCommunityDescription(community.getCommunityDescription());

        var creator = getCreator(communityId).get();

        var savedCommunity = communitiesRepository.save(gottenCommunity);
        var fetchedCommunity = getOneCommunity(communityId).get();

        Chat discussionsChannelCommunityUpdated = null;

        // Then we send a chat to each channel's DM that the community has been updated.
        for(var rawChannel: fetchedCommunity.getChannels()){
            var channel = (Channel) rawChannel;

            var chat = new Chat();
            chat.setDmId(channel.getId());
            chat.setSenderProfile(creator.getProfile());
            chat.setSenderId(creator.getId());
            chat.setTimestamp(LocalDateTime.now());
            chat.setMessageType(MessageType.update);
            chat.setMessage(creator.getFirstName() + " updated community '" + community.getCommunityName() + "'.");
            chat.setReadReceipt(List.of(creator.getId()));
            var createdChat = mongoTemplate.save(chat, "chats");

            if(channel.getChannelType() == ChannelType.discussions){
                discussionsChannelCommunityUpdated = createdChat;
            }

            /// To update the websocket that a new chat has been added
            var chatsResponse = new ApiResponse("Community Updated", createdChat);
            messagingTemplate.convertAndSend("/chats/" + channel.getId(), chatsResponse);
        }


        var discussionChannels = fetchedCommunity.getChannels().stream().map(o -> (Channel)o).filter(channel -> channel.getChannelType() == ChannelType.discussions).toList();
        if(discussionChannels.isEmpty()){
            return fetchedCommunity;
        }

        var discussionChannel = discussionChannels.get(0);
        fetchedCommunity.setChannels(List.of());
        discussionChannel.setCommunity(fetchedCommunity);

        // We're going to broadcast the information to all the member's DMs
        var dm = DirectMessage.fromChannel(discussionChannel);
        dm.setTime(LocalDateTime.now());
        dm.setLatestMessage(discussionsChannelCommunityUpdated);
        directMessageService.updateDirectMessage(dm.getId(), dm);

        return fetchedCommunity;
    }

    public Community updateCommunityProfile(String communityId, MultipartFile file) throws Exception{
        /// Intentionally using the repository's findById method instead
        // of the getOneChannel because the getOneChannel transforms
        // the members list from strings to Members. So we have to use
        // The one that brings the exact form it is in the database which is the good ol
        // findById Method.
        var gottenCommunityExists = communitiesRepository.findById(communityId);
        if(gottenCommunityExists.isEmpty()){
            throw new Exception("Community does not exist");
        }

        var dotenv = Dotenv.load();
        var cloudinary = new Cloudinary(dotenv.get("CLOUDINARY_URL"));

        var params = ObjectUtils.asMap(
                "use_filename", true,
                "unique_filename", false,
                "overwrite", true
        );

        var result = cloudinary.uploader().upload(file.getBytes(), params);

        var secure_url = result.get("secure_url");
        if(secure_url == null){
            throw new Exception("Error Uploading File");
        }

        var gottenCommunity = gottenCommunityExists.get();
        var creator = getCreator(communityId).get();
        gottenCommunity.setCommunityProfile(secure_url.toString());
        communitiesRepository.save(gottenCommunity);
        var fetchedCommunity = getOneCommunity(communityId).get();

        // Once community photo changed,
        // We send a chat indicating that community photo has changed

        // To update all the community's channel's chat websockets that a photo changed
        for(var rawChannels : fetchedCommunity.getChannels()){
            var channel = (Channel) rawChannels;
            var chat = new Chat();
            chat.setSenderProfile(creator.getProfile());
            chat.setSenderId(creator.getId());
            chat.setTimestamp(LocalDateTime.now());
            chat.setMessageType(MessageType.update);
            chat.setAttachment(secure_url.toString());
            chat.setAttachmentType(AttachmentType.image);
            chat.setMessage("Community's photo was changed");
            chat.setReadReceipt(List.of(creator.getId()));
            chat.setDmId(channel.getId());
            var savedChat = mongoTemplate.save(chat, "chats");
            messagingTemplate.convertAndSend("/chats/" + channel.getId(), new ApiResponse("Community Photo Updated", savedChat));

            // Once the chat is sent,
            // We're going to send the updated corresponding DM of this channel (If it exists)
            // to each channel's recipient.
            var dmExists = mongoTemplate.exists(Query.query(Criteria.where("_id").is(channel.getId())), DirectMessage.class, "direct messages");
            if(!dmExists){
                continue;
            }

            var response  = new ApiResponse();
            response.setMessage("Community Profile Updated");
            var channelMembers = channel.getMembers().stream().map(o -> (User)o).toList();
            for(var member : channelMembers){
                response.setData(directMessageService.getOneDirectMessage(channel.getId(), member.getId()));
                messagingTemplate.convertAndSend("/dms/"+member.getId(), response);
            }

        }


//        var discussionChannels = fetchedCommunity.getChannels().stream().map(o -> (Channel)o).filter(channel -> channel.getChannelType() == ChannelType.discussions).toList();
//        if(discussionChannels.isEmpty()){
//            return fetchedCommunity;
//        }
//        var discussionChannel = discussionChannels.get(0);
//        fetchedCommunity.setChannels(List.of());
//        discussionChannel.setCommunity(fetchedCommunity);
//
//        // We're going to broadcast the information to all the member's DMs
//        var dm = DirectMessage.fromChannel(discussionChannel);
//        dm.setTime(LocalDateTime.now());
//        directMessageService.updateDirectMessage(dm.getId(), dm);

        return fetchedCommunity;
    }

}
