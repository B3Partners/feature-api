package nl.b3p.featureapi.helpers;

import nl.b3p.featureapi.feature.FeatureHelper;
import nl.b3p.featureapi.repository.LayerRepo;
import nl.b3p.featureapi.resource.Field;
import nl.b3p.featureapi.resource.Feature;
import nl.viewer.config.app.Application;
import nl.viewer.config.app.ApplicationLayer;
import nl.viewer.config.services.Layer;
import nl.viewer.config.services.SimpleFeatureType;
import org.geotools.data.*;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.identity.FeatureIdImpl;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.PrimaryKey;
import org.json.JSONArray;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.identity.FeatureId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

public class EditFeatureHelper {
    private static Logger log = LoggerFactory.getLogger(EditFeatureHelper.class);
    private static final SimpleDateFormat datetime = new SimpleDateFormat("dd-MM-yyy HH:mm:ss");
    private static final SimpleDateFormat date = new SimpleDateFormat("dd-MM-yyy");
    private static FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

    public static Feature update(ApplicationLayer appLayer, Layer layer, Feature feature, String fid,
                                 EntityManager em, SimpleFeatureType sft, Application app, LayerRepo layerRepo) throws Exception {
        SimpleFeatureStore store = getDatastore(sft);

        Transaction transaction = new DefaultTransaction("edit");
        store.setTransaction(transaction);

        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
        Filter filter = ff.id(new FeatureIdImpl(fid));

        List<String> attributes = new ArrayList<String>();
        List values = new ArrayList();
        for (Field attr: feature.getAttributes()) {


            String attribute = attr.getKey();
            if (!Feature.FID.equals(attribute)) {

                AttributeDescriptor ad = store.getSchema().getDescriptor(attribute);

                if (ad != null) {
                    Object value = attr.getValue();
                    if(value!= null) {
                        if (!isAttributeUserEditingDisabled(attribute, appLayer, layer, sft)) {

                            attributes.add(attribute);


                            if (ad.getType() instanceof org.opengis.feature.type.GeometryType) {
                                String wkt = (String) value;
                                Geometry g = null;
                                if (wkt != null) {
                                    g = new WKTReader().read(wkt);
                                }
                                values.add(g);
                            } else if (ad.getType().getBinding().getCanonicalName().equals("byte[]")) {
                                values.add(value);
                            } else {
                                //    hier gaat iets niet goed: als een attribuut een int is die niet is ingevuld, is de value een lege string
                                values.add(value);
                            }
                        } else {
                            log.info(String.format("Attribute \"%s\" not user editable; ignoring", attribute));
                        }
                    }
                } else {
                    log.warn(String.format("Attribute \"%s\" not in feature type; ignoring", attribute));
                }
            }
        }

        log.error(String.format("Modifying feature source #%d fid=%s, attributes=%s, values=%s",
                layer.getFeatureType().getId(),
                fid,
                attributes.toString(),
                values.toString()));

        try {
            store.modifyFeatures(attributes.toArray(new String[]{}), values.toArray(), filter);

            transaction.commit();
            feature = getFeature(fid, store, appLayer, sft, em, app, layerRepo);
        } catch (Exception e) {
            log.error("Cannot update: ", e);
            transaction.rollback();
            throw e;
        } finally {
            transaction.close();
            if(store != null){
                store.getDataStore().dispose();
            }
        }
        return feature;
    }

    public static boolean updateBulk(SimpleFeatureType sft, EntityManager em, String filter, Map<String, String> updateFields) throws Exception {
        SimpleFeatureStore store = getDatastore(sft);
        if (store.getDataStore() instanceof JDBCDataStore) {
            FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
            JDBCDataStore da = (JDBCDataStore) store.getDataStore();
            PrimaryKey pk = da.getPrimaryKey(store.getSchema());
            if(pk.getColumns().size() > 1) {
                throw new IllegalArgumentException("No support for multiple column primary key");
            }
            String pkColumn = pk.getColumns().get(0).getName();
            String sqlWhere = "";
            if(!filter.isEmpty()) {
                sqlWhere = FilterHelper.getSQLQuery(da, sft.getTypeName(), em, filter);
            }
            List<String> attributes = new ArrayList<>();
            List values = new ArrayList();
            Filter finalFilter  = getFeaturesWithSQL(da, sft, sqlWhere, pkColumn, ff);
            updateFields.forEach((k,v) -> {
                if(v != null) {
                    attributes.add(k);
                    values.add(v);
                }
            });
            try {
                Transaction transaction = new DefaultTransaction("bulk");
                store.setTransaction(transaction);

                store.modifyFeatures(attributes.toArray(new String[]{}), values.toArray(), finalFilter);
                transaction.commit();
            } catch (Exception e) {
                throw (e);
            }
        }
         return true;
    }

    public static boolean deleteFeature(ApplicationLayer appLayer, EntityManager em, String fid, SimpleFeatureType sft) throws Exception {
        SimpleFeatureStore store = getDatastore(sft);

        Transaction transaction = new DefaultTransaction("edit");
        store.setTransaction(transaction);

        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
        Filter filter = ff.id(new FeatureIdImpl(fid));

        try {
            store.removeFeatures(filter);
            transaction.commit();
            return true;
        } catch (Exception e) {
            transaction.rollback();
            throw e;
        } finally {
            transaction.close();
        }
    }

    public static Feature save(ApplicationLayer appLayer, EntityManager em, Feature feature,
                               SimpleFeatureType sft, Application app, LayerRepo layerRepo) throws Exception {
        SimpleFeatureStore store = getDatastore(sft);

        SimpleFeature f = DataUtilities.template(store.getSchema());

        Transaction transaction = new DefaultTransaction("create");
        store.setTransaction(transaction);
        Map<String, Object> attributes = featureToMap(feature);
        for(AttributeDescriptor ad: store.getSchema().getAttributeDescriptors()) {
            Object valueObj = attributes.getOrDefault(ad.getLocalName(), null);

            if(valueObj instanceof String && valueObj.equals("")){
                continue;
            }

            if(ad.getType() instanceof GeometryType) {
                Geometry g = null;
                String value = valueObj != null ? (String) valueObj : null;
                if(value != null) {
                    g = new WKTReader().read(value);
                }
                f.setDefaultGeometry(g);
            } else if(ad.getType().getBinding().equals(java.sql.Date.class) || ad.getType().getBinding().equals(java.sql.Timestamp.class)){

                Date d = null;
                String value = valueObj != null ? (String) valueObj : null;
                if (value != null && !value.isEmpty()) {
                    if (ad.getType().getBinding().equals(java.sql.Timestamp.class)) {
                        d = datetime.parse(value);
                    }else{
                        d = date.parse(value);
                    }
                }
                f.setAttribute(ad.getLocalName(), d);
            } else {
                f.setAttribute(ad.getLocalName(), valueObj);
            }
        }

        log.debug(String.format("Creating new feature in applayer #%d: %s",
                appLayer.getId(),
                f.toString()));

        try {
            List<FeatureId> ids = store.addFeatures(DataUtilities.collection(f));

            transaction.commit();
            String id =ids.get(0).getID();
            Feature saved = getFeature(id, store, appLayer,sft,em, app, layerRepo);
            return saved;
        } catch (Exception e) {
            transaction.rollback();
            throw e;
        } finally {
            transaction.close();
        }
    }

    public static Feature getFeature(String fid, SimpleFeatureStore store, ApplicationLayer appLayer,
                                     SimpleFeatureType sft, EntityManager em, Application app, LayerRepo layerRepo) throws Exception {
        Filter f = ff.id(ff.featureId(fid));
        Query q = new Query(store.getSchema().getTypeName(),f);
        List<Feature> features = FeatureHelper.getFeatures(appLayer,sft,store,q,null, null, em, app, layerRepo);
        return features.get(0);
    }

    private static Map<String, Object> featureToMap(Feature feature){
        Map<String, Object> attrs = new HashMap<>();
        for (Field attr: feature.getAttributes()) {
            attrs.put(attr.getKey(), attr.getValue());
        }
        return attrs;
    }

    /*protected void deleteFeature(String fid) throws IOException, Exception {
        Transaction transaction = new DefaultTransaction("edit");
        store.setTransaction(transaction);

        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
        Filter filter = ff.id(new FeatureIdImpl(fid));

        try {
            store.removeFeatures(filter);
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            throw e;
        } finally {
            transaction.close();
        }
    }
*/

    protected static boolean isAttributeUserEditingDisabled(String attrName, ApplicationLayer appLayer, Layer layer, SimpleFeatureType sft) {
        return appLayer.getAttribute(sft, attrName).isDisableUserEdit();
    }


    private static SimpleFeatureStore getDatastore(ApplicationLayer appLayer, EntityManager em) throws Exception {
        Layer layer = appLayer.getService().getLayer(appLayer.getLayerName(), em);
        if (layer.getFeatureType() == null) {
            // ToDo handle error
            throw new IllegalArgumentException("Layer has no featuretype configured");
        }
        SimpleFeatureType sft = layer.getFeatureType();
        return getDatastore(sft);
    }

    private static SimpleFeatureStore getDatastore(SimpleFeatureType sft ) throws Exception {

        FeatureSource ds = FeatureSourceFactoryHelper.getFeatureSource(sft);
        SimpleFeatureStore store =(SimpleFeatureStore) ds;
        return store;
    }

    private static Filter getFeaturesWithSQL(JDBCDataStore da, SimpleFeatureType ft, String sql, String pkColumn, FilterFactory2 ff) throws SQLException {
        JSONArray features = new JSONArray();
        Connection con = null;
        Filter filter = null;
        try {
            con = da.getConnection(new DefaultTransaction("get-features"));
            // Gebruik orginele laag naam om de PK('s) te vinden voor een user-layer
            // Deze is nodig om een __FID te maken
            String featureTypeName = ft.getTypeName();
            PrimaryKey pk = da.getPrimaryKey(da.getSchema(featureTypeName));

            PreparedStatement prep;
            prep = con.prepareStatement("SELECT " + pkColumn + " FROM " + ft.getTypeName() + " " + sql);
            ResultSet rs = prep.executeQuery();
            Set<FeatureId> fids = new HashSet<FeatureId>();
            while(rs.next()) {
                String s = rs.getObject(pkColumn).toString();
                fids.add(ff.featureId(s));
            }
            filter = ff.id(fids);
        } catch (IOException | SQLException e) {
            log.error("Can't get features with sql for JDBCDatastore: " + e.getMessage());
        } finally {
            if (con != null) {
                con.close();
            }
        }
        return filter;
    }
}
