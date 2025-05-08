package org.niit_project.backend.service;

import org.niit_project.backend.dto.LanguageUpdateRequestDTO;
import org.niit_project.backend.entities.Admin;
import org.niit_project.backend.entities.Student;
import org.niit_project.backend.enums.Language;
import org.niit_project.backend.models.ApiException;
import org.niit_project.backend.repository.AdminRepository;
import org.niit_project.backend.repository.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LanguageSettingsService {

    private final StudentRepository studentRepository;
    private final AdminRepository adminRepository;

    public LanguageSettingsService(StudentRepository studentRepository, AdminRepository adminRepository) {
        this.studentRepository = studentRepository;
        this.adminRepository = adminRepository;
    }

    /**
     * Change a user's preferred language.
     * @throws ApiException(404) if no Student or Admin ID is found.
     */
    @Transactional
    public void changeUserLanguage (LanguageUpdateRequestDTO languageUpdateRequest) throws ApiException {
        // Try Student
        if (studentRepository.existsById(languageUpdateRequest.getUserId())) {
            Student student = studentRepository.findById(languageUpdateRequest.getUserId()).get();
            student.setPreferredLanguage(languageUpdateRequest.getLanguage());
            studentRepository.save(student);
            return;
        }

        // Try Admin
        if (adminRepository.existsById(languageUpdateRequest.getUserId())) {
            Admin admin = adminRepository.findById(languageUpdateRequest.getUserId()).get();
            admin.setPreferredLanguage(languageUpdateRequest.getLanguage());
            adminRepository.save(admin);
            return;
        }
        throw new ApiException("User not found", HttpStatus.NOT_FOUND);
    }

    //GET endpoint will use this method to read and return the language before any update.
    public Language getPreferredLanguage(String userId) throws ApiException {
        return studentRepository.findById(userId).map(Student::getPreferredLanguage)
                .or(() -> adminRepository.findById(userId).map(Admin::getPreferredLanguage))
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
    }
}
