package org.niit_project.backend.controller;

import org.niit_project.backend.dto.ApiResponse;
import org.niit_project.backend.models.ApiException;
import org.niit_project.backend.service.BatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class WebsocketBatchController {

    @Autowired
    private BatchService batchService;

    @MessageMapping("/getBatches")
    @SendTo("/batches")
    public ApiResponse getAllBatches(){
        var response = new ApiResponse();
        response.setMessage("Got Batches");
        response.setData(batchService.getAllBatches());
        return response;
    }

    @MessageMapping("/getFacultyBatches/{id}")
    @SendTo("/my-batches/{id}")
    public ApiResponse getFacultyBatches(@DestinationVariable String id){
        var response = new ApiResponse();
        response.setMessage("Got Batches");
        try {
            response.setData(batchService.getBatchesForFaculty(id));
        } catch (ApiException e) {
            response.setMessage(e.getMessage());
        }
        return response;
    }
}
