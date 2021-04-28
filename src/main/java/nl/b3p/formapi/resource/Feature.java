package nl.b3p.formapi.resource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface Feature {

    String clazz = null;
    List<Feature> children = null;

    default List<Feature> getChildren(){
        return children;
    }

    Map<String, Object> attributes = new HashMap<>();
    default void put(String key, Object value){
        attributes.put(key, value);
    }

    default Map<String, Object> getAttributes(){
        return attributes;
    }
}
