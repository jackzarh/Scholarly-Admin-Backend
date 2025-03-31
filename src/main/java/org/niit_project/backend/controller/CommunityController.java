package org.niit_project.backend.controller;

import org.niit_project.backend.dto.ApiResponse;
import org.niit_project.backend.entities.Community;
import org.niit_project.backend.service.CommunityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("scholarly/api/v1/community")
public class CommunityController {

    @Autowired
    private CommunityService communityService;

    @PostMapping(path = "/createCommunity/{creatorId}")
    public ResponseEntity<ApiResponse> createCommunity(@PathVariable String creatorId, @RequestBody Community community){
        var response = new ApiResponse();

        try{
            var createdCommunity = communityService.createCommunity(creatorId, community);
            response.setMessage("Community Created Successfully");
            response.setData(createdCommunity);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping(path = "/getOneCommunity/{communityId}")
    public ResponseEntity<ApiResponse> getOneCommunity(@PathVariable String communityId){
        var response = new ApiResponse();

        try{
            var gottenCommunity = communityService.getOneCommunity(communityId);
            response.setMessage("Got community successfully");
            response.setData(gottenCommunity);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    @PatchMapping(path = "/addMember/{communityId}/{memberId}")
    public ResponseEntity<ApiResponse> addMember(@PathVariable String communityId, @PathVariable String memberId){
        var response = new ApiResponse();

        try {
            var addedMember = communityService.addMember(memberId, communityId);
            response.setMessage("Added User Successfully");
            response.setData(addedMember);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping(path = "/removeMember/{communityId}/{memberId}")
    public ResponseEntity<ApiResponse> removeMember(@PathVariable String communityId, @PathVariable String memberId){
        var response = new ApiResponse();

        try {
            var addedMember = communityService.removeMember(memberId, communityId);
            response.setMessage("Removed User Successfully");
            response.setData(addedMember);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping(path = "/sendInvitation/{communityId}")
    public ResponseEntity<ApiResponse> sendInvitation(@PathVariable String communityId, @RequestBody Map<String, String> body){
        var response = new ApiResponse();

        try {
            var addedMember = communityService.sendInvitation(body.get("email"), communityId);
            response.setMessage("Sent Invitation Successfully");
            response.setData(addedMember);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    @PatchMapping("/respondToInvitation/{invitationId}")
    public ResponseEntity<ApiResponse> respondToInvitation(@PathVariable String invitationId, @RequestBody Map<String, Object> body){
        var apiResponse = new ApiResponse();

        try{
            var notification = communityService.respondToInvitation(invitationId, (Boolean) body.get("accept"));
            apiResponse.setMessage((Boolean) body.get("accept")?"Accepted ": "Rejected " + "invitation successfully");
            apiResponse.setData(notification);

            return new ResponseEntity<>(apiResponse, HttpStatus.OK);

        } catch (Exception e) {
            apiResponse.setMessage(e.getMessage());
            return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
        }
    }


    @PatchMapping(path = "/updateCommunity/{communityId}/{creatorId}")
    public ResponseEntity<ApiResponse> editCommunity(@PathVariable String communityId, @PathVariable String creatorId, @RequestBody Community community){
        var response = new ApiResponse();

        try{
            var updatedCommunity = communityService.updateCommunity(communityId, community);
            response.setMessage("Community Updated Successfully");
            response.setData(updatedCommunity);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    @PatchMapping(path = "/updateCommunityProfile/{communityId}/{userId}", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse> updateCommunityProfile(@PathVariable String communityId, @PathVariable("userId") String creatorId, @RequestPart("photo") MultipartFile file){
        var response = new ApiResponse();

        try{
            var updatedCommunity = communityService.updateCommunityProfile(communityId, file);
            response.setMessage("Community Profile Updated Successfully");
            response.setData(updatedCommunity);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

    }

}
