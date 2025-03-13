package org.niit_project.backend.repository;

import org.niit_project.backend.entities.Event;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends MongoRepository<Event, String> {
    List<Event> findByDesignatedTimeBetween(LocalDateTime startDate, LocalDateTime endDate);
    List<Event> findByEventTitleContainingIgnoreCaseOrEventDescriptionContainingIgnoreCase(String titleKeyword, String descriptionKeyword);
}
