package org.niit_project.backend.entities;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "courses")
public class Course {

    @Id
    private String id;
    private String courseName;
    private String instructorId;
    private List<String> studentIds = new ArrayList<>();
    private List<String> students = new ArrayList<>();
    private LocalDateTime createdAt;


    public Course(){
        this.createdAt = LocalDateTime.now();
    }

    public Course(String id, String courseName, String instructorId, List<String> studentIds, List<String> students) {
        this.id = id;
        this.courseName = courseName;
        this.instructorId = instructorId;
        this.studentIds = studentIds;
        this.students = students;
        this.createdAt = LocalDateTime.now();
    }

    // Ensure students list is never null
    public List<String> getStudents() {
        if (students == null) {
            students = new ArrayList<>();
        }
        return students;
    }

    // Add a student by ID
    public void addStudent(String studentId) {
        if (!studentIds.contains(studentId)) {
            studentIds.add(studentId);
        }
    }

    // Remove a student by ID
    public void removeStudent(String studentId) {
        studentIds.remove(studentId);
    }

    // Add a student object
    public void addStudent(Student student) {
        if (!students.contains(student.getId())) {
            students.add(student.getId());
        }
    }

    // Remove a student object
    public void removeStudent(Student student) {
        students.remove(student.getId());
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getInstructorId() {return id;}

    public void setInstructorId(String instructorId) {this.instructorId = instructorId;}





}
