package org.niit_project.backend.service;

import org.niit_project.backend.dto.ApiResponse;
import org.niit_project.backend.entities.Notification;
import org.niit_project.backend.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MongoTemplate mongoTemplate;


    public Notification sendNotification(Notification notification) throws Exception{
        notification.setId(null);
        notification.setTimestamp(LocalDateTime.now());
        if(notification.getUserId() == null){
            throw new Exception("Target User Is Null");
        }

        if(notification.getContent() == null || notification.getCategory() == null || notification.getTitle() == null){
            throw new Exception("Notification Data is Null");
        }

        var sentNotification = notificationRepository.save(notification);

        var response = new ApiResponse("New notification", sentNotification);
        messagingTemplate.convertAndSend("/notifications/" + notification.getUserId(), response);

        return sentNotification;
    }

    public Notification updateNotification(String notificationId, Notification notification) throws Exception{
        var notificationExists = notificationRepository.findById(notificationId);
        if(notificationExists.isEmpty()){
            throw new Exception("Notification Doesn't exist");
        }
        var gottenNotification = notificationExists.get();

        gottenNotification.setTimestamp(LocalDateTime.now());

        if(notification.getContent() != null){
            gottenNotification.setContent(notification.getContent());
        }

        if(notification.getTitle() != null){
            gottenNotification.setTitle(notification.getTitle());
        }

        gottenNotification.setRead(notification.isRead());

        var sentNotification = notificationRepository.save(gottenNotification);

        var response = new ApiResponse("Notification updated", sentNotification);
        messagingTemplate.convertAndSend("/notifications/" + notification.getUserId(), response);

        return sentNotification;
    }

    public Notification markAsRead(String notificationId, boolean read) throws Exception{
        var notificationExists = notificationRepository.findById(notificationId);
        if(notificationExists.isEmpty()){
            throw new Exception("Notification Doesn't exist");
        }
        var gottenNotification = notificationExists.get();

        gottenNotification.setRead(read);

        var sentNotification = notificationRepository.save(gottenNotification);

        var response = new ApiResponse("Marked notification as read", sentNotification);
        messagingTemplate.convertAndSend("/notifications/" + gottenNotification.getUserId(), response);
        return sentNotification;
    }

    public Notification deleteNotification(String notificationId) throws Exception{

        var gottenNotification = notificationRepository.findById(notificationId);

        if(gottenNotification.isEmpty()){
            throw new Exception("Notification Doesn't exist");
        }

        notificationRepository.deleteById(notificationId);

        // To immediately, re-updated the notifications on the user's list
        messagingTemplate.convertAndSend("scholarly/getNotifications/" + gottenNotification.get().getUserId());

        return gottenNotification.get();
    }

    public Notification getNotification (String notificationId) throws Exception{
        var notification = notificationRepository.findById(notificationId);

        if(notification.isEmpty()){
            throw new Exception("Notification doesn't exist");
        }

        return notification.get();
    }

    public List<Notification> getUserNotification(String userId){
        var matchPipeline = Aggregation.match(Criteria.where("userId").is(userId));
        var aggregation = Aggregation.newAggregation(matchPipeline);

        var results = mongoTemplate.aggregate(aggregation, "notifications",Notification.class).getMappedResults();

        return results;
    }
}
