package nl.b3p.featureapi.resource.koppellijst;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import javax.persistence.*;
import java.util.List;

@Entity
public class Domein {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    private String naam;

    private Boolean leeg_toestaan;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "domein_id")
    private List<Domeinwaarde> waardes;

    @JsonProperty(value = "linkedDomains")
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonIdentityReference(alwaysAsId = true)
    @ManyToMany()
    @JoinTable(
            name = "domein_koppeling",
            joinColumns = { @JoinColumn(name = "domein_parent_id") },
            inverseJoinColumns = { @JoinColumn(name = "domein_child_id") }
    )
    private List<Domein> linkedDomein;

    @ManyToOne  @JoinTable(
            name = "domein_koppeling",
            joinColumns = { @JoinColumn(name = "domein_child_id") },
            inverseJoinColumns = { @JoinColumn(name = "domein_parent_id") }
    )
    private Domein parent;

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

    public Boolean getLeeg_toestaan() {
        return leeg_toestaan;
    }

    public void setLeeg_toestaan(Boolean leeg_toestaan) {
        this.leeg_toestaan = leeg_toestaan;
    }

    public List<Domeinwaarde> getWaardes() {
        return waardes;
    }

    public void setWaardes(List<Domeinwaarde> waardes) {
        this.waardes = waardes;
    }

    public List<Domein> getLinkedDomein() {
        return linkedDomein;
    }

    public void setLinkedDomein(List<Domein> linkedDomein) {
        this.linkedDomein = linkedDomein;
    }

    public Domein getParent() {
        return parent;
    }

    public void setParent(Domein parent) {
        this.parent = parent;
    }
}
