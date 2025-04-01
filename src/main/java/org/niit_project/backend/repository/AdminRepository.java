package org.niit_project.backend.repository;
import org.niit_project.backend.entities.Admin;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface AdminRepository extends MongoRepository<Admin, String> {
    Optional<Admin> findByEmail(String email);

    Optional<Admin> findByPhoneNumber(String phoneNumber);

    boolean existsById(String id);

    Optional<Admin> findById(String id);
}

