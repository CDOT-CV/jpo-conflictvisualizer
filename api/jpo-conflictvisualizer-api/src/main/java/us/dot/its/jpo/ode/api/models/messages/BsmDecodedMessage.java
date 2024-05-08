package us.dot.its.jpo.ode.api.models.messages;

import us.dot.its.jpo.ode.api.models.MessageType;
import us.dot.its.jpo.ode.model.OdeBsmData;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Setter
@Getter
public class BsmDecodedMessage extends DecodedMessage{
    public OdeBsmData bsm;

    public BsmDecodedMessage(OdeBsmData bsm, String asn1Text, MessageType type, String decodeErrors) {
        super(asn1Text, type, decodeErrors);
        this.bsm = bsm;
    }

}
