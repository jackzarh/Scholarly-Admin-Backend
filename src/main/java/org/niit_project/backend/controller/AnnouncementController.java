package org.niit_project.backend.controller;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import io.github.cdimascio.dotenv.Dotenv;
import org.niit_project.backend.dto.ApiResponse;
import org.niit_project.backend.entities.Announcement;
import org.niit_project.backend.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("scholarly/api/v1/announcement")
public class AnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    @PostMapping("/createAnnouncement/{adminId}")
    public ResponseEntity<ApiResponse> createAnnouncement(@RequestBody Announcement announcement, @PathVariable String adminId){
        var response = new ApiResponse();

        try{
            var createdAnnouncement = announcementService.createAnnouncement(adminId, announcement);
            response.setMessage("Created Announcement Successfully");
            response.setData(createdAnnouncement);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<ApiResponse>(response, HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/updateAnnouncement/{id}")
    public ResponseEntity<ApiResponse> updateAnnouncement(@RequestBody Announcement announcement, @PathVariable("id") String announcementId){
        var response = new ApiResponse();

        try{
            var createdAnnouncement = announcementService.updateAnnouncement(announcementId, announcement);
            response.setMessage("Updated Announcement Successfully");
            response.setData(createdAnnouncement);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<ApiResponse>(response, HttpStatus.NOT_FOUND);
        }
    }

    @PatchMapping(path = "/updateAnnouncementPhoto/{id}", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse> updateAnnouncementPhoto(@PathVariable("id") String announcementId, @RequestPart("photo") MultipartFile file, @RequestPart String announcementTitle, @RequestPart String announcementDescription){
        var response = new ApiResponse();

        var dotenv = Dotenv.load();
        var cloudinary = new Cloudinary(dotenv.get("CLOUDINARY_URL"));

        var params = ObjectUtils.asMap(
                "use_filename", true,
                "unique_filename", false,
                "overwrite", true
        );

        try{
            var result = cloudinary.uploader().upload(file.getBytes(), params);

            var secure_url = result.get("secure_url");
            if(secure_url == null){
                response.setMessage("Error Uploading File");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            var announcement = new Announcement();
            announcement.setAnnouncementTitle(announcementTitle);
            announcement.setAnnouncementDescription(announcementDescription);
            announcement.setAnnouncementPhoto(secure_url.toString());


            var createdAnnouncement = announcementService.updateAnnouncement(announcementId, announcement);

            response.setMessage("Announcement Photo Updated Successfully");
            response.setData(createdAnnouncement);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }
}
