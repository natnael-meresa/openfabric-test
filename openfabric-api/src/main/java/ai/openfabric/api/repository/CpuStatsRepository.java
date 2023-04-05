package ai.openfabric.api.repository;

import ai.openfabric.api.model.CpuStats;
import org.springframework.data.repository.CrudRepository;

public interface CpuStatsRepository extends CrudRepository<CpuStats, String> {
}
