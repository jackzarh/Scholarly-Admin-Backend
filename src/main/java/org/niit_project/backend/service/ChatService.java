package org.niit_project.backend.service;

import org.niit_project.backend.dto.ApiResponse;
import org.niit_project.backend.entities.*;
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
import java.util.Objects;
import java.util.Optional;

@Service
public class ChatService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private AdminService adminService;

    @Autowired
    private ChannelService channelService;

    @Autowired
    private MongoTemplate mongoTemplate;


    public Chat createChat(Chat chat, String channelId, String memberId) throws Exception{
        /// By default, we set the id to null and the time to the current time
        chat.setId(null);
        chat.setTimestamp(LocalDateTime.now());

        ///We also validate the chat too

        /// If trying to send a message chat and both message and attachment are null,
        /// throw an exception. The exception will be caught though.
        if(chat.getMessage() == null && chat.getAttachment() == null && chat.getMessageType() == MessageType.chat){
            throw new Exception("Both Message and Attachment Cannot be null");
        }

        // If it's a joined or removed message and the message is null,
        // Throw an exception too
        if(chat.getMessage() == null && chat.getMessageType() != MessageType.chat){
            throw new Exception("Teni, Message Cannot be null here");
        }

        // We check if the member and channel exists
        var getChannel = channelService.getCompactChannel(channelId);
        if(getChannel.isEmpty()){
            throw new Exception("Channel Doesn't exist");
        }

        var channel = getChannel.get();
        if(!channel.getMembers().contains(memberId)){
            throw new Exception("Member is not part of channel");
        }

        var member = adminService.getAdmin(memberId).get();

        chat.setChannelId(channelId);
        chat.setSenderId(memberId);
        chat.setSenderProfile(member.getProfile());
        chat.setReadReceipt(List.of(memberId));

        var savedChat = chatRepository.save(chat);

        /// To update the websocket that a new chat has been added
        var chatsResponse = new ApiResponse("Sent Chats", savedChat);
        messagingTemplate.convertAndSend("/chats/" + channelId, chatsResponse);

        var members = channel.getMembers().stream().map(Object::toString).toList();
        for(var membersId : members){
            channel.setLatestMessage(savedChat);
            channel.setUnreadMessages(getUnseenChatsCount(channelId, membersId).orElse(0));

            var channelResponse = new ApiResponse("Chat Sent To Channel", channel);
            messagingTemplate.convertAndSend("/channels/" + memberId, channelResponse);
        }

        return savedChat;
    }

    public List<Chat> getChats(String channelId) throws Exception{

        var getChannel = channelService.getOneChannel(channelId);

        if(getChannel.isEmpty()){
            throw new Exception("Channel Doesn't exist");
        }


        /// Normal Chat Aggregation Pipelines
        var matchPipeline = Aggregation.match(Criteria.where("channelId").is(channelId));
        var sortPipeline = Aggregation.sort(Sort.by("timestamp"));
        var aggregation = Aggregation.newAggregation(matchPipeline, sortPipeline);

        /// Aggregation To get View Receipt as Members
        var members = getChannel.get().getMembers().stream().map(Object::toString).toList();
        var membersMatch = Aggregation.match(Criteria.where("_id").in(members));
        var membersAggregations = Aggregation.newAggregation(membersMatch);

        /// The collate all members
        var studentMembers = mongoTemplate.aggregate(membersAggregations, "users", Student.class).getMappedResults();
        var adminMembers = mongoTemplate.aggregate(membersAggregations, "admins", Admin.class).getMappedResults();
        var allMembers = new ArrayList<>(studentMembers.stream().map(Member::fromStudent).toList());
        allMembers.addAll(adminMembers.stream().map(Member::fromAdmin).toList());

        /// Perform the chat aggregation
        var results = mongoTemplate.aggregate(aggregation,"chats" , Chat.class).getMappedResults();



        /// Convert chats from having list of ids as read receipt
        /// To List of members as read receipt
        /// We filter the readReceipts and then set it to the chats
        var chats = results.stream().map(chat -> {
            /// We filter the readReceipts and then set it to the chats
            var readReceiptsStrings = chat.getReadReceipt().stream().map(Objects::toString).toList();
            var readReceipt = allMembers.stream().filter(member -> readReceiptsStrings.contains(member.getId())).map(member -> (Object) member).toList();

            chat.setReadReceipt(readReceipt);
            return chat;
        }).toList();
        return chats;
    }

    public Optional<Integer> getUnseenChatsCount(String channelId, String memberId){
        /// Aggregate Chats that belong to this channel
        /// And have not been read by this member
        var matchPipeline = Aggregation.match(Criteria.where("channelId").is(channelId).andOperator(Criteria.where("readReceipt").nin(memberId)));
        var aggregation = Aggregation.newAggregation(matchPipeline);

        var results = mongoTemplate.aggregate(aggregation, "chats", Chat.class).getMappedResults();

        if(results.isEmpty()){
            return Optional.empty();
        }
        return Optional.of(results.size());
    }
}
