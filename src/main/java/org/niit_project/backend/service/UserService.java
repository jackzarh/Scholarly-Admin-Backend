package org.niit_project.backend.service;

import org.niit_project.backend.entities.Student;
import org.niit_project.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;


    Optional<Student> getCompactStudent(String userId){
        return userRepository.findById(userId);
    }
}
