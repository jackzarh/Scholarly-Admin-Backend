package org.niit_project.backend.controller;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import io.github.cdimascio.dotenv.Dotenv;
import org.niit_project.backend.dto.ApiResponse;
import org.niit_project.backend.entities.Admin;
import org.niit_project.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("scholarly/api/v1/admin")
public class AdminController {

    @Autowired
    private UserService userService;


    @GetMapping("/getOneAdmin/{id}")
    public ResponseEntity<ApiResponse> getOneAdmin(@PathVariable String id){
        var response = new ApiResponse();
        var gottenAdmin = userService.getAdmin(id);

        if(gottenAdmin.isEmpty()){
            response.setMessage("Admin doesn't exist");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
        response.setMessage("Found Admin Successfully");
        response.setData(gottenAdmin.get());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/getAllAdmins")
    public ResponseEntity<ApiResponse> getAllAdmins(){
        var allAdmins = userService.getAllAdmins();

        if(allAdmins.isPresent()){
            var response = new ApiResponse();
            response.setMessage("Gotten All Admins");
            response.setData(allAdmins);

            return new ResponseEntity<>(response, HttpStatus.OK);
        }

        var response = new ApiResponse();
        response.setMessage("Error getting admins");
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @PatchMapping("/updateAdmin/{id}")
    public ResponseEntity<ApiResponse> updateAdmin(@PathVariable String id, @RequestBody Admin admin){
        // In this endpoint, we're not editing the email or phone number

        var gottenAdmin = userService.getAdmin(id);

        if(gottenAdmin.isEmpty()){
            var response = new ApiResponse();
            response.setMessage("Admin Doesn't exist");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        var editedAdmin = userService.updateAdmin(id, admin);

        if(editedAdmin.isEmpty()){
            var response = new ApiResponse();
            response.setMessage("Error updating Admin");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        var response = new ApiResponse();
        response.setMessage("Edited Admin Successfully");
        response.setData(editedAdmin.get());
        return new ResponseEntity<>(response, HttpStatus.OK);

    }

    @PatchMapping(value = "/uploadAdminProfile/{id}", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse> updateProfile(@PathVariable String id, @RequestPart("file") MultipartFile file){
        var response = new ApiResponse();
        var gottenAdmin = userService.getAdmin(id);

        if(gottenAdmin.isEmpty()){
            response.setMessage("Admin doesn't exist");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        if(file == null){
            response.setMessage("Profile should not be Empty");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        try {

            var fileName = file.getOriginalFilename();
            var fileSize = file.getSize();

            var dotenv = Dotenv.load();
            var cloudinary = new Cloudinary(dotenv.get("CLOUDINARY_URL"));

            var params = ObjectUtils.asMap(
                    "use_filename", true,
                    "unique_filename", false,
                    "overwrite", true
            );

            var result = cloudinary.uploader().upload(file.getBytes(), params);
            var secure_url = result.get("secure_url");
            if(secure_url == null){
                response.setMessage("Error Uploading File");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            var updatedAdminExists = userService.updateAdminProfile(id, secure_url.toString().trim());

            if(updatedAdminExists.isEmpty()){
                response.setMessage("Error updating admin profile photo");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            var updatedAdmin = updatedAdminExists.get();
            response.setMessage("Updated " + updatedAdmin.getFirstName() + "'s profile");
            response.setData(updatedAdmin);
            return new ResponseEntity<>(response, HttpStatus.OK);

        }
        catch (Exception e){
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
