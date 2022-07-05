package nl.b3p.featureapi.resource.gbi;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "collection_values")
public class CollectionValues {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String object_guid;
    private Integer object_id;
    private String tabel;
    private UUID collection_guid;

    public String getObject_guid() {
        return object_guid;
    }

    public void setObject_guid(String object_guid) {
        this.object_guid = object_guid;
    }

    public Integer getObject_id() {
        return object_id;
    }

    public void setObject_id(Integer object_id) {
        this.object_id = object_id;
    }

    public String getTabel() {
        return tabel;
    }

    public void setTabel(String tabel) {
        this.tabel = tabel;
    }

    public UUID getCollection_guid() {
        return collection_guid;
    }

    public void setCollection_guid(UUID collection_guid) {
        this.collection_guid = collection_guid;
    }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }
}
