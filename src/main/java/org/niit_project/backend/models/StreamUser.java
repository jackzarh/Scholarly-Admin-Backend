package org.niit_project.backend.models;

import io.getstream.models.UserRequest;
import lombok.Data;
import org.niit_project.backend.enums.Colors;
import org.springframework.data.annotation.Transient;

import java.util.Map;
import java.util.Objects;

@Data
public class StreamUser {
    private String id,name,image;


    private Custom custom;

    public Custom getCustom() {
        return custom;
    }

    public void setCustom(Custom custom) {
        this.custom = custom;
    }

    public void setRole(String role) {
        this.role = role;
    }

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

    public UserRequest toUserRequest(){
        return UserRequest.builder()
                .id(id)
                .name(name)
                .image(image)
                .role(Objects.equals(role, "admin") ? "admin": "user")
                .custom(Map.of("color",custom.getColor().name().toLowerCase())).build();
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
