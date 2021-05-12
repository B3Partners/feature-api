package nl.b3p.featureapi.repository;

import nl.viewer.config.services.FeatureSource;
import nl.viewer.config.services.SimpleFeatureType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeatureSourceRepo extends JpaRepository<FeatureSource, Long> {
}