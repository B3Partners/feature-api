package nl.b3p.featureapi.repository;

import nl.b3p.viewer.config.app.ApplicationLayer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationLayerRepo extends JpaRepository<ApplicationLayer, Long> {
}
