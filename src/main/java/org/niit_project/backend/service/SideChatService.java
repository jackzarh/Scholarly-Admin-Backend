package org.niit_project.backend.service;

import org.niit_project.backend.entities.Channel;
import org.niit_project.backend.entities.Chat;
import org.niit_project.backend.repository.ChannelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SideChatService {

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    public Optional<Channel> getCompactChannel(String id) {
        var channelExists = channelRepository.existsById(id);

        if(!channelExists){
            return Optional.empty();
        }

        var channel = channelRepository.findById(id).get();
        return Optional.of(channel);

    }

    public Optional<Chat> getLastChat(String dmId){

        /// Aggregate Chats and Get the Latest One.
        var matchPipeline = Aggregation.match(Criteria.where("dmId").is(dmId));
        var limitPipeline = Aggregation.limit(1);
        var sortPipeline = Aggregation.sort(Sort.by(Sort.Direction.DESC, "_id"));
        var aggregation = Aggregation.newAggregation(matchPipeline, sortPipeline, limitPipeline);

        var results = mongoTemplate.aggregate(aggregation, "chats", Chat.class).getMappedResults();

        if(results.isEmpty()){
            return Optional.empty();
        }
        return Optional.of(results.get(0));
    }


    public Integer getUnseenChatsCount(String dmId, String memberId){
        /// Aggregate Chats that belong to this channel
        /// And have not been read by this member
        var matchPipeline = Aggregation.match(
                new Criteria().andOperator(
                        Criteria.where("dmId").is(dmId),
                        Criteria.where("senderId").ne(memberId),
                        Criteria.where("readReceipt").nin(memberId)
                )
        );
        var aggregation = Aggregation.newAggregation(matchPipeline);

        var results = mongoTemplate.aggregate(aggregation, "chats", Chat.class).getMappedResults();


        return results.size();
    }

}
