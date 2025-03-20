package org.niit_project.backend.repository;

import org.niit_project.backend.entities.Community;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommunitiesRepository extends MongoRepository<Community, String> {
}
