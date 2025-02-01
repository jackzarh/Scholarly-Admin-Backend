package org.niit_project.backend.controller;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.niit_project.backend.dto.ApiResponse;
import org.niit_project.backend.dto.FeedbackDTO;
import org.niit_project.backend.entities.Feedback;
import org.niit_project.backend.service.FeedbackService;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.beans.factory.annotation.Autowired;
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

    @PostMapping
    public ResponseEntity<Feedback> createFeedback(@RequestBody Feedback feedback) {
        Feedback createdFeedback = feedbackService.createFeedback(feedback);
        return ResponseEntity.status(201).body(createdFeedback);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Feedback> getFeedback(@PathVariable String id) {
        Optional<Feedback> feedback = feedbackService.getFeedbackById(id);
        return feedback.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Feedback> updateFeedback(@PathVariable String id, @RequestBody Feedback feedback) {
        Optional<Feedback> updatedFeedback = feedbackService.updateFeedback(id, feedback);
        return updatedFeedback.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/compact")
    public ResponseEntity<List<FeedbackDTO>> getCompactFeedbacks() {
        List<FeedbackDTO> compactFeedbacks = feedbackService.getCompactFeedbacks();
        return ResponseEntity.ok(compactFeedbacks);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFeedback(@PathVariable String id) {
        feedbackService.deleteFeedback(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Iterable<Feedback>> getAllFeedbacks() {
        Iterable<Feedback> feedbacks = feedbackService.getAllFeedbacks();
        return ResponseEntity.ok(feedbacks);
    }

    @PostMapping("/{id}/uploadImage")
    public ResponseEntity<String> uploadImageForFeedback(@PathVariable String id, @RequestPart("file") MultipartFile file) {
        ApiResponse response = new ApiResponse();
        var feedback = feedbackService.getFeedbackById(id);

        if (feedback.isEmpty()) {
            response.setMessage("Feedback doesn't exist");
            return ResponseEntity.status(404).body("Feedback not found");
        }

        if (file == null || file.isEmpty()) {
            response.setMessage("File should not be empty");
            return ResponseEntity.status(400).body("File should not be empty");
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

            // After successful upload, update the feedback record with the image URL
            feedback.get().setImageUrl(secureUrl);
            feedbackService.updateFeedback(id, feedback.get());

            // Update the feedback record
            Feedback existingFeedback = feedback.get();
            existingFeedback.setImageUrl(secureUrl); // Ensure it updates the evidenceUrl field
            feedbackService.updateFeedback(id, existingFeedback);

            response.setMessage("Image uploaded successfully");
            response.setData(secureUrl);
            return ResponseEntity.status(200).body("Image uploaded successfully: " + secureUrl);
        } catch (IOException e) {
            response.setMessage("Error uploading file: " + e.getMessage());
            return ResponseEntity.status(500).body("Error uploading image");
        }
    }
}
