package ai.openfabric.api.repository;

import ai.openfabric.api.model.NetworkStats;
import org.springframework.data.repository.CrudRepository;

public interface NetworkStatsRepository  extends CrudRepository<NetworkStats, String> {
}
