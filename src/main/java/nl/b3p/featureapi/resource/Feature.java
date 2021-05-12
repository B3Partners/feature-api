package nl.b3p.featureapi.resource;

import org.geojson.Geometry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Feature {
    public static final String FID = "__fid";

    protected String clazz = null;
    protected List<Attribute> attributes = new ArrayList<>();
    protected List<Relation> relations = new ArrayList<>();
    protected List<Feature> children = new ArrayList<>();


    public String getFID(){
        return attributes.stream().filter(attribute -> attribute.getKey().equals(FID)).findFirst().orElseThrow().getValue().toString();
    }
    public void put(String key, Object value){
        attributes.add(new Attribute(key, value));
    }

    public void setClazz(String clazz){
        this.clazz = clazz;
    }

    public String getClazz() {
        return clazz;
    }

    public String getObjecttype(){
        return clazz;
    }

    public void joinAttributes(Feature other){
        other.getAttributes().forEach(attribute -> {
            getAttributes().add(attribute);
        });
    }
    public List<Relation> getRelations(){
        return relations;
    }
    public List<Feature> getChildren(){
        return children;
    }
    public List<Attribute> getAttributes(){
        return attributes;
    }

    public Geometry getDefaultGeometry(){
        // todo implement
        return null;
    }
    public String getDefaultGeometryField(){
        // todo implement
        return null;
    }
}
