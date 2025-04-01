package org.niit_project.backend.controller;

import org.niit_project.backend.dto.ApiResponse;
import org.niit_project.backend.enums.AdminRole;
import org.niit_project.backend.entities.Batch;
import org.niit_project.backend.models.ApiException;
import org.niit_project.backend.service.BatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

    @PostMapping("/createBatch/{adminId}/{facultyId}")
    public ResponseEntity<ApiResponse> createBatch(@PathVariable String adminId, @PathVariable String facultyId, @RequestBody Batch batch) {
        var response = new ApiResponse();
        try {
            var createdBatch = batchService.createBatch(batch, facultyId, adminId);
            response.setMessage("Batch Created Successfully");
            response.setData(createdBatch);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ApiException e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, e.getStatusCode());
        }
        catch (Exception e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/addMember/{adminId}/{batchId}/{memberId}")
    public ResponseEntity<ApiResponse> addMember(@PathVariable String adminId, @PathVariable String batchId, @PathVariable String memberId) {
        var response = new ApiResponse();

        try {
            var updatedBatch = batchService.addMemberToBatch(adminId, batchId, memberId);
            response.setMessage("Added Member");
            response.setData(updatedBatch);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        catch (ApiException e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, e.getStatusCode());
        }
        catch (Exception e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping
    public ResponseEntity<List<Batch>> getAllBatches() {
        List<Batch> batches = batchService.getAllBatches();
        return ResponseEntity.ok(batches);
    }

    @GetMapping("/getOneBatch/{id}")
    public ResponseEntity<ApiResponse> getOneBatch(@PathVariable String id) {
        var response = new ApiResponse();

        try {
            var createdBatch = batchService.getOneBatch(id);
            response.setMessage("Batch Gotten Successfully");
            response.setData(createdBatch);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ApiException e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, e.getStatusCode());
        }
        catch (Exception e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/updateBatch/{id}")
    public ResponseEntity<ApiResponse> updateBatch(@PathVariable String id, @RequestBody Batch updatedBatch) {
        var response = new ApiResponse();
        try {
            var createdBatch = batchService.updateBatch(id, updatedBatch);
            response.setMessage("Batch Created Successfully");
            response.setData(createdBatch);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ApiException e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, e.getStatusCode());
        }
        catch (Exception e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/deleteBatch/{id}")
    public ResponseEntity<ApiResponse> deleteBatch(@PathVariable String id) {
        var response = new ApiResponse();
        try {
            var deletedBatch = batchService.deleteBatch(id);
            response.setMessage("Batch Created Successfully");
            response.setData(deletedBatch);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ApiException e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, e.getStatusCode());
        }
        catch (Exception e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/removeMember/{adminId}/{batchId}/{memberId}")
    public ResponseEntity<?> removeMember(@PathVariable String adminId, @PathVariable String batchId, @PathVariable String memberId) {
        var response = new ApiResponse();

        try {
            var updatedBatch = batchService.removeMemberFromBatch(adminId, batchId, memberId);
            response.setMessage("Removed Member");
            response.setData(updatedBatch);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        catch (ApiException e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, e.getStatusCode());
        }
        catch (Exception e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/getFacultyBatches/{facultyId}")
    public ResponseEntity<ApiResponse> getBatchesByFaculty(@PathVariable String facultyId) {
        var response = new ApiResponse();

        try {
            var batchesForFaculty = batchService.getBatchesForFaculty(facultyId);
            response.setMessage("Gotten Faculty Batches");
            response.setData(batchesForFaculty);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        catch (ApiException e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, e.getStatusCode());
        }
        catch (Exception e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/getStudentBatches/{studentId}")
    public ResponseEntity<?> getBatchesForStudent(@PathVariable String studentId) {
        var response = new ApiResponse();

        try {
            var batchesForStudent = batchService.getBatchesForStudent(studentId);
            response.setMessage("Got Student Batches");
            response.setData(batchesForStudent);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        catch (ApiException e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, e.getStatusCode());
        }
        catch (Exception e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }


    @GetMapping("/getStudents")
    public ResponseEntity<?> getStudentsInTimeFrame(
            @RequestParam("start") String start,
            @RequestParam("end") String end
    ) {
        var response = new ApiResponse();
        try {
            LocalDate startDate = LocalDate.parse(start);
            LocalDate endDate = LocalDate.parse(end);
            var students = batchService.getStudentsInTimeFrame(startDate, endDate);
            response.setMessage("Got Students Successfully");
            response.setData(students);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/count")
    public ResponseEntity<Long> countBatches() {
        long count = batchService.countBatches();
        return ResponseEntity.ok(count);
    }

}

