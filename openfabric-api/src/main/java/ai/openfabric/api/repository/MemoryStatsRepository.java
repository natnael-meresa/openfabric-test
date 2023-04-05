package ai.openfabric.api.repository;

import ai.openfabric.api.model.MemoryStats;
import org.springframework.data.repository.CrudRepository;

public interface MemoryStatsRepository extends CrudRepository<MemoryStats, String> {
}
