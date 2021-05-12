package nl.b3p.featureapi.feature;

import nl.b3p.featureapi.controller.FeatureController;
import nl.b3p.featureapi.helpers.FeatureSourceFactoryHelper;
import nl.b3p.featureapi.helpers.FilterHelper;
import nl.b3p.featureapi.helpers.UploadsHelper;
import nl.b3p.featureapi.resource.Feature;
import nl.b3p.featureapi.resource.Relation;
import nl.viewer.config.app.Application;
import nl.viewer.config.app.ApplicationLayer;
import nl.viewer.config.app.ConfiguredAttribute;
import nl.viewer.config.services.AttributeDescriptor;
import nl.viewer.config.services.FeatureTypeRelation;
import nl.viewer.config.services.FeatureTypeRelationKey;
import nl.viewer.config.services.SimpleFeatureType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.wfs.WFSDataStore;
import org.geotools.feature.FeatureIterator;
import org.geotools.filter.text.cql2.CQL;
import org.json.JSONException;
import org.json.JSONObject;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeatureHelper {
    private static final Log log = LogFactory.getLog(FeatureHelper.class);
    public static final int MAX_FEATURES = 1000;

    public static List<Feature> getFeatures(ApplicationLayer al, SimpleFeatureType ft, FeatureSource fs, Query q,
                                            String sort, String dir,
                                            EntityManager em, Application application) throws Exception {
        List<Feature> features = new ArrayList<>();

        Map<String, String> attributeAliases = new HashMap<>();
        for (AttributeDescriptor ad : ft.getAttributes()) {
            if (ad.getAlias() != null) {
                attributeAliases.put(ad.getName(), ad.getAlias());
            }
        }
        List<String> propertyNames;
        if (al != null) {
            propertyNames = getPropertyNames(al, q, ft);
        } else {
            propertyNames = new ArrayList<>();
            for (AttributeDescriptor ad : ft.getAttributes()) {
                propertyNames.add(ad.getName());
            }
        }

        setSort(q, propertyNames, sort, dir, ft, fs);
        Integer start = q.getStartIndex();
        if (start == null) {
            start = 0;
        }
        boolean offsetSupported = fs.getQueryCapabilities().isOffsetSupported();
        //if offSet is not supported, get more features (start + the wanted features)
        if (!offsetSupported && q.getMaxFeatures() < MAX_FEATURES || fs.getDataStore() instanceof WFSDataStore) {
            q.setMaxFeatures(q.getMaxFeatures() + start);
        }
        FeatureIterator<SimpleFeature> it = null;
        try {

            it = fs.getFeatures(q).features();
            int featureIndex = 0;
            while (it.hasNext()) {
                SimpleFeature feature = it.next();
                /* if offset not supported and there are more features returned then
                 * only get the features after index >= start*/
                if (offsetSupported || featureIndex >= start) {
                    JSONObject uploads = null;
                    if (al != null
                            && al.getDetails().containsKey("summary.retrieveUploads")
                            && Boolean.parseBoolean(al.getDetails().get("summary.retrieveUploads").getValue())
                            && application != null) {
                        // 'al' can be null when this method is called from DirectSearch
                        uploads = UploadsHelper.retrieveUploads(al, em, feature.getID());
                    }
                    JSONObject jsonFeature = new JSONObject();
                    jsonFeature.put("__UPLOADS__", uploads);
                    // todo process uploads
                    Feature j = createFeature(feature, ft, al, propertyNames, attributeAliases, 0);

                    features.add(j);
                }
                featureIndex++;
            }
        } finally {
            if (it != null) {
                it.close();
            }
            fs.getDataStore().dispose();
        }

        return features;
    }


    private static Feature createFeature(SimpleFeature f, SimpleFeatureType ft, ApplicationLayer al,
                                         List<String> propertyNames, Map<String, String> attributeAliases, int index) throws JSONException, Exception {
        Feature j = new Feature();
        String typename = ft.getTypeName();
        j.setClazz(typename.startsWith(FeatureController.GBI_PREFIX) ? typename.substring(FeatureController.GBI_PREFIX.length()) : typename );

        for (String name : propertyNames) {
            if (f.getAttribute(name) instanceof Geometry) {
                j.put(name, ((Geometry) f.getAttribute(name)).toText());
            } else {
                j.put(name, f.getAttribute(name));
            }
        }

        if (ft.hasRelations()) {
            populateWithRelatedFeatures(j, f, ft, al, index);
        }

        j.put(Feature.FID, f.getIdentifier());
        return j;
    }

    /**
     * Populate the json object with related featues
     */
    private static void populateWithRelatedFeatures(Feature parent, SimpleFeature feature, SimpleFeatureType ft,
                                                    ApplicationLayer al, int index) throws Exception {
        for (FeatureTypeRelation rel : ft.getRelations()) {
            boolean isJoin = rel.getType().equals(FeatureTypeRelation.JOIN);

            FeatureSource foreignFs = FeatureSourceFactoryHelper.getFeatureSource(rel.getForeignFeatureType());
            FeatureIterator<SimpleFeature> foreignIt = null;
            try {
                Query foreignQ = new Query(foreignFs.getName().toString());
                //create filter
                Filter filter = FilterHelper.createFilter(feature, rel);
                if (filter == null) {
                    return;
                }
                //if join only get 1 feature
                if (isJoin) {
                    foreignQ.setMaxFeatures(1);
                }else{
                    Relation r = new Relation();
                    r.setFilter(CQL.toCQL(filter));
                    r.setForeignFeatureTypeId(rel.getForeignFeatureType().getId());
                    r.setForeignFeatureTypeName(rel.getForeignFeatureType().getTypeName());
                    parent.getRelations().add(r);
                }
                foreignQ.setFilter(filter);
                //set propertynames
                List<String> propertyNames;
                if (al != null) {
                    propertyNames = getPropertyNames(null, foreignQ, rel.getForeignFeatureType());
                } else {
                    propertyNames = new ArrayList<>();
                    for (AttributeDescriptor ad : rel.getForeignFeatureType().getAttributes()) {
                        propertyNames.add(ad.getName());
                    }
                }
                if (propertyNames.isEmpty()) {
                    // if there are no properties to retrieve just get out
                    return;
                }
                //get aliases
                Map<String, String> attributeAliases = new HashMap<>();
                for (AttributeDescriptor ad : rel.getForeignFeatureType().getAttributes()) {
                    if (ad.getAlias() != null) {
                        attributeAliases.put(ad.getName(), ad.getAlias());
                    }
                }
                //Get Feature and populate JSON object with the values.
                foreignIt = foreignFs.getFeatures(foreignQ).features();
                while (foreignIt.hasNext()) {
                    SimpleFeature foreignFeature = foreignIt.next();
                    //join it in the same json
                    Feature other = createFeature(foreignFeature, rel.getForeignFeatureType(), al, propertyNames, attributeAliases, index);
                    if (isJoin) {
                        parent.joinAttributes(other);
                    } else {
                        parent.getChildren().add(other);
                    }
                }
            } finally {
                if (foreignIt != null) {
                    foreignIt.close();
                }
                foreignFs.getDataStore().dispose();
            }

        }
    }

    private static List<String> getPropertyNames(ApplicationLayer appLayer, Query q, SimpleFeatureType sft) {
        List<String> propertyNames = new ArrayList<String>();
        boolean haveInvisibleProperties = false;

        if(appLayer != null) {
            for (ConfiguredAttribute ca : appLayer.getAttributes(sft)) {
           /* if ((!edit && !graph && ca.isVisible()) || (edit && ca.isEditable()) || (graph && attributesToInclude.contains(ca.getId()))) {
                propertyNames.add(ca.getAttributeName());
            } else {
                haveInvisibleProperties = true;
            }*/
                // TODO make better
                if (ca.isVisible()) {
                    propertyNames.add(ca.getAttributeName());
                }
            }
        }else{
            for(AttributeDescriptor ad : sft.getAttributes()){
                propertyNames.add(ad.getName());
            }
        }
        if (haveInvisibleProperties) {
            // By default Query retrieves Query.ALL_NAMES
            // Query.NO_NAMES is an empty String array
            q.setPropertyNames(propertyNames);
            // If any related featuretypes are set, add the leftside names in the query
            // don't add them to propertynames, maybe they are not visible
            if (sft.getRelations() != null) {
                List<String> withRelations = new ArrayList<String>();
                withRelations.addAll(propertyNames);
                for (FeatureTypeRelation ftr : sft.getRelations()) {
                    if (ftr.getRelationKeys() != null) {
                        for (FeatureTypeRelationKey key : ftr.getRelationKeys()) {
                            if (!withRelations.contains(key.getLeftSide().getName())) {
                                withRelations.add(key.getLeftSide().getName());
                            }
                        }
                    }
                }
                q.setPropertyNames(withRelations);
            }
        }
        return propertyNames;

    }

    /**
     * Set sort in query based on the index of the propertynames list.
     *
     * @param q             the query on which the sort is added
     * @param propertyNames a list of propertynames for this featuretype
     * @param sort          a Stringified integer. The index of the propertyname
     * @param dir           sorting direction DESC or ASC
     */
    private static void setSort(Query q, List<String> propertyNames, String sort, String dir, SimpleFeatureType ft, FeatureSource fs) {
        String sortAttribute = null;
        if (sort != null) {
            sortAttribute = sort;
        } else {
            /* Use the first property as sort field, otherwise geotools while give a error when quering
             * a featureType without a primary key.
             */
            if ((fs instanceof org.geotools.jdbc.JDBCFeatureSource || fs.getDataStore() instanceof WFSDataStore) && !propertyNames.isEmpty()) {
                int index = 0;
                if (fs.getSchema().getGeometryDescriptor() != null && fs.getSchema().getGeometryDescriptor().getLocalName().equals(propertyNames.get(0))) {
                    if (propertyNames.size() > 1) {
                        index = 1;
                    } else {
                        index = -1;
                    }
                }
                if (index != -1) {
                    sortAttribute = propertyNames.get(index);
                } else if (index == -1 && fs.getSchema().getGeometryDescriptor() != null && fs.getSchema().getGeometryDescriptor().getLocalName().equals(propertyNames.get(0))) {
                    // only requested attribute is the geometry, so figure out a non-requested non-geometry attribute for sorting
                    for (AttributeDescriptor attribute : ft.getAttributes()) {
                        if (!attribute.getName().equals(fs.getSchema().getGeometryDescriptor().getLocalName())) {
                            sortAttribute = attribute.getName();
                            break;
                        }
                    }
                }
            }
        }
        FilterHelper.setSortBy(q, sortAttribute, dir);
    }

}
