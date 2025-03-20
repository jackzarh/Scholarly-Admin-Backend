package org.niit_project.backend.repository;

import org.niit_project.backend.entities.DirectMessage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DirectMessageRepository extends MongoRepository<DirectMessage, String> {
}
