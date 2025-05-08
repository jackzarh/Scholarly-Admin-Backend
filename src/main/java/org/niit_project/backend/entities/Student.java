package org.niit_project.backend.entities;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.niit_project.backend.enums.Colors;
import org.niit_project.backend.enums.Language;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "students")
@Data
public class Student {

    @Id
    private String id;

    @NotNull(message = "first name cannot be empty")
    private String firstName;

    @NotNull(message = "last name cannot be empty")
    private String lastName;

    @NotNull(message = "password cannot be null or empty")
    private String password;

    @NotNull(message = "email  cannot be empty")
    @Indexed(unique = true)
    private String email;

    private String profile;

    @NotNull(message = "phone number cannot be empty")
    @Indexed(unique = true)
    private String phoneNumber;

    private String playerId;

    private LocalDateTime createdAt;

    private String token;

    private Colors color;

    private boolean online;



    private Object counselor;

    private Language preferredLanguage = Language.ENGLISH_UK; //setting default language to English (UK)


    public Student(){}

    public Colors getColor() {
        return color == null? Colors.getRandomColor(): color;
    }

    public void setColor(Colors color) {
        this.color = color;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isOnline() {
        return online;
    }

    public Object getCounselor() {
        return counselor;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public void setCounselor(Object counselor) {
        this.counselor = counselor;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getFullName(){
        return firstName + " " + lastName;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public @NotNull(message = "phone number cannot be empty") String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(@NotNull(message = "phone number cannot be empty") String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public @NotNull(message = "email  cannot be empty") String getEmail() {
        return email;
    }

    public void setEmail(@NotNull(message = "email  cannot be empty") String email) {
        this.email = email;
    }

    public @NotNull(message = "password cannot be null or empty") String getPassword() {
        return password;
    }

    public void setPassword(@NotNull(message = "password cannot be null or empty") String password) {
        this.password = password;
    }

    public @NotNull(message = "last name cannot be empty") String getLastName() {
        return lastName;
    }

    public void setLastName(@NotNull(message = "last name cannot be empty") String lastName) {
        this.lastName = lastName;
    }

    public @NotNull(message = "first name cannot be empty") String getFirstName() {
        return firstName;
    }

    public void setFirstName(@NotNull(message = "first name cannot be empty") String firstName) {
        this.firstName = firstName;
    }

    public String getId() {
        return id;
    }


    public void setId(String id) {
        this.id = id;
    }

    public Student(Language preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    public Language getPreferredLanguage() {
        return preferredLanguage;
    }

    public void setPreferredLanguage(Language preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }
}
