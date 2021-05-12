package nl.b3p.featureapi.resource.koppellijst;

import java.math.BigInteger;
import java.util.List;

public class LinkedValue {

    private BigInteger id;
    private String value;
    private BigInteger domeinid;
    private BigInteger domeinchildid;



    private List<LinkedAttribute> linkedAttributes;

    public List<LinkedAttribute> getLinkedAttributes() {
        return linkedAttributes;
    }

    public void setLinkedAttributes(List<LinkedAttribute> linkedAttributes) {
        this.linkedAttributes = linkedAttributes;
    }

    public BigInteger getId() {
        return id;
    }

    public void setId(BigInteger id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setDomeinparentid(BigInteger domeinparentid) {
        this.domeinid = domeinparentid;
    }

    public BigInteger getDomeinid() {
        return domeinid;
    }

    public void setDomeinid(BigInteger domeinid) {
        this.domeinid = domeinid;
    }

    public BigInteger getDomeinchildid() {
        return domeinchildid;
    }

    public void setDomeinchildid(BigInteger domeinchildid) {
        this.domeinchildid = domeinchildid;
    }
}
