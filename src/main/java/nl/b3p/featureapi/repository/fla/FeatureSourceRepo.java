package nl.b3p.featureapi.repository.fla;

import nl.tailormap.viewer.config.services.FeatureSource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeatureSourceRepo extends JpaRepository<FeatureSource, Long> {
}
