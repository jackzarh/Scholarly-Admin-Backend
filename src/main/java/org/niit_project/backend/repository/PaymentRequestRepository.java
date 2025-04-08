package org.niit_project.backend.repository;

import org.niit_project.backend.entities.PaymentRequest;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PaymentRequestRepository extends MongoRepository<PaymentRequest, String> {
}
