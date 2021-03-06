package nl.b3p.featureapi.resource.fla;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Feature {
    public static final String FID = "__fid";

    protected String layerName = null;
    protected String tableName = null;
    protected List<Field> featureAttributes = new ArrayList<>();
    protected List<Relation> relations = new ArrayList<>();
    protected List<Feature> children = new ArrayList<>();


    public String getFID(){
        return featureAttributes.stream().filter(featureAttribute -> featureAttribute.getKey().equals(FID)).findFirst().orElseThrow().getValue().toString();
    }
    public void put(String key, Object value, String type){
        featureAttributes.add(new Field(key, value, type));
    }



    public void joinAttributes(Feature other){
        other.getAttributes().forEach(featureAttribute -> {
            getAttributes().add(featureAttribute);
        });
    }
    public List<Relation> getRelations(){
        return relations;
    }
    public List<Feature> getChildren(){
        return children;
    }
    public List<Field> getAttributes(){
        return featureAttributes;
    }

    public String getDefaultGeometry(){
        Optional<Field> opt =  featureAttributes.stream().filter(featureAttribute -> {
            Optional<GeometryType> gt = GeometryType.fromValue(featureAttribute.getType());
            return gt.isPresent() ;
        }).findFirst();
        return opt.isPresent() ? (String) opt.get().getValue() : null;
    }
    public String getDefaultGeometryField(){
        Optional<Field> opt =  featureAttributes.stream().filter(featureAttribute -> {
            Optional<GeometryType> gt = GeometryType.fromValue(featureAttribute.getType());
            return gt.isPresent() ;
        }).findFirst();
        return opt.isPresent() ? opt.get().getKey() : null;
    }

    public String getLayerName() {
        return layerName;
    }

    public void setLayerName(String layerName) {
        this.layerName = layerName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
}
