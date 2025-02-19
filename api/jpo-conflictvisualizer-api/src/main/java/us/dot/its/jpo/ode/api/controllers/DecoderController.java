package us.dot.its.jpo.ode.api.controllers;

import java.time.ZonedDateTime;

// import jakarta.ws.rs.core.Response;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.ObjectMapper;

import us.dot.its.jpo.ode.api.models.MessageType;
import us.dot.its.jpo.ode.api.models.messages.DecodedMessage;
import us.dot.its.jpo.ode.api.models.messages.EncodedMessage;
import us.dot.its.jpo.ode.mockdata.MockDecodedMessageGenerator;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import us.dot.its.jpo.ode.api.ConflictMonitorApiProperties;
import us.dot.its.jpo.ode.api.asn1.DecoderManager;

// import us.dot.its.jpo.ode.coder.StringPublisher;

@RestController
public class DecoderController {

    private static final Logger logger = LoggerFactory.getLogger(AssessmentController.class);

    ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    ConflictMonitorApiProperties props;

    @Autowired
    DecoderManager decoderManager;

    public String getCurrentTime() {
        return ZonedDateTime.now().toInstant().toEpochMilli() + "";
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @RequestMapping(value = "/decoder/upload", method = RequestMethod.POST, produces = "application/json")
    public @ResponseBody ResponseEntity<String> decode_request(
            @RequestBody EncodedMessage encodedMessage,
            @RequestParam(name = "test", required = false, defaultValue = "false") boolean testData) {
        try {
            if (testData) {
                if (encodedMessage.getType() == MessageType.BSM || encodedMessage.getType() == MessageType.UNKNOWN) {
                    return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                            .body(MockDecodedMessageGenerator.getBsmDecodedMessage().toString());
                } else if (encodedMessage.getType() == MessageType.MAP) {
                    return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                            .body(MockDecodedMessageGenerator.getMapDecodedMessage().toString());
                } else if (encodedMessage.getType() == MessageType.SPAT) {
                    return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                            .body(MockDecodedMessageGenerator.getSpatDecodedMessage().toString());
                } else if (encodedMessage.getType() == MessageType.SRM) {
                    return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                            .body(MockDecodedMessageGenerator.getSpatDecodedMessage().toString());
                } else if (encodedMessage.getType() == MessageType.SSM) {
                    return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                            .body(MockDecodedMessageGenerator.getSpatDecodedMessage().toString());
                } else if (encodedMessage.getType() == MessageType.TIM) {
                    return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                            .body(MockDecodedMessageGenerator.getSpatDecodedMessage().toString());
                } else {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.TEXT_PLAIN)
                            .body("No available mapping for Message Type " + encodedMessage.getType());
                }
            } else {

                if (encodedMessage.getType() == MessageType.UNKNOWN) {
                    EncodedMessage newEncodedMessage = DecoderManager.identifyAsn1(encodedMessage.getAsn1Message());

                    if (newEncodedMessage.getType() != MessageType.UNKNOWN) {
                        encodedMessage = newEncodedMessage;
                    } else {
                        return ResponseEntity.status(HttpStatus.OK)
                                .contentType(MediaType.TEXT_PLAIN)
                                .body(new DecodedMessage(encodedMessage.getAsn1Message(), MessageType.UNKNOWN,
                                        "Unable to identify Message Type from ASN.1").toString());
                    }
                }

                DecodedMessage decodedMessage = decoderManager.decode(encodedMessage);

                return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.TEXT_PLAIN)
                        .body(decodedMessage.toString());
            }

        } catch (Exception e) {
            logger.info("Failed to Upload Data");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.TEXT_PLAIN)
                    .body(ExceptionUtils.getStackTrace(e));
        }
    }
}