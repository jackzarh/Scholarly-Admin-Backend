package org.niit_project.backend.controller;

import org.niit_project.backend.dto.ApiResponse;
import org.niit_project.backend.entities.Notification;
import org.niit_project.backend.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import java.util.List;

@Controller
public class WebSocketNotificationController {

    @Autowired
    private NotificationService notificationService;

    @SendTo("/notifications/{userId}")
    @MessageMapping("/getNotifications/{userId}")
    public ApiResponse getNotifications(@DestinationVariable String userId){
        var response = new ApiResponse();
        response.setMessage("Gotten notifications");
        response.setData(notificationService.getUserNotification(userId));
        return response;
    }




}
