package org.niit_project.backend.repository;

import org.niit_project.backend.entities.Course;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends MongoRepository<Course, String> {
    Optional<Course> findByCourseName(String name); //Finds a course by its name.
}
