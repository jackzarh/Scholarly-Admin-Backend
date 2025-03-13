package org.niit_project.backend.entities;

import lombok.Data;

import java.util.List;

@Data
public class Counselor extends Admin{

    private List<Object> mentees;

    public List<Object> getMentees() {
        return mentees == null? List.of(): mentees;
    }

    public void setMentees(List<Object> mentees) {
        this.mentees = mentees;
    }

    public void addMentee(Object mentee){
        mentees.add(mentee);
    }

    public void removeMentee(int index){
        mentees.remove(index);
    }

    public void removeMentee(Object o){
        mentees.remove(o);
    }
}
