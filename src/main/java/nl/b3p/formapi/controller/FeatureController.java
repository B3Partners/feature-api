package nl.b3p.formapi.controller;

import nl.b3p.formapi.repository.ApplicationRepo;
import nl.b3p.formapi.repository.SimpleFeatureTypeRepo;
import nl.b3p.formapi.resource.Feature;
import nl.b3p.formapi.resource.TailormapFeature;
import nl.b3p.geotools.data.arcgis.ArcGISDataStoreFactory;
import nl.b3p.viewer.config.app.Application;
import nl.b3p.viewer.config.app.ApplicationLayer;
import nl.b3p.viewer.config.services.JDBCFeatureSource;
import nl.b3p.viewer.config.services.SimpleFeatureType;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.wfs.WFSDataStoreFactory;
import org.geotools.feature.FeatureIterator;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.geotools.referencing.CRS;
import org.json.JSONException;
import org.json.JSONObject;
import org.opengis.feature.simple.SimpleFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class FeatureController {
    Logger log = LoggerFactory.getLogger(FeatureController.class);

    @Autowired
    private ApplicationRepo appRepo;

    @Autowired
    private EntityManager em;

    @Autowired
    private SimpleFeatureTypeRepo featureTypeRepo;

    @GetMapping(value = "/features/{application}/{featuretypes}/{x}/{y}/{scale}")
    public List<Feature> onPoint(@PathVariable Long application, @PathVariable Long[] featuretypes, @PathVariable double x, @PathVariable double y, @PathVariable double scale){


        /*
            gegeven een featuretype[] en een locatie, ga per featuretype
                creeer je simplefeaturetypeschema obv type uit flamingodb
                doe een select obv locatie
                geef JSON features terug.
        */

        List<Feature> features = new ArrayList<>();

        for (Long featureType : featuretypes) {
            features.addAll(onPoint(application, featureType, x, y, scale));
        }


        return features;
    }

    public List<Feature> onPoint(@PathVariable Long application, @PathVariable Long featuretype, @PathVariable double x, @PathVariable double y, @PathVariable double scale){

        List<Feature> features = new ArrayList<>();
        // get feautresource
        FeatureSource fs = null;
        FeatureIterator<SimpleFeature> it = null;
        try {

            Application app = appRepo.findById(application).orElseThrow();
            app.loadTreeCache(this.em);
            List<ApplicationLayer> applayers = app.getTreeCache().getApplicationLayers();
            SimpleFeatureType sft = featureTypeRepo.findById(featuretype).orElseThrow();
            fs =getFeatureSource(sft);
            // haal layers op adhv sft
            // haal applayers op adhv layers en application
            // build filter
            Query q = new Query(sft.getTypeName());
            q.setMaxFeatures(10);


            it=fs.getFeatures(q).features();
            List<String> propertyNames = getAttributes();

            // do request
            while (it.hasNext()) {
                SimpleFeature feature = it.next();
                Feature jsonFeature = new TailormapFeature();
                Feature j = this.toJSONFeature(jsonFeature, feature,  propertyNames);
                features.add(j);
            }
            // convert response to json
        } catch (IOException e) {
            log.error("IOException ", e);
        } catch (Exception e) {
            log.error("Exception ", e);
        }finally{
            if (it!=null){
                it.close();
            }
            fs.getDataStore().dispose();
        }
        return features;
    }

    public static final String FID = "__fid";
    private Feature toJSONFeature(Feature j, SimpleFeature f, List<String> propertyNames) throws JSONException, Exception {

        for (String name : propertyNames) {
            j.put(name, f.getAttribute(name));
        }

        //if edit and not yet set
        // removed check for edit variable here because we need to compare features in edit component and feature info attributes
        // was if(edit && j.optString(FID,null)==null) {

        return j;
    }


    public List<String> getAttributes(){
        List<String> attrs = new ArrayList<>();
        attrs.add("object_guid");
        attrs.add("verhardingstype");
        attrs.add("structuurelement");
        attrs.add("std_structuurelement");
        return attrs;
    }

    public FeatureSource getFeatureSource(SimpleFeatureType sft) throws Exception {

        DataStore ds = getDatastore(sft);

        return ds.getFeatureSource(sft.getTypeName());
    }

    public DataStore getDatastore(SimpleFeatureType sft) throws Exception {
        nl.b3p.viewer.config.services.FeatureSource fs = sft.getFeatureSource();
        Map params = new HashMap();
        switch(fs.getProtocol()){
            case "jdbc":
                String connectionJSON = fs.getUrl();
                JSONObject obj = new JSONObject(connectionJSON);

                params.put("dbtype", obj.get("dbtype"));
                params.put("host", obj.get("host"));
                params.put("port", obj.get("port"));
                params.put("database", obj.get("database"));
                params.put(JDBCDataStoreFactory.FETCHSIZE.key,50);
                params.put(JDBCDataStoreFactory.EXPOSE_PK.key, true);
                params.put(JDBCDataStoreFactory.PK_METADATA_TABLE.key, "gt_pk_metadata");
                params.put("schema", ((JDBCFeatureSource)fs).getSchema());

                break;
            case "wfs":
                String wfsUrl = fs.getUrl();
                if (!wfsUrl.endsWith("&") && !wfsUrl.endsWith("?")) {
                    wfsUrl += wfsUrl.indexOf("?") >= 0 ? "&" : "?";
                }
                wfsUrl = wfsUrl + "REQUEST=GetCapabilities&SERVICE=WFS";
                if(!wfsUrl.toUpperCase().contains("VERSION")){
                    wfsUrl += "&VERSION=1.1.0";
                }
                params.put(WFSDataStoreFactory.URL.key, wfsUrl);
                break;
            case "arcgis":

                params.put(ArcGISDataStoreFactory.CRS.key, CRS.decode("EPSG:28992"));
                break;

        }
        params.put("user", fs.getUsername());
        params.put("passwd", fs.getPassword());


        log.debug("Opening datastore using parameters: " + params);
        try {
            DataStore ds = DataStoreFinder.getDataStore(params);
            if(ds == null) {
                throw new Exception("Cannot open datastore using parameters " + params);
            }
            return ds;
        } catch(Exception e) {
            throw new Exception("Cannot open datastore using parameters " + params, e);
        }
    }
}
