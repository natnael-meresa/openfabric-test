package ai.openfabric.api.service;

import ai.openfabric.api.dto.WorkerDTO;
import ai.openfabric.api.model.Port;
import ai.openfabric.api.model.Worker;
import ai.openfabric.api.model.WorkerStatistics;
import ai.openfabric.api.model.WorkerStatus;
import ai.openfabric.api.repository.WorkerRepository;
import ai.openfabric.api.repository.WorkerStatisticsRepository;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Statistics;
import com.github.dockerjava.core.InvocationBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@Service
public class WorkerService {
    private final WorkerRepository workerRepository;

    private final WorkerStatisticsRepository workerStatisticsRepository;

    private static final Logger logger = Logger.getLogger(WorkerService.class.getName());
    DockerClientService dockerClientService = new DockerClientService();
    DockerClient dockerClient  = dockerClientService.InstantiatDockerClient();

    public WorkerService(WorkerRepository workerRepository, WorkerStatisticsRepository workerStatisticsRepository) {
        this.workerRepository = workerRepository;
        this.workerStatisticsRepository = workerStatisticsRepository;
    }

    public Worker createWorker(WorkerDTO workerDTO) throws Exception {
        try {
            // create container using DockerClient
            List<ExposedPort> exposedPorts = new ArrayList<>();
            for (Port port: workerDTO.getPorts()) {
                ExposedPort exposedPort;
                if (port.getType().equals("udp")) {
                    exposedPort = ExposedPort.udp(port.getPublicPort());
                } else {
                    exposedPort = ExposedPort.tcp(port.getPublicPort());
                }

                exposedPorts.add(exposedPort);
            }

            CreateContainerResponse containerResponse = dockerClient
                    .createContainerCmd(workerDTO.getImageName())
                    .withName(workerDTO.getName())
                    .withLabels(workerDTO.labels)
                    .withCmd(workerDTO.command)
                    .withExposedPorts(exposedPorts)
                    .exec();

            // create worker entity and save to database
            Worker worker = new Worker();
            worker.setName(workerDTO.getName());
            worker.setStatus(WorkerStatus.created);
            worker.setStatusDescription(workerDTO.getStatusDescription());
            worker.setImageName(workerDTO.getImageName());
            worker.setLabels(workerDTO.getLabels());
            worker.setCommand(workerDTO.getCommand());
            worker.setContainerId(containerResponse.getId());

            // create an empty WorkerStatistics object and associate it with the worker
            WorkerStatistics workerStats = new WorkerStatistics();
            workerStats.setWorker(worker);
            worker.setStatistics(workerStats);

            worker = workerRepository.save(worker);
            return  worker;
        } catch (Exception e) {
            throw new Exception("Failed to create worker", e);
        }
    }

    public List<Container> listWorkers() {
        return dockerClient.listContainersCmd()
                .withShowAll(true)
                .withShowSize(true)
                .withStatusFilter(Collections.singleton("container_status"))
                .exec();
    }


    @Scheduled(fixedDelay = 60000) // Collect stats every 60 seconds
    public void collectWorkerStats() {
        for (Worker worker : workerRepository.findAll()) {
            collectStatsForWorker(worker);
        }
    }

    private void collectStatsForWorker(Worker worker) {
        try {
            InvocationBuilder.AsyncResultCallback<Statistics> callback = new InvocationBuilder.AsyncResultCallback<>();

            dockerClient.statsCmd(worker.getContainerId()).exec(callback);

            Statistics stats = callback.awaitResult();
            callback.close();

            if (stats != null) {
                WorkerStatistics workerStats = new WorkerStatistics();
                workerStats.setCpuUsage(stats.getCpuStats().getCpuUsage().getTotalUsage());
                workerStats.setMemoryUsage(stats.getMemoryStats().getUsage());

                // Associate the statistics object with the worker
                worker.setStatistics(workerStats);
                workerStats.setWorker(worker);

                // Save the updated entities
                workerRepository.save(worker);
                workerStatisticsRepository.save(workerStats);
            }
        } catch (IOException e) {
            logger.warning("Error collecting stats for worker");
        }
    }

    public WorkerDTO getWorkerById(String id) {
        Optional<Worker> optionalWorker = workerRepository.findById(id);
        if (optionalWorker.isPresent()) {
            Worker worker = optionalWorker.get();
            WorkerDTO workerDTO = new WorkerDTO();
            workerDTO.setName(worker.getName());
            workerDTO.setStatus(worker.getStatus().name());
            workerDTO.setImageName(worker.getImageName());
            workerDTO.setStatusDescription(worker.getStatusDescription());
            workerDTO.setLabels(worker.getLabels());
            workerDTO.setCommand(worker.getCommand());
            workerDTO.setPorts(worker.getPorts());

            return workerDTO;
        }
        return null;
    }

    public void startWorker(String id) {
        Worker worker = workerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Worker with id " + id + " not found"));
        // Start the container and update the worker status to "running" in the database
        try {
            dockerClient.startContainerCmd(worker.getContainerId());
            worker.setStatus(WorkerStatus.running);
            workerRepository.save(worker);
        } catch (Exception e) {
            throw new RuntimeException("Failed to start worker with id " + id, e);
        }
    }

    public void stopWorker(String id) {
        Worker worker = workerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Worker with id " + id + " not found"));
        // Stop the container and update the worker status to "stopped" in the database
        try {
            dockerClient.stopContainerCmd(worker.getContainerId());
            worker.setStatus(WorkerStatus.exited);
            workerRepository.save(worker);
        } catch (Exception e) {
            throw new RuntimeException("Failed to stop worker with id " + id, e);
        }
    }


    public void listenForContainerEvents() {
        dockerClient.eventsCmd().exec(new ResultCallback.Adapter<com.github.dockerjava.api.model.Event>() {
            @Override
            public void onNext(com.github.dockerjava.api.model.Event event) {
                // Handle the event here
                System.out.println("Received event: " + event.getType() +"action:"+event.getAction() + "form:" + event.getFrom());
            }

            @Override
            public void onError(Throwable throwable) {
                listenForContainerEvents();
            }

        });
    }
}
