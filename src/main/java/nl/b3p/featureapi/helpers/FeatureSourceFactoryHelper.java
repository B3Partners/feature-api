package nl.b3p.featureapi.helpers;

import nl.tailormap.geotools.data.arcgis.ArcGISDataStoreFactory;
import nl.tailormap.viewer.config.services.*;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.data.wfs.WFSDataStoreFactory;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.geotools.referencing.CRS;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class FeatureSourceFactoryHelper {
    private static Logger log = LoggerFactory.getLogger(FeatureSourceFactoryHelper.class);
    private static final int TIMEOUT = 5000;

    public static SimpleFeatureType getSimpleFeatureType(Layer layer, String typename){
        SimpleFeatureType type = layer.getFeatureType();
        Set<String> visitedFeatureTypes = new LinkedHashSet<>();
        return getSimpleFeatureType(type, typename, visitedFeatureTypes);
    }
    public static SimpleFeatureType getSimpleFeatureType(SimpleFeatureType sft, String typename, Set<String> visitedFeatureTypes){
        if(isSimpleFeatureTypeTypename(typename, sft)){
            return sft;
        }else if(visitedFeatureTypes.contains(sft.getTypeName())){
            return null;
        }else{
            visitedFeatureTypes.add(sft.getTypeName());
            for(FeatureTypeRelation rel : sft.getRelations()){
                SimpleFeatureType relSft = getSimpleFeatureType(rel.getForeignFeatureType(), typename, visitedFeatureTypes);
                if(relSft != null){
                    return relSft;
                }
            }
            return null;
        }
    }

    public static FeatureTypeRelation getParentRelation(SimpleFeatureType sft, String typename, FeatureTypeRelation parentRel){
        if(isSimpleFeatureTypeTypename(typename, sft)){
            return parentRel;
        }else{
            for(FeatureTypeRelation rel : sft.getRelations()){
                FeatureTypeRelation finalParentRel = getParentRelation(rel.getForeignFeatureType(), typename, rel);
                if(finalParentRel != null){
                    return finalParentRel;
                }
            }
            return null;
        }
    }

    private static boolean isSimpleFeatureTypeTypename(String name, SimpleFeatureType sft){
        return  name.equals(sft.getTypeName());
    }

    public static FeatureSource getFeatureSource(SimpleFeatureType sft) throws Exception {

        DataStore ds = getDatastore(sft);

        return ds.getFeatureSource(sft.getTypeName());
    }

    public static DataStore getDatastore(SimpleFeatureType sft) throws Exception {
        nl.tailormap.viewer.config.services.FeatureSource fs = sft.getFeatureSource();
        return getDatastore(fs);
    }

    public static DataStore getDatastore(nl.tailormap.viewer.config.services.FeatureSource fs) throws Exception {
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

                params.put(WFSDataStoreFactory.TIMEOUT.key, TIMEOUT);
                break;
            case "arcgis":
                params.put(ArcGISDataStoreFactory.CRS.key, CRS.decode("EPSG:28992"));
                params.put(ArcGISDataStoreFactory.TIMEOUT.key, TIMEOUT);
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
