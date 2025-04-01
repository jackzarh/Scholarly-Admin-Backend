package org.niit_project.backend.controller;

import org.niit_project.backend.dto.ApiResponse;
import org.niit_project.backend.models.ApiException;
import org.niit_project.backend.service.MenteeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketMenteesController {

    @Autowired
    private MenteeService menteeService;


    @MessageMapping("/getMentees/{counselorId}")
    @SendTo("/mentees/{counselorId}")
    public ApiResponse getMentees(@DestinationVariable String counselorId){
        var response = new ApiResponse();

        try {
            var mentees = menteeService.getMentees(counselorId);
            response.setMessage("Got Mentees Successfully");
            response.setData(mentees);
            return response;
        } catch (Exception e) {
            response.setMessage(e.getMessage());
            return response;
        }
    }
}
