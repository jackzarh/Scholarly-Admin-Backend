package org.niit_project.backend.controller;

import org.niit_project.backend.dto.ApiResponse;
import org.niit_project.backend.service.FeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketFeedbacksController {

    @Autowired
    private FeedbackService feedbackService;

    @MessageMapping("/getFeedbacks")
    @SendTo("/announcements")
    public ApiResponse listAnnouncements(){
        var response = new ApiResponse();

        try {
            var channels = feedbackService.getFeedbacks();
            response.setMessage("Gotten Feedbacks Successfully");
            response.setData(channels);
            return response;
        } catch (Exception e) {
            response.setMessage("Unable to get feedbacks");
            return response;
        }
    }
}
