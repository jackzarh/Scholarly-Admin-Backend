package org.niit_project.backend.service;

import io.getstream.exceptions.StreamException;
import io.getstream.models.*;
import io.getstream.services.Call;
import io.getstream.services.framework.StreamSDKClient;
import org.niit_project.backend.dto.ApiResponse;
import org.niit_project.backend.entities.*;
import org.niit_project.backend.enums.MeetStatus;
import org.niit_project.backend.enums.MeetType;
import org.niit_project.backend.enums.MessageType;
import org.niit_project.backend.models.ApiException;
import org.niit_project.backend.models.User;
import org.niit_project.backend.repository.MeetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class MeetService {


    @Autowired
    private MeetRepository meetRepo;

    @Autowired
    private StreamSDKClient client;

    @Autowired
    private DirectMessageService directMessageService;

    @Autowired
    private ChatService chatService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MongoTemplate mongoTemplate;


    public Meet createCall(String dmId, String creatorId) throws Exception {
        // Generating random call dmId token
        var callID  = UUID.randomUUID().toString();
        var participants = getCallParticipants(dmId, creatorId);

        // Creating StreamSDK Call
        var call = new Call("default", callID, client.video());
        call.getOrCreate(
                GetOrCreateCallRequest.builder()
                        .data(CallRequest.builder()
                                .createdByID(creatorId)
                                .video(true)
                                .members(participants)
                        .build())
                        .membersLimit(50)
                        .ring(true)
                        .build()
        );


        // Create a Meet, to save as call history.
        var meet = new Meet();
        meet.setId(callID);
        meet.setStartDate(LocalDateTime.now());
        meet.setSourceId(dmId);
        meet.setCallerId(creatorId);
        meet.setStartDate(LocalDateTime.now());
        meet.setStatus(MeetStatus.ongoing);
        meet.setListeners(List.of(creatorId));
        meet.setType(MeetType.dm);
        meet.setParticipants(participants.stream().map(memberRequest -> memberRequest.getUserID()).toList());
        var savedMeet = meetRepo.save(meet);

        // We send new created call to the participant's websockets
        var callResponse = new ApiResponse("Call created", savedMeet);
        for(var participant: meet.getParticipants()){
            messagingTemplate.convertAndSend("/meets/"+participant, callResponse);
        }

        // A Call Chat is sent to the DM.
        var chat = new Chat();
        chat.setId(callID);
        chat.setDmId(dmId);
        chat.setSenderId(creatorId);
        chat.setMessageType(MessageType.call);
        chat.setMessage(callID);
        chatService.createChat(chat, dmId, creatorId);

        return savedMeet;

    }

    public Meet addUserToCall (String callID, String userId) throws Exception{
        var meet = meetRepo.findById(callID).orElseThrow(() -> new ApiException("Meet doesn't exist", HttpStatus.NOT_FOUND));
        var dm = mongoTemplate.findOne(Query.query(Criteria.where("_id").is(meet.getSourceId())), DirectMessage.class,"direct messages");

        var call = new Call("default", callID, client.video());
        //Update the call
        call.updateCallMembers(UpdateCallMembersRequest.builder()
                        .updateMembers(getMembers(List.of(userId), meet.getCallerId(),  dm.getCommunity() == null))
                .build());
        //Ring again
        call.get(GetCallRequest.builder()
                .Ring(true).Notify(true).build());


        var participants = new ArrayList<>(meet.getParticipants());
        if(!meet.getParticipants().contains(userId)){
            participants.add(userId);
        }

        meet.setParticipants(participants);

        var savedMeet = meetRepo.save(meet);
        var response = new ApiResponse<Meet>();
        response.setMessage("Someone was added to the call");
        response.setData(savedMeet);

        for(var participant: participants){
            messagingTemplate.convertAndSend("/meets/"+participant, response);
        }

        return savedMeet;

    }

    public Meet removeUserFromCall (String callID, String userId) throws Exception{
        var meet = meetRepo.findById(callID).orElseThrow(() -> new ApiException("Meet doesn't exist", HttpStatus.NOT_FOUND));
        if(!meet.getParticipants().contains(userId)){
            throw new ApiException("User is not part of the call", HttpStatus.NOT_FOUND);
        }

        var call = new Call("default", callID, client.video());
        //Update the call
        call.updateCallMembers(UpdateCallMembersRequest.builder()
                .removeMembers(List.of(userId))
                .build());
        //Ring again
        call.get(GetCallRequest.builder()
                .Ring(true).Notify(true).build());


//        var participants = new ArrayList<>(meet.getParticipants());
//        participants.remove(userId);
//        meet.setParticipants(participants);
//
//        var savedMeet = meetRepo.save(meet);
//        var response = new ApiResponse<Meet>();
//        response.setMessage("Someone was removed from the call");
//        response.setData(savedMeet);
//
//        for(var participant: participants){
//            messagingTemplate.convertAndSend("/meets/"+participant, response);
//        }

        return meet;

    }

    private List<MemberRequest> getMembers(List<String> userIds, String creatorId,boolean isDM) {
        var students = mongoTemplate.find(Query.query(Criteria.where("_id").in(userIds)), Student.class ,"students");
        var admins = mongoTemplate.find(Query.query(Criteria.where("_id").in(userIds)), Admin.class ,"admins");

        var members = new ArrayList<User>();
        members.addAll(students.stream().map(User::fromStudent).toList());
        members.addAll(admins.stream().map(User::fromAdmin).toList());


        return members.stream().map(user -> MemberRequest.builder().userID(user.getId()).role(isDM? "admin" : Objects.equals(creatorId, user.getId()) || user.getRole() == User.MemberRole.admin? "admin" : "user").custom(getCallUser(user)).build()).toList();
    }

    private List<MemberRequest> getCallParticipants(String id, String creatorId) throws Exception{
        var callDM = directMessageService.getOneDirectMessage(id);
        if(callDM == null){
            throw new ApiException("DM not found", HttpStatus.NOT_FOUND);
        }
        var isDM = callDM.getCommunity() == null;

        var members = new ArrayList<>(callDM.getRecipients().stream().map(o -> (User)o).toList());
        List<MemberRequest> list = members.stream().map(user -> MemberRequest.builder()
                .userID(user.getId())
                .role(isDM? "admin" : Objects.equals(creatorId, user.getId()) || user.getRole() == User.MemberRole.admin? "admin" : "user")
                .custom(getCallUser(user))
                .build())
                .toList();


        return list;
    }

    private Map<String, Object> getCallUser(User user){
        Map<String, Object> customMap = new HashMap<>();
        customMap.put("color", user.getColor());
        customMap.put("firstName", user.getFirstName());
        customMap.put("lastName", user.getLastName());
        customMap.put("profile", user.getProfile());

        return customMap;
    }

    public Meet endCall(String callId, String userId) throws Exception{
        var call = new Call("default", callId, client.video());
        var meet = meetRepo.findById(callId).orElseThrow(() -> new ApiException("Call not found", HttpStatus.NOT_FOUND));

//        if(!Objects.equals(meet.getCallerId(), userId)){
//            throw new ApiException("User does not have permission to end call", HttpStatus.UNAUTHORIZED);
//        }

        call.end();
        meet.setStatus(MeetStatus.ended);
        meet.setListeners(List.of());
        meet.setEndDate(LocalDateTime.now());

        var savedMeet = meetRepo.save(meet);
        var response = new ApiResponse<Meet>();
        response.setMessage("Meet has ended");
        response.setData(savedMeet);
        for(var participant: meet.getParticipants()){
            messagingTemplate.convertAndSend("/meet/"+participant, response);
        }

        return meet;
    }

    // Not complete yet
    public Meet leaveCall(String callId, String userId) throws Exception{
        var meet = meetRepo.findById(callId).orElseThrow(() -> new ApiException("Meet not found", HttpStatus.NOT_FOUND));

        var listeners = meet.getListeners();
        listeners.remove(userId);
        meet.setListeners(listeners);
        var savedMeet = meetRepo.save(meet);
        var response = new ApiResponse<Meet>();
        response.setMessage("Someone left call");
        response.setData(savedMeet);
        for(var participant: meet.getParticipants()){
            messagingTemplate.convertAndSend("/meets/"+participant, response);
        }

        return meet;
    }

    // Not complete yet
    public Meet joinCall(String callId, String userId) throws Exception{
        var meet = meetRepo.findById(callId).orElseThrow(() -> new ApiException("Meet not found", HttpStatus.NOT_FOUND));

        var listeners = meet.getListeners();
        listeners.add(userId);
        meet.setListeners(listeners);
        var savedMeet = meetRepo.save(meet);
        var response = new ApiResponse<Meet>();
        response.setMessage("Some joined call");
        response.setData(savedMeet);
        for(var participant: meet.getParticipants()){
            messagingTemplate.convertAndSend("/meets/"+participant, response);
        }

        return meet;
    }

    public List<Meet> listCalls(String userId){
        var criteria = Criteria.where("participants").in(userId);
        return mongoTemplate.find(Query.query(criteria), Meet.class,"meets");
    }
}
