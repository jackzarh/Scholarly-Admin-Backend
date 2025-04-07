package org.niit_project.backend.controller;

import org.niit_project.backend.dto.ApiResponse;
import org.niit_project.backend.service.AdminService;
import org.niit_project.backend.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketStudentController {

    @Autowired
    private StudentService studentService;

    @MessageMapping("/getStudents")
    @SendTo("/students")
    public ApiResponse listStudents(){
        var response = new ApiResponse();

        try {
            var students = studentService.getStudents();
            response.setMessage("Gotten Students Successfully");
            response.setData(students);
            return response;
        } catch (Exception e) {
            response.setMessage("Unable to get students");
            return response;
        }
    }
}
