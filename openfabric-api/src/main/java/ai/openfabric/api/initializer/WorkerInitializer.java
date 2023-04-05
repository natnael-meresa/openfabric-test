package ai.openfabric.api.initializer;

import ai.openfabric.api.model.*;
import ai.openfabric.api.repository.*;
import ai.openfabric.api.service.DockerClientService;
import ai.openfabric.api.service.WorkerService;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class WorkerInitializer implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private WorkerRepository workerRepository;
    @Autowired
    private WorkerService workerService;
    @Autowired
    private NetworkStatsRepository networkStatsRepository;
    @Autowired
    private MemoryStatsRepository memoryStatsRepository;
    @Autowired
    private CpuStatsRepository cpuStatsRepository;

    @Autowired
    private WorkerStatisticsRepository workerStatisticsRepository;

    DockerClientService dockerClientService = new DockerClientService();
    DockerClient dockerClient  = dockerClientService.InstantiatDockerClient();
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {

        List<Container> allWorkers = dockerClient.listContainersCmd().withShowAll(true).exec();

        for (Container worker : allWorkers) {
            Optional<Worker> existingWorkerEntity = workerRepository.findByName(worker.getNames()[0].substring(1));
            if (existingWorkerEntity.isPresent()) {
                // Check for updates
                Worker workerEntity = existingWorkerEntity.get();
                if (worker.getState().toLowerCase().equals(workerEntity.getStatus())) {
                    continue;
                } else {
                    workerEntity.setStatus(WorkerStatus.valueOf(worker.getState().toLowerCase()));
                    workerRepository.save(workerEntity);
                }
            } else {
                // Create a new container entity
                Worker workerEntity = new Worker();
                workerEntity.setId(worker.getId());
                workerEntity.setName(worker.getNames()[0].substring(1));
                workerEntity.setImageName(worker.getImage());
                workerEntity.setStatus(WorkerStatus.valueOf(worker.getState().toLowerCase()));
                workerEntity.setStatusDescription(worker.getStatus());
                workerEntity.setCommand(worker.getCommand());
                workerEntity.setLabels(worker.getLabels());

                // create an empty WorkerStatistics object and associate it with the worker
                WorkerStatistics workerStats = new WorkerStatistics();

                CpuStats cpuStats = new CpuStats();
                cpuStats.setWorkerStatistics(workerStats);
                workerStats.setCpuStats(cpuStats);
                cpuStatsRepository.save(cpuStats);

                MemoryStats memoryStats = new MemoryStats();
                memoryStats.setWorkerStatistics(workerStats);
                workerStats.setMemoryStats(memoryStats);
                memoryStatsRepository.save(memoryStats);

                NetworkStats networkStats = new NetworkStats();
                networkStats.setWorkerStatistics(workerStats);
                workerStats.setNetworkStats(networkStats);
                networkStatsRepository.save(networkStats);

                workerStats.setWorker(workerEntity);
                workerEntity.setStatistics(workerStats);


                List<Port> ports = Arrays.stream(worker.getPorts())
                        .map(cp -> {
                           Port p =  new Port();
                           p.setIp(cp.getIp());
                           p.setPrivatePort(cp.getPrivatePort());
                           p.setPublicPort(cp.getPublicPort());
                           p.setType(cp.getType());
                           p.setWorker(workerEntity);
                           return p;
                        })
                        .collect(Collectors.toList());

                workerEntity.setPorts(ports);
                workerEntity.setContainerId(worker.getId());
                workerRepository.save(workerEntity);
            }
        }

        workerService.listenForContainerEvents();
    }
}
