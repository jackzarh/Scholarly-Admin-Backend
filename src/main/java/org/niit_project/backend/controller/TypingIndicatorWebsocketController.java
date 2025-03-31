package org.niit_project.backend.controller;

import org.niit_project.backend.models.TypingIndicator;
import org.niit_project.backend.service.TypingIndicatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
public class TypingIndicatorWebsocketController {

    @Autowired
    private TypingIndicatorService typingIndicatorService;

    @MessageMapping("/showTyping/{dmId}")
    public void showTyping(@DestinationVariable String dmId, @Payload TypingIndicator typingIndicator){
        typingIndicator.setDmId(dmId);
        typingIndicatorService.updateTypingStatus(typingIndicator);
    }
}
