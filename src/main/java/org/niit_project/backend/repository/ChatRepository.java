package org.niit_project.backend.repository;

import org.niit_project.backend.entities.Chat;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatRepository extends MongoRepository<Chat, String> {
}
