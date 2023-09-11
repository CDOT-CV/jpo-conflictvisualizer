
package us.dot.its.jpo.ode.api.accessors.events.SignalStateEvent;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.Sort;
import us.dot.its.jpo.conflictmonitor.monitor.models.events.StopLinePassageEvent;

import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.ConvertOperators;
import org.springframework.data.mongodb.core.aggregation.DateOperators;
import us.dot.its.jpo.ode.api.models.IDCount;

@Component
public class SignalStateEventRepositoryImpl implements SignalStateEventRepository {

    @Autowired
    private MongoTemplate mongoTemplate;

    private String collectionName = "CmSignalStateEvent";

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
        }
        return query;
    }

    public long getQueryResultCount(Query query) {
        return mongoTemplate.count(query, StopLinePassageEvent.class, collectionName);
    }

    public List<StopLinePassageEvent> find(Query query) {
        return mongoTemplate.find(query, StopLinePassageEvent.class, collectionName);
    }

    public List<IDCount> getSignalStateEventsByDay(int intersectionID, Long startTime, Long endTime){
        if (startTime == null) {
            startTime = 0L;
        }
        if (endTime == null) {
            endTime = Instant.now().toEpochMilli();
        }

        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.match(Criteria.where("intersectionID").is(intersectionID)),
            Aggregation.match(Criteria.where("timestamp").gte(startTime).lte(endTime)),
            Aggregation.project("timestamp"),
            Aggregation.project()
                .and(ConvertOperators.ToDate.toDate("$timestamp")).as("date"),
            Aggregation.project()
                .and(DateOperators.DateToString.dateOf("date").toString("%Y-%m-%d")).as("dateStr"),
            Aggregation.group("dateStr").count().as("count")
        );

        AggregationResults<IDCount> result = mongoTemplate.aggregate(aggregation, collectionName, IDCount.class);
        List<IDCount> results = result.getMappedResults();

        return results;
    }

    @Override
    public void add(StopLinePassageEvent item) {
        mongoTemplate.save(item, collectionName);
    }

}
