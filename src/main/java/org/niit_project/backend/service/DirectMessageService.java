package org.niit_project.backend.service;

import org.niit_project.backend.dto.ApiResponse;
import org.niit_project.backend.entities.*;
import org.niit_project.backend.models.ApiException;
import org.niit_project.backend.models.Delete;
import org.niit_project.backend.models.User;
import org.niit_project.backend.repository.DirectMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
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
        dm.setId(dm.getId());

        //DMs should have at least 1 recipient
        if(dm.getRecipients() == null || dm.getRecipients().isEmpty()){
            throw new Exception("DM must have at least 1 recipient");
        }

        if(dm.getTime() == null){
            dm.setTime(LocalDateTime.now());
        }


        var createdDM = dmRepository.save(dm);
        //Send new DM created to the DMs websocket;

        var response = new ApiResponse();
        response.setMessage("New Direct Message");
        for(var recipient: createdDM.getRecipients()){
            response.setData(getOneDirectMessage(createdDM.getId(), recipient.toString()));
            messagingTemplate.convertAndSend("/dms/"+recipient.toString(), response);
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
        var oldDM = dmRepository.findById(dmId).orElseThrow(() -> new Exception("DM doesn't exist"));

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


        for(var recipient: updatedDM.getRecipients()){
            var response = new ApiResponse();
            response.setMessage("Updated Direct Message");
            response.setData(getOneDirectMessage(updatedDM.getId(), recipient.toString()));
            messagingTemplate.convertAndSend("/dms/"+recipient.toString(), response);
        }

        return updatedDM;
    }

    /**
     * Service method to add member to DMs.
     * After it's saved to the DM, the DM is sent to the websocket of all recipients.
     * @throws Exception throws exceptions if these conditions are not met or an unprecedented error occurred.
     */
    public DirectMessage addMember(String userId, String dmId) throws Exception{
        var dm = dmRepository.findById(dmId).orElseThrow(() -> new Exception("DM doesn't exist"));

        if(dm.getRecipients().contains(userId)){
            throw new Exception("User is already part of DM");
        }

        var recipients = new ArrayList<>(dm.getRecipients());
        recipients.add(userId);

        dm.setRecipients(recipients);
        return updateDirectMessage(dmId, dm);

    }

    /**
     * Service method to remove member from DMs.
     * After it's saved to the DM, the DM is sent to the websocket of all recipients.
     * @throws Exception throws exceptions if these conditions are not met or an unprecedented error occurred.
     */
    public DirectMessage removeMember(String userId, String dmId) throws Exception{
        var dm = dmRepository.findById(dmId).orElseThrow(() -> new Exception("DM doesn't exist"));

        if(!dm.getRecipients().contains(userId)){
            throw new Exception("User is not part of DM");
        }

        var recipients = new ArrayList<>(dm.getRecipients());
        recipients.remove(userId);

        dm.setRecipients(recipients);
        return updateDirectMessage(dmId, dm);

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
        var members = new ArrayList<User>();
        var students = mongoTemplate.find(query, Student.class, "students");
        var admins = mongoTemplate.find(query, Admin.class, "admins");
        members.addAll(students.stream().map(User::fromStudent).toList());
        members.addAll(admins.stream().map(User::fromAdmin).toList());
        directMessage.setRecipients(Arrays.asList(members.toArray()));

        // (To know if the DM is a community/channel DM or one-to-one
        var isCommunityDM = mongoTemplate.exists(Query.query(Criteria.where("_id").is(directMessage.getId())), "channels");


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

        // 2. We get and set the full data of members:
        var query = Query.query(Criteria.where("_id").in(directMessage.getRecipients()));
        var members = new ArrayList<User>();
        var students = mongoTemplate.find(query, Student.class, "students");
        var admins = mongoTemplate.find(query, Admin.class, "admins");
        members.addAll(students.stream().map(User::fromStudent).toList());
        members.addAll(admins.stream().map(User::fromAdmin).toList());
        directMessage.setRecipients(Arrays.asList(members.toArray()));

        // (To know if the DM is a community/channel DM or one-to-one
        var isCommunityDM = mongoTemplate.exists(Query.query(Criteria.where("_id").is(directMessage.getId())), "channels");


        // 3. Get the names, latest message, unread message count, color and/or photo of the DM (person, for one-to-one or channel)
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

    public List<User> searchUser(String name) throws Exception{
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

        var members = new ArrayList<User>();
        members.addAll(students.stream().map(User::fromStudent).toList());
        members.addAll(admins.stream().map(User::fromAdmin).toList());
        return members;

    }

    public DirectMessage clearDm(String dmId) throws ApiException{
        var dm = dmRepository.findById(dmId).orElseThrow(() -> new ApiException("Dm not found", HttpStatus.NOT_FOUND));

        // We first clear chats sent to the DM.
        var queryToDelete = Query.query(Criteria.where("dmId").is(dmId));
        mongoTemplate.findAllAndRemove(queryToDelete, "chats");

        // Then we delete the dm.
        dmRepository.deleteById(dmId);

        // The data is sent to the websockets indicating that the DM has been deleted
        var delete = new Delete();
        delete.setId(dmId);
        delete.setDeleted(true);
        for(var recipient : dm.getRecipients()){
            messagingTemplate.convertAndSend("/dms/"+recipient.toString(), delete);
        }

        return dm;
    }
}
