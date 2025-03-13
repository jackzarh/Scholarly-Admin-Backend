package org.niit_project.backend.repository;

import org.niit_project.backend.entities.Batch;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BatchRepository extends MongoRepository<Batch, String> {
}
