package org.niit_project.backend.service;

import org.niit_project.backend.entities.Feedback;
import org.niit_project.backend.entities.FeedbackSetting;
import org.niit_project.backend.enums.FeedbackRateLimit;
import org.niit_project.backend.models.ApiException;
import org.niit_project.backend.repository.FeedbackSettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
//import java.util.Optional;

//import static java.time.LocalDate.now;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;
//import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class FeedbackSettingService {

    @Autowired
    private FeedbackSettingRepository feedbackSettingRepository;

    @Autowired
    private MongoTemplate mongoTemplate;


    //faculty sets feedback rate limit
    public FeedbackSetting upsert(String facultyId, FeedbackRateLimit limit) {
        var fs = feedbackSettingRepository.findByFacultyId(facultyId).orElse(new FeedbackSetting());
        fs.setFacultyId(facultyId);
        fs.setRateLimit(limit);
        return feedbackSettingRepository.save(fs);
    }

    public FeedbackRateLimit getLimit(String facultyId) {
        return feedbackSettingRepository.findByFacultyId(facultyId).map(FeedbackSetting::getRateLimit).orElse(FeedbackRateLimit.HIGH);
    }


    //method to check if student has reached feedback rate limit
    public void checkAllowed(String facultyId, String studentId) throws ApiException {
        FeedbackRateLimit limit = getLimit(facultyId);
        long days = (limit == FeedbackRateLimit.LOW) ? 30 : 7;

        // Build a Query: match on facultyId, reporter (studentId), and createdAt >= now - days
        Query q = query(
                where("perpetrator").is(facultyId)
                        .and("reporter").is(studentId)
                        .and("createdAt").gte(LocalDateTime.now().minusDays(days))
        );

        long count = mongoTemplate.count(q, Feedback.class);

        if (count > 0) {
            throw new ApiException("Rate limit exceeded", HttpStatus.BAD_REQUEST);
        }
    }

}
