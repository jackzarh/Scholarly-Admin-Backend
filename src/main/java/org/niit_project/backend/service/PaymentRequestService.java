package org.niit_project.backend.service;

import org.niit_project.backend.dto.ApiResponse;
import org.niit_project.backend.entities.Admin;
import org.niit_project.backend.entities.Batch;
import org.niit_project.backend.entities.PaymentRequest;
import org.niit_project.backend.entities.Student;
import org.niit_project.backend.enums.MenteeStatus;
import org.niit_project.backend.enums.PaymentStatus;
import org.niit_project.backend.models.ApiException;
import org.niit_project.backend.models.Delete;
import org.niit_project.backend.models.Mentee;
import org.niit_project.backend.repository.PaymentRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class PaymentRequestService {

    @Autowired
    private PaymentRequestRepository paymentRequestRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MongoTemplate mongoTemplate;

    public PaymentRequest createPaymentRequest(PaymentRequest paymentRequest, String studentId, String batchId) throws Exception{
        var student = mongoTemplate.findById(studentId, Student.class, "students");
        if(student == null){
            throw new ApiException("Student does not exist", HttpStatus.NOT_FOUND);
        }

        var batch = mongoTemplate.findById(batchId, Batch.class, "batches");
        if(batch == null){
            throw new ApiException("Cohort does not exist", HttpStatus.NOT_FOUND);
        }

        if(paymentRequest.getType() == null){
            throw new ApiException("Payment Type cannot be null", HttpStatus.UNAUTHORIZED);
        }
        if(paymentRequest.getReceipt() == null){
            throw new ApiException("Payment Receipt cannot be null", HttpStatus.UNAUTHORIZED);
        }

        paymentRequest.setIssuer(studentId);
        paymentRequest.setBatch(batchId);
        paymentRequest.setDateSubmitted(LocalDateTime.now());
        paymentRequest.setStatus(PaymentStatus.pending);

        var savedRequest = paymentRequestRepository.save(paymentRequest);

        savedRequest.setIssuer(student);
        savedRequest.setBatch(batch);
        messagingTemplate.convertAndSend("/payment-requests", new ApiResponse("Payment Created", savedRequest));
        updateCounselorMenteesWebSocket(student, PaymentStatus.pending);
        return savedRequest;
    }

    private void updateCounselorMenteesWebSocket(Student student, PaymentStatus status) throws Exception{
        var counselorId = student.getCounselor() instanceof  String? student.getCounselor().toString():((Admin) student.getCounselor()).getId();

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
            messagingTemplate.convertAndSend("/mentees/"+counselorId, new ApiResponse("Mentee Updated", mentee));
            return;
        }

        // Payment Requests:
        if(status == PaymentStatus.pending){
            mentee.setStatus(MenteeStatus.pending);
            messagingTemplate.convertAndSend("/mentees/"+counselorId, new ApiResponse("Mentee Updated", mentee));
            return;
        }

        // For CONFIRMED:
        mentee.setStatus(MenteeStatus.confirmed);
        messagingTemplate.convertAndSend("/mentees/"+counselorId, new ApiResponse("Mentee Updated", mentee));
    }

    public PaymentRequest getOnePayment(String requestId) throws Exception{
        var paymentRequest = paymentRequestRepository.findById(requestId).orElseThrow(() -> new ApiException("Payment Request does not exist", HttpStatus.NOT_FOUND));
        var issuer = mongoTemplate.findById(paymentRequest.getIssuer().toString(), Student.class, "students");
//        if(issuer == null){
//            throw new ApiException("Student does not exist", HttpStatus.NOT_FOUND);
//        }
        paymentRequest.setIssuer(issuer);

        var batch = mongoTemplate.findById(paymentRequest.getBatch().toString(), Batch.class, "batches");
//        if(batch == null){
//            throw new ApiException("Cohort does not exist", HttpStatus.NOT_FOUND);
//        }
        paymentRequest.setBatch(batch);

        return paymentRequest;
    }

    private PaymentRequest getOnePayment(PaymentRequest paymentRequest) throws Exception{
        var issuer = mongoTemplate.findById(paymentRequest.getIssuer().toString(), Student.class, "students");
//        if(issuer == null){
//            throw new ApiException("Student does not exist", HttpStatus.NOT_FOUND);
//        }
        paymentRequest.setIssuer(issuer);

        var batch = mongoTemplate.findById(paymentRequest.getBatch().toString(), Batch.class, "batches");
//        if(batch == null){
//            throw new ApiException("Cohort does not exist", HttpStatus.NOT_FOUND);
//        }

        return paymentRequest;
    }

    public List<PaymentRequest> getPaymentRequests() throws Exception{
        var payments = paymentRequestRepository.findAll(Sort.by(Sort.Direction.DESC, "dateSubmitted"));
        var paymentRequests = new ArrayList<PaymentRequest>();

        for(var payment: payments){
            var fullPayment = getOnePayment(payment);
            paymentRequests.add(fullPayment);
        }

       return paymentRequests;
    }

    public PaymentRequest verifyPayment(boolean accepted, String requestId) throws Exception{
        var paymentRequest = paymentRequestRepository.findById(requestId).orElseThrow(() -> new ApiException("Payment Request does not exist", HttpStatus.NOT_FOUND));
        paymentRequest.setStatus(accepted? PaymentStatus.approved: PaymentStatus.declined);

        paymentRequestRepository.save(paymentRequest);

        var fullRequest = getOnePayment(paymentRequest);
        messagingTemplate.convertAndSend("/payment-requests", new ApiResponse("Request Updated", fullRequest));
        updateCounselorMenteesWebSocket((Student) fullRequest.getIssuer(), fullRequest.getStatus());

        return fullRequest;

    }

    public PaymentRequest deletePaymentRequest(String requestId) throws Exception{
        // Make sure payment request exists
        var paymentRequest = paymentRequestRepository.findById(requestId).orElseThrow(() -> new ApiException("Payment Request does not exist", HttpStatus.NOT_FOUND));

        // A payment request cannot be deleted if it's not been declined
        if(paymentRequest.getStatus() != PaymentStatus.declined){
            throw new ApiException("Only declined payments can be deleted", HttpStatus.UNAUTHORIZED);
        }

        paymentRequestRepository.deleteById(requestId);

        var delete = new Delete();
        delete.setId(requestId);
        delete.setDeleted(true);
        messagingTemplate.convertAndSend("/payment-requests", delete);


        return paymentRequest;



    }
}
