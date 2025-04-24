package org.niit_project.backend.service;

import io.getstream.exceptions.StreamException;
import io.getstream.models.CallRequest;
import io.getstream.models.EndCallRequest;
import io.getstream.models.GetOrCreateCallRequest;
import io.getstream.models.MemberRequest;
import io.getstream.services.Call;
import io.getstream.services.framework.StreamSDKClient;
import org.niit_project.backend.entities.Meet;
import org.niit_project.backend.enums.MeetType;
import org.niit_project.backend.repository.MeetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class MeetService {


    @Autowired
    private MeetRepository meetRepo;

    @Autowired
    private StreamSDKClient client;


    public void createCall(MeetType meetType, String id, String creatorId) throws StreamException {
        // Generating random call id token
        var callID  = UUID.randomUUID().toString();
        var participants = getCallParticipants(meetType, id);

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
        meet.setSourceId(id);
        meet.setCallerId(creatorId);
        meet.setType(meetType);
        meet.setParticipants(participants.stream().map(memberRequest -> memberRequest.getUserID()).toList());
        meetRepo.save(meet);

        // Send A Chat Indicating call created;

    }

    private List<MemberRequest> getCallParticipants(MeetType meetType, String id){
        List<MemberRequest> list = List.of();

        return list;
    }

    public void endCall(EndCallRequest request){

    }

    public void leaveCall(){ }

    public void sendMessage(){}

    public void addMember(){}

    public void listCalls(){

    }
}
