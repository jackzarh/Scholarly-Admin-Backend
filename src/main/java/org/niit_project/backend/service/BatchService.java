package org.niit_project.backend.service;

import org.niit_project.backend.entities.Admin;
import org.niit_project.backend.entities.AdminRole;
import org.niit_project.backend.entities.Batch;
import org.niit_project.backend.entities.Student;
import org.niit_project.backend.repository.AdminRepository;
import org.niit_project.backend.repository.BatchRepository;
import org.niit_project.backend.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BatchService {

    @Autowired
    private final BatchRepository batchRepository;

    @Autowired
    private AdminService adminService;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private final StudentRepository studentRepository;


    @Autowired
    private MongoTemplate mongoTemplate;

    private final List<AdminRole> allowedRoles = List.of(AdminRole.counselor, AdminRole.manager);

    public Batch createBatch(Batch batch) throws Exception {
        batch.setId(null);
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
        if (batch.getAdmin() == null) {
            throw new Exception("Admin cannot be null");
        }

        var admin = adminService.getAdmin(batch.getAdmin().toString());
        if (admin.isEmpty()) {
            throw new Exception("Admin doesn't exist");
        }

        return batchRepository.save(batch);
    }

    public List<Batch> getAllBatches() {
        return batchRepository.findAll(Sort.by(Sort.Direction.DESC, "startPeriod"));
    }

    public Optional<Batch> getBatchById(String id) {
        return batchRepository.findById(id);
    }

    public List<Batch> getCompactBatches() {
        return batchRepository.findAll().stream()
                .map(batch -> new Batch(
                        batch.getId(),
                        batch.getBatchName(),
                        batch.getCourse(),
                        batch.getStartPeriod(),
                        batch.getEndPeriod()
                ))
                .collect(Collectors.toList());
    }

    public Batch getCompactBatch(String id) throws Exception {
        Optional<Batch> batchOptional = batchRepository.findById(id);
        if (batchOptional.isEmpty()) {
            throw new Exception("Batch doesn't exist");
        }

        Batch batch = batchOptional.get();

        return new Batch(
                batch.getId(),
                batch.getBatchName(),
                batch.getCourse(),
                batch.getStartPeriod(),
                batch.getEndPeriod()
        );
    }

    public List<Object> getStudentsInTimeFrame(LocalDate startDate, LocalDate endDate) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("registrationDate")
                        .gte(startDate)
                        .lte(endDate))
        );

        AggregationResults<Object> results = mongoTemplate.aggregate(aggregation, "students", Object.class);
        return results.getMappedResults();
    }

    public void deleteBatch(String id) {
        batchRepository.deleteById(id);
    }

    public Optional<Batch> updateBatch(String id, Batch updatedBatch) {
        Optional<Batch> existingBatch = batchRepository.findById(id);
        if (existingBatch.isPresent()) {
            Batch batch = existingBatch.get();

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
            if (updatedBatch.getAdmin() != null) {
                batch.setAdmin(updatedBatch.getAdmin());
            }
            return Optional.of(batchRepository.save(batch));
        }
        return Optional.empty();
    }

    public long countBatches() {
        return batchRepository.count();
    }


    public Batch addMemberToBatch(String adminId, String batchId, String memberId) throws Exception {
        Optional<Admin> adminOpt = adminRepository.findById(adminId);
        if (adminOpt.isEmpty() || !allowedRoles.contains(adminOpt.get().getRole())) {
            throw new Exception("Only Counselors or Managers can add members to a batch.");
        }

        Optional<Batch> batchOpt = batchRepository.findById(batchId);
        if (batchOpt.isEmpty()) {
            throw new Exception("Batch not found.");
        }

        boolean isStudent = studentRepository.existsById(memberId);
        boolean isAdmin = adminRepository.existsById(memberId);

        if (!isStudent && !isAdmin) {
            throw new Exception("Member not found.");
        }

        Batch batch = batchOpt.get();
        batch.addMember(memberId);
        return batchRepository.save(batch);
    }

    public Batch removeMemberFromBatch(String adminId, String batchId, String memberId) throws Exception {
        Optional<Admin> adminOpt = adminRepository.findById(adminId);
        if (adminOpt.isEmpty() || !allowedRoles.contains(adminOpt.get().getRole())) {
            throw new Exception("Only Counselors or Managers can remove members from a batch.");
        }

        Optional<Batch> batchOpt = batchRepository.findById(batchId);
        if (batchOpt.isEmpty()) {
            throw new Exception("Batch not found.");
        }

        Batch batch = batchOpt.get();
        batch.removeMember(memberId);
        return batchRepository.save(batch);
    }

    public BatchService(BatchRepository batchRepository, StudentRepository studentRepository) {
        this.batchRepository = batchRepository;
        this.studentRepository = studentRepository;
    }

    public List<Batch> getBatchesByFaculty(AdminRole.Faculty faculty) {
        return batchRepository.findByFaculty(faculty);
    }

    public List<Batch> getBatchesForStudent(String studentId) throws Exception {
        // Check if student exists
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new Exception("Student not found"));

        // Fetch batches where the student is a member
        List<Batch> studentBatches = batchRepository.findByMembersContaining(studentId);

        if (studentBatches.isEmpty()) {
            throw new Exception("Student is not part of any batch");
        }

        return studentBatches;
    }
}
