package org.niit_project.backend.service;

import org.niit_project.backend.entities.Admin;
import org.niit_project.backend.entities.Channel;
import org.niit_project.backend.entities.Member;
import org.niit_project.backend.entities.Student;
import org.niit_project.backend.repository.ChannelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class ChannelService {

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private MongoTemplate  mongoTemplate;

    public Optional<List<Channel>> getAllChannels(){
        try{
            return Optional.of(channelRepository.findAll());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public List<Channel> getAdminChannels(String adminId){
        var allChannels = channelRepository.findAll();
        return allChannels.stream().filter(channel -> channel.getMembers().contains(adminId)).toList();
    }

    public Optional<Channel> getOneChannel(String id) {
        var channelExists = channelRepository.existsById(id);

        if(!channelExists){
            return Optional.empty();
        }

        var channel = channelRepository.findById(id).get();
        var members = channel.getMembers();

        // Aggregation stage to match the members base on their id
        var membersMatch = Aggregation.match(Criteria.where("_id").in(members));

        var aggregations = Aggregation.newAggregation(membersMatch);

        var studentMembers = mongoTemplate.aggregate(aggregations, "users", Student.class).getMappedResults();
        var adminMembers = mongoTemplate.aggregate(aggregations, "admins", Admin.class).getMappedResults();

        var allMembers = new ArrayList<>(studentMembers.stream().map(Member::fromStudent).toList());
        allMembers.addAll(adminMembers.stream().map(Member::fromAdmin).toList());

        channel.setMembers(allMembers.stream().map(member -> (Object) member).toList());

        return Optional.of(channel);

    }

    public Optional<Channel> getCompactChannel(String id) {
        var channelExists = channelRepository.existsById(id);

        if(!channelExists){
            return Optional.empty();
        }

        var channel = channelRepository.findById(id).get();
        return Optional.of(channel);

    }


    public Optional<Channel> createChannel(String creatorId, Channel channel){
        channel.setId(null);
        channel.setCreator(creatorId);
        channel.setMembers(List.of(creatorId));
        channel.setCreatedAt(LocalDateTime.now());

        try{
            var createdChannel = channelRepository.save(channel);
            return Optional.of(createdChannel);
        } catch (Exception e) {
            return Optional.empty();
        }

    }

    public Optional<Channel> updateChannel(String channelId,Channel channel){
        // Only Channel name and Channel Description are edited;

        /// Intentionally using the repository's findById method instead
        // of the getOneChannel because the getOneChannel transforms
        // the members list from strings to Members. So we have to use
        // The one that brings the exact form it is in the database which is the good ol
        // findById Method.
        var gottenChannelExists = channelRepository.findById(channelId);

        if(gottenChannelExists.isEmpty()){
            return Optional.empty();
        }

        var gottenChannel = gottenChannelExists.get();
        gottenChannel.setChannelName(channel.getChannelName());
        gottenChannel.setChannelDescription(channel.getChannelDescription());
        gottenChannel.setChannelType(channel.getChannelType());

        return Optional.of(channelRepository.save(gottenChannel));
    }

    public Optional<Channel> updateChannelProfile(String channelId, String url){
        /// Intentionally using the repository's findById method instead
        // of the getOneChannel because the getOneChannel transforms
        // the members list from strings to Members. So we have to use
        // The one that brings the exact form it is in the database which is the good ol
        // findById Method.
        var gottenChannel = channelRepository.findById(channelId);

        if(gottenChannel.isEmpty()){
            return Optional.empty();
        }

        var channel = gottenChannel.get();
        channel.setChannelProfile(url);

        return Optional.of(channelRepository.save(channel));
    }
}
