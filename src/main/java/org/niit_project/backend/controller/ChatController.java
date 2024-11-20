package org.niit_project.backend.controller;

import org.niit_project.backend.dto.ApiResponse;
import org.niit_project.backend.entities.Chat;
import org.niit_project.backend.entities.MessageType;
import org.niit_project.backend.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("scholarly/api/v1/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @PostMapping(path = "/sendChat/{channelId}/{senderId}")
    public ResponseEntity<ApiResponse> sendChat(@PathVariable String channelId, @PathVariable String senderId, @RequestBody Chat chat){
        var response = new ApiResponse();


        /// This endpoint is run by the front-end for only message-based (text or attachment) chats
        /// Not Joined or Removed chats
        chat.setMessageType(MessageType.chat);

        try {
            var sentChat = chatService.createChat(chat, channelId, senderId);
            response.setMessage("Sent Chat Successfully");
            response.setData(sentChat);

            return new ResponseEntity<>(response, HttpStatus.OK);

        }
        catch (Exception e){
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(path = "/getChats/{channelId}")
    public ResponseEntity<ApiResponse> getChat(@PathVariable String channelId){
        var response = new ApiResponse();

        try {
            var gottenChats = chatService.getChats(channelId);
            response.setMessage("Gotten Chats Successfully");
            response.setData(gottenChats);
            return new ResponseEntity<>(response, HttpStatus.OK);

        }
        catch (Exception e){
            response.setMessage(e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
    }
}
