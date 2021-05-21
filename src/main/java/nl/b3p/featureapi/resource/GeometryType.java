package nl.b3p.featureapi.resource;

import java.util.Arrays;
import java.util.Optional;

public enum GeometryType {
    LINESTRING("linestring"),
    MULTILINESTRING("multilinestring"),
    POINT("point"),
    MULTIPOINT("multipoint"),
    POLYGON ("polygon"),
    MULTIPOLYGON ("multipolygon"),
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
