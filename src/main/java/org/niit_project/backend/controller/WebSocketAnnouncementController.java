package org.niit_project.backend.controller;

import org.niit_project.backend.dto.ApiResponse;
import org.niit_project.backend.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketAnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    @MessageMapping("/getAnnouncements/{userId}")
    @SendTo("/announcements/{userId}")
    public ApiResponse listAnnouncements(@DestinationVariable String userId){
        var response = new ApiResponse();

        try {
            var channels = announcementService.getUserAnnouncements(userId);
            response.setMessage("Gotten Announcements Successfully");
            response.setData(channels);
            return response;
        } catch (Exception e) {
            response.setMessage("Unable to get announcements");
            return response;
        }
    }
}
