package nl.b3p.featureapi.repository.gbi;

import nl.b3p.featureapi.resource.gbi.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollectionRepository extends JpaRepository<Collection, String> {
}
