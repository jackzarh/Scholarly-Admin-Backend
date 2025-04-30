package org.niit_project.backend.service;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FirebaseMessagingService {

    @Autowired
    private FirebaseMessaging firebaseMessaging;

    /// NOTE: Some of the Notification Objects may not hae ID's _id.
    // That is, they may not have been saved as notification on the DB.

    public String sendNotification(org.niit_project.backend.entities.Notification notif, String token) throws Exception {
        var notification = notif.toFirebaseNotification();
        Message message = Message.builder()
                .setNotification(notification)
                .setAndroidConfig(androidConfig())
                .setWebpushConfig(webConfig(notif))
                .setApnsConfig(iOSConfig())
                .setToken(token)
                .build();

        var sentMessage = firebaseMessaging.send(message);
        System.out.println(sentMessage);

        return sentMessage;
    }

    public BatchResponse sendNotification(org.niit_project.backend.entities.Notification notif, List<String> tokens) throws Exception {
        var notification = notif.toFirebaseNotification();
        MulticastMessage message = MulticastMessage.builder()
                .setNotification(notification)
                .setAndroidConfig(androidConfig())
                .setWebpushConfig(webConfig(notif))
                .setApnsConfig(iOSConfig())
                .addAllTokens(tokens)
                .build();

        var sentMessage = firebaseMessaging.sendEachForMulticast(message);
        System.out.println(sentMessage);

        return sentMessage;
    }

    public String sendNotification(org.niit_project.backend.entities.Notification notif) throws Exception{
        var notification = notif.toFirebaseNotification();

        var message = Message.builder()
                .setNotification(notification)
                .setTopic(notif.getTarget())
                .setAndroidConfig(androidConfig())
                .setWebpushConfig(webConfig(notif))
                .setApnsConfig(iOSConfig())
                .build();

        var sentMessage = firebaseMessaging.send(message);
        System.out.println(sentMessage);

        return sentMessage;

    }

    private AndroidConfig androidConfig(){
        return AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH)
                .build();
    }

    private ApnsConfig iOSConfig(){
        return ApnsConfig.builder()
                .setAps(Aps.builder()
                        .setContentAvailable(true)
                        .setSound("default")
                        .build())
                .build();
    }


    private WebpushConfig webConfig(org.niit_project.backend.entities.Notification notification){
        return WebpushConfig.builder()
                .setNotification(new WebpushNotification(notification.getTitle(), notification.getContent(), notification.getImage()))
                .build();
    }
}
