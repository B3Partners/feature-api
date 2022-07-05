package nl.b3p.featureapi.resource.fla;

public class RemoveRelationBody {
    private String featureId;
    private String relationColumn;

    public String getFeatureId() {
        return featureId;
    }

    public void setFeatureId(String featureId) {
        this.featureId = featureId;
    }

    public String getRelationColumn() {
        return relationColumn;
    }

    public void setRelationColumn(String relationColumn) {
        this.relationColumn = relationColumn;
    }
}
