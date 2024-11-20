package org.niit_project.backend.repository;

import org.niit_project.backend.entities.Student;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<Student, String> {
}
