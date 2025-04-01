package org.niit_project.backend.service;

import org.niit_project.backend.entities.Admin;
import org.niit_project.backend.entities.Course;
import org.niit_project.backend.entities.Student;
import org.niit_project.backend.enums.AdminRole;
import org.niit_project.backend.entities.Batch;
import org.niit_project.backend.models.ApiException;
import org.niit_project.backend.models.Delete;
import org.niit_project.backend.models.User;
import org.niit_project.backend.repository.AdminRepository;
import org.niit_project.backend.repository.BatchRepository;
import org.niit_project.backend.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class BatchService {

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private AdminService adminService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private List<AdminRole> allowedRoles = List.of(AdminRole.counselor, AdminRole.manager);

    public Batch createBatch(Batch batch, String facultyId, String adminId) throws Exception {

        batch.setId(null);
        batch.setFaculty(facultyId);
        if (batch.getBatchName() == null) {
            throw new Exception("Batch Name cannot be null");
        }
        if (batch.getCourse() == null) {
            throw new Exception("Course cannot be null");
        }
        if (batch.getStartPeriod() == null) {
            throw new Exception("Start Period cannot be null");
        }
        if (batch.getEndPeriod() == null) {
            throw new Exception("End Period cannot be null");
        }
        if (batch.getFaculty() == null) {
            throw new Exception("Faculty cannot be null");
        }

        var admin = adminRepository.findById(adminId).orElseThrow(() -> new ApiException("Admin does not exist", HttpStatus.NOT_FOUND));
        if(!allowedRoles.contains(admin.getRole())){
            throw new ApiException("Only counselors and managers can create batches", HttpStatus.UNAUTHORIZED);
        }
        var faculty = adminRepository.findById(adminId).orElseThrow(() -> new ApiException("Faculty does not exist", HttpStatus.NOT_FOUND));

        return batchRepository.save(batch);
    }

    public List<Batch> getAllBatches() {
        return batchRepository.findAll(Sort.by(Sort.Direction.DESC, "startPeriod"));
    }

    public Batch getOneBatch(String id) throws Exception{
        var batch = batchRepository.findById(id).orElseThrow(() -> new ApiException("Batch does not exist", HttpStatus.NOT_FOUND));

        // We set the batch's faculty,
        batch.setFaculty(User.fromAdmin(adminRepository.findById(batch.getFaculty().toString()).orElseThrow(() -> new ApiException("This admin does not exist", HttpStatus.NOT_FOUND))));

        // We set the batch's course,
        var courseQuery = Query.query(Criteria.where("_id").is(batch.getCourse().toString()));
        var course = mongoTemplate.findOne(courseQuery,  Course.class, "courses");
        if(course == null){
            throw new ApiException("This course doesn't exist", HttpStatus.NOT_FOUND);
        }
        batch.setCourse(course);

        // Finally We Get and Set the full details of the all the members
        var match = Aggregation.match(Criteria.where("_id").in(batch.getMembers()));
        var sort = Aggregation.sort(Sort.Direction.DESC, "createdAt");
        var aggregation = Aggregation.newAggregation(match, sort);
        var results = mongoTemplate.aggregate(aggregation, "students", Student.class).getMappedResults();
        var students = results.stream().map(User::fromStudent).toList();
        var paidStudents = students.stream().filter(user -> batch.getPaidMembers().contains(user.getId())).toList();
        batch.setMembers(new ArrayList<>(students));
        batch.setPaidMembers(new ArrayList<>(paidStudents));

        return batch;
    }

    private Batch getOneBatch(Batch batch) throws ApiException {
        // If this batch's faculty property is already a User Object,
        // We don't need to fetch the admin anymore.
        var facultyIsFetched = batch.getFaculty() instanceof User;
        if(!facultyIsFetched){
            batch.setFaculty(User.fromAdmin(adminRepository.findById(batch.getFaculty().toString()).orElseThrow(() -> new ApiException("This admin does not exist", HttpStatus.NOT_FOUND))));
        }

        // If this batch's course property is already a Course Object,
        // We don't need to fetch the course anymore.
        var courseIsFetched = batch.getCourse() instanceof Course;
        if(!courseIsFetched){
            var courseQuery = Query.query(Criteria.where("_id").is(batch.getCourse().toString()));
            var course = mongoTemplate.findOne(courseQuery,  Course.class, "courses");
            if(course == null){
                throw new ApiException("This course doesn't exist", HttpStatus.NOT_FOUND);
            }
            batch.setCourse(course);
        }

        // Finally We Get and Set the full details of the all the members
        var match = Aggregation.match(Criteria.where("_id").in(batch.getMembers()));
        var sort = Aggregation.sort(Sort.Direction.DESC, "createdAt");
        var aggregation = Aggregation.newAggregation(match, sort);
        var results = mongoTemplate.aggregate(aggregation, "students", Student.class).getMappedResults();
        var students = results.stream().map(User::fromStudent).toList();
        var paidStudents = students.stream().filter(user -> batch.getPaidMembers().contains(user.getId())).toList();
        batch.setMembers(new ArrayList<>(students));
        batch.setPaidMembers(new ArrayList<>(paidStudents));

        return batch;

    }

    public List<Student> getStudentsInTimeFrame(LocalDate startDate, LocalDate endDate) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("createdAt")
                        .gte(startDate)
                        .lte(endDate))
        );

        var results = mongoTemplate.aggregate(aggregation, "students", Student.class);
        return results.getMappedResults();
    }

    public Batch deleteBatch(String id) throws Exception {
        var batch = batchRepository.findById(id).orElseThrow(() -> new ApiException("Could not find batch", HttpStatus.BAD_REQUEST));
        batchRepository.deleteById(id);

        // Send a delete message to the faculty's and all the member's websocket
        var delete = new Delete();
        delete.setId(id);
        delete.setDeleted(true);
        messagingTemplate.convertAndSend("/batches/"+batch.getFaculty(), delete);
        for(var member: batch.getMembers()){
            messagingTemplate.convertAndSend("/batches/"+member, delete);
        }

        return batch;
    }

    public Batch updateBatch(String id, Batch updatedBatch) throws Exception {
        var batch = batchRepository.findById(id).orElseThrow(() -> new ApiException("Batch does not exist", HttpStatus.NOT_FOUND));
        if (updatedBatch.getBatchName() != null) {
            batch.setBatchName(updatedBatch.getBatchName());
        }
        if (updatedBatch.getStartPeriod() != null) {
            batch.setStartPeriod(updatedBatch.getStartPeriod());
        }
        if (updatedBatch.getEndPeriod() != null) {
            batch.setEndPeriod(updatedBatch.getEndPeriod());
        }
        if (updatedBatch.getCourse() != null) {
            batch.setCourse(updatedBatch.getCourse());
        }
        if (updatedBatch.getFaculty() != null) {
            batch.setFaculty(updatedBatch.getFaculty());
        }
        return batchRepository.save(batch);
    }

    public long countBatches() {
        return batchRepository.count();
    }

    public Batch addMemberToBatch(String adminId, String batchId, String memberId) throws Exception {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found."));

        if (!allowedRoles.contains(admin.getRole())) {
            throw new IllegalArgumentException("Only Counselors or Managers can add members.");
        }

        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found."));

        if (!studentRepository.existsById(memberId) && !adminRepository.existsById(memberId)) {
            throw new IllegalArgumentException("Member not found.");
        }

        batch.addMember(memberId);
        return batchRepository.save(batch);
    }

    public Batch removeMemberFromBatch(String adminId, String batchId, String memberId) throws Exception {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found."));

        if (!allowedRoles.contains(admin.getRole())) {
            throw new IllegalArgumentException("Only Counselors or Managers can remove members.");
        }

        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found."));

        if (!batch.getMembers().contains(memberId)) {
            throw new IllegalArgumentException("Member is not part of this batch.");
        }

        batch.removeMember(memberId);
        return batchRepository.save(batch);
    }

    public List<Batch> getBatchesForFaculty(String facultyId) throws ApiException {
        // Ensures faculties exists before proceeding
        var faculty = adminRepository.findById(facultyId).orElseThrow(() -> new ApiException("This admin does not exist", HttpStatus.NOT_FOUND));
        if (faculty.getRole() != AdminRole.faculty) {
            throw new ApiException("This admin is not a faculty.", HttpStatus.UNAUTHORIZED);
        }

        // Aggregation operations to aggregate and get batches the student is involved in.
        var match = Aggregation.match(Criteria.where("members").in(facultyId));
        var sort = Aggregation.sort(Sort.Direction.DESC, "timestamp");
        var aggregation = Aggregation.newAggregation(match, sort);
        var results = mongoTemplate.aggregate(aggregation, "batches", Batch.class).getMappedResults();

        // We then convert it batch to get the full batch details
        // ...such as expanding the members list (from a list of Strings) to show full member details
        // ... and also getting the full details of the batches course and faculty.
        var fullBatches = new ArrayList<Batch>();
        for (Batch batch : results) {
            // pre-set the faculty before we get the full batch details
            batch.setFaculty(User.fromAdmin(faculty));
            Batch oneBatch = getOneBatch(batch);
            fullBatches.add(oneBatch);
        }

        return fullBatches;
    }

    public List<Batch> getBatchesForStudent(String studentId) throws ApiException {
        // Ensure student exists before proceeding
        if (!studentRepository.existsById(studentId)) {
            throw new ApiException("Student not found.", HttpStatus.NOT_FOUND);
        }

        // Aggregation operations to aggregate and get batches the student is involved in.
        var match = Aggregation.match(Criteria.where("members").in(studentId));
        var sort = Aggregation.sort(Sort.Direction.DESC, "timestamp");
        var aggregation = Aggregation.newAggregation(match, sort);
        var results = mongoTemplate.aggregate(aggregation, "batches", Batch.class).getMappedResults();

        // We then convert it batch to get the full batch details
        // ...such as expanding the members list (from a list of Strings) to show full member details
        // ... and also getting the full details of the batches course and faculty.
        var fullBatches = new ArrayList<Batch>();
        for (Batch batch : results) {
            Batch oneBatch = getOneBatch(batch);
            fullBatches.add(oneBatch);
        }

        return fullBatches;
    }

}
