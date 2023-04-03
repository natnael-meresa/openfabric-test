package ai.openfabric.api.repository;

import ai.openfabric.api.model.WorkerStatistics;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkerStatisticsRepository extends CrudRepository<WorkerStatistics, String> {
}
