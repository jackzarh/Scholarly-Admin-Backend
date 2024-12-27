package org.niit_project.backend.entities;

import lombok.Data;
import org.springframework.data.annotation.Transient;

@Data
public class StreamUser {
    private String id,name,image;


    private Custom custom;

    @Transient
    private String role;

    public StreamUser(){
        custom = new Custom();
        custom.setColor(Colors.getRandomColor());
    }

    public String getRole(){
        return "user";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setColor(Colors color){
        this.custom.setColor(color);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}

@Data
class Custom{
    private Colors color;

    public Colors getColor() {
        return color;
    }

    public void setColor(Colors color) {
        this.color = color;
    }
}
