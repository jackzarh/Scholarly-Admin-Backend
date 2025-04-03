package org.niit_project.backend.controller;

import org.niit_project.backend.dto.ApiResponse;
import org.niit_project.backend.entities.Course;
import org.niit_project.backend.models.ApiException;
import org.niit_project.backend.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("scholarly/api/v1/course")
public class CourseController {

    @Autowired
    private CourseService courseService;

    @PostMapping("/createCourse")
    public ResponseEntity<?> createCourse(@RequestBody Course course) {
        var response = new ApiResponse();
        try {
            var savedCourse = courseService.createCourse(course);
            response.setMessage("Course Created");
            response.setData(savedCourse);
            return ResponseEntity.ok(response);
        }catch (ApiException e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, e.getStatusCode());
        } catch (Exception e) {
            response.setMessage(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PatchMapping("/updateCourse/{courseId}")
    public ResponseEntity<?> updateCourse(@PathVariable String courseId, @RequestBody Course course) {
        var response = new ApiResponse();
        try {
            var savedCourse = courseService.updateCourse(courseId, course);
            response.setMessage("Course Updated");
            response.setData(savedCourse);
            return ResponseEntity.ok(response);
        }catch (ApiException e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, e.getStatusCode());
        } catch (Exception e) {
            response.setMessage(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PatchMapping(value = "/updateCoursePhoto/{courseId}", consumes = "multipart/form-data")
    public ResponseEntity<?> updateCoursePhoto(@PathVariable String courseId, @RequestPart("photo")MultipartFile file) {
        var response = new ApiResponse();
        try {
            var savedCourse = courseService.updateCoursePhoto(courseId, file);
            response.setMessage("Course Photo Updated");
            response.setData(savedCourse);
            return ResponseEntity.ok(response);
        }catch (ApiException e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, e.getStatusCode());
        } catch (Exception e) {
            response.setMessage(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }


    @GetMapping("/getOneCourse/{courseId}")
    public ResponseEntity<?> getOneCourse(@PathVariable String courseId) {
        var response = new ApiResponse();
        try {
            var gottenCourse = courseService.getOneCourse(courseId);
            response.setMessage("Got Course");
            response.setData(gottenCourse);
            return ResponseEntity.ok(response);
        }catch (ApiException e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, e.getStatusCode());
        } catch (Exception e) {
            response.setMessage(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/getAllCourses")
    public ResponseEntity<?> getAllCourses(){
        var response = new ApiResponse();
        response.setMessage("Got Courses");
        response.setData(courseService.getAllCourses());
        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/deleteCourse/{courseId}")
    public ResponseEntity<?> deleteCourse(@PathVariable String courseId) {
        var response = new ApiResponse();
        try {
            var savedCourse = courseService.deleteCourse(courseId);
            response.setMessage("Course Deleted");
            response.setData(savedCourse);
            return ResponseEntity.ok(response);
        }catch (ApiException e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, e.getStatusCode());
        } catch (Exception e) {
            response.setMessage(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
