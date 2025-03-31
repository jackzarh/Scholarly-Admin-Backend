package org.niit_project.backend.service;

import org.niit_project.backend.models.TypingIndicator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TypingIndicatorService {

    private final Map<String, TypingIndicator> typingMap = new ConcurrentHashMap<>();

    @Autowired
    private SimpMessagingTemplate messagingTemplate;


    public void updateTypingStatus(TypingIndicator status) {
        status.setTimestamp(System.currentTimeMillis());
        String key = status.getDmId() + ":" + status.getTyper();
        typingMap.put(key, status);

        messagingTemplate.convertAndSend("/typing/" + status.getDmId(), status);
    }

    @Scheduled(fixedRate = 1000) // runs every second
    public void checkTypingTimeouts() {
        long now = System.currentTimeMillis();
        long timeout = 3000;

        Iterator<Map.Entry<String, TypingIndicator>> iterator = typingMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, TypingIndicator> entry = iterator.next();
            TypingIndicator status = entry.getValue();

            if (now - status.getTimestamp() > timeout) {
                status.setTyping(false);
                messagingTemplate.convertAndSend("/typing/" + status.getDmId(), status);
                iterator.remove();
            }
        }
    }
}
