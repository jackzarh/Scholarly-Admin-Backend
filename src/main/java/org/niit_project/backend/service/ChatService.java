package org.niit_project.backend.service;

import org.niit_project.backend.models.ApiException;
import org.niit_project.backend.dto.ApiResponse;
import org.niit_project.backend.entities.*;
import org.niit_project.backend.enums.MessageType;
import org.niit_project.backend.repository.ChatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
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
    private DirectMessageService directMessageService;

    @Autowired
    private ChannelService channelService;

    @Autowired
    private MongoTemplate mongoTemplate;

    public Chat startChat(String userId, String recipientId) throws Exception{
        // First of all we have to know if the users exist exists.
        var query = Query.query(Criteria.where("_id").in(userId, recipientId));
        var studentExists = mongoTemplate.exists(query, "students");
        var adminExists = mongoTemplate.exists(query, "admins");

        // Check and ensure users exists
        if(!studentExists && !adminExists){
            throw new ApiException("User does not exist", HttpStatus.NOT_FOUND);
        }

        var dm = new DirectMessage();
        dm.setRecipients(List.of(userId, recipientId));
        dm.setTime(LocalDateTime.now());
        var createdDM = directMessageService.createDirectMessage(dm);

        var createdNewDMChat = new Chat();
        createdNewDMChat.setMessage("The Beginning of your legendary discussions");
        createdNewDMChat.setMessageType(MessageType.create);

        var createdChat = createChat(createdNewDMChat, createdDM.getId(), userId);

        // Assuming the recipients are Strings, Since they are...
        var response = new ApiResponse();
        response.setMessage("Created DM");
        for(var recipient : dm.getRecipients()){
            response.setData(directMessageService.getOneDirectMessage(createdDM.getId(), recipient.toString()));
            messagingTemplate.convertAndSend("/dms/" + recipient, response);
        }


        return createdChat;



    }

    public Chat createChat(Chat chat,final String dmId, String senderId) throws Exception{
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

        // We check if the DM exists
        var dm = mongoTemplate.findOne(Query.query(Criteria.where("_id").is(dmId)), DirectMessage.class,"direct messages");
        if(!dm.getRecipients().contains(senderId)){
            throw new Exception("Member is not part of dm");
        }

//        var memberProfile = dm.getMembers().stream().map(o-> (Member)o).filter(member1 -> member1.getId().equals(senderId)).toList().get(0);

        chat.setDmId(dmId);
        chat.setSenderId(senderId);
//        chat.setSenderProfile(member.getProfile());
        chat.setReadReceipt(List.of(senderId));

        var savedChat = chatRepository.save(chat);

        /// To update the websocket that a new chat has been added
        var chatsResponse = new ApiResponse("Sent Chats", savedChat);
        messagingTemplate.convertAndSend("/chats/" + dmId, chatsResponse);

        // To update the dms websocket that a new chat has been added
        var dmResponse = new ApiResponse();
        dmResponse.setMessage("Chat Sent To DM");
        var recipients = dm.getRecipients().stream().map(Object::toString).toList();
        for(var recipient : recipients){
            var updatedDM = directMessageService.getOneDirectMessage(dmId, recipient);
            dmResponse.setData(updatedDM);
            messagingTemplate.convertAndSend("/dms/" + recipient, dmResponse);
        }

        return savedChat;
    }

    public List<Chat> getChats(String dmId) throws Exception{

        var dmExists = mongoTemplate.exists(Query.query(Criteria.where("_id").is(dmId)), "direct messages");

        if(!dmExists){
            throw new Exception("DM Doesn't exist");
        }


        /// Normal Chat Aggregation Pipelines
        var matchPipeline = Aggregation.match(Criteria.where("dmId").is(dmId));
        var sortPipeline = Aggregation.sort(Sort.by("timestamp"));
        var aggregation = Aggregation.newAggregation(matchPipeline, sortPipeline);



        /// Perform the chat aggregation
        var results = mongoTemplate.aggregate(aggregation,"chats" , Chat.class).getMappedResults();

        return results;
    }

    public Optional<Chat> getCompactChat(String chatId){
        return chatRepository.findById(chatId);
    }

    public Integer getUnseenChatsCount(String dmId, String memberId){
        /// Aggregate Chats that belong to this channel
        /// And have not been read by this member
        var matchPipeline = Aggregation.match(
                new Criteria().andOperator(
                        Criteria.where("dmId").is(dmId),
                        Criteria.where("senderId").ne(memberId),
                        Criteria.where("readReceipt").nin(memberId)
                )
        );
        var aggregation = Aggregation.newAggregation(matchPipeline);

        var results = mongoTemplate.aggregate(aggregation, "chats", Chat.class).getMappedResults();

       return results.size();
    }

    public Chat markChatAsRead(String userId, String dmId, String chatId) throws Exception{
        var chat = getCompactChat(chatId);

        // We then get the channel and get all it's recipients
        var dm = mongoTemplate.findOne(Query.query(Criteria.where("_id").is(dmId)), DirectMessage.class,"direct messages");
        var recipients = dm.getRecipients().stream().map(Object::toString).toList();

        // Secondly, we make sure the member is a part of the channel
        if(!recipients.contains(userId)){
            throw new Exception("This user is not part of this DM");
        }

        // We also make sure that the chat itself also exists.
        if(chat.isEmpty()){
            throw new Exception("Chat Not Found");
        }

        // We get the chats, and also it's read receipts.
        var gottenChat = chat.get();
        gottenChat.setId(chatId);
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
        messagingTemplate.convertAndSend("/chats/" + dmId, response);


        /**
         * If the dm is a personal dm (one-to-one), we would update the dms websocket of all (the two)
         * the recipients. Else, we would just update the websocket of the reader of the message.
         */
        var receiptRecipients = dm.getCommunity() != null? List.of(userId): recipients;
        response.setMessage("Chat marked as read successfully");
        for(var recipient: receiptRecipients){
            response.setData(directMessageService.getOneDirectMessage(recipient));
            messagingTemplate.convertAndSend("/dms/" + recipient, response);
        }

        return savedChat;
    }

    public Optional<Chat> getLastChat(String dmId){
        var getChannel = channelService.getCompactChannel(dmId);

        if(getChannel.isEmpty()){
            return Optional.empty();
        }

        /// Aggregate Chats and Get the Latest One.
        var matchPipeline = Aggregation.match(Criteria.where("dmId").is(dmId));
        var limitPipeline = Aggregation.limit(1);
        var sortPipeline = Aggregation.sort(Sort.by(Sort.Direction.DESC, "_id"));
        var aggregation = Aggregation.newAggregation(matchPipeline, sortPipeline, limitPipeline);

        var results = mongoTemplate.aggregate(aggregation, "chats", Chat.class).getMappedResults();

        if(results.isEmpty()){
            return Optional.empty();
        }
        return Optional.of(results.get(0));
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
