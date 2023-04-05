package ai.openfabric.api.repository;

import ai.openfabric.api.model.NetworkStats;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NetworkStatsRepository  extends CrudRepository<NetworkStats, String> {
}
