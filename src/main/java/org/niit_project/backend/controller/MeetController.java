package org.niit_project.backend.controller;

import io.getstream.exceptions.StreamException;
import org.niit_project.backend.dto.ApiResponse;
import org.niit_project.backend.entities.Meet;
import org.niit_project.backend.models.ApiException;
import org.niit_project.backend.service.MeetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("scholarly/api/v1/meet")
public class MeetController {

    @Autowired
    private MeetService meetService;

    @PostMapping("/createMeet/{dmId}/{creatorId}")
    public ResponseEntity<ApiResponse<Meet>> createMeet(@PathVariable String dmId, @PathVariable String creatorId){
        var response = new ApiResponse<Meet>();

        try{
            var createdMeet = meetService.createCall(dmId, creatorId);
            response.setMessage("Created Meet Successfully");
            response.setData(createdMeet);
            return ResponseEntity.ok(response);

        } catch (StreamException e) {
            response.setMessage(e.getResponseData().getMessage());
            return new ResponseEntity<>(response, HttpStatusCode.valueOf(e.getResponseData().getStatusCode()));
        }
        catch (ApiException e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, e.getStatusCode());
        }
        catch (Exception e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

    }

    @PatchMapping("/addUser/{meetId}/{userId}")
    public ResponseEntity<ApiResponse<Meet>> addUserToCall(@PathVariable String meetId, @PathVariable String userId){
        var response = new ApiResponse<Meet>();

        try{
            var updatedMeet = meetService.addUserToCall(meetId, userId);
            response.setMessage("Added User To Meet");
            response.setData(updatedMeet);
            return ResponseEntity.ok(response);

        } catch (ApiException e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, e.getStatusCode());
        }
        catch (Exception e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/removeUser/{meetId}/{userId}")
    public ResponseEntity<ApiResponse<Meet>> removeUserFromCall(@PathVariable String meetId, @PathVariable String userId){
        var response = new ApiResponse<Meet>();

        try{
            var updatedMeet = meetService.removeUserFromCall(meetId, userId);
            response.setMessage("Removed User from Meet");
            response.setData(updatedMeet);
            return ResponseEntity.ok(response);

        } catch (ApiException e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, e.getStatusCode());
        }
        catch (Exception e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    @PatchMapping("/join/{meetId}/{userId}")
    public ResponseEntity<ApiResponse<Meet>> joinCall(@PathVariable String meetId, @PathVariable String userId){
        var response = new ApiResponse<Meet>();

        try{
            var updatedMeet = meetService.joinCall(meetId, userId);
            response.setMessage("Someone joined");
            response.setData(updatedMeet);
            return ResponseEntity.ok(response);

        } catch (ApiException e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, e.getStatusCode());
        }
        catch (Exception e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/leave/{meetId}/{userId}")
    public ResponseEntity<ApiResponse<Meet>> leaveMeet(@PathVariable String meetId, @PathVariable String userId){
        var response = new ApiResponse<Meet>();

        try{
            var updatedMeet = meetService.leaveCall(meetId, userId);
            response.setMessage("Someone Left");
            response.setData(updatedMeet);
            return ResponseEntity.ok(response);

        } catch (ApiException e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, e.getStatusCode());
        }
        catch (Exception e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/endMeet/{meetId}/{userId}")
    public ResponseEntity<ApiResponse<Meet>> endMeet(@PathVariable String meetId, @PathVariable String userId)  {
        var response = new ApiResponse<Meet>();

        try{
            var updatedMeet = meetService.endCall(meetId, userId);
            response.setMessage("Ended Meet Successfully");
            response.setData(updatedMeet);
            return ResponseEntity.ok(response);

        } catch (ApiException e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, e.getStatusCode());
        }
        catch (Exception e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

    }
}
