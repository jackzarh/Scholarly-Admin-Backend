package org.niit_project.backend.controller;

import org.niit_project.backend.dto.ApiResponse;
import org.niit_project.backend.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class WebsocketCourseController {

    @Autowired
    private CourseService courseService;

    @MessageMapping("/getCourses")
    @SendTo("/courses")
    public ApiResponse getAllCourses(){
        var response = new ApiResponse();
        response.setMessage("Got Courses");
        response.setData(courseService.getAllCourses());
        return response;
    }
}
