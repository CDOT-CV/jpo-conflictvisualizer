package us.dot.its.jpo.ode.api.topologies;

import lombok.Getter;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.KafkaStreams.StateListener;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;

import us.dot.its.jpo.geojsonconverter.pojos.geojson.LineString;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.map.ProcessedMap;
import us.dot.its.jpo.geojsonconverter.serialization.JsonSerdes;
import us.dot.its.jpo.ode.api.controllers.StompController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.Properties;

public class MapSocketForwardTopology extends BaseTopology {

    private static final Logger logger = LoggerFactory.getLogger(MapSocketForwardTopology.class);

    StompController controller;

    public MapSocketForwardTopology(String topicName, StompController controller, Properties streamsProperties){
        super(topicName, streamsProperties);
        this.controller = controller;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    public Topology buildTopology() {
        StreamsBuilder builder = new StreamsBuilder();

        KStream<String, ProcessedMap<LineString>> inputStream = builder.stream(topicName, Consumed.with(Serdes.String(), JsonSerdes.ProcessedMapGeoJson()));

        inputStream.foreach((key, value) -> {
            controller.broadcastMap(value);
        });

        return builder.build();

    }

}
