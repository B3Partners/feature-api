package nl.b3p.featureapi.repository.fla;

import nl.tailormap.viewer.config.services.Layer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LayerRepo extends JpaRepository<Layer, Long> {
    Layer findByNameAndUserlayer(String layerName, boolean userLayer);
}
