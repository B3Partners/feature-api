package nl.b3p.featureapi.controller;

import nl.b3p.featureapi.feature.FeatureHelper;
import nl.b3p.featureapi.helpers.FeatureSourceFactoryHelper;
import nl.b3p.featureapi.repository.ApplicationLayerRepo;
import nl.b3p.featureapi.repository.ApplicationRepo;
import nl.b3p.featureapi.repository.SimpleFeatureTypeRepo;
import nl.b3p.featureapi.resource.Feature;
import nl.b3p.viewer.config.app.Application;
import nl.b3p.viewer.config.app.ApplicationLayer;
import nl.b3p.viewer.config.services.Layer;
import nl.b3p.viewer.config.services.SimpleFeatureType;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;


@RestController
public class FeatureController {
    Logger log = LoggerFactory.getLogger(FeatureController.class);


    private boolean graph = false;

    private boolean ordered = false;

    private boolean edit = false;

    @Autowired
    private ApplicationRepo appRepo;
    @Autowired
    private ApplicationLayerRepo appLayerRepo;

    @Autowired
    private EntityManager em;

    @Autowired
    private SimpleFeatureTypeRepo featureTypeRepo;

    @GetMapping(value = "/features/2/{application}/{appLayerIds}/{x}/{y}/{scale}")
    public List<Feature> onPoint(@PathVariable Long application, @PathVariable Long[] appLayerIds, @PathVariable double x, @PathVariable double y, @PathVariable double scale) throws Exception {

        log.error("pietje");

        /*
            gegeven een featuretype[] en een locatie, ga per featuretype
                creeer je simplefeaturetypeschema obv type uit flamingodb
                doe een select obv locatie
                geef JSON features terug.
        */

        List<Feature> features = new ArrayList<>();

        for (Long appLayerId : appLayerIds) {
            features.addAll(onPoint(application, appLayerId, x, y, scale));
        }


        return features;
    }

    @GetMapping(value = "/features/{application}/{appLayerId}/{x}/{y}/{scale}")
    public List<Feature> onPoint(@PathVariable Long application, @PathVariable Long appLayerId, @PathVariable double x, @PathVariable double y, @PathVariable double scale) throws Exception {
         List<Feature> features = new ArrayList<>();
        FeatureSource fs = null;
        FeatureIterator<SimpleFeature> it = null;
        try {

            Application app = appRepo.findById(application).orElseThrow();
            app.loadTreeCache(this.em);
            ApplicationLayer appLayer = appLayerRepo.findById(appLayerId).orElseThrow();
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

}
