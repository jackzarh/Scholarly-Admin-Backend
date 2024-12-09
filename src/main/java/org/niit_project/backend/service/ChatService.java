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
import java.util.*;

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
        var getChannel = channelService.getOneChannel(channelId);
        if(getChannel.isEmpty()){
            throw new Exception("Channel Doesn't exist");
        }

        var channel = getChannel.get();
        if(!channel.getMembers().stream().map(o -> ((Member)o).getId()).toList().contains(memberId)){
            throw new Exception("Member is not part of channel");
        }

        var member = channel.getMembers().stream().map(o-> (Member)o).filter(member1 -> member1.getId().equals(memberId)).toList().get(0);

        chat.setChannelId(channelId);
        chat.setSenderId(memberId);
        chat.setSenderProfile(member.getProfile());
        var readReceipt = new ArrayList<>();
        readReceipt.add(member.getId());
        chat.setReadReceipt(readReceipt);

        var savedChat = chatRepository.save(chat);

        /// To update the websocket that a new chat has been added
        var chatsResponse = new ApiResponse("Sent Chats", savedChat);
        messagingTemplate.convertAndSend("/chats/" + channelId, chatsResponse);

        var members = channel.getMembers().stream().map(o -> ((Member)o).getId()).toList();
        for(var membersId : members){
            channel.setLatestMessage(savedChat);
            channel.setUnreadMessages(getUnseenChatsCount(channelId, membersId).orElse(0));

            var channelResponse = new ApiResponse("Chat Sent To Channel", channel);
            messagingTemplate.convertAndSend("/channels/" + memberId, channelResponse);
        }

        return savedChat;
    }

    public List<Chat> getChats(String channelId) throws Exception{

        var getChannel = channelService.getCompactChannel(channelId);

        if(getChannel.isEmpty()){
            throw new Exception("Channel Doesn't exist");
        }


        /// Normal Chat Aggregation Pipelines
        var matchPipeline = Aggregation.match(Criteria.where("channelId").is(channelId));
        var sortPipeline = Aggregation.sort(Sort.by("timestamp"));
        var aggregation = Aggregation.newAggregation(matchPipeline, sortPipeline);



        /// Perform the chat aggregation
        var results = mongoTemplate.aggregate(aggregation,"chats" , Chat.class).getMappedResults();

        return results;
    }

    public Optional<Chat> getCompactChat(String chatId){
        return chatRepository.findById(chatId);
    }

    public Optional<Integer> getUnseenChatsCount(String channelId, String memberId){
        /// Aggregate Chats that belong to this channel
        /// And have not been read by this member
        var matchPipeline = Aggregation.match(
                new Criteria().andOperator(
                        Criteria.where("channelId").is(channelId),
                        Criteria.where("readReceipt").size(0).not(),
                        Criteria.where("senderId").ne(memberId),
                        Criteria.where("readReceipt").nin(memberId)
                )
        );
        var aggregation = Aggregation.newAggregation(matchPipeline);

        var results = mongoTemplate.aggregate(aggregation, "chats", Chat.class).getMappedResults();

        if(results.isEmpty()){
            return Optional.empty();
        }
        return Optional.of(results.size());
    }

    public Chat markChatAsRead(String userId, String channelId, String chatId) throws Exception{
        var chat = getCompactChat(chatId);
        var channel = channelService.getOneChannel(channelId);

        // First, We make sure the channel exists
        if(channel.isEmpty()){
            throw new Exception("Channel Not Found");
        }

        // We then get the channel and get all it's members
        var gottenChannel = channel.get();
        var members = gottenChannel.getMembers().stream().map(o -> ((Member)o).getId()).toList();

        // Secondly, we make sure the member is a part of the channel
        if(!members.contains(userId)){
            throw new Exception("This member is not part of this channel");
        }

        // We also make sure that the chat itself also exists.
        if(chat.isEmpty()){
            throw new Exception("Chat Not Found");
        }

        // We get the chats and also it's read receipts.
        var gottenChat = chat.get();
        var readReceipt = new ArrayList<Object>(gottenChat.getReadReceipt().stream().map(Object::toString).toList());

        // We then make sure that we're only updating the read receipt if the user hasn't
        // ... read it to prevent redundancy.
        if(readReceipt.contains(userId)){
            return gottenChat;
        }

        // If the member hasn't read the chat, we add he/she to the read receipt
        // ... and then save it in the chat
        readReceipt.add(userId);
        gottenChat.setReadReceipt(readReceipt);

        var savedChat = chatRepository.save(gottenChat);

        // We send the updated chat to the websocket endpoint
        var response = new ApiResponse();
        response.setMessage("Chat has been marked read");
        response.setData(savedChat);
        messagingTemplate.convertAndSend("/chats/" + channelId, response);

        // Then we update the channel websocket of the member who read the message
        response.setMessage("Chat marked as read successfully");
        gottenChannel.setUnreadMessages(0);
        gottenChannel.setLatestMessage(savedChat);
        response.setData(gottenChannel);
        messagingTemplate.convertAndSend("/channels/" + userId, response);


        return savedChat;
    }

//    public Chat markAllChatsAsRead(String userId, String channelId) throws Exception{
//        var chat = getCompactChat(chatId);
//        var channel = channelService.getCompactChannel(channelId);
//
//        // First, We make sure the channel exists
//        if(channel.isEmpty()){
//            throw new Exception("Channel Not Found");
//        }
//
//        // We then get the channel and get all it's members
//        var gottenChannel = channel.get();
//        var members = gottenChannel.getMembers().stream().map(Object::toString).toList();
//
//        // Secondly, we make sure the member is a part of the channel
//        if(!members.contains(userId)){
//            throw new Exception("This member is not part of this channel");
//        }
//
//        // We also make sure that the chat itself also exists.
//        if(chat.isEmpty()){
//            throw new Exception("Chat Not Found");
//        }
//
//        // We get the chats and also it's read receipts.
//        var gottenChat = chat.get();
//        var readReceipt = new ArrayList<String>(gottenChat.getReadReceipt().stream().map(Object::toString).toList());
//
//        // We then make sure that we're only updating the read receipt if the user hasn't
//        // ... read it to prevent redundancy.
//        if(readReceipt.contains(userId)){
//            return gottenChat;
//        }
//
//        // If the member hasn't read the chat, we add he/she to the read receipt
//        // ... and then save it in the chat
//        readReceipt.add(userId);
//        gottenChat.setReadReceipt(Collections.singletonList(readReceipt));
//
//        return chatRepository.save(gottenChat);
//    }
}
