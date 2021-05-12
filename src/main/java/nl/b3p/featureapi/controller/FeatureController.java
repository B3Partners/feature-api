package nl.b3p.featureapi.controller;

import nl.b3p.featureapi.feature.FeatureHelper;
import nl.b3p.featureapi.helpers.FeatureSourceFactoryHelper;
import nl.b3p.featureapi.repository.ApplicationLayerRepo;
import nl.b3p.featureapi.repository.ApplicationRepo;
import nl.b3p.featureapi.repository.SimpleFeatureTypeRepo;
import nl.b3p.featureapi.resource.Feature;
import nl.b3p.featureapi.resource.FeaturetypeMetadata;
import nl.b3p.featureapi.resource.GeometryType;
import nl.viewer.config.app.Application;
import nl.viewer.config.app.ApplicationLayer;
import nl.viewer.config.services.Layer;
import nl.viewer.config.services.SimpleFeatureType;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureIterator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.util.GeometricShapeFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManager;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@RequestMapping("features")
@RestController
public class FeatureController {
    Logger log = LoggerFactory.getLogger(FeatureController.class);

    public static final String GBI_PREFIX = "gb_";

    private boolean graph = false;

    private boolean ordered = false;

    private boolean edit = false;

    @Autowired
    private ApplicationRepo appRepo;
    @Autowired
    private ApplicationLayerRepo appLayerRepo;

    @Autowired
    private EntityManager em;

    @GetMapping(value = "/{application}/{x}/{y}/{scale}")
    public List<Feature> featuretypeOnPoint(@PathVariable Long application, @PathVariable double x,
                                            @PathVariable double y, @PathVariable double scale)  throws Exception {

        Application app = appRepo.findById(application).orElseThrow();
        Application.TreeCache tc = app.loadTreeCache(this.em);
        List<ApplicationLayer> all = tc.getApplicationLayers();

        List<ApplicationLayer> appLayers = filterAppLayers(all);
        List<Feature> features = new ArrayList<>();
        appLayers.forEach(appLayer -> {
            try {
                features.addAll(getFeatures(app, appLayer, x, y, scale));
            } catch (Exception e) {
                log.error("cannot get features for applayer " + appLayer.getLayerName(), e);
            }
        });
        return features;
    }
    @GetMapping(value = "/{application}/{featureTypes}/{x}/{y}/{scale}")
    public List<Feature> featuretypeOnPoint(@PathVariable Long application, @PathVariable List<String> featureTypes, @PathVariable double x,
                                            @PathVariable double y, @PathVariable double scale)  throws Exception {
        List<ApplicationLayer> applicationLayers = getApplayers(featureTypes, application);
        Application app = appRepo.findById(application).orElseThrow();
        List<Feature> features = new ArrayList<>();
        applicationLayers.forEach(appLayer -> {
            try {
                features.addAll(getFeatures(app, appLayer, x, y, scale));
            } catch (Exception e) {
                log.error("cannot get features for applayer " + appLayer.getLayerName(), e);
            }
        });
        return features;
    }

    private List<Feature> getFeatures(Application app, ApplicationLayer appLayer, double x, double y, double scale) throws Exception {
        List<Feature> features = new ArrayList<>();
        FeatureSource fs = null;
        FeatureIterator<SimpleFeature> it = null;
        try {

            Layer layer = appLayer.getService().getLayer(appLayer.getLayerName(), em);
            if (layer.getFeatureType() == null) {
                // ToDo handle error
                throw new IllegalArgumentException("Layer has no featuretype configured");
            }

            SimpleFeatureType sft = layer.getFeatureType();
            fs = FeatureSourceFactoryHelper.getFeatureSource(sft);
            // haal layers op adhv sft
            // haal applayers op adhv layers en application
            // build filter
            Query q = new Query(sft.getTypeName());
            q.setMaxFeatures(10);

            String geomAttribute = fs.getSchema().getGeometryDescriptor().getLocalName();

            FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
            GeometricShapeFactory shapeFact = new GeometricShapeFactory();
            shapeFact.setNumPoints(32);
            shapeFact.setCentre(new Coordinate(x, y));
            shapeFact.setSize(scale * 2);
            Polygon p = shapeFact.createCircle();
            Filter spatialFilter = ff.intersects(ff.property(geomAttribute), ff.literal(p));

            q.setFilter(spatialFilter);

            features = FeatureHelper.getFeatures(appLayer, sft, fs, q, null, null, em, app);

        } catch (Exception e) {
            log.error("IOException ", e);
            throw e;
        } finally {
            if (it != null) {
                it.close();
            }
            if (fs != null) {
                fs.getDataStore().dispose();
            }
        }
        return features;
    }

    @PostMapping
    public Feature save(@RequestBody Feature f, @RequestParam(required = false) String parentId) throws Exception {
        return null;
    }

    @PutMapping("/{objectGuid}")
    public Feature update(@PathVariable  String objectGuid, @RequestBody Feature feature) throws Exception {
        return null;
    }
    @Transactional
    @DeleteMapping("{featuretype}/{objectGuid}")
    public void delete(@PathVariable String featuretype, @PathVariable String objectGuid) throws Exception {

    }

    @GetMapping(value = "/info/{appId}/{featureTypes}")
    public List<FeaturetypeMetadata> featuretypeInformation(@PathVariable Long appId,
                                                            @PathVariable List<String> featureTypes) {
        List<FeaturetypeMetadata> md = new ArrayList<>();
        List<ApplicationLayer> appLayers = getApplayers(featureTypes, appId);

        appLayers.forEach(al -> {
            FeaturetypeMetadata fm = new FeaturetypeMetadata();
            Layer l = getLayer(al);
            if(l !=null && l.getFeatureType() != null){
                SimpleFeatureType sft =l.getFeatureType();

                fm.featuretypeName = sft.getTypeName();
                fm.geometryAttribute = sft.getGeometryAttribute();
                fm.geometryType = GeometryType.fromValue(sft.getAttribute(fm.geometryAttribute).getType()).orElse(GeometryType.GEOMETRY);
                md.add(fm);
            }
        });
        return md;
    }

    private Layer getLayer(ApplicationLayer al){
        Layer l =  al.getService().getLayer(al.getLayerName(), em);
        if( l== null){//this check must/can be removed if the layername isn't changed by the frontend and passport converter anymore
            l =  al.getService().getLayer(GBI_PREFIX+al.getLayerName(), em);
        }
        return l;
    }

    private List<ApplicationLayer> getApplayers(List<String> featureTypes, Long appId) {
        Application app = appRepo.findById(appId).orElseThrow();
        Application.TreeCache tc = app.loadTreeCache(this.em);
        List<ApplicationLayer> all = tc.getApplicationLayers();
        List<ApplicationLayer> appLayers = filterAppLayers(featureTypes, all);
        return appLayers;
    }

    private List<ApplicationLayer> filterAppLayers(List<String> featureTypes,List<ApplicationLayer> all){
        all = filterAppLayers(all);
        List<ApplicationLayer> appLayers = all.stream().filter(applicationLayer ->
                appLayerInFeatureTypes(applicationLayer, featureTypes)).collect(Collectors.toList());
        return appLayers;
    }

    private List<ApplicationLayer> filterAppLayers(List<ApplicationLayer> all){
        List<ApplicationLayer> appLayers = all.stream().filter(applicationLayer -> {
            Layer l = applicationLayer.getService().getLayer(applicationLayer.getLayerName(), em);
            return l != null && l.getFeatureType() != null;
        }).collect(Collectors.toList());
        return appLayers;
    }

   //this method must/can be removed if the layername isn't changed by the frontend and passport converter anymore
    private boolean appLayerInFeatureTypes(ApplicationLayer al, List<String> featureTypes){
        String origName = al.getLayerName();
        if(origName.contains(GBI_PREFIX)){
            String strippedGbiName = origName.substring(origName.indexOf(GBI_PREFIX) + GBI_PREFIX.length());

            return featureTypes.contains(strippedGbiName) || featureTypes.contains(origName);

        }else{
            return  featureTypes.contains(origName);
        }
    }
}