package org.niit_project.backend.controller;

import org.niit_project.backend.dto.ApiResponse;
import org.niit_project.backend.entities.FeedbackSetting;
import org.niit_project.backend.enums.FeedbackRateLimit;
import org.niit_project.backend.service.FeedbackSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("scholarly/api/v1/feedback-setting")
public class FeedbackSettingController {
    @Autowired private FeedbackSettingService feedbackSettingService;

    @GetMapping("/{facultyId}")
    @PreAuthorize("hasRole('faculty')")
    public ResponseEntity<ApiResponse> get(@PathVariable String facultyId) {
        var limit = feedbackSettingService.getLimit(facultyId);
        return ResponseEntity.ok(new ApiResponse("The Current Feedback Rate Limit is", limit));
    }

    @PutMapping("/{facultyId}")
    @PreAuthorize("hasRole('faculty')")
    public ResponseEntity<ApiResponse> set(
            @PathVariable String facultyId,
            @RequestParam FeedbackRateLimit rate) {
        FeedbackSetting saved = feedbackSettingService.upsert(facultyId, rate);
        return ResponseEntity.ok(new ApiResponse("Updated", saved));
    }
}
