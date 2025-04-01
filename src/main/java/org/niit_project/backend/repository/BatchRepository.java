package org.niit_project.backend.repository;

import org.niit_project.backend.enums.AdminRole;
import org.niit_project.backend.entities.Batch;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface BatchRepository extends MongoRepository<Batch, String> {

    // Find all batches where a given student ID exists in the members list
    List<Batch> findByMembersContaining(String studentId);
}
