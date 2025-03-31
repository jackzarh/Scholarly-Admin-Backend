package org.niit_project.backend.entities;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "batches")
public class Batch {

    @Id
    private String id;

    private String batchName;

    private Object course;

    private LocalDate startPeriod;

    private Object admin;

    private LocalDate endPeriod;

    private List<Object> candidates;

    private List<String> members = new ArrayList<>();

    private AdminRole.Faculty faculty;

    public Batch(){}

    public Batch(String id, LocalDate endPeriod, Object admin, LocalDate startPeriod, Object course, String batchName) {
        this.id = id;
        this.endPeriod = endPeriod;
        this.admin = admin;
        this.startPeriod = startPeriod;
        this.course = course;
        this.batchName = batchName;
    }

    public Batch(String id, String batchName, Object course, LocalDate startPeriod, LocalDate endPeriod) {
    }

    public void addMember(String memberId) {
        if (!members.contains(memberId)) {
            members.add(memberId);
        }
    }

    public void removeMember(String memberId) {
        members.remove(memberId);
    }

    public List<Object> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<Object> candidates) {
        this.candidates = candidates;
    }

    public LocalDate getEndPeriod() {
        return endPeriod;
    }

    public void setEndPeriod(LocalDate endPeriod) {
        this.endPeriod = endPeriod;
    }

    public Object getAdmin() {
        return admin;
    }

    public void setAdmin(Object admin) {
        this.admin = admin;
    }

    public LocalDate getStartPeriod() {
        return startPeriod;
    }

    public void setStartPeriod(LocalDate startPeriod) {
        this.startPeriod = startPeriod;
    }

    public Object getCourse() {
        return course;
    }

    public void setCourse(Object course) {
        this.course = course;
    }

    public String getBatchName() {
        return batchName;
    }

    public void setBatchName(String batchName) {
        this.batchName = batchName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
