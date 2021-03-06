package nl.b3p.featureapi.controller;

import nl.b3p.featureapi.feature.FeatureHelper;
import nl.b3p.featureapi.helpers.FeatureSourceFactoryHelper;
import nl.b3p.featureapi.helpers.EditFeatureHelper;
import nl.b3p.featureapi.helpers.UserLayerHelper;
import nl.b3p.featureapi.repository.fla.ApplicationLayerRepo;
import nl.b3p.featureapi.repository.fla.ApplicationRepo;
import nl.b3p.featureapi.repository.fla.LayerRepo;
import nl.b3p.featureapi.resource.fla.*;
import nl.tailormap.viewer.config.app.Application;
import nl.tailormap.viewer.config.app.ApplicationLayer;
import nl.tailormap.viewer.config.services.FeatureTypeRelation;
import nl.tailormap.viewer.config.services.Layer;
import nl.tailormap.viewer.config.services.SimpleFeatureType;
import org.apache.commons.io.IOUtils;
import org.geotools.data.*;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureIterator;
import org.geotools.filter.identity.FeatureIdImpl;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.util.GeometricShapeFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;


@RequestMapping("features")
@RestController
public class FeatureController {
    Logger log = LoggerFactory.getLogger(FeatureController.class);

    @Value("${image.path}")
    private String imagePath;

    private boolean graph = false;

    private boolean ordered = false;

    private boolean edit = false;

    @Autowired
    private LayerRepo layerRepo;
    @Autowired
    private ApplicationRepo appRepo;
    @Autowired
    private ApplicationLayerRepo appLayerRepo;

    @PersistenceContext
    private EntityManager em;

    @GetMapping(value = "/{application}/{x}/{y}/{scale}")
    public List<Feature> onPoint(@PathVariable Long application, @PathVariable double x,
                                 @PathVariable double y, @PathVariable double scale) throws Exception {

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

    @GetMapping(value = "/{image}")
    public @ResponseBody void getImage(@PathVariable String image, HttpServletResponse response) throws IOException {
        try {
            File parent = new File(imagePath);
            File imageFile = new File(parent, image);
            if(!imageFile.getCanonicalPath().startsWith(imagePath)) {
                log.error("Path traversal found: " + image);
                return;
            }
            response.setContentType("image/jpeg");
            InputStream in = new FileInputStream(imageFile);
            IOUtils.copy(in, response.getOutputStream());
        } catch (Exception e) {
            log.error("Cant read image", e);
            File imageFile = new ClassPathResource("broken-image.png").getFile();
            response.setContentType("image/png");
            InputStream in = new FileInputStream(imageFile);
            IOUtils.copy(in, response.getOutputStream());
        }
    }

    @GetMapping(value = "/{application}/{featureTypes}/{x}/{y}/{scale}")
    public List<Feature> featuretypeOnPoint(@PathVariable Long application, @PathVariable List<String> featureTypes,
                                            @PathVariable double x,
                                            @PathVariable double y, @PathVariable double scale) throws Exception {
        List<ApplicationLayer> applicationLayers = getApplayers(featureTypes, application, false);
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

    @PostMapping(value = "/by-id/{application}/{featureType}")
    public List<Feature> getFeaturesForIds(@PathVariable Long application, @PathVariable String featureType,
                                 @RequestBody List<String> featureIds) throws Exception {
        List<String> featureTypes = new ArrayList<>();
        featureTypes.add(featureType);
        List<ApplicationLayer> applicationLayers = getApplayers(featureTypes, application, false);
        Application app = appRepo.findById(application).orElseThrow();
        List<Feature> features = new ArrayList<>();
        applicationLayers.forEach(appLayer -> {
            try {
                Layer layer = appLayer.getService().getLayer(appLayer.getLayerName(), em);
                SimpleFeatureType sft = FeatureSourceFactoryHelper.getSimpleFeatureType(layer, featureType);
                FeatureSource fs = FeatureSourceFactoryHelper.getFeatureSource(sft);
                Query q = new Query(sft.getTypeName());
                FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
                List<Filter> featureIdFilterList = featureIds.stream().map(fid -> ff.id(new FeatureIdImpl(fid))).collect(Collectors.toList());
                Filter filter = ff.or(featureIdFilterList);
                q.setFilter(filter);
                features.addAll(FeatureHelper.getFeatures(appLayer, sft, fs, q, null, null, em, app, layerRepo));
            } catch (Exception e) {
                log.error("cannot get features for featuretype " + featureType, e);
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

            features = FeatureHelper.getFeatures(appLayer, sft, fs, q, null, null, em, app, layerRepo);
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

    @PostMapping("/{application}/{featuretype}")
    public Feature save(@RequestBody Feature f, @RequestParam(required = false) String parentId,
                        @PathVariable Long application, @PathVariable String featuretype) throws Exception {
        List<ApplicationLayer> applicationLayers = getApplayers(Collections.singletonList(featuretype), application, true);

        if (applicationLayers.isEmpty()) {
            throw new IllegalArgumentException("Featuretype has no applayer in db");
        }

        ApplicationLayer appLayer = null;
        for (ApplicationLayer layer: applicationLayers) {
            if(!getLayer(layer).isUserlayer()) {
                appLayer = layer;
            }
        }

        if (appLayer == null) {
            throw new IllegalArgumentException("No original layer found");
        }

        Layer layer = getLayer(appLayer);
        if (layer.getFeatureType() == null) {
            throw new IllegalArgumentException("Layer has no featuretype configured");
        }

        SimpleFeatureType sft = FeatureSourceFactoryHelper.getSimpleFeatureType(layer, featuretype);
        if (!parentId.equals("-1")) {
            FeatureTypeRelation rel = FeatureSourceFactoryHelper.getParentRelation(layer.getFeatureType(), featuretype, null);
            f.put(rel.getRelationKeys().get(0).getRightSide().getName(), parentId, rel.getRelationKeys().get(0).getRightSide().getType());
        }
        Application app = this.appRepo.findById(application).orElseThrow();
        Feature savedFeature = EditFeatureHelper.save(appLayer, em, f, sft, app, layerRepo);

        return savedFeature;
    }

    @PostMapping("/removerelation/{application}/{featureType}")
    public Feature removeRelation(@PathVariable Long application, @PathVariable String featureType,
                          @RequestBody RemoveRelationBody requestBody) throws Exception {

        ApplicationLayer appLayer = getAppLayer(featureType, application);
        Application app = this.appRepo.findById(application).orElseThrow();

        Layer layer = getLayer(appLayer);
        if (layer.getFeatureType() == null) {
            throw new IllegalArgumentException("Layer has no featuretype configured");
        }

        String featureId = requestBody.getFeatureId();
        String relationColumn = requestBody.getRelationColumn();

        if(featureId == null || relationColumn == null) {
            log.error("Can not delete relation; featureId or RelationColumn can not be null or empty");
            return null;
        }

        SimpleFeatureType sft = FeatureSourceFactoryHelper.getSimpleFeatureType(layer, featureType);

        return EditFeatureHelper.removeRelation(appLayer, relationColumn, featureId, em, sft, app, layerRepo);
    }

    @PutMapping("/{application}/{featuretype}/{fid}")
    public Feature update(@PathVariable Long application, @PathVariable String featuretype,
                          @PathVariable String fid, @RequestBody Feature feature) throws Exception {

        ApplicationLayer appLayer = getAppLayer(featuretype, application);
        Application app = this.appRepo.findById(application).orElseThrow();

        Layer layer = getLayer(appLayer);
        if (layer.getFeatureType() == null) {
            throw new IllegalArgumentException("Layer has no featuretype configured");
        }

        SimpleFeatureType sft = FeatureSourceFactoryHelper.getSimpleFeatureType(layer, featuretype);

        feature = EditFeatureHelper.update(appLayer, layer, feature, fid, em, sft, app, layerRepo, imagePath);
        return feature;
    }

    @PutMapping("/updatebulk/{application}/{featureType}")
    public boolean updateBulk(@PathVariable Long application, @PathVariable String featureType,
                          @RequestBody BulkUpdateBody bulkBody) throws Exception {
        String filter = bulkBody.getFilter();
        boolean useSQLFiltering = bulkBody.isUseSQLFiltering();
        ApplicationLayer appLayer = getAppLayer(featureType, application);
        Layer layer = getLayer(appLayer);
        if (layer.getFeatureType() == null) {
            throw new IllegalArgumentException("Layer has no featuretype configured");
        }
        SimpleFeatureType sft = FeatureSourceFactoryHelper.getSimpleFeatureType(layer, featureType);
        return EditFeatureHelper.updateBulk(sft, em, filter, bulkBody.getUpdatedFields(), useSQLFiltering);
    }

    @DeleteMapping("/{application}/{featuretype}/{fid}")
    public boolean delete(@PathVariable Long application, @PathVariable String featuretype,
                          @PathVariable String fid) throws Exception {
        List<ApplicationLayer> applicationLayers = getApplayers(Collections.singletonList(featuretype), application, true);

        if (applicationLayers.isEmpty()) {
            throw new IllegalArgumentException("Featuretype has no applayers configured in DB");
        }
        ApplicationLayer appLayer = applicationLayers.get(0);

        Layer layer = getLayer(appLayer);
        if (layer.getFeatureType() == null) {
            throw new IllegalArgumentException("Layer has no featuretype configured");
        }

        SimpleFeatureType sft = FeatureSourceFactoryHelper.getSimpleFeatureType(layer, featuretype);

        try {
            EditFeatureHelper.deleteFeature(appLayer, em, fid, sft);
            return true;
        } catch (Exception e) {
            log.error("Cannot remove feature: ", e);
            return false;
        }
    }

    @GetMapping(value = "/info/{appId}/{featureTypes}")
    public List<FeaturetypeMetadata> featuretypeInformation(@PathVariable Long appId,
                                                            @PathVariable List<String> featureTypes) {
        List<FeaturetypeMetadata> md = new ArrayList<>();
        List<ApplicationLayer> appLayers = getApplayers(featureTypes, appId, false);

        appLayers.forEach(al -> {
            FeaturetypeMetadata fm = new FeaturetypeMetadata();
            Layer l = getLayer(al);
            if (l != null && l.getFeatureType() != null) {
                SimpleFeatureType sft = l.getFeatureType();
                String featureTypeName = sft.getTypeName();
                fm.featuretypeName = featureTypeName;
                fm.geometryAttribute = sft.getGeometryAttribute();
                fm.geometryType = GeometryType.fromValue(sft.getAttribute(fm.geometryAttribute).getType()).orElse(GeometryType.GEOMETRY);
                md.add(fm);
            }
        });
        return md;
    }

    private Layer getLayer(ApplicationLayer al) {
        Layer l = al.getService().getLayer(al.getLayerName(), em);
        return l;
    }

    private List<ApplicationLayer> getApplayers(List<String> featureTypes, Long appId, boolean forWriting) {
        Application app = appRepo.findById(appId).orElseThrow();
        Application.TreeCache tc = app.loadTreeCache(this.em);
        List<ApplicationLayer> all = tc.getApplicationLayers();
        List<ApplicationLayer> appLayers = filterAppLayers(featureTypes, all);

        appLayers.addAll(UserLayerHelper.getOriginalsFromUserLayers(featureTypes, all, forWriting, layerRepo));
        return appLayers;
    }

    private List<ApplicationLayer> filterAppLayers(List<String> featureTypes, List<ApplicationLayer> all) {
        all = filterAppLayers(all);
        List<ApplicationLayer> appLayers = all.stream().filter(applicationLayer ->
                appLayerInFeatureTypes(applicationLayer, featureTypes)).collect(Collectors.toList());
        return appLayers;
    }

    private List<ApplicationLayer> filterAppLayers(List<ApplicationLayer> all) {
        List<ApplicationLayer> appLayers = all.stream().filter(applicationLayer -> {
            Layer l = applicationLayer.getService().getLayer(applicationLayer.getLayerName(), em);
            return l != null && l.getFeatureType() != null;
        }).collect(Collectors.toList());
        return appLayers;
    }

    //this method must/can be removed if the layername isn't changed by the frontend and passport converter anymore
    private boolean appLayerInFeatureTypes(ApplicationLayer al, List<String> featureTypes) {
        String origName = al.getLayerName();
        Set<String> visitedFeatureTypes = new LinkedHashSet<>();
        Layer l = getLayer(al);

        SimpleFeatureType sft = l.getFeatureType();
        return appLayerInFeatureTypes(sft, featureTypes, visitedFeatureTypes);
    }

    private boolean appLayerInFeatureTypes(SimpleFeatureType sft, List<String> featureTypes, Set<String> visitedFeatureTypes) {
        if(visitedFeatureTypes.contains(sft.getTypeName())){
            return false;
        } else {
            visitedFeatureTypes.add(sft.getTypeName());
        }
        if (appLayerInFeatureTypes(sft.getTypeName(), featureTypes)) {
            return true;
        }

        List<SimpleFeatureType> sfts = sft.getRelations().stream().map(featureTypeRelation -> featureTypeRelation.getForeignFeatureType()).collect(Collectors.toList());
        for (SimpleFeatureType type : sfts) {
            if (appLayerInFeatureTypes(type, featureTypes, visitedFeatureTypes)) {
                return true;
            }
        }
        return false;
    }

    private boolean appLayerInFeatureTypes(String origName, List<String> featureTypes) {
        int index = origName.indexOf(":");
        boolean inNonNamespaced = false;
        if(index != -1 ){
            String nonNameSpaced =FeatureHelper.stripNamespace(origName);
            inNonNamespaced = itemInList(nonNameSpaced, featureTypes);
        }

        return itemInList(origName, featureTypes) || inNonNamespaced;
    }

    private boolean itemInList(String item, List<String> list){
        for (String st: list) {
            if(st.equals(item) || FeatureHelper.stripNamespace(st).equals(item)){
                return true;
            }
        }
        return false;
    }

    private ApplicationLayer getAppLayer(String featuretype, Long application) {
        List<ApplicationLayer> applicationLayers = getApplayers(Collections.singletonList(featuretype), application, true);

        if (applicationLayers.isEmpty()) {
            throw new IllegalArgumentException("Featuretype has no applayers configured in DB");
        }
        for (ApplicationLayer layer: applicationLayers) {
            if(layer.getLayerName().equals(featuretype)){
                return layer;
            }
        }
        return applicationLayers.get(applicationLayers.size()-1);
    }

}
