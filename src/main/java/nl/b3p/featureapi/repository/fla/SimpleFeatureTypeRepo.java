package nl.b3p.featureapi.repository.fla;

import nl.tailormap.viewer.config.services.SimpleFeatureType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SimpleFeatureTypeRepo extends JpaRepository<SimpleFeatureType, Long> {
}
