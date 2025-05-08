package org.niit_project.backend.entities;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.niit_project.backend.enums.AdminRole;
import org.niit_project.backend.enums.Colors;
import org.niit_project.backend.enums.Language;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "admins")
@Data
public class Admin {
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

    private String token;

    private String playerId;


    private boolean online;

    private LocalDateTime createdAt;

    private Colors color;

    @NotNull
    private Language preferredLanguage = Language.ENGLISH_UK; //setting default language to English (UK)

    public Colors getColor() {
        return color == null? Colors.getRandomColor(): color;
    }

    public void setColor(Colors color) {
        this.color = color;
    }

    public Admin(){}

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

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

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
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

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public Admin(Language preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    public Language getPreferredLanguage() {
        return preferredLanguage;
    }

    public void setPreferredLanguage(Language preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }
}
