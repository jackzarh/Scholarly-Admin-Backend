package org.niit_project.backend.controller;

import jakarta.validation.Valid;
import org.niit_project.backend.dto.ApiResponse;
import org.niit_project.backend.entities.Admin;
import org.niit_project.backend.repository.AdminRepository;
import org.niit_project.backend.service.AdminService;
import org.niit_project.backend.utils.PhoneNumberConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("scholarly/api/v1/auth")
@Validated
public class AuthController {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private AdminService adminService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse>signUp(@Valid @RequestBody Admin admin){

        // Null checks
        if(admin.getEmail() == null){
            var response = new ApiResponse();
            response.setMessage("Email cannot be null");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
        if(admin.getPhoneNumber() == null){
            var response = new ApiResponse();
            response.setMessage("Phone Number cannot be null");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
        if(admin.getFirstName() == null){
            var response = new ApiResponse();
            response.setMessage("First Name cannot be null");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
        if(admin.getLastName() == null){
            var response = new ApiResponse();
            response.setMessage("Last Name cannot be null");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
        if(admin.getRole() == null){
            var response = new ApiResponse();
            response.setMessage("Role cannot be null");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
        if(admin.getPassword() == null){
            var response = new ApiResponse();
            response.setMessage("Password cannot be null");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        // Just to convert the phone number properly
        // To +234 format
        admin.setPhoneNumber(PhoneNumberConverter.convertToFull(admin.getPhoneNumber()));

        /// To make sure that you're not trying to register with an email
        /// Or phone number that already exists
        var gottenEmail = adminRepository.findByEmail(admin.getEmail());
        var gottenPhoneNumber = adminRepository.findByPhoneNumber(admin.getPhoneNumber());
        if(gottenEmail.isPresent() || gottenPhoneNumber.isPresent()){
            var response = new ApiResponse();
            response.setMessage(gottenEmail.isPresent()? "email already exists":"phone number already exists");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }


        final Optional<Admin> createdUser = adminService.registerUser(admin);

        if(createdUser.isPresent()){
            final Admin gottenAdmin = createdUser.get();
            final ApiResponse response = new ApiResponse();
            response.setMessage("Registered " + gottenAdmin.getFirstName() + " Successfully");
            response.setData(gottenAdmin);
            return new ResponseEntity<ApiResponse>(response, HttpStatus.OK);
        }

        final ApiResponse response = new ApiResponse();
        response.setMessage("Error creating user");
        return new ResponseEntity<ApiResponse>(response, HttpStatus.NOT_FOUND);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@RequestBody Admin admin) {
        final ApiResponse response = new ApiResponse();

        // Just to convert the phone number properly
        // To +234 format
        if(admin.getPhoneNumber() != null){
            admin.setPhoneNumber(PhoneNumberConverter.convertToFull(admin.getPhoneNumber()));
        }

        Admin user;
        try {
            user = adminService.login(admin);
            response.setMessage("Successfully Logged In as " + user.getFullName());
            response.setData(user);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
    }

}
