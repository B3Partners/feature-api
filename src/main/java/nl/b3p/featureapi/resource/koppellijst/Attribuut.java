package nl.b3p.featureapi.resource.koppellijst;

import javax.persistence.*;

/*
@SqlResultSetMappings(value = {

        @SqlResultSetMapping(name = "EmployeeScheduleResults",
                entities = { @EntityResult(entityClass = com.baeldung.jpa.sqlresultsetmapping.Employee.class),
                        @EntityResult(entityClass = com.baeldung.jpa.sqlresultsetmapping.ScheduledDay.class)
                }) })*/
@Entity
public class Attribuut {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String naam;

    private String kolom_naam;
    private String object_naam;
    private String tabel_naam;
    private Boolean muteerbaar;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "domein_id")
    private Domein domein;

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

    public Domein getDomein() {
        return domein;
    }

    public void setDomein(Domein domein) {
        this.domein = domein;
    }

    public String getKolom_naam() {
        return kolom_naam;
    }

    public void setKolom_naam(String kolom_naam) {
        this.kolom_naam = kolom_naam;
    }

    public String getObject_naam() {
        return object_naam;
    }

    public void setObject_naam(String object_naam) {
        this.object_naam = object_naam;
    }

    public String getTabel_naam() {
        return tabel_naam;
    }

    public void setTabel_naam(String tabel_naam) {
        this.tabel_naam = tabel_naam;
    }

    public Boolean getMuteerbaar() {
        return muteerbaar;
    }

    public void setMuteerbaar(Boolean muteerbaar) {
        this.muteerbaar = muteerbaar;
    }
}
