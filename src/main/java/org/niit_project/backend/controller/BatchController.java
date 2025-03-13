package org.niit_project.backend.controller;

import org.niit_project.backend.entities.Batch;
import org.niit_project.backend.service.BatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("scholarly/api/v1/batches")
public class BatchController {

    @Autowired
    private BatchService batchService;

    @PostMapping
    public ResponseEntity<Batch> createBatch(@RequestBody Batch batch) {
        try {
            Batch createdBatch = batchService.createBatch(batch);
            return ResponseEntity.ok(createdBatch);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping
    public ResponseEntity<List<Batch>> getAllBatches() {
        List<Batch> batches = batchService.getAllBatches();
        return ResponseEntity.ok(batches);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Batch> getBatchById(@PathVariable String id) {
        Optional<Batch> batch = batchService.getBatchById(id);
        return batch.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/compact")
    public ResponseEntity<List<Batch>> getCompactBatches() {
        List<Batch> compactBatches = batchService.getCompactBatches();
        return ResponseEntity.ok(compactBatches);
    }

    @GetMapping("/compact/{id}")
    public ResponseEntity<Batch> getCompactBatchById(@PathVariable String id) {
        try {
            Batch compactBatch = batchService.getCompactBatch(id);
            return ResponseEntity.ok(compactBatch);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/students")
    public ResponseEntity<List<Object>> getStudentsInTimeFrame(
            @RequestParam("start") String start,
            @RequestParam("end") String end) {
        try {
            LocalDate startDate = LocalDate.parse(start);
            LocalDate endDate = LocalDate.parse(end);
            List<Object> students = batchService.getStudentsInTimeFrame(startDate, endDate);
            return ResponseEntity.ok(students);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Batch> updateBatch(@PathVariable String id, @RequestBody Batch updatedBatch) {
        Optional<Batch> batch = batchService.updateBatch(id, updatedBatch);
        return batch.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBatch(@PathVariable String id) {
        try {
            batchService.deleteBatch(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/count")
    public ResponseEntity<Long> countBatches() {
        long count = batchService.countBatches();
        return ResponseEntity.ok(count);
    }
}
