package org.niit_project.backend.entities;

import lombok.Data;
import org.niit_project.backend.enums.MeetStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import org.niit_project.backend.enums.MeetType;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "meets")

public class Meet {
    @Id
    private String id;
    private String sourceId;
    private String callerId;
    private MeetType type;
    private MeetStatus status;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private List<String> participants, listeners;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getCallerId() {
        return callerId;
    }

    public void setCallerId(String callerId) {
        this.callerId = callerId;
    }

    public MeetType getType() {
        return type;
    }

    public void setType(MeetType type) {
        this.type = type;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }

    public MeetStatus getStatus() {
        return status;
    }

    public void setStatus(MeetStatus status) {
        this.status = status;
    }

    public List<String> getListeners() {
        return listeners;
    }

    public void setListeners(List<String> listeners) {
        this.listeners = listeners;
    }
}
