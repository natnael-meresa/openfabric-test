package ai.openfabric.api.repository;

import ai.openfabric.api.model.CpuStats;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CpuStatsRepository extends CrudRepository<CpuStats, String> {
}
