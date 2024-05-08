package us.dot.its.jpo.ode.api.models.messages;

import us.dot.its.jpo.ode.api.models.MessageType;
import us.dot.its.jpo.ode.model.OdeSrmData;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Setter
@Getter
public class SrmDecodedMessage extends DecodedMessage{
    public OdeSrmData srm;

    public SrmDecodedMessage(OdeSrmData srm, String asn1Text, MessageType type, String decodeErrors) {
        super(asn1Text, type, decodeErrors);
        this.srm = srm;
    }

}
