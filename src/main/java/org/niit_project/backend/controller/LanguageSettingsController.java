package org.niit_project.backend.controller;

import jakarta.validation.Valid;
import org.niit_project.backend.dto.ApiResponse;
import org.niit_project.backend.dto.LanguageUpdateRequestDTO;
import org.niit_project.backend.enums.Language;
import org.niit_project.backend.models.ApiException;
import org.niit_project.backend.service.LanguageSettingsService;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@RestController
@RequestMapping("/api/v1/settings/language")
@Validated
public class LanguageSettingsController {

    private final LanguageSettingsService languageSettingsService;
    private final MessageSource messageSource;

    public LanguageSettingsController(LanguageSettingsService languageSettingsService, MessageSource messageSource) {
        this.languageSettingsService = languageSettingsService;
        this.messageSource = messageSource;
    }

    /**
     * GET current preferred language for a given user ID.
     * Returns 404 if user not found.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse> getPreferredLanguage(
            @PathVariable String userId,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {
        try {
            Language lang = languageSettingsService.getPreferredLanguage(userId);
            String message = messageSource.getMessage("language.retrieved.success", null, locale != null ? locale : Locale.ENGLISH);
            return ResponseEntity.ok(new ApiResponse(message, lang));
        } catch (ApiException ex) {
            String errorMessage = messageSource.getMessage("language.retrieve.fail",
                    new Object[]{ex.getMessage()}, locale != null ? locale : Locale.ENGLISH);
            return ResponseEntity.status(ex.getStatusCode()).body(new ApiResponse(errorMessage, null));
        }
    }

    /**
     * PUT update preferred language.
     * Expects JSON:
     * {
     *   "userId": "jack123",
     *   "language": "JAPANESE"
     * }
     */
    @PutMapping
    public ResponseEntity<ApiResponse> updatePreferredLanguage(
            @Valid @RequestBody LanguageUpdateRequestDTO request,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {
        try {
            languageSettingsService.changeUserLanguage(request);
            String message = messageSource.getMessage("language.updated.success", null, locale != null ? locale : Locale.ENGLISH);
            return ResponseEntity.ok(new ApiResponse(message, null));
        } catch (ApiException ex) {
            String errorMessage = messageSource.getMessage("language.update.fail",
                    new Object[]{ex.getMessage()}, locale != null ? locale : Locale.ENGLISH);
            return ResponseEntity.status(ex.getStatusCode()).body(new ApiResponse(errorMessage, null));
        }
    }
}
