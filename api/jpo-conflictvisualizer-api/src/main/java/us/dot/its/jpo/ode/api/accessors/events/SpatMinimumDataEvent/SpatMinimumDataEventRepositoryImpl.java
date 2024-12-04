package us.dot.its.jpo.ode.api.accessors.events.SpatMinimumDataEvent;


import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;


import org.springframework.data.domain.Sort;

import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.DateOperators;

import us.dot.its.jpo.conflictmonitor.monitor.models.events.minimum_data.SpatMinimumDataEvent;
import us.dot.its.jpo.ode.api.ConflictMonitorApiProperties;
import us.dot.its.jpo.ode.api.models.IDCount;

@Component
public class SpatMinimumDataEventRepositoryImpl implements SpatMinimumDataEventRepository {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    ConflictMonitorApiProperties props;

    private String collectionName = "CmSpatMinimumDataEvents";

    public Query getQuery(Integer intersectionID, Long startTime, Long endTime, boolean latest) {
        Query query = new Query();

        if (intersectionID != null) {
            query.addCriteria(Criteria.where("intersectionID").is(intersectionID));
        }
        Date startTimeDate = new Date(0);
        Date endTimeDate = new Date();

        if (startTime != null) {
            startTimeDate = new Date(startTime);
        }
        if (endTime != null) {
            endTimeDate = new Date(endTime);
        }

        query.addCriteria(Criteria.where("eventGeneratedAt").gte(startTimeDate).lte(endTimeDate));
        if (latest) {
            query.with(Sort.by(Sort.Direction.DESC, "eventGeneratedAt"));
            query.limit(1);
        }else{
            query.limit(props.getMaximumResponseSize());
        }
        return query;
    }

    public long getQueryResultCount(Query query) {
        return mongoTemplate.count(query, SpatMinimumDataEvent.class, collectionName);
    }

    public long getQueryFullCount(Query query){
        int limit = query.getLimit();
        query.limit(-1);
        long count = mongoTemplate.count(query, SpatMinimumDataEvent.class, collectionName);
        query.limit(limit);
        return count;
    }

    public List<SpatMinimumDataEvent> find(Query query) {
        return mongoTemplate.find(query, SpatMinimumDataEvent.class, collectionName);
    }

    public List<IDCount> getSpatMinimumDataEventsByDay(int intersectionID, Long startTime, Long endTime) {
        if (startTime == null) {
            startTime = 0L;
        }
        if (endTime == null) {
            endTime = System.currentTimeMillis();
        }

        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.match(Criteria.where("intersectionID").is(intersectionID)),
            Aggregation.match(Criteria.where("eventGeneratedAt").gte(startTime).lte(endTime)),
            Aggregation.project()
                .andExpression("{$toDate: '$eventGeneratedAt'}").as("eventGeneratedAtDate"),
            Aggregation.project()
                .andExpression("{$dateToString: { format: '%Y-%m-%d', date: '$eventGeneratedAtDate' }}").as("dateStr"),
            Aggregation.group("dateStr").count().as("count")
        );

        AggregationResults<IDCount> result = mongoTemplate.aggregate(aggregation, collectionName, IDCount.class);
        List<IDCount> results = result.getMappedResults();

        return results;
    }

    @Override
    public void add(SpatMinimumDataEvent item) {
        mongoTemplate.save(item, collectionName);
    }


}
