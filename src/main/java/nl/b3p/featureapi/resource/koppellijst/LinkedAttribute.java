package nl.b3p.featureapi.resource.koppellijst;

import java.util.List;

public class LinkedAttribute {

    private Long id;
    private String naam;
    private String featureType;
    private String tabel_naam;
    private Long domeinId;

    private List<LinkedValue> values;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNaam() {
        return naam;
    }

    public void setNaam(String naam) {
        this.naam = naam;
    }

    public String getFeatureType() {
        return featureType;
    }

    public void setFeatureType(String featureType) {
        this.featureType = featureType;
    }

    public String getTabel_naam() {
        return tabel_naam;
    }

    public void setTabel_naam(String tabel_naam) {
        this.tabel_naam = tabel_naam;
    }

    public Long getDomeinId() {
        return domeinId;
    }

    public void setDomeinId(Long domeinId) {
        this.domeinId = domeinId;
    }

    public List<LinkedValue> getValues() {
        return values;
    }

    public void setValues(List<LinkedValue> values) {
        this.values = values;
    }
}
