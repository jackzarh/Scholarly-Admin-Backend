package org.niit_project.backend.controller;

import org.niit_project.backend.dto.ApiResponse;
import org.niit_project.backend.entities.Meet;
import org.niit_project.backend.service.MeetService;
import org.niit_project.backend.service.MenteeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class WebSocketMeetsController {

    @Autowired
    private MeetService meetService;


    @MessageMapping("/getMeets/{userId}")
    @SendTo("/meets/{userId}")
    public ApiResponse<List<Meet>> getMentees(@DestinationVariable String userId){
        var response = new ApiResponse<List<Meet>>();

        try {
            var meets = meetService.listCalls(userId);
            response.setMessage("Got Meets Successfully");
            response.setData(meets);
            return response;
        } catch (Exception e) {
            response.setMessage(e.getMessage());
            return response;
        }
    }
}
