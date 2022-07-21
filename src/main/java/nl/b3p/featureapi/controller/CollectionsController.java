package nl.b3p.featureapi.controller;

import nl.b3p.featureapi.helpers.FilterHelper;
import nl.b3p.featureapi.repository.gbi.CollectionRepository;
import nl.b3p.featureapi.resource.gbi.Collection;
import nl.b3p.featureapi.resource.gbi.CollectionModel;
import nl.b3p.featureapi.resource.gbi.CollectionValues;
import nl.b3p.featureapi.resource.gbi.LayerModel;
import org.geotools.data.jdbc.FilterToSQLException;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.jdbc.BasicSQLDialect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("collection")
public class CollectionsController {
    Logger log = LoggerFactory.getLogger(CollectionsController.class);

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private CollectionRepository repo;

    @Autowired
    private DataSource dataSource;

    @PostMapping
    public boolean add(@RequestBody CollectionModel collectionModel, @RequestHeader("X-Remote-User") String username) {
        try {
            String uuid = UUID.randomUUID().toString();
            Collection collection = new Collection();
            collection.setCollection_guid(uuid);
            collection.setValues(getDataStore(collectionModel.getLayers(),uuid));
            collection.setDatum(new Date());
            collection.setGuid_based("Y");
            collection.setBron("GBImaps");
            collection.setGebruiker(username);
            collection.setNaam(collectionModel.getNaam());
            collection.setRetentie(collectionModel.getRetentie());
            collection.setOmschrijving(collectionModel.getOmschrijving());
            repo.save(collection);
            return true;
        } catch (Exception e) {
            log.error("Kan geen gbiCollections maken", e);
            return false;
        }
    }

    private List<CollectionValues> getDataStore(LayerModel[] layers, String uuid) throws SQLException, FilterToSQLException, CQLException, IOException {
        List<CollectionValues> values = new ArrayList<>();
        try {
            Connection c = dataSource.getConnection();
            for (LayerModel layerModel : layers) {
                String featureTypeName = layerModel.getFeatureTypeName();
                String sqlWhere = "";
                if (!layerModel.getStringFilter().isEmpty()) {
                    sqlWhere = FilterHelper.getSQLQuery(layerModel.getStringFilter(), em);
                }
                layerModel.getStringFilter();
                ResultSet res = c.prepareStatement("select id, object_guid from " + featureTypeName + " " + sqlWhere).executeQuery();
                String originalFeatureTypeName = layerModel.getUserlayer_original_feature_type_name();
                while (res.next()) {
                    CollectionValues cv = new CollectionValues();
                    cv.setObject_guid(res.getString("object_guid"));
                    cv.setTabel(originalFeatureTypeName);
                    cv.setObject_id(res.getInt("id"));
                    values.add(cv);
                }
            }
            return values;
        } catch (Exception e) {
            log.error("Kan geen collectionValues maken", e);
            throw e;
        }
    }
}
