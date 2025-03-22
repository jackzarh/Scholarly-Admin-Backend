package org.niit_project.backend.service;

import org.niit_project.backend.dto.ApiResponse;
import org.niit_project.backend.entities.*;
import org.niit_project.backend.repository.DirectMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class DirectMessageService {
    @Autowired
    private DirectMessageRepository dmRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private SideChatService sideChatService;

    @Autowired
    private MongoTemplate mongoTemplate;

    public List<DirectMessage> getDirectMessages(String userId) throws Exception {
        var matchAggregation = Aggregation.match(Criteria.where("recipients").in(userId));
        var ordinaryDMsList = mongoTemplate.aggregate(Aggregation.newAggregation(matchAggregation),"direct messages", DirectMessage.class).getMappedResults();

        var dms = new ArrayList<DirectMessage>();
        for (DirectMessage message : ordinaryDMsList) {
            System.out.println(message);
            DirectMessage apply = getOneDirectMessage(message.getId(), userId);
            dms.add(apply);
        }
        dms.sort((o1, o2) -> o2.getTime().compareTo(o1.getTime()));

        return dms;
    }

    /**
     * Service method to create DMs.
     * Validation checks are first of all made before saving to DB.
     * After it's saved to the DM, the DM is sent to the websocket of all recipients.
     * @param dm the direct message. Must have at least {@code a recipient}
     * @throws Exception throws exceptions if these conditions are not met or an unprecedented error occurred.
     */
    public DirectMessage createDirectMessage(DirectMessage dm) throws Exception{
        // Dms should have names
//        if(dm.getName() == null){
//            throw new Exception("Direct Message must have a name");
//        }

        // DMs should have at least a color
//        if(dm.getProfile() == null){
//            if(dm.getColor() == null){
//                throw new Exception("DM must have at least a color");
//            }
//        }

        //DMs should have at least 1 recipient
        if(dm.getRecipients() == null || dm.getRecipients().isEmpty()){
            throw new Exception("DM must have at least 1 recipient");
        }

        if(dm.getTime() == null){
            dm.setTime(LocalDateTime.now());
        }


        var createdDM = dmRepository.save(dm);
        //Send new DM created to the DMs websocket;

        var response = new ApiResponse("New Direct Message", createdDM);
        for(var recipient: createdDM.getRecipients()){
            messagingTemplate.convertAndSend("/dms/"+recipient, response);
        }

        return createdDM;
    }

    /**
     * Service method to update DMs.
     * After it's saved to the DM, the DM is sent to the websocket of all recipients.
     * @param dm the direct message. Must have a {@code name}, {@code color} and at least {@code 1 recipient}
     * @throws Exception throws exceptions if these conditions are not met or an unprecedented error occurred.
     */
    public DirectMessage updateDirectMessage(String dmId,DirectMessage dm) throws Exception{
        var dmExists = dmRepository.findById(dmId);
        if(dmExists.isEmpty()){
            throw new Exception("The DM doesn't exist");
        }

        var oldDM = dmExists.get();

        if(dm.getName() == null){
            dm.setName(oldDM.getName());
        }


        if(dm.getColor() == null){
            dm.setColor(oldDM.getColor());
        }

        if(dm.getProfile() == null){
            dm.setProfile(oldDM.getProfile());
        }

        if(dm.getRecipients() == null || dm.getRecipients().isEmpty()){
            dm.setRecipients(oldDM.getRecipients());
        }

        if(dm.getTime() == null){
            dm.setTime(oldDM.getTime());
        }

        if(dm.getUnreadMessages() == null){
            dm.setUnreadMessages(0);
        }


        var updatedDM = dmRepository.save(dm);
        //Send new DM created to the DMs websocket;

        var response = new ApiResponse("Updated Direct Message", updatedDM);
        for(var recipient: updatedDM.getRecipients()){
            updatedDM.setUnreadMessages(sideChatService.getUnseenChatsCount(dmId, recipient.toString()));
            messagingTemplate.convertAndSend("/dms/"+recipient, response);
        }

        return updatedDM;
    }

    public DirectMessage getOneDirectMessage(String dmId) throws Exception{

        // 1. We get the DM
        var directMessage = mongoTemplate.findOne(Query.query(Criteria.where("_id").is(dmId)), DirectMessage.class, "direct messages");
        if(directMessage == null){
            throw new Exception("DM doesn't exist");
        }

        // 2. We set the last chat of the DM:
        var lastChatOption = sideChatService.getLastChat(directMessage.getId());
        directMessage.setLatestMessage(lastChatOption.orElse(null));

        // 3. We get the full data of members:
        var query = Query.query(Criteria.where("_id").in(directMessage.getRecipients()));
        var members = new ArrayList<Member>();
        var students = mongoTemplate.find(query, Student.class, "students");
        var admins = mongoTemplate.find(query, Admin.class, "admins");
        members.addAll(students.stream().map(Member::fromStudent).toList());
        members.addAll(admins.stream().map(Member::fromAdmin).toList());
        directMessage.setRecipients(Arrays.asList(members.toArray()));

        // (To know if the DM is a community/channel DM or one-to-one
        var isCommunityDM = mongoTemplate.exists(Query.query(Criteria.where("_id").in(directMessage.getId())), "channels");


        // 4. Get the names, color and/or photo of the DM (only channel/community DMs)
        if(!isCommunityDM){
            return directMessage;
        }

        var channelOfThisDM = mongoTemplate.findById(directMessage.getId(), Channel.class, "channels");
        if(channelOfThisDM == null){
            return directMessage;
        }
        var community = mongoTemplate.findById(channelOfThisDM.getCommunityId(), Community.class, "communities");
        directMessage.setName(channelOfThisDM.getChannelName());
        directMessage.setColor(channelOfThisDM.getColor());
        directMessage.setProfile(channelOfThisDM.getChannelProfile());
        directMessage.setCommunity(community);

        return directMessage;
    }

    public DirectMessage getOneDirectMessage(String dmId, String userId) throws Exception{

        // 1. We get the DM
        var directMessage = mongoTemplate.findOne(Query.query(Criteria.where("_id").is(dmId)), DirectMessage.class, "direct messages");
        if(directMessage == null){
            throw new Exception("DM doesn't exist");
        }

        // 2. We set the last chat of the DM:
        var lastChatOption = sideChatService.getLastChat(directMessage.getId());
        directMessage.setLatestMessage(lastChatOption.orElse(null));

        // 3. We get the full data of members:
        var query = Query.query(Criteria.where("_id").in(directMessage.getRecipients()));
        var members = new ArrayList<Member>();
        var students = mongoTemplate.find(query, Student.class, "students");
        var admins = mongoTemplate.find(query, Admin.class, "admins");
        members.addAll(students.stream().map(Member::fromStudent).toList());
        members.addAll(admins.stream().map(Member::fromAdmin).toList());
        directMessage.setRecipients(Arrays.asList(members.toArray()));

        // (To know if the DM is a community/channel DM or one-to-one
        var isCommunityDM = mongoTemplate.exists(Query.query(Criteria.where("_id").in(directMessage.getId())), "channels");


        // 4. Get the names, latest message, unread message count, color and/or photo of the DM (person, for one-to-one or channel)
        directMessage.setLatestMessage(sideChatService.getLastChat(dmId).orElse(null));
        directMessage.setUnreadMessages(sideChatService.getUnseenChatsCount(dmId, userId));
        if(!isCommunityDM){
            var person = members.stream().filter(member -> !member.getId().equals(userId)).toList().get(0);
            directMessage.setName(person.getFirstName() + " " + person.getLastName());
            directMessage.setProfile(person.getProfile());
            directMessage.setColor(person.getColor().name());
            return directMessage;
        }


        // Since DM is not one-to-one:
        var channelOfThisDM = mongoTemplate.findById(directMessage.getId(), Channel.class, "channels");
        if(channelOfThisDM == null){
            return directMessage;
        }
        var community = mongoTemplate.findById(channelOfThisDM.getCommunityId(), Community.class, "communities");
        directMessage.setCommunity(community);
        directMessage.setName(channelOfThisDM.getChannelName());
        directMessage.setColor(channelOfThisDM.getColor());
        directMessage.setProfile(channelOfThisDM.getChannelProfile());

        return directMessage;
    }

    public List<Member> searchUser(String name) throws Exception{
        var nameParts = name.toLowerCase().split(" ");

        var regexCriteria = Arrays.stream(nameParts)
                .map(part -> new Criteria().orOperator(
                        Criteria.where("firstName").regex(part, "i"),
                        Criteria.where("lastName").regex(part, "i")
                ))
                .toArray(Criteria[]::new);

        var matchOperation = Aggregation.match(new Criteria().orOperator(regexCriteria));
        var aggregation = Aggregation.newAggregation(matchOperation);
        var students = mongoTemplate.aggregate(aggregation, "students", Student.class).getMappedResults();
        var admins = mongoTemplate.aggregate(aggregation, "admins", Admin.class).getMappedResults();

        var members = new ArrayList<Member>();
        members.addAll(students.stream().map(Member::fromStudent).toList());
        members.addAll(admins.stream().map(Member::fromAdmin).toList());
        return members;

    }
}
