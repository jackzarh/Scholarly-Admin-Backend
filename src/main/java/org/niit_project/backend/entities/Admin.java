package org.niit_project.backend.entities;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "admins")
@Data
public abstract class Admin {
    @Id
    private String id;

    @NotNull(message = "first name cannot be empty")
    private String firstName;

    @NotNull(message = "last name cannot be empty")
    private String lastName;

    @NotNull(message = "password cannot be null or empty")
    private String password;

    @NotNull(message = "roles cannot be null")
    private AdminRole role;

    @NotNull(message = "email  cannot be empty")
    @Indexed(unique = true)
    private String email;

    private String profile;

    @NotNull(message = "phone number cannot be empty")
    @Indexed(unique = true)
    private String phoneNumber;

    private LocalDateTime createdAt;


    public Admin(){}

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }


    public String getLastName() {
        return lastName;
    }

    public void setLastName(@NotNull(message = "last name cannot be empty") String lastName) {
        this.lastName = lastName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(@NotNull(message = "phone number cannot be empty") String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public @NotNull(message = "roles cannot be null") AdminRole getRole() {
        return role;
    }

    public void setRole(@NotNull(message = "roles cannot be null") AdminRole role) {
        this.role = role;
    }

    public String getId() {
        return id;
    }

    public String getFullName(){
        return firstName + " " + lastName;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
