package org.niit_project.backend.repository;

import org.niit_project.backend.entities.Announcement;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AnnouncementRepository extends MongoRepository<Announcement, String> {
}
