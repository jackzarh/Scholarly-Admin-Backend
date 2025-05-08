package org.niit_project.backend.repository;

import org.niit_project.backend.entities.FeedbackSetting;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface FeedbackSettingRepository extends MongoRepository<FeedbackSetting,String> {
    Optional<FeedbackSetting> findByFacultyId(String facultyId);
}
