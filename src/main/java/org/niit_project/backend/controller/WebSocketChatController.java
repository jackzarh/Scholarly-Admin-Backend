package org.niit_project.backend.controller;

import org.niit_project.backend.dto.ApiResponse;
import org.niit_project.backend.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketChatController {

    @Autowired
    private ChatService chatService;

    @MessageMapping("/getChats/{dmId}")
    @SendTo("/chats/{dmId}")
    public ApiResponse listChats(@DestinationVariable String dmId){
        var response = new ApiResponse();

        try {
            var chats = chatService.getChats(dmId);
            response.setMessage("Gotten Chats Successfully");
            response.setData(chats);
            return response;
        } catch (Exception e) {
            response.setMessage("Unable to load chats");
            return response;
        }
    }
}
