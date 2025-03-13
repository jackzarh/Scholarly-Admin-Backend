package org.niit_project.backend.service;

import org.niit_project.backend.entities.Batch;
import org.niit_project.backend.repository.BatchRepository;
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
    private BatchRepository batchRepository;

    @Autowired
    private AdminService adminService;

    @Autowired
    private MongoTemplate mongoTemplate;

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
}
