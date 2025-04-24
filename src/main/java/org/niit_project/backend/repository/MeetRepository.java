package org.niit_project.backend.repository;

import org.niit_project.backend.entities.Meet;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MeetRepository extends MongoRepository<Meet, String> {
}
