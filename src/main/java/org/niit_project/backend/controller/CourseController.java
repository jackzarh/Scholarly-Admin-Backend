package org.niit_project.backend.controller;

import org.niit_project.backend.entities.Course;
import org.niit_project.backend.service.CourseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    /**
     * Create a new course.
     */
    @PostMapping
    public ResponseEntity<?> createCourse(@RequestBody Course course) {
        try {
            Course savedCourse = courseService.createCourse(course);
            return ResponseEntity.ok(savedCourse);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Get a course by ID.
     */
    @GetMapping("/{courseId}")
    public ResponseEntity<?> getCourseById(@PathVariable String courseId) {
        Optional<Course> course = courseService.getCourseById(courseId);
        return course.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Get a course by name.
     */
    @GetMapping("/name/{courseName}")
    public ResponseEntity<?> getCourseByName(@PathVariable String courseName) {
        Optional<Course> course = courseService.getCourseByName(courseName);
        return course.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Get courses taught by an instructor.
     */
    @GetMapping("/instructor/{instructorId}")
    public ResponseEntity<List<Course>> getCoursesByInstructor(@PathVariable String instructorId) {
        List<Course> courses = courseService.getCoursesByInstructor(instructorId);
        return ResponseEntity.ok(courses);
    }


    /**
     * Get all batches for an instructor.
     */
    @GetMapping("/instructor/{instructorId}/batches")
    public ResponseEntity<List<Course>> getBatchesForInstructor(@PathVariable String instructorId) {
        List<Course> batches = courseService.getBatchesForInstructor(instructorId);
        return ResponseEntity.ok(batches);
    }

    /**
     * Get courses a student is enrolled in.
     */
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<Course>> getCoursesByStudent(@PathVariable String studentId) {
        List<Course> courses = courseService.getCoursesByStudent(studentId);
        return ResponseEntity.ok(courses);
    }

    @GetMapping("/student/{studentId}/all")
    public ResponseEntity<?> getCoursesForStudent(@PathVariable String studentId) {
        try {
            List<Course> courses = courseService.getCoursesByStudent(studentId);
            return ResponseEntity.ok(courses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Update a course.
     */
    @PutMapping("/{courseId}")
    public ResponseEntity<?> updateCourse(@PathVariable String courseId, @RequestBody Course updatedCourse) {
        try {
            Course course = courseService.updateCourse(courseId, updatedCourse);
            return ResponseEntity.ok(course);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Delete a course.
     */
    @DeleteMapping("/{courseId}")
    public ResponseEntity<?> deleteCourse(@PathVariable String courseId) {
        try {
            courseService.deleteCourse(courseId);
            return ResponseEntity.ok("Course deleted successfully.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Enroll a student in a course.
     */
    @PostMapping("/{courseId}/enroll/{studentId}")
    public ResponseEntity<?> enrollStudent(@PathVariable String courseId, @PathVariable String studentId) {
        try {
            courseService.enrollStudentInCourse(courseId, studentId);
            return ResponseEntity.ok("Student enrolled successfully.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Remove a student from a course.
     */
    @DeleteMapping("/{courseId}/remove/{studentId}")
    public ResponseEntity<?> removeStudent(@PathVariable String courseId, @PathVariable String studentId) {
        try {
            courseService.removeStudentFromCourse(courseId, studentId);
            return ResponseEntity.ok("Student removed from course.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
