package org.niit_project.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import io.github.cdimascio.dotenv.Dotenv;
import org.niit_project.backend.dto.FeedbackDTO;
import org.niit_project.backend.entities.Feedback;
import org.niit_project.backend.entities.FeedbackType;
import org.niit_project.backend.repository.FeedbackRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

    public Feedback createFeedback(Feedback feedback) {
        if (feedback == null) {
            throw new IllegalArgumentException("Feedback must not be null");
        }

        feedback.setCreatedAt(LocalDateTime.now());

        if (feedback.getAnonymous() == null) {
            feedback.setAnonymous(false);
        }

        if (Boolean.TRUE.equals(feedback.getAnonymous())) {
            feedback.setReporterName("Anonymous");
        } else {
            if (feedback.getReporterName() == null || feedback.getReporterName().isEmpty()) {
                throw new IllegalArgumentException("Reporter name must be provided for non-anonymous feedback.");
            }
        }

        return feedbackRepository.save(feedback);
    }

    public Optional<Feedback> getFeedbackById(String id) {
        return feedbackRepository.findById(id);
    }

    public Iterable<Feedback> getAllFeedbacks() {
        return feedbackRepository.findAll();
    }

    public Optional<Feedback> updateFeedback(String id, Feedback updatedFeedback) {
        if (updatedFeedback == null) {
            throw new IllegalArgumentException("Updated feedback must not be null");
        }

        Optional<Feedback> existingFeedbackOptional = feedbackRepository.findById(id);

        if (existingFeedbackOptional.isPresent()) {
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
                if (updatedFeedback.getAnonymous()) {
                    existingFeedback.setReporterName("Anonymous");
                } else {
                    existingFeedback.setReporterName(updatedFeedback.getReporterName());
                }
            }

            if (updatedFeedback.getPerpetrator() != null) {
                existingFeedback.setPerpetrator(updatedFeedback.getPerpetrator());
            }

            // Save and return the updated feedback
            return Optional.of(feedbackRepository.save(existingFeedback));
        } else {
            return Optional.empty();
        }
    }

    public void deleteFeedback(String id) {
        feedbackRepository.deleteById(id);
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

    public List<FeedbackDTO> getCompactFeedbacks() {
        Iterable<Feedback> allFeedbacks = feedbackRepository.findAll();
        return convertToCompactDTO(allFeedbacks);
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
