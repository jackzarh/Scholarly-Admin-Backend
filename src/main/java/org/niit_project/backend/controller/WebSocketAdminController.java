package org.niit_project.backend.controller;

import org.niit_project.backend.dto.ApiResponse;
import org.niit_project.backend.service.AdminService;
import org.niit_project.backend.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketAdminController {

    @Autowired
    private AdminService adminService;

    @MessageMapping("/getAdmins")
    @SendTo("/admins")
    public ApiResponse listAdmins(){
        var response = new ApiResponse();

        try {
            var channels = adminService.getAllAdmins();
            response.setMessage("Gotten Admins Successfully");
            response.setData(channels);
            return response;
        } catch (Exception e) {
            response.setMessage("Unable to get admins");
            return response;
        }
    }
}
