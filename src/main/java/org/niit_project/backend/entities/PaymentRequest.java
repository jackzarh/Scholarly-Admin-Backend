package org.niit_project.backend.entities;

import lombok.Data;
import org.niit_project.backend.enums.PaymentStatus;
import org.niit_project.backend.enums.PaymentType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "payment requests")
public class PaymentRequest {

    @Id
    private String id;

    private Object issuer;
    private Object batch;

    private PaymentType type;
    private PaymentStatus status;
    private String receipt;
    private LocalDateTime dateSubmitted;

    public Object getBatch() {
        return batch;
    }

    public void setBatch(Object batch) {
        this.batch = batch;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Object getIssuer() {
        return issuer;
    }

    public void setIssuer(Object issuer) {
        this.issuer = issuer;
    }

    public PaymentType getType() {
        return type;
    }

    public void setType(PaymentType type) {
        this.type = type;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getReceipt() {
        return receipt;
    }

    public void setReceipt(String receipt) {
        this.receipt = receipt;
    }

    public LocalDateTime getDateSubmitted() {
        return dateSubmitted;
    }

    public void setDateSubmitted(LocalDateTime dateSubmitted) {
        this.dateSubmitted = dateSubmitted;
    }
}
