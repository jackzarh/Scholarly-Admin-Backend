package org.niit_project.backend.repository;

import org.niit_project.backend.entities.Channel;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChannelRepository extends MongoRepository<Channel, String> {
}
