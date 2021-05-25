package nl.b3p.featureapi.helpers;

import nl.b3p.featureapi.feature.FeatureHelper;
import nl.b3p.featureapi.repository.LayerRepo;
import nl.viewer.config.app.ApplicationLayer;
import nl.viewer.config.services.Layer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserLayerHelper {

    public static List<ApplicationLayer> getOriginalsFromUserLayers(List<String>featuretypes, List<ApplicationLayer> all, boolean forWriting, LayerRepo layerRepo){
        List<ApplicationLayer> userLayers = new ArrayList<>();
        for (String featureType: featuretypes) {
            ApplicationLayer appLayer = getOriginalFromUserLayer(featureType, all, forWriting, layerRepo);
            if(appLayer != null){
                userLayers.add(appLayer);
            }
        }

        return userLayers;
    }

    public static ApplicationLayer getOriginalFromUserLayer(Layer l, List<ApplicationLayer> all, boolean forWriting, LayerRepo layerRepo) {
        if(forWriting){
            String origLayerId = l.getDetails().get(Layer.DETAIL_USERLAYER_ORIGINAL_LAYER_ID).getValue();
            Layer origLayer = layerRepo.findById(Long.parseLong(origLayerId)).orElse(null);
            Optional<ApplicationLayer> found = all.stream().filter(al -> al.getLayerName().equals(origLayer.getName())).findFirst();
            return found.isPresent() ? found.get() : null;
        }else{
            Optional<ApplicationLayer> found = all.stream().filter(al -> al.getLayerName().equals(l.getName())).findFirst();
            return found.isPresent() ? found.get() : null;
        }
    }

    public static ApplicationLayer getOriginalFromUserLayer(String featureType, List<ApplicationLayer> all, boolean forWriting, LayerRepo layerRepo){
        if(featureType.contains(FeatureHelper.USERLAYER_SEPARATOR)){
            String layerId = featureType.substring(FeatureHelper.USERLAYER_SEPARATOR.length());
            Layer l = layerRepo.findById(Long.parseLong(layerId)).orElse(null);
            if(l!=null){
                return getOriginalFromUserLayer(l, all, forWriting, layerRepo);
            }else{
                return null;
            }
        }else{
            return null;
        }
    }
}
