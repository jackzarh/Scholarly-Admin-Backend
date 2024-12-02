package org.niit_project.backend.controller;

import org.niit_project.backend.dto.ApiResponse;
import org.niit_project.backend.service.ChannelService;
import org.niit_project.backend.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketChannelController {

    @Autowired
    private ChannelService channelService;

    @MessageMapping("/getChannels/{userId}")
    @SendTo("/channels/{userId}")
    public ApiResponse listChannels(@DestinationVariable String userId){
        var response = new ApiResponse();

        try {
            var channels = channelService.getAdminChannels(userId);
            response.setMessage("Gotten Channels Successfully");
            response.setData(channels);
            return response;
        } catch (Exception e) {
            response.setMessage("Unable to channels");
            return response;
        }
    }
}
