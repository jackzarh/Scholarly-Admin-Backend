package org.niit_project.backend.repository;

import org.niit_project.backend.entities.Course;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CourseRepository extends MongoRepository<Course, String> {
}
