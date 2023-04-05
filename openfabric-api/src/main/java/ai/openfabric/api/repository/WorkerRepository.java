package ai.openfabric.api.repository;

import ai.openfabric.api.model.Worker;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

public interface WorkerRepository extends PagingAndSortingRepository<Worker, String> {
    Optional<Worker> findByName(String name);
}
