package org.niit_project.backend.controller;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import io.github.cdimascio.dotenv.Dotenv;
import org.niit_project.backend.dto.ApiResponse;
import org.niit_project.backend.entities.Channel;
import org.niit_project.backend.entities.ChannelType;
import org.niit_project.backend.service.AdminService;
import org.niit_project.backend.service.ChannelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;

@RestController
@RequestMapping("scholarly/api/v1/channel")
public class ChannelController {

    @Autowired
    private ChannelService channelService;



    @Autowired
    private AdminService adminService;


    @GetMapping("/getAllChannels")
    public ResponseEntity<ApiResponse> getAllChannels(){
        var response = new ApiResponse();
        var data = channelService.getAllChannels();

        if(data.isEmpty()){
            response.setMessage("Error occurred when getting channels");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        response.setMessage("Gotten Channels All Successfully");
        response.setData(data.get());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    @GetMapping("/getAllAdminChannels/{id}")
    public ResponseEntity<ApiResponse> getAllAdminChannels(@PathVariable String id){
        var response = new ApiResponse();
        var adminExists = adminService.getAdmin(id).isPresent();

        if(!adminExists){
            response.setMessage("Admin does not exist");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        var allChannels = channelService.getAdminChannels(id);
        response.setMessage("Got Admin Channels Successfully");
        response.setData(allChannels);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/getOneChannel/{id}")
    public ResponseEntity<ApiResponse> getOneChannel(@PathVariable String id){
        var gottenChannel = channelService.getOneChannel(id);
        var response = new ApiResponse();

        if(gottenChannel.isEmpty()){
            response.setMessage("Channel Does not exist");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        response.setMessage("Channel gotten successfully");
        response.setData(gottenChannel.get());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/createChannel/{userId}")
    public ResponseEntity<ApiResponse> createChannel(@PathVariable String userId, @RequestBody Channel channel){
        var response = new ApiResponse();

        var adminExists = adminService.getAdmin(userId).isPresent();

        if(!adminExists){
            response.setMessage("Admin Doesn't Exist");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        if(channel.getChannelName() == null){
            response.setMessage("Channel Name Cannot Be Null");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        if(channel.getChannelType() == null){
            response.setMessage("Channel Type Cannot Be Null");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        if(channel.getChannelDescription() == null){
            response.setMessage("Channel Description Cannot Be Null");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        var createdChannel = channelService.createChannel(userId,channel);

        if(createdChannel.isEmpty()){
            response.setMessage("Error occurred when creating channel");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        response.setMessage("Created Channel Successfully");
        response.setData(createdChannel.get());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PatchMapping("/editChannel/{channelId}")
    public ResponseEntity<ApiResponse> editChannel(@PathVariable String channelId, @RequestBody Channel channel){

        var response = new ApiResponse();

        var channelExists = channelService.getOneChannel(channelId).isPresent();

        if(!channelExists){
            response.setMessage("Channel doesn't exist");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        var savedChannel = channelService.updateChannel(channelId, channel);

        if(savedChannel.isEmpty()){
            response.setMessage("Error when editing channel");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        response.setMessage("Updated Channel Successfully");
        response.setData(savedChannel.get());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PatchMapping(path = "/updateChannelProfile/{channelId}", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse> updateChannelPhoto(@PathVariable String channelId, @RequestPart("file") MultipartFile file){
        var response = new ApiResponse();

        var gottenChannel = channelService.getOneChannel(channelId);

        if(gottenChannel.isEmpty()){
            response.setMessage("Channel doesn't exist");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        var dotenv = Dotenv.load();
        var cloudinary = new Cloudinary(dotenv.get("CLOUDINARY_URL"));

        var params = ObjectUtils.asMap(
                "use_filename", true,
                "unique_filename", false,
                "overwrite", true
        );

        try {
            var result = cloudinary.uploader().upload(file.getBytes(), params);

            var secure_url = result.get("secure_url");
            if(secure_url == null){
                response.setMessage("Error Uploading File");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            var updatedChannel = channelService.updateChannelProfile(channelId, secure_url.toString().trim());
            if(updatedChannel.isEmpty()){
                response.setMessage("Error when updating channel");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            response.setMessage("Updated Channel Profile Successfully");
            response.setData(updatedChannel.get());
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (IOException e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PatchMapping("/addMember/{channelId}/{memberId}")
    public ResponseEntity<ApiResponse> addMemberToChannel(@PathVariable String channelId, @PathVariable String memberId){
        var apiResponse = new ApiResponse();

        try{
            var member = channelService.addMember(memberId, channelId);
            apiResponse.setMessage("Added " + member.getFirstName() + " successfully");
            apiResponse.setData(member);

            return new ResponseEntity<>(apiResponse, HttpStatus.OK);

        } catch (Exception e) {
            apiResponse.setMessage(e.getMessage());
            return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/sendInvitation/{channelId}/{memberId}")
    public ResponseEntity<ApiResponse> sendChannelInvitation(@PathVariable String channelId, @PathVariable String memberId){
        var apiResponse = new ApiResponse();

        try{
            var notification = channelService.sendInvitation(memberId, channelId);
            apiResponse.setMessage("Sent invitation to successfully");
            apiResponse.setData(notification);

            return new ResponseEntity<>(apiResponse, HttpStatus.OK);

        } catch (Exception e) {
            apiResponse.setMessage(e.getMessage());
            return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @PatchMapping("/respondToInvitation/{invitationId}")
    public ResponseEntity<ApiResponse> respondToInvitation(@PathVariable String invitationId, @RequestBody boolean accept){
        var apiResponse = new ApiResponse();

        try{
            var notification = channelService.respondToInvitation(invitationId, accept);
            apiResponse.setMessage(accept?"Accepted ": "Rejected " + "invitation successfully");
            apiResponse.setData(notification);

            return new ResponseEntity<>(apiResponse, HttpStatus.OK);

        } catch (Exception e) {
            apiResponse.setMessage(e.getMessage());
            return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
        }
    }


    @PatchMapping("/removeMember/{channelId}/{memberId}")
    public ResponseEntity<ApiResponse> removeMemberFromChannel(@PathVariable String channelId, @PathVariable String memberId){
        var apiResponse = new ApiResponse();

        try{
            var memberRemoved = channelService.removeMember(memberId, channelId);
            apiResponse.setMessage("Removed User Successfully");
            apiResponse.setData(memberRemoved);

            return new ResponseEntity<>(apiResponse, HttpStatus.OK);

        } catch (Exception e) {
            apiResponse.setMessage(e.getMessage());
            return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
        }
    }


}
