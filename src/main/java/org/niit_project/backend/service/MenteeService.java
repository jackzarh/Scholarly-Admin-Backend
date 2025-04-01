package org.niit_project.backend.service;

import org.niit_project.backend.entities.Batch;
import org.niit_project.backend.entities.PaymentRequest;
import org.niit_project.backend.entities.Student;
import org.niit_project.backend.enums.AdminRole;
import org.niit_project.backend.enums.MenteeStatus;
import org.niit_project.backend.enums.PaymentStatus;
import org.niit_project.backend.models.ApiException;
import org.niit_project.backend.models.Mentee;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MenteeService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private AdminService adminService;


    public List<Mentee> getMentees(String counselorId) throws ApiException {
        // Check if admin exists. Throw an error if they do not exist
        var admin = adminService.getAdmin(counselorId).orElseThrow(() -> new ApiException("Admin doesn't exist", HttpStatus.NOT_FOUND));

        // Make sure admin is a counselor
        if(admin.getRole() != AdminRole.counselor){
            throw new ApiException("This admin is not a counselor", HttpStatus.UNAUTHORIZED);
        }

        // Create Aggregation Operations
        var match = Aggregation.match(Criteria.where("counselorId").is(counselorId));
        var sort = Aggregation.sort(Sort.Direction.DESC, "createdAt");
        var aggregation = Aggregation.newAggregation(match, sort);

        var results = mongoTemplate.aggregate(aggregation, "students", Student.class).getMappedResults();

        var mentees = results.stream().map(student -> {
            var mentee = new Mentee();
            mentee.setFirstName(student.getFirstName());
            mentee.setLastName(student.getLastName());
            mentee.setColor(student.getColor());
            mentee.setCreatedTime(student.getCreatedAt());
            mentee.setProfile(student.getProfile());

            // Functionalities to set the status
            // NEW: if the student's hasn't been assigned to any batch
            // PENDING: if the student has a payment confirmation request that hasn't been confirmed
            // CONFIRMED: if the student's latest payment confirmation request has been confirmed

            // For NEW:
            var memberMatch = Aggregation.match(Criteria.where("members").in(student.getId()));
            var memberAggregation = Aggregation.newAggregation(memberMatch);
            var studentsBatches = mongoTemplate.aggregate(memberAggregation, "batches", Batch.class).getMappedResults();
            if(studentsBatches.isEmpty()){
                mentee.setStatus(MenteeStatus.created);
                return mentee;
            }

            // Payment Requests:
            var requestMatch = Aggregation.match(Criteria.where("issuer").is(student.getId()));
            var requestAggregation = Aggregation.newAggregation(requestMatch);
            var requests = mongoTemplate.aggregate(requestAggregation, "payment requests", PaymentRequest.class).getMappedResults();

            // For PENDING:
            if(requests.stream().anyMatch(paymentRequest -> paymentRequest.getStatus() == PaymentStatus.pending)){
                mentee.setStatus(MenteeStatus.pending);
                return  mentee;
            }

            // For CONFIRMED:
            mentee.setStatus(MenteeStatus.confirmed);

            return mentee;
        }).toList();

        return mentees;


    }
}
