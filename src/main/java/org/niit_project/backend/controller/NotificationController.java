package org.niit_project.backend.controller;

import org.niit_project.backend.dto.ApiResponse;
import org.niit_project.backend.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("scholarly/api/v1/notification")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @PatchMapping("/markAsRead/{id}")
    public ResponseEntity<ApiResponse> markAsRead(@PathVariable("id") String notificationId, @RequestBody boolean read){
        var response = new ApiResponse();

        try{
            var notification = notificationService.markAsRead(notificationId, read);

            response.setMessage("Marked notification as read");
            response.setData(notification);
            return new ResponseEntity<>(response, HttpStatus.OK);


        }
        catch (Exception e){
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }
}
