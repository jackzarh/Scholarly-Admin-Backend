package org.niit_project.backend.controller;

import org.niit_project.backend.dto.ApiResponse;
import org.niit_project.backend.models.ApiException;
import org.niit_project.backend.service.MenteeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("scholarly/api/v1/mentee")
public class MenteeController {

    @Autowired
    private MenteeService menteeService;

    @GetMapping("/getMentees/{counselorId}")
    public ResponseEntity<ApiResponse> getMentees(@PathVariable String counselorId){
        var response = new ApiResponse();

        try {
            var mentees = menteeService.getMentees(counselorId);
            response.setMessage("Got Mentees Successfully");
            response.setData(mentees);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ApiException e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, e.getStatusCode());
        }
        catch (Exception e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }
}
