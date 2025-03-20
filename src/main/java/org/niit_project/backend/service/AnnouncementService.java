package org.niit_project.backend.service;

import org.niit_project.backend.dto.ApiResponse;
import org.niit_project.backend.entities.*;
import org.niit_project.backend.repository.AnnouncementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class AnnouncementService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private AnnouncementRepository announcementRepository;

    public List<Announcement> getUserAnnouncements(String userId) throws Exception{

        // Where the audience is for everyone or for some people
        var matchAggregation = Aggregation.match(new Criteria().orOperator(
                Criteria.where("audience").in(userId),
                Criteria.where("audience").size(0)));
        var sortAggregation = Aggregation.sort(Sort.by(Sort.Direction.DESC, "createdTime"));
        var aggregation = Aggregation.newAggregation(matchAggregation, sortAggregation);

        var results = mongoTemplate.aggregate(aggregation, "announcements", Announcement.class).getMappedResults();

        var formed = results.stream().peek(announcement -> {
            var membersAggregation = Aggregation.newAggregation(Aggregation.match(Criteria.where("_id").in(announcement.getAudience())));
            var studentMembers = mongoTemplate.aggregate(membersAggregation, "students", Student.class).getMappedResults();
            var adminMembers = mongoTemplate.aggregate(membersAggregation, "admins", Admin.class).getMappedResults();

            //Then collate all members
            var allMembers = new ArrayList<Member>();
            allMembers.addAll(studentMembers.stream().map(Member::fromStudent).toList());
            allMembers.addAll(adminMembers.stream().map(Member::fromAdmin).toList());
            announcement.setAudience(Arrays.asList(allMembers.toArray()));
        });

        var sorted = formed.sorted((announcement1, announcement2) -> announcement2.getCreatedTime().compareTo(announcement1.getCreatedTime()));


        return sorted.toList();
    }

    public Announcement createAnnouncement(String adminId, Announcement announcement) throws Exception {
        var exists = mongoTemplate.exists(Query.query(Criteria.where("_id").is(adminId)), "admins");

        if (!exists){
            throw new Exception("Admin doesn't exist");
        }

        if(announcement.getAnnouncementTitle() == null){
            throw new Exception("Announcement title cannot be null");
        }

        if(announcement.getAnnouncementDescription() == null){
            throw new Exception("Announcement description cannot be null");
        }

        announcement.setId(null);
        announcement.setCreatedTime(LocalDateTime.now());
        announcement.setColor(Colors.getRandomColor().name());

        if(announcement.getAudience() == null){
            announcement.setAudience(List.of());
        }

        var createdAnnouncement = announcementRepository.save(announcement);


        /// Send Notification To All The Audience
        /// And Send the Announcement to all the websockets of the audience
        var notification = new Notification();
        var response = new ApiResponse();
        response.setMessage("New Announcement");
        response.setData(getOneAnnouncement(createdAnnouncement.getId()));
        notification.setTitle(announcement.getAnnouncementTitle());
        notification.setContent(announcement.getAnnouncementDescription());
        notification.setImage(announcement.getAnnouncementPhoto());
        notification.setCategory(NotificationCategory.announcement);
        notification.setTimestamp(announcement.getCreatedTime());
        notification.setTarget(announcement.getId());

        var audienceList = new ArrayList<>(announcement.getAudience().stream().map(o -> o.toString()).toList());
        if(announcement.getAudience().isEmpty()){
            var allAdmins = mongoTemplate.findAll(Admin.class, "admins");
            var allStudents = mongoTemplate.findAll(Student.class, "students");

            var allUsers = new ArrayList<>(allAdmins.stream().map(Admin::getId).toList());
            allUsers.addAll(allStudents.stream().map(Student::getId).toList());

            audienceList.addAll(allUsers);
        }
        notification.setRecipients(audienceList);
        notificationService.sendPushNotification(notification);
        for(final String user : audienceList){
            messagingTemplate.convertAndSend("/announcements/"+user,response);
        }

        return createdAnnouncement;
    }

    public Announcement getOneAnnouncement(String announcementId) throws Exception {
        if(!announcementRepository.existsById(announcementId)){
            throw new Exception("Announcement is not found");
        }

        var announcement = announcementRepository.findById(announcementId).get();

        var membersAggregation = Aggregation.newAggregation(Aggregation.match(Criteria.where("_id").in(announcement.getAudience())));
        var studentMembers = mongoTemplate.aggregate(membersAggregation, "students", Student.class).getMappedResults();
        var adminMembers = mongoTemplate.aggregate(membersAggregation, "admins", Admin.class).getMappedResults();

        //Then collate all members
        var allMembers = new ArrayList<Member>();
        allMembers.addAll(studentMembers.stream().map(Member::fromStudent).toList());
        allMembers.addAll(adminMembers.stream().map(Member::fromAdmin).toList());
        announcement.setAudience(Arrays.asList(allMembers.toArray()));

        return announcement;

    }

    public Announcement updateAnnouncement(String announcementId, Announcement announcement) throws Exception{
        var exists = announcementRepository.findById(announcementId);

        if(exists.isEmpty()){
            throw new Exception("Announcement doesn't exist");
        }

        var gottenAnnouncement = exists.get();

        if(announcement.getAnnouncementPhoto() != null){
            gottenAnnouncement.setAnnouncementPhoto(announcement.getAnnouncementPhoto());
        }

        if(announcement.getAnnouncementTitle() != null){
            gottenAnnouncement.setAnnouncementTitle(announcement.getAnnouncementTitle());
        }

        if(announcement.getAnnouncementDescription() != null){
            gottenAnnouncement.setAnnouncementDescription(announcement.getAnnouncementDescription());
        }

        var saveAnnoucement = announcementRepository.save(gottenAnnouncement);

        var response = new ApiResponse();
        response.setMessage("New Announcement");
        response.setData(getOneAnnouncement(saveAnnoucement.getId()));
        var audienceList = new ArrayList<>(announcement.getAudience().stream().map(o -> o.toString()).toList());
        if(announcement.getAudience().isEmpty()){
            var allAdmins = mongoTemplate.findAll(Admin.class, "admins");
            var allStudents = mongoTemplate.findAll(Student.class, "students");

            var allUsers = new ArrayList<>(allAdmins.stream().map(Admin::getId).toList());
            allUsers.addAll(allStudents.stream().map(Student::getId).toList());

            audienceList.addAll(allUsers);
        }
        for(final String user : audienceList){
            messagingTemplate.convertAndSend("/announcements/"+user,response);
        }
        return saveAnnoucement;
    }
}
