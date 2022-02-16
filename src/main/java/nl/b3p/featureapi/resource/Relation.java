package nl.b3p.featureapi.resource;

public class Relation {

    String filter;
    Long foreignFeatureTypeId;
    String foreignFeatureTypeName;
    String columnName;
    String columnType;
    String foreignColumnName;
    String foreignColumnType;
    boolean searchNextRelation;
    boolean canCreateNewRelation;

    public boolean isCanCreateNewRelation() { return canCreateNewRelation; }

    public void setCanCreateNewRelation(boolean canCreateNewRelation) { this.canCreateNewRelation = canCreateNewRelation; }

    public boolean isSearchNextRelation() { return searchNextRelation; }

    public void setSearchNextRelation(boolean searchNextRelation) { this.searchNextRelation = searchNextRelation; }

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

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getColumnType() {
        return columnType;
    }

    public void setColumnType(String columnType) {
        this.columnType = columnType;
    }

    public String getForeignColumnName() {
        return foreignColumnName;
    }

    public void setForeignColumnName(String foreignColumnName) {
        this.foreignColumnName = foreignColumnName;
    }

    public String getForeignColumnType() {
        return foreignColumnType;
    }

    public void setForeignColumnType(String foreignColumnType) {
        this.foreignColumnType = foreignColumnType;
    }

}
/*
   related_ft.put("filter", CQL.toCQL(filter));
                related_ft.put("id", rel.getForeignFeatureType().getId());
                related_ft.put("foreignFeatureTypeName", rel.getForeignFeatureType().getTypeName());
 */
