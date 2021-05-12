package nl.b3p.featureapi.resource.koppellijst;

import javax.persistence.*;
import java.util.List;

@Entity
public class Domeinwaarde {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    private String waarde;

    private String afkorting;

    private String synoniem;

    private Double volgorde;

    @Column(name="domein_id", insertable=false, updatable=false)
    private Integer domein_id;

    @ManyToMany()
    @JoinTable(
            name = "domwrd_koppeling",
            joinColumns = { @JoinColumn(name = "domwrd_parent_id") },
            inverseJoinColumns = { @JoinColumn(name = "domwrd_child_id") }
    )
    private List<Domeinwaarde> linkedDomeinwaardes;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getWaarde() {
        return waarde;
    }

    public void setWaarde(String waarde) {
        this.waarde = waarde;
    }

    public String getAfkorting() {
        return afkorting;
    }

    public void setAfkorting(String afkorting) {
        this.afkorting = afkorting;
    }

    public String getSynoniem() {
        return synoniem;
    }

    public void setSynoniem(String synoniem) {
        this.synoniem = synoniem;
    }

    public Double getVolgorde() {
        return volgorde;
    }

    public void setVolgorde(Double volgorde) {
        this.volgorde = volgorde;
    }

    public List<Domeinwaarde> getLinkedDomeinwaardes() {
        return linkedDomeinwaardes;
    }

    public void setLinkedDomeinwaardes(List<Domeinwaarde> linkedDomeinwaardes) {
        this.linkedDomeinwaardes = linkedDomeinwaardes;
    }

    public Integer getDomein_id() {
        return domein_id;
    }

    public void setDomein_id(Integer domein_id) {
        this.domein_id = domein_id;
    }
}
