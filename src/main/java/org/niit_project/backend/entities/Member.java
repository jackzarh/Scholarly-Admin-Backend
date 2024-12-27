package org.niit_project.backend.entities;

public class Member {
    public enum MemberRole {admin, student}
    private String id;
    private String firstName, lastName;
    private MemberRole role;
    private Colors color;
    private String email,phoneNumber, profile;

    public Member(){}

    private Member(String id, String firstName, MemberRole role, String lastName, String email, String phoneNumber, String profile) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
        this.phoneNumber = phoneNumber;
        this.profile = profile;
    }

    private Member(String id, String firstName, MemberRole role, String lastName, String email, String phoneNumber, String profile, Colors color) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
        this.phoneNumber = phoneNumber;
        this.profile = profile;
        this.color = color;
    }

    public static Member fromStudent(Student student){
        return new Member(student.getId(), student.getFirstName(), MemberRole.student, student.getLastName(), student.getEmail(), student.getPhoneNumber(), student.getProfile());
    }
    public static Member fromAdmin(Admin admin){
        return new Member(admin.getId(), admin.getFirstName(), MemberRole.admin, admin.getLastName(), admin.getEmail(), admin.getPhoneNumber(), admin.getProfile(), admin.getColor());
    }

    public Colors getColor() {
        return color == null? Colors.getRandomColor(): color;
    }

    public void setColor(Colors color) {
        this.color = color;
    }

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

    public MemberRole getRole() {
        return role;
    }

    public void setRole(MemberRole role) {
        this.role = role;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }
}
