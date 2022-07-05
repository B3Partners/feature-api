package nl.b3p.featureapi.feature;

import nl.b3p.featureapi.resource.fla.Feature;

import java.util.Collections;
import java.util.List;

public class StandardFeatureProcessor implements FeatureProcessor{

    static{
        ProcessorFactory.register(new StandardFeatureProcessor());
    }

    @Override
    public boolean canProcess(Feature feature) {
        return true;
    }

    @Override
    public List<Feature> preprocess(Feature feature) {
        return Collections.singletonList(feature);
    }

    @Override
    public List<Feature> postprocess(Feature feature) {
        return Collections.singletonList(feature);
    }
}
