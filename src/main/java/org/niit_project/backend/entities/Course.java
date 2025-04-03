package org.niit_project.backend.entities;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
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
    private String courseDescription;
    private String courseProfile;
    private Long recommendedPrice;

    @Transient
    private List<Object> students = new ArrayList<>();

    private LocalDateTime createdAt;


    public Course(){
        this.createdAt = LocalDateTime.now();
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

    public String getCourseDescription() {
        return courseDescription;
    }

    public void setCourseDescription(String courseDescription) {
        this.courseDescription = courseDescription;
    }

    public String getCourseProfile() {
        return courseProfile;
    }

    public void setCourseProfile(String courseProfile) {
        this.courseProfile = courseProfile;
    }

    public Long getRecommendedPrice() {
        return recommendedPrice;
    }

    public void setRecommendedPrice(Long recommendedPrice) {
        this.recommendedPrice = recommendedPrice;
    }

    public List<Object> getStudents() {
        return students;
    }

    public void setStudents(List<Object> students) {
        this.students = students;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
