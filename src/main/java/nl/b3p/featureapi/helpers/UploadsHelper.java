package nl.b3p.featureapi.helpers;

import nl.viewer.config.app.ApplicationLayer;
import nl.viewer.config.app.FileUpload;
import nl.viewer.config.services.Layer;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.persistence.EntityManager;
import java.util.List;

public class UploadsHelper {
    public static JSONObject retrieveUploads(ApplicationLayer appLayer, EntityManager em, String fid){
        JSONObject uploads = new JSONObject();

        Layer layer = appLayer.getService().getLayer(appLayer.getLayerName(), em);
        List<FileUpload> fups = em.createQuery("FROM FileUpload WHERE sft = :sft and fid = :fid", FileUpload.class)
                .setParameter("sft", layer.getFeatureType()).setParameter("fid", fid).getResultList();

        for (FileUpload fup : fups) {
            if (!uploads.has(fup.getType_())) {
                uploads.put(fup.getType_(), new JSONArray());
            }
            JSONArray ar = uploads.getJSONArray(fup.getType_());
            ar.put(fup.toJSON());
        }
        return uploads;
    }
}
