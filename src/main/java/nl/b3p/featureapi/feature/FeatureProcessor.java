package nl.b3p.featureapi.feature;

import nl.b3p.featureapi.resource.Feature;

import java.util.List;

public interface FeatureProcessor {
    boolean canProcess(Feature feature);
    List<Feature> preprocess(Feature feature);
    List<Feature> postprocess(Feature feature);
}
