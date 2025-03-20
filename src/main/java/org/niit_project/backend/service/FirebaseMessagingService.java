package org.niit_project.backend.service;
import com.google.firebase.messaging.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FirebaseMessagingService {

    public String sendNotification(org.niit_project.backend.entities.Notification notif, String token) throws Exception {
        var notification = notif.toFirebaseNotification();
        Message message = Message.builder()
                .setNotification(notification)
                .setToken(token)
                .build();

        var sentMessage = FirebaseMessaging.getInstance().send(message);
        System.out.println(sentMessage);

        return sentMessage;
    }

    public BatchResponse sendNotification(org.niit_project.backend.entities.Notification notif, List<String> tokens) throws Exception {
        var notification = notif.toFirebaseNotification();
        MulticastMessage message = MulticastMessage.builder()
                .setNotification(notification)
                .addAllTokens(tokens)
                .build();

        var sentMessage = FirebaseMessaging.getInstance().sendEachForMulticast(message);
        System.out.println(sentMessage);

        return sentMessage;
    }

    public String sendNotification(org.niit_project.backend.entities.Notification notif) throws Exception{
        var notification = notif.toFirebaseNotification();

        var message = Message.builder()
                .setNotification(notification)
                .setTopic(notif.getTarget())
                .build();

        var sentMessage = FirebaseMessaging.getInstance().send(message);
        System.out.println(sentMessage);

        return sentMessage;

    }
}
