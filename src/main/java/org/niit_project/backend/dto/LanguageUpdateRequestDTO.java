package org.niit_project.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.niit_project.backend.enums.Language;

public class LanguageUpdateRequestDTO {
    @NotBlank
    private String userId;
    @NotNull
    private Language language;

    public LanguageUpdateRequestDTO(String userId, Language language) {
        this.userId = userId;
        this.language = language;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }
}
