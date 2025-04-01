package org.niit_project.backend.repository;

import org.niit_project.backend.entities.Course;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends MongoRepository<Course, String> {
    Optional<Course> findByCourseName(String name); //Finds a course by its name.

    List<Course> findByInstructorId(String instructorId); //Retrieves all courses taught by a specific instructor.

    List<Course> findByStudentsContaining(String studentId); //Finds all courses where a given student is enrolled.

    List<Course> findAllByInstructorId(String instructorId); // New method to get batches for an instructor.


}
