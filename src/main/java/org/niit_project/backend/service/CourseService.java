package org.niit_project.backend.service;

import org.niit_project.backend.dto.ApiResponse;
import org.niit_project.backend.entities.Batch;
import org.niit_project.backend.entities.Course;
import org.niit_project.backend.entities.Student;
import org.niit_project.backend.enums.AttachmentType;
import org.niit_project.backend.models.ApiException;
import org.niit_project.backend.models.Delete;
import org.niit_project.backend.models.User;
import org.niit_project.backend.repository.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CourseService {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private BatchService batchService;

    @Autowired
    private CloudinaryService cloudinaryService;


    public Course createCourse(Course course) throws Exception {

        var existingCourse = courseRepository.findByCourseName(course.getCourseName());
        if (existingCourse.isPresent()) {
            throw new ApiException("A course with this name already exists.", HttpStatus.UNAUTHORIZED);
        }

        if(course.getCourseName() == null){
            throw new ApiException("Course name cannot be null", HttpStatus.BAD_REQUEST);
        }
        if(course.getCourseDescription() == null){
            throw new ApiException("Course description cannot be null", HttpStatus.BAD_REQUEST);
        }
        if(course.getCoursePhoto() == null){
            throw new ApiException("Course profile cannot be null", HttpStatus.BAD_REQUEST);
        }
        if(course.getRecommendedPrice() == null){
            course.setRecommendedPrice(300_000L);
        }

        course.setCreatedAt(LocalDateTime.now());
        var savedCourse = courseRepository.save(course);

        var response = new ApiResponse();
        response.setMessage("New Course Created");
        response.setData(savedCourse);
        messagingTemplate.convertAndSend("/courses", response);

        return savedCourse;
    }

    public Course createCourse(Course course, MultipartFile file) throws Exception {

        var existingCourse = courseRepository.findByCourseName(course.getCourseName());
        if (existingCourse.isPresent()) {
            throw new ApiException("A course with this name already exists.", HttpStatus.UNAUTHORIZED);
        }

        if(course.getCourseName() == null){
            throw new ApiException("Course name cannot be null", HttpStatus.BAD_REQUEST);
        }
        if(course.getCourseDescription() == null){
            throw new ApiException("Course description cannot be null", HttpStatus.BAD_REQUEST);
        }
        if(course.getCoursePhoto() == null){
            throw new ApiException("Course profile cannot be null", HttpStatus.BAD_REQUEST);
        }
        if(course.getRecommendedPrice() == null){
            course.setRecommendedPrice(300_000L);
        }
        if(file == null){
            throw new ApiException("Profile cannot be null", HttpStatus.BAD_REQUEST);
        }

        var profileUrl = cloudinaryService.uploadFile(file, AttachmentType.image);
        course.setCoursePhoto(profileUrl);



        course.setCreatedAt(LocalDateTime.now());
        var savedCourse = courseRepository.save(course);

        var response = new ApiResponse();
        response.setMessage("New Course Created");
        response.setData(savedCourse);
        messagingTemplate.convertAndSend("/courses", response);

        return savedCourse;
    }

    public Course updateCourse(String courseId, Course updatedCourse) throws Exception {
        var course = courseRepository.findById(courseId).orElseThrow(() -> new ApiException("Course not found", HttpStatus.NOT_FOUND));
        if(updatedCourse.getCourseName() != null){
            course.setCourseName(updatedCourse.getCourseName());
        }
        if(updatedCourse.getCourseDescription() != null){
            course.setCourseDescription(updatedCourse.getCourseDescription());
        }
        if(updatedCourse.getRecommendedPrice() != null){
            course.setRecommendedPrice(updatedCourse.getRecommendedPrice());
        }
        var savedCourse = courseRepository.save(course);

        var response = new ApiResponse();
        response.setMessage("Course Updated");
        response.setData(savedCourse);
        messagingTemplate.convertAndSend("/courses", response);

        return savedCourse;
    }

    public List<Course> getAllCourses(){
        var courses = courseRepository.findAll();

        var convertedCourses = courses.stream().peek(course -> {
            var students = new ArrayList<>();
            var match = Aggregation.match(Criteria.where("course").is(course.getId()));
            var aggregation = Aggregation.newAggregation(match);
            var batches = mongoTemplate.aggregate(aggregation, "batches", Batch.class).getMappedResults();

            batches.forEach(batch -> {
                var members = mongoTemplate.find(Query.query(Criteria.where("_id").in(batch.getMembers())), Student.class,"students");
                students.addAll(members.stream().map(User::fromStudent).toList());
            });
            course.setStudents(students);
        }).toList();
        return convertedCourses.stream().sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt())).toList();
    }

    public Course getOneCourse(String courseId) throws Exception{
        var course = courseRepository.findById(courseId).orElseThrow(() -> new ApiException("Course not found", HttpStatus.NOT_FOUND));
        var students = new ArrayList<>();
        var match = Aggregation.match(Criteria.where("course").is(course.getId()));
        var aggregation = Aggregation.newAggregation(match);
        var batches = mongoTemplate.aggregate(aggregation, "batches", Batch.class).getMappedResults();

        batches.forEach(batch -> {
            var members = mongoTemplate.find(Query.query(Criteria.where("_id").in(batch.getMembers())), Student.class,"students");
            students.addAll(members.stream().map(User::fromStudent).toList());
        });
        course.setStudents(students);

        return course;
    }

    public  Course updateCoursePhoto(String courseId, MultipartFile file) throws Exception{
        var course = courseRepository.findById(courseId).orElseThrow(() -> new ApiException("Course not found", HttpStatus.NOT_FOUND));

        var profileUrl = cloudinaryService.uploadFile(file, AttachmentType.image);
        course.setCoursePhoto(profileUrl);
        var savedCourse = courseRepository.save(course);

        var response = new ApiResponse();
        response.setMessage("Profile Photo Updated");
        response.setData(savedCourse);
        messagingTemplate.convertAndSend("/courses", response);

        return savedCourse;
    }

    public Course deleteCourse(String courseId) throws Exception {
        var course = courseRepository.findById(courseId).orElseThrow(() -> new ApiException("Course does not exist", HttpStatus.NOT_FOUND));
        courseRepository.deleteById(courseId);

        // We delete all batches with this course
        var match = Aggregation.match(Criteria.where("course").is(course));
        var aggregation = Aggregation.newAggregation(match);
        var results = mongoTemplate.aggregate(aggregation, "batches", Batch.class).getMappedResults();
        for (var batch : results) {
            batchService.deleteBatch(batch.getId());
        }


        // Send delete message to course websockets
        var delete = new Delete();
        delete.setId(courseId);
        delete.setDeleted(false);
        messagingTemplate.convertAndSend("/courses", delete);

        return course;
    }

   }
