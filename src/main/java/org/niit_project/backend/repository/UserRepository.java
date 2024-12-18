package org.niit_project.backend.repository;

import org.niit_project.backend.entities.Student;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<Student, String> {

    Optional<Student> findByEmail(String email);
}
