package nl.b3p.featureapi.resource.gbi;

public class LayerModel {
    private String featureTypeName;
    private String stringFilter;
    private String alias;
    private String userlayer_original_layername;
    private String userlayer_original_feature_type_name;

    public String getFeatureTypeName() {
        return featureTypeName;
    }

    public void setFeatureTypeName(String featureTypeName) {
        this.featureTypeName = featureTypeName;
    }

    public String getStringFilter() {
        return stringFilter;
    }

    public void setStringFilter(String stringFilter) {
        this.stringFilter = stringFilter;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getUserlayer_original_layername() {
        return userlayer_original_layername;
    }

    public void setUserlayer_original_layername(String userlayer_original_layername) {
        this.userlayer_original_layername = userlayer_original_layername;
    }

    public String getUserlayer_original_feature_type_name() {
        return userlayer_original_feature_type_name;
    }

    public void setUserlayer_original_feature_type_name(String userlayer_original_feature_type_name) {
        this.userlayer_original_feature_type_name = userlayer_original_feature_type_name;
    }
}
