package ai.openfabric.api.repository;

import ai.openfabric.api.model.MemoryStats;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemoryStatsRepository extends CrudRepository<MemoryStats, String> {
}
