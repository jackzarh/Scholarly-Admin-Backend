package org.niit_project.backend.entities;

import lombok.Data;
import org.niit_project.backend.enums.Days;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.List;

@Data
@Document(collection = "batches")
public class Batch {

    @Id
    private String id;

    private String batchName;

    private Object course;

    private LocalDate startPeriod;

    private Object faculty;

    private LocalDate endPeriod;

    private List<Object> members;

    private List<Object> paidMembers;

    private List<Days> timetable;

    public Batch(){}

    public Batch(String id, LocalDate endPeriod, Object faculty, LocalDate startPeriod, Object course, String batchName) {
        this.id = id;
        this.endPeriod = endPeriod;
        this.faculty = faculty;
        this.startPeriod = startPeriod;
        this.course = course;
        this.batchName = batchName;
    }


    public void addMember(String memberId) {
        if (!members.contains(memberId)) {
            members.add(memberId);
        }
    }

    public void removeMember(String memberId) {
        members.remove(memberId);
    }

    public List<Object> getMembers() {
        return members;
    }

    public List<Days> getTimetable() {
        return timetable;
    }

    public List<Object> getPaidMembers() {
        return paidMembers;
    }

    public void setPaidMembers(List<Object> paidMembers) {
        this.paidMembers = paidMembers;
    }

    public void setTimetable(List<Days> timetable) {
        this.timetable = timetable;
    }

    public void setMembers(List<Object> members) {
        this.members = members;
    }

    public LocalDate getEndPeriod() {
        return endPeriod;
    }

    public void setEndPeriod(LocalDate endPeriod) {
        this.endPeriod = endPeriod;
    }

    public Object getFaculty() {
        return faculty;
    }

    public void setFaculty(Object faculty) {
        this.faculty = faculty;
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
