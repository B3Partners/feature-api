package nl.b3p.featureapi.resource;

public class Relation {
    String filter;
    Long foreignFeatureTypeId;
    String foreignFeatureTypeName;

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public Long getForeignFeatureTypeId() {
        return foreignFeatureTypeId;
    }

    public void setForeignFeatureTypeId(Long foreignFeatureTypeId) {
        this.foreignFeatureTypeId = foreignFeatureTypeId;
    }

    public String getForeignFeatureTypeName() {
        return foreignFeatureTypeName;
    }

    public void setForeignFeatureTypeName(String foreignFeatureTypeName) {
        this.foreignFeatureTypeName = foreignFeatureTypeName;
    }
}
/*
   related_ft.put("filter", CQL.toCQL(filter));
                related_ft.put("id", rel.getForeignFeatureType().getId());
                related_ft.put("foreignFeatureTypeName", rel.getForeignFeatureType().getTypeName());
 */
