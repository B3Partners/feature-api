package nl.b3p.featureapi.repository;

import nl.b3p.viewer.config.app.Application;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepo  extends JpaRepository<Application, Long> {
}
