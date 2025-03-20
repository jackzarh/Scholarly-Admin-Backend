package org.niit_project.backend.controller;

import org.niit_project.backend.dto.ApiResponse;
import org.niit_project.backend.service.DirectMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketDirectMessagesController {

    @Autowired
    private DirectMessageService directMessageService;

    @MessageMapping("/getDirectMessages/{userId}")
    @SendTo("/dms/{userId}")
    public ApiResponse listDirectMessages(@DestinationVariable String userId){
        var response = new ApiResponse();

        try {
            var directMessages = directMessageService.getDirectMessages(userId);
            response.setMessage("Gotten DMs Successfully");
            response.setData(directMessages);
            return response;
        } catch (Exception e) {
            response.setMessage("Unable to get DMs");
            return response;
        }
    }
}
