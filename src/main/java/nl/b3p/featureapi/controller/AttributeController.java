package nl.b3p.featureapi.controller;


import nl.b3p.featureapi.helpers.FeatureSourceFactoryHelper;
import nl.b3p.featureapi.repository.fla.FeatureSourceRepo;
import nl.b3p.featureapi.resource.koppellijst.Attribuut;
import nl.b3p.featureapi.resource.koppellijst.Domein;
import nl.b3p.featureapi.resource.koppellijst.Domeinwaarde;
import nl.tailormap.viewer.config.metadata.Metadata;
import nl.tailormap.viewer.config.services.FeatureSource;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureIterator;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.util.factory.GeoTools;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.identity.FeatureId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.io.IOException;
import java.util.*;

@RestController
public class AttributeController {

/*
    @GetMapping("attributes2/{ids}")
    public Map<BigInteger,LinkedAttribute> attributes2(@PathVariable Set<Long> ids){

        // haal per id een attribuut op
            // per attribuut het domein
                // per domein de waardes
                    // per waarde de parentwaarde
                // per domein de gelinkte domeinen
                    // per gelinkt domein de waardes
                // per domein het ouderdomein
                    // per ouderdomein de waarde voor

        List<Attribuut> attrs = em.createQuery("FROM Attribuut a Where a.id in :ids",
                Attribuut.class).setParameter("ids", ids).getResultList();
        Map<Long,LinkedAttribute> linkedAttributes = new HashMap<>();
        for (Attribuut attr: attrs) {
            linkedAttributes.put(attr.getId(), processAttribute(attr));
    }
        return linkedAttributes;
    }

    private LinkedAttribute processAttribute(Attribuut attr){
        LinkedAttribute la = new LinkedAttribute();
        la.setFeatureType(attr.getObject_naam());
        la.setDomeinId(attr.getDomein().getId());
        la.setId(attr.getId());
        la.setNaam(attr.getNaam());
        la.setTabel_naam(attr.getTabel_naam());

        List<LinkedValue> values = new ArrayList<>();
        la.setValues(values);
        attr.getDomein().getWaardes().forEach(waarde ->{

            LinkedValue lv = domeinWaardeToLinkedValue(waarde);
            values.add(lv);

            List<Domeinwaarde> linkedDomeinWaardes = waarde.getLinkedDomeinwaardes();
            Map<Long, List<LinkedValue>> childsPerDomain = lv.getChildDomainValues();
            for (Domeinwaarde childwaarde: linkedDomeinWaardes) {
                Long childdomeinid = childwaarde.getDomein().getId();
                if(!childsPerDomain.containsKey(childdomeinid)){
                    childsPerDomain.put(childdomeinid, new ArrayList<>());
        }
                LinkedValue child = domeinWaardeToLinkedValue(childwaarde);
                childsPerDomain.get(childdomeinid).add(child);
            }
        });
        return la;
    }

    private LinkedValue domeinWaardeToLinkedValue(Domeinwaarde waarde){
        LinkedValue lv = new LinkedValue();
        lv.setId(waarde.getId());
        lv.setDomeinid(waarde.getDomein().getId());
        lv.setValue(waarde.getWaarde());
        Domein parent = waarde.getDomein().getParent();
        if(parent != null){
            lv.setParentdomeinid(parent.getId());
            lv.setParentValue(retrieveParentValue(parent, waarde));
        }
        return lv;
    }

    private LinkedValue retrieveParentValue(Domein parent, Domeinwaarde current){
        List<Domeinwaarde> parentwaardes = parent.getWaardes();
        for (Domeinwaarde parentwaarde: parentwaardes) {
            List<Domeinwaarde> linkedParentWaardes = parentwaarde.getLinkedDomeinwaardes();
            for (Domeinwaarde value : linkedParentWaardes) {
                if (value.getDomein().getId() == current.getDomein().getId() && value.getWaarde().equals(current.getWaarde())) {
                    return domeinWaardeToLinkedValue(parentwaarde);
                }
            }
        }
        return null;
    }

    @GetMapping("attributes")
    public Page<Attribuut> paged(@ParameterObject Pageable page){
        return repo.findAll(page);
    }
*/

    Logger log = LoggerFactory.getLogger(AttributeController.class);
    @Autowired
    private FeatureSourceRepo featureSourceRepo;

    @PersistenceContext
    private EntityManager em;

    @GetMapping("attributes/{ids}")
    public List<Attribuut> attributes(@PathVariable Set<Long> ids) {
        return getAttributes(ids);
    }

    private FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());

    private List<Attribuut> getAttributes(Set<Long> ids) {
        List<Attribuut> attributes = new ArrayList<>();
        DataStore datastore = null;
        try {
            Metadata meta = em.createQuery("FROM Metadata where configKey = :formFeatureSource", Metadata.class)
                    .setParameter("formFeatureSource", Metadata.DEFAULT_FORM_FEATURESOURCE).getSingleResult();
            Long fsId = Long.parseLong(meta.getConfigValue());
            FeatureSource fs = featureSourceRepo.findById(fsId).orElseThrow();
            datastore = FeatureSourceFactoryHelper.getDatastore(fs);
            SimpleFeatureSource attribuutSft = datastore.getFeatureSource("attribuut");
            SimpleFeatureSource domeinSft = datastore.getFeatureSource("domein");
            SimpleFeatureSource domeinKoppelingSft = datastore.getFeatureSource("domein_koppeling");
            SimpleFeatureSource domeinwaardeSft = datastore.getFeatureSource("domeinwaarde");
            SimpleFeatureSource domwrdKoppelingSft = datastore.getFeatureSource("domwrd_koppeling");

            Set<FeatureId> fids = new HashSet<>();
            for (Long id : ids) {
                fids.add(ff.featureId("attribuut." + id));
            }
            Filter filter = ff.id(fids);
            List<SimpleFeature> features = getFeatures(filter, attribuutSft);
            for (SimpleFeature feature : features) {
                attributes.add(createAttribuut(feature, domeinKoppelingSft, domeinSft, domeinwaardeSft, domwrdKoppelingSft));
            }
        } catch (NoResultException e) {
            log.error("Cannot retrieve default featuresource for attributes", e);
        } catch (Exception e) {
            log.error("Cannot retrieve default featuresource for attributes", e);
        } finally {
            if (datastore != null) {
                datastore.dispose();
            }
        }
        return attributes;
    }

    private List<SimpleFeature> getFeatures(Filter f, SimpleFeatureSource fs) throws IOException {
        FeatureIterator<SimpleFeature> it = null;
        List<SimpleFeature> features = new ArrayList<>();
        try {
            it = fs.getFeatures(f).features();
            while (it.hasNext()) {
                SimpleFeature feature = it.next();
                features.add(feature);
            }
        } finally {
            if (it != null) {
                it.close();
            }
        }
        return features;
    }

    private Attribuut createAttribuut(SimpleFeature f, SimpleFeatureSource domeinKoppelingSft, SimpleFeatureSource domeinSft,
                                      SimpleFeatureSource domeinwaardeSft, SimpleFeatureSource domeinwaardeKoppelingSft) throws IOException, CQLException {
        Attribuut a = new Attribuut();
        String id = f.getID();
        a.setId(Long.parseLong(id.substring(id.lastIndexOf(".") + 1)));
        a.setNaam((String) f.getAttribute("naam"));
        Object mut = f.getAttribute("muteerbaar");
        a.setMuteerbaar(mut != null ? (Boolean) mut : null);
        a.setKolom_naam((String) f.getAttribute("kolom_naam"));
        a.setObject_naam((String) f.getAttribute("object_naam"));
        a.setTabel_naam((String) f.getAttribute("tabel_naam"));
        a.setObject_naam((String) f.getAttribute("object_naam"));
        Integer domeinId = (Integer) f.getAttribute("domein_id");
        a.setDomein(createDomein(domeinKoppelingSft, domeinSft, domeinwaardeSft, domeinwaardeKoppelingSft, domeinId));
        return a;
    }

    private Domein createDomein(SimpleFeatureSource domeinKoppelingSft, SimpleFeatureSource domeinSft,
                                SimpleFeatureSource domeinwaardeSft, SimpleFeatureSource domeinwaardeKoppelingSft, Integer domeinId) throws IOException, CQLException {
        Domein d = new Domein();

        Filter fid = ff.id(ff.featureId("domein." + domeinId));
        List<SimpleFeature> domeinFeatures = getFeatures(fid, domeinSft);
        for (SimpleFeature feature : domeinFeatures) {
            String id = feature.getID();
            d.setId(Long.parseLong(id.substring(id.lastIndexOf(".") + 1)));
            d.setNaam((String) feature.getAttribute("naam"));
            Object lt = feature.getAttribute("leeg_toestaan");
            d.setLeeg_toestaan(lt != null ? (lt.equals("Y")) : null);
            d.setLinkedDomein(getLinkedDomeins(domeinKoppelingSft, domeinSft, domeinwaardeSft,domeinwaardeKoppelingSft, d));
            d.setWaardes(getWaardes(domeinwaardeSft, domeinwaardeKoppelingSft, d));
        }
        return d;
    }

    private List<Domeinwaarde> getWaardes(SimpleFeatureSource domeinwaardeSft, SimpleFeatureSource domeinwaardeKoppelingSft, Domein domein) throws CQLException, IOException {
        List<Domeinwaarde> waardes = new ArrayList<>();
        Filter domeinId = ECQL.toFilter("domein_id = " + domein.getId());
        List<SimpleFeature> features = getFeatures(domeinId, domeinwaardeSft);
        for (SimpleFeature feature : features) {
            waardes.add(createWaarde(feature, domeinwaardeSft, domeinwaardeKoppelingSft, true));
        }

        return waardes;
    }

    private Domeinwaarde createWaarde(SimpleFeature f, SimpleFeatureSource domeinwaardeSft, SimpleFeatureSource domeinwaardeKoppelingSft, boolean first) throws CQLException, IOException {
        Domeinwaarde domeinwaarde = new Domeinwaarde();
        String id = f.getID();
        domeinwaarde.setId(Long.parseLong(id.substring(id.lastIndexOf(".")+1)));
        domeinwaarde.setDomein_id((Integer)f.getAttribute("domein_id"));
        domeinwaarde.setWaarde((String)f.getAttribute("waarde"));
        domeinwaarde.setAfkorting(nullOrValue(f.getAttribute("afkorting")));
        domeinwaarde.setSynoniem(nullOrValue(f.getAttribute("synoniem")));
        Object volgorde = f.getAttribute("volgorde");
        domeinwaarde.setVolgorde(volgorde != null ? (Double) volgorde : null);

        if(first){
            domeinwaarde.setLinkedDomeinwaardes(getLinkedWaardes(domeinwaarde, domeinwaardeSft, domeinwaardeKoppelingSft));
        }

        return domeinwaarde;
    }

    private List<Domeinwaarde> getLinkedWaardes(Domeinwaarde parent, SimpleFeatureSource domeinwaardeSft, SimpleFeatureSource domeinwaardeKoppelingSft) throws CQLException, IOException {
        List<Domeinwaarde> waardes = new ArrayList<>();

        Filter parentId = ECQL.toFilter("domwrd_parent_id = " + parent.getId());
        List<SimpleFeature> children = getFeatures(parentId, domeinwaardeKoppelingSft);
        List<Filter> linkedIds = new ArrayList<>();
        for (SimpleFeature child : children) {
            Integer childId = (Integer) child.getAttribute("domwrd_child_id");
            linkedIds.add(ff.id(ff.featureId("domeinwaarde." +childId)));
        }

        if(!linkedIds.isEmpty()) {
            Filter linkedWaardes = ff.and(linkedIds);
            List<SimpleFeature> fs = getFeatures(linkedWaardes, domeinwaardeSft);
            for (SimpleFeature f : fs) {
                waardes.add(createWaarde(f, domeinwaardeSft, domeinwaardeKoppelingSft, false));
            }
        }
        return waardes;
    }

    private List<Domein> getLinkedDomeins(SimpleFeatureSource domeinKoppelingSft, SimpleFeatureSource domeinSft,
                                          SimpleFeatureSource domeinwaardeSft, SimpleFeatureSource domeinwaardeKoppelingSft, Domein parent) throws IOException, CQLException {
        List<Domein> children = new ArrayList<>();
        Filter parentid = ECQL.toFilter("domein_parent_id = " + parent.getId());
        List<SimpleFeature> koppelingen = getFeatures(parentid, domeinKoppelingSft);
        for (SimpleFeature f : koppelingen) {
            Domein child = createDomein(domeinKoppelingSft, domeinSft, domeinwaardeSft, domeinwaardeKoppelingSft, (Integer) f.getAttribute("domein_child_id"));
            child.setParent(parent);
            children.add(child);
        }
        return children;
    }

    private String nullOrValue(Object val){
        return val != null ? (String) val : null;
    }

}
