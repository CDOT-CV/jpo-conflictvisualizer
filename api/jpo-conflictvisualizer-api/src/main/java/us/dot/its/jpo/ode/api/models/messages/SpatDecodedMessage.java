package us.dot.its.jpo.ode.api.models.messages;

import us.dot.its.jpo.geojsonconverter.pojos.spat.ProcessedSpat;
import us.dot.its.jpo.ode.api.models.MessageType;
import us.dot.its.jpo.ode.model.OdeSpatData;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Setter
@Getter
public class SpatDecodedMessage extends DecodedMessage{
    public OdeSpatData spat;
    public ProcessedSpat processedSpat;

    public SpatDecodedMessage(ProcessedSpat processedSpat, OdeSpatData spat, String asn1Text, MessageType type, String decodeErrors) {
        super(asn1Text, type, decodeErrors);
        this.processedSpat = processedSpat;
        this.spat = spat;
        
    }

}
