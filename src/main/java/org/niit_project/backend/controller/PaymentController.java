package org.niit_project.backend.controller;

import org.niit_project.backend.dto.ApiResponse;
import org.niit_project.backend.entities.Course;
import org.niit_project.backend.entities.PaymentRequest;
import org.niit_project.backend.models.ApiException;
import org.niit_project.backend.service.CourseService;
import org.niit_project.backend.service.PaymentRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("scholarly/api/v1/payment")
public class PaymentController {

    @Autowired
    private PaymentRequestService paymentService;

    @PostMapping("/createPayment/{studentId}/{batchId}")
    public ResponseEntity<?> createPayment(@PathVariable String studentId, @PathVariable String batchId, @RequestBody PaymentRequest payment) {
        var response = new ApiResponse();
        try {
            var paymentRequest = paymentService.createPaymentRequest(payment, studentId, batchId);
            response.setMessage("Payment Created");
            response.setData(paymentRequest);
            return ResponseEntity.ok(response);
        }catch (ApiException e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, e.getStatusCode());
        } catch (Exception e) {
            response.setMessage(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

//    @PatchMapping("/updateCourse/{courseId}")
//    public ResponseEntity<?> updateCourse(@PathVariable String courseId, @RequestBody Course course) {
//        var response = new ApiResponse();
//        try {
//            var savedCourse = paymentService.updateCourse(courseId, course);
//            response.setMessage("Course Updated");
//            response.setData(savedCourse);
//            return ResponseEntity.ok(response);
//        }catch (ApiException e) {
//            response.setMessage(e.getMessage());
//            return new ResponseEntity<>(response, e.getStatusCode());
//        } catch (Exception e) {
//            response.setMessage(e.getMessage());
//            return ResponseEntity.badRequest().body(response);
//        }
//    }
//
//    @PatchMapping(value = "/updateCoursePhoto/{courseId}", consumes = "multipart/form-data")
//    public ResponseEntity<?> updateCoursePhoto(@PathVariable String courseId, @RequestPart("photo")MultipartFile file) {
//        var response = new ApiResponse();
//        try {
//            var savedCourse = paymentService.updateCoursePhoto(courseId, file);
//            response.setMessage("Course Photo Updated");
//            response.setData(savedCourse);
//            return ResponseEntity.ok(response);
//        }catch (ApiException e) {
//            response.setMessage(e.getMessage());
//            return new ResponseEntity<>(response, e.getStatusCode());
//        } catch (Exception e) {
//            response.setMessage(e.getMessage());
//            return ResponseEntity.badRequest().body(response);
//        }
//    }


    @GetMapping("/getOnePayment/{paymentId}")
    public ResponseEntity<?> getOnePayment(@PathVariable String paymentId) {
        var response = new ApiResponse();
        try {
            var gottenCourse = paymentService.getOnePayment(paymentId);
            response.setMessage("Got Payment");
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

    @GetMapping("/getAllPayments")
    public ResponseEntity<?> getAllPayments(){
        var response = new ApiResponse();
        try {
            var gottenCourse = paymentService.getPaymentRequests();
            response.setMessage("Got Payments");
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

    @PatchMapping("/respondToPayment/{paymentId}")
    public ResponseEntity<?> respondToPayment(@PathVariable String paymentId, @RequestBody Map<String, Boolean> body){
        var response = new ApiResponse();
        try {
            var deletePaymentRequest = paymentService.verifyPayment(body.get("accepted"), paymentId);
            response.setMessage("Payment Updated");
            response.setData(deletePaymentRequest);
            return ResponseEntity.ok(response);
        }catch (ApiException e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, e.getStatusCode());
        } catch (Exception e) {
            response.setMessage(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }


    @DeleteMapping("/deletePayment/{paymentId}")
    public ResponseEntity<?> deletePayment(@PathVariable String paymentId) {
        var response = new ApiResponse();
        try {
            var deletePaymentRequest = paymentService.deletePaymentRequest(paymentId);
            response.setMessage("Payment Deleted");
            response.setData(deletePaymentRequest);
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
