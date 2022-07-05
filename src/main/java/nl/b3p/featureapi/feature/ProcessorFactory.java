package nl.b3p.featureapi.feature;

import nl.b3p.featureapi.resource.fla.Feature;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProcessorFactory {
    private static List<FeatureProcessor> processors = new ArrayList<>();

    public static void register(FeatureProcessor processor){
        processors.add(processor);
    }

    public static List<FeatureProcessor> getProcessors(Feature feature){
        return processors.stream().filter(featureProcessor -> featureProcessor.canProcess(feature)).collect(Collectors.toList());
    }
}
