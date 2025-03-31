package org.niit_project.backend.models;

import lombok.Data;
import org.niit_project.backend.enums.Colors;
import org.niit_project.backend.enums.MenteeStatus;

import java.time.LocalDateTime;

@Data
public class Mentee {
    private String id, firstName, lastName, profile;
    private MenteeStatus status;
    private Colors color;
    private LocalDateTime createdTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public MenteeStatus getStatus() {
        return status;
    }

    public void setStatus(MenteeStatus status) {
        this.status = status;
    }

    public Colors getColor() {
        return color;
    }

    public void setColor(Colors color) {
        this.color = color;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }
}
