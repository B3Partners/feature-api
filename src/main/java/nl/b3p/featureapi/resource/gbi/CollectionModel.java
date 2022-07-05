package nl.b3p.featureapi.resource.gbi;

public class CollectionModel {
    private String naam;
    private String omschrijving;
    private Integer retentie;
    private Long application;
    private LayerModel[] layers;

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

    public Integer getRetentie() {
        return retentie;
    }

    public void setRetentie(Integer retentie) {
        this.retentie = retentie;
    }

    public LayerModel[] getLayers() {
        return layers;
    }

    public void setLayers(LayerModel[] layers) {
        this.layers = layers;
    }

    public Long getApplication() {
        return application;
    }

    public void setApplication(Long application) {
        this.application = application;
    }
}
