package org.niit_project.backend.repository;

import org.niit_project.backend.entities.Feedback;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FeedbackRepository extends MongoRepository<Feedback, String> {
}
