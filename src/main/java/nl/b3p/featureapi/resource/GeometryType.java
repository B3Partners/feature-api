package nl.b3p.featureapi.resource;

import java.util.Arrays;
import java.util.Optional;

public enum GeometryType {
    LINESTRING("linestring"),
    POINT("point"),
    POLYGON ("polygon"),
    GEOMETRY ("geometry");

    GeometryType(String type){
        this.type = type;
    }
    private String type;

    public static Optional<GeometryType> fromValue(String type){
        return Arrays.stream(GeometryType.values()).filter(geometryType -> {
            return geometryType.type.equalsIgnoreCase(type);
        }).findFirst();
    }
}
