package org.niit_project.backend.controller;

import org.niit_project.backend.model.User;
import org.niit_project.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    protected ResponseEntity<User>signUp(@RequestBody User user){
        return new ResponseEntity<>(userService.registerUser(user), HttpStatus.OK);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User loginRequest) {
        Optional<User> user = userService.login(loginRequest.getUsername(), loginRequest.getPassword());
        if (user.isPresent()) {
            return ResponseEntity.ok(user.get());
        } else {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid credentials");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }

}
