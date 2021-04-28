package nl.b3p.formapi.repository;

import nl.b3p.viewer.config.app.Application;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepo  extends JpaRepository<Application, Long> {
}
