package org.niit_project.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import io.github.cdimascio.dotenv.Dotenv;
import org.niit_project.backend.dto.ApiResponse;
import org.niit_project.backend.dto.FeedbackDTO;
import org.niit_project.backend.entities.Feedback;
import org.niit_project.backend.enums.FeedbackType;
import org.niit_project.backend.entities.Student;
import org.niit_project.backend.repository.FeedbackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FeedbackService {

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MongoTemplate mongoTemplate;

    public Feedback createFeedback(Feedback feedback) throws Exception {
        if (feedback == null) {
            throw new IllegalArgumentException("Feedback must not be null");
        }

        feedback.setCreatedAt(LocalDateTime.now());

        if (feedback.getAnonymous() == null) {
            feedback.setAnonymous(false);
        }

        var savedFeedback = feedbackRepository.save(feedback);

        messagingTemplate.convertAndSend("/feedbacks", new ApiResponse("Created Feedback successfully", getOneFeedback(savedFeedback.getId())));

        return savedFeedback;
    }

    public Optional<Feedback> getFeedbackById(String id) {
        return feedbackRepository.findById(id);
    }

    public Iterable<Feedback> getAllFeedbacks() {
        return feedbackRepository.findAll();
    }

    public Feedback updateFeedback(String id, Feedback updatedFeedback) throws Exception {
        if (updatedFeedback == null) {
            throw new IllegalArgumentException("Updated feedback must not be null");
        }

        var existingFeedbackOptional = feedbackRepository.findById(id);

        if(existingFeedbackOptional.isEmpty()){
            throw new Exception("Feedback doesn't exist");
        }

        Feedback existingFeedback = existingFeedbackOptional.get();

        // Update fields only if they are not null
        if (updatedFeedback.getDescription() != null) {
            existingFeedback.setDescription(updatedFeedback.getDescription());
        }
        if (updatedFeedback.getEvidenceUrl() != null) {
            existingFeedback.setEvidenceUrl(updatedFeedback.getEvidenceUrl());
        }
        if (updatedFeedback.getType() != null) {
            existingFeedback.setType(updatedFeedback.getType());
        }

        // Handle anonymous feedback
        if (updatedFeedback.getAnonymous() != null) {
            existingFeedback.setAnonymous(updatedFeedback.getAnonymous());
        }

        if (updatedFeedback.getPerpetrator() != null) {
            existingFeedback.setPerpetrator(updatedFeedback.getPerpetrator());
        }

        var justUpdatedFeedback = feedbackRepository.save(existingFeedback);

        messagingTemplate.convertAndSend("/feedbacks", new ApiResponse("Created Feedback successfully", getOneFeedback(justUpdatedFeedback.getId())));


        // Save and return the updated feedback
        return justUpdatedFeedback;
    }

    public void deleteFeedback(String id) {
        feedbackRepository.deleteById(id);
    }

    public Feedback getOneFeedback(String id) throws Exception{
        var exists = feedbackRepository.findById(id);

        if(exists.isEmpty()){
            throw new Exception("Feedback doesn't exist");
        }

        var feedback = exists.get();

        var match = Aggregation.match(Criteria.where("_id").is(feedback.getPerpetrator()));
        var aggregation = Aggregation.newAggregation(match);
        var perpetrator = mongoTemplate.aggregate(aggregation, "students", Student.class).getUniqueMappedResult();
        feedback.setPerpetrator(perpetrator);

        match = Aggregation.match(Criteria.where("_id").is(feedback.getReporter()));
        aggregation = Aggregation.newAggregation(match);
        var reporter = mongoTemplate.aggregate(aggregation, "students", Student.class).getUniqueMappedResult();
        feedback.setPerpetrator(reporter);

        return feedback;
    }

    public Feedback processFeedback(Feedback feedback) {
        if (feedback.getType() == null) {
            throw new IllegalArgumentException("Feedback type must not be null");
        }

        if (feedback.getType() == FeedbackType.REPORT) {
            // Special handling for report feedback (example, logging, notifying, etc.)
            System.out.println("Processing report feedback...");
        }

        return feedback;
    }

    public List<Feedback> getFeedbacks()  {
        var allFeedbacks = feedbackRepository.findAll().stream();
        var formed = allFeedbacks.peek(feedback -> {
            var match = Aggregation.match(Criteria.where("_id").is(feedback.getPerpetrator()));
            var aggregation = Aggregation.newAggregation(match);
            var perpetrator = mongoTemplate.aggregate(aggregation, "students", Student.class).getUniqueMappedResult();
            feedback.setPerpetrator(perpetrator);

            match = Aggregation.match(Criteria.where("_id").is(feedback.getReporter()));
            aggregation = Aggregation.newAggregation(match);
            var reporter = mongoTemplate.aggregate(aggregation, "students", Student.class).getUniqueMappedResult();
            feedback.setPerpetrator(reporter);
        }).toList();

        return formed;
    }

    private List<FeedbackDTO> convertToCompactDTO(Iterable<Feedback> feedbacks) {
        return ((List<Feedback>) feedbacks).stream()
                .map(feedback -> new FeedbackDTO(
                        feedback.getId(),
                        feedback.getType().name(),
                        feedback.getDescription(),
                        feedback.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    public String handleEvidence(String evidence, MultipartFile file) {
        if (file != null && !file.isEmpty()) {
            try {
                // Load environment variables from .env file
                var dotenv = Dotenv.load();
                var cloudinary = new Cloudinary(dotenv.get("CLOUDINARY_URL"));
                var params = ObjectUtils.asMap(
                        "use_filename", true,
                        "unique_filename", false,
                        "overwrite", true
                );

                // Upload file to Cloudinary and get the URL
                var result = cloudinary.uploader().upload(file.getBytes(), params);
                String cloudinaryUrl = result.get("secure_url").toString().trim();
                return cloudinaryUrl;  // Return the Cloudinary URL after successful upload

            } catch (IOException e) {
                throw new RuntimeException("Failed to upload file to Cloudinary", e);
            }
        } else if (evidence != null && !evidence.isEmpty()) {
            // If evidence is already a URL, return it as is
            return evidence;
        } else {
            throw new IllegalArgumentException("Either a valid file or a URL for evidence must be provided.");
        }
    }
}
