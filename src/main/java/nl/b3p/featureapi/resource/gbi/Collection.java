package nl.b3p.featureapi.resource.gbi;


import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Entity
public class Collection {
  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(
    name = "UUID",
    strategy = "org.hibernate.id.UUIDGenerator"
  )
  @Column(name = "collection_guid", updatable = false, nullable = false)
    private UUID collection_guid;
    private String naam;
    private String omschrijving;
    private String gebruiker;
    private Date datum;
    private String bron;
    private String guid_based;
    private Integer retentie;

    @JoinColumn(name = "collection_guid")
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CollectionValues> values = new ArrayList<>();

    public UUID getCollection_guid() {
        return collection_guid;
    }

    public void setCollection_guid(UUID collection_guid) {
        this.collection_guid = collection_guid;
    }

    public String getNaam() {
        return naam;
    }

    public void setNaam(String naam) {
        this.naam = naam;
    }

    public String getOmschrijving() {
        return omschrijving;
    }

    public void setOmschrijving(String omschrijving) {
        this.omschrijving = omschrijving;
    }

    public String getGebruiker() {
        return gebruiker;
    }

    public void setGebruiker(String gebruiker) {
        this.gebruiker = gebruiker;
    }

    public Date getDatum() {
        return datum;
    }

    public void setDatum(Date datum) {
        this.datum = datum;
    }

    public String getBron() {
        return bron;
    }

    public void setBron(String bron) {
        this.bron = bron;
    }

    public String getGuid_based() {
        return guid_based;
    }

    public void setGuid_based(String guid_based) {
        this.guid_based = guid_based;
    }

    public Integer getRetentie() {
        return retentie;
    }

    public void setRetentie(Integer retentie) {
        this.retentie = retentie;
    }

    public List<CollectionValues> getValues() {
        return values;
    }

    public void setValues(List<CollectionValues> values) {
        this.values = values;
    }

}
