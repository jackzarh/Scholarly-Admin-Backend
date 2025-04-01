package org.niit_project.backend.service;

import org.niit_project.backend.entities.Course;
import org.niit_project.backend.entities.Student;
import org.niit_project.backend.repository.CourseRepository;
import org.niit_project.backend.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;

    public CourseService(CourseRepository courseRepository, StudentRepository studentRepository) {
        this.courseRepository = courseRepository;
        this.studentRepository = studentRepository;
    }

    public Course createCourse(Course course) throws Exception {
        Optional<Course> existingCourse = courseRepository.findByCourseName(course.getCourseName());
        if (existingCourse.isPresent()) {
            throw new Exception("A course with this name already exists.");
        }
        course.setCreatedAt(LocalDateTime.now());
        return courseRepository.save(course);
    }

    public Optional<Course> getCourseById(String courseId) {
        return courseRepository.findById(courseId);
    }

    public Optional<Course> getCourseByName(String courseName) {
        return courseRepository.findByCourseName(courseName);
    }

    public List<Course> getCoursesByInstructor(String instructorId) {
        return courseRepository.findByInstructorId(instructorId);
    }

    /**
     * Retrieves all batches (courses) for a given instructor.
     * @param instructorId The instructor's ID.
     * @return A list of courses (batches).
     */
    public List<Course> getBatchesForInstructor(String instructorId) {
        return courseRepository.findAllByInstructorId(instructorId);
    }

    public List<Course> getCoursesByStudent(String studentId)  {
        return courseRepository.findByStudentsContaining(studentId);
    }

    public Course updateCourse(String courseId, Course updatedCourse) throws Exception {
        Optional<Course> existingCourse = courseRepository.findById(courseId);
        if (existingCourse.isEmpty()) {
            throw new Exception("Course not found.");
        }
        Course course = existingCourse.get();
        course.setCourseName(updatedCourse.getCourseName());
        course.setInstructorId(updatedCourse.getInstructorId());
        return courseRepository.save(course);
    }

    public void deleteCourse(String courseId) throws Exception {
        if (!courseRepository.existsById(courseId)) {
            throw new Exception("Course not found.");
        }
        courseRepository.deleteById(courseId);
    }

    public void enrollStudentInCourse(String courseId, String studentId) throws Exception {
        Optional<Course> courseOpt = courseRepository.findById(courseId);
        Optional<Student> studentOpt = studentRepository.findById(studentId);

        if (courseOpt.isEmpty()) {
            throw new Exception("Course not found.");
        }
        if (studentOpt.isEmpty()) {
            throw new Exception("Student not found.");
        }

        Course course = courseOpt.get();
        Student student = studentOpt.get();

        if (!course.getStudents().contains(student.getId())) {
            course.addStudent(student);
            courseRepository.save(course);
        } else {
            throw new Exception("Student is already enrolled in this course.");
        }
    }

    public void removeStudentFromCourse(String courseId, String studentId) throws Exception {
        Optional<Course> courseOpt = courseRepository.findById(courseId);
        Optional<Student> studentOpt = studentRepository.findById(studentId);

        if (courseOpt.isEmpty()) {
            throw new Exception("Course not found.");
        }
        if (studentOpt.isEmpty()) {
            throw new Exception("Student not found.");
        }

        Course course = courseOpt.get();
        Student student = studentOpt.get();

        if (course.getStudents().contains(studentId)) {
            course.removeStudent(student);
            courseRepository.save(course);
        } else {
            throw new Exception("Student is not enrolled in this course.");
        }
    }
}
