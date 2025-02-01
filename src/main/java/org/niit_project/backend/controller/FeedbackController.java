package org.niit_project.backend.controller;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.niit_project.backend.dto.ApiResponse;
import org.niit_project.backend.dto.FeedbackDTO;
import org.niit_project.backend.entities.Feedback;
import org.niit_project.backend.service.FeedbackService;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("scholarly/api/v1/feedback")
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    @PostMapping("/createFeedback")
    public ResponseEntity<ApiResponse> createFeedback(@RequestBody Feedback feedback) {
        var response = new ApiResponse();

        try{
            Feedback createdFeedback = feedbackService.createFeedback(feedback);
            response.setMessage("Created Feedback");
            response.setData(createdFeedback);

            return new ResponseEntity<>(response,HttpStatus.OK);

        }
        catch (Exception e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/getOneFeedback/{id}")
    public ResponseEntity<ApiResponse> getOneFeedback(@PathVariable String id) {
        var response = new ApiResponse();

        try{
            var feedback = feedbackService.getOneFeedback(id);
            response.setMessage("Gotten Feedback");
            response.setData(feedback);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/updateFeedback/{id}")
    public ResponseEntity<ApiResponse> updateFeedback(@PathVariable String id, @RequestBody Feedback feedback) {
        var response = new ApiResponse();

        try{
            Feedback updatedFeedback = feedbackService.updateFeedback(id,feedback);
            response.setMessage("Updated Feedback");
            response.setData(updatedFeedback);

            return new ResponseEntity<>(response,HttpStatus.OK);

        }
        catch (Exception e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/deleteFeedback/{id}")
    public ResponseEntity<ApiResponse> deleteFeedback(@PathVariable String id) {
        feedbackService.deleteFeedback(id);
        return new ResponseEntity<>(new ApiResponse("Deleted", null), HttpStatus.OK);
    }

    @GetMapping("/getFeedbacks")
    public ResponseEntity<ApiResponse> getAllFeedbacks() {
        var response = new ApiResponse();

        try{
            var feedbacks = feedbackService.getAllFeedbacks();
            response.setMessage("Got Feedbacks");
            response.setData(feedbacks);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/uploadImage/{id}")
    public ResponseEntity<ApiResponse> uploadImageForFeedback(@PathVariable String id, @RequestPart("file") MultipartFile file) {
        ApiResponse response = new ApiResponse();
        var feedback = feedbackService.getFeedbackById(id);

        if (feedback.isEmpty()) {
            response.setMessage("Feedback doesn't exist");
            return ResponseEntity.status(404).body(response);
        }

        if (file == null || file.isEmpty()) {
            response.setMessage("File should not be empty");
            return ResponseEntity.status(400).body(response);
        }

        try {
            // Cloudinary file upload logic
            var dotenv = Dotenv.load();
            var cloudinary = new Cloudinary(dotenv.get("CLOUDINARY_URL"));
            var params = ObjectUtils.asMap(
                    "use_filename", true,
                    "unique_filename", false,
                    "overwrite", true
            );

            var result = cloudinary.uploader().upload(file.getBytes(), params);
            var secureUrl = result.get("secure_url").toString().trim();

            // Update the feedback record
            Feedback existingFeedback = feedback.get();
            existingFeedback.setImageUrl(secureUrl); // Ensure it updates the evidenceUrl field
            feedbackService.updateFeedback(id, existingFeedback);

            response.setMessage("Image uploaded successfully");
            response.setData(secureUrl);
            return ResponseEntity.status(200).body(response);
        } catch (Exception e) {
            response.setMessage("Error uploading file: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
