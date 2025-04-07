package org.niit_project.backend.controller;

import org.niit_project.backend.dto.ApiResponse;
import org.niit_project.backend.enums.AttachmentType;
import org.niit_project.backend.models.ApiException;
import org.niit_project.backend.service.CloudinaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/scholarly/api/v1/upload")
public class CloudinaryController {

    @Autowired
    private CloudinaryService cloudinaryService;

    @PostMapping(value = "/uploadFile", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadFile(@RequestPart(required = true) MultipartFile file, @RequestPart(value = "type", required = true) String type){
        var response = new ApiResponse();
        try {
            var url = cloudinaryService.uploadFile(file, AttachmentType.valueOf(type));
            response.setMessage("Uploaded File Successfully");
            response.setData(url);
            return ResponseEntity.ok(response);
        }
        catch (ApiException e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, e.getStatusCode());
        }
        catch (Exception e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }
}
