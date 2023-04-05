package ai.openfabric.api.service;

import ai.openfabric.api.dto.WorkerDTO;
import ai.openfabric.api.model.*;
import ai.openfabric.api.repository.*;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Statistics;
import com.github.dockerjava.core.InvocationBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@Service
public class WorkerService {
    private final WorkerRepository workerRepository;
    private final WorkerStatisticsRepository workerStatisticsRepository;
    private final NetworkStatsRepository networkStatsRepository;
    private  final MemoryStatsRepository memoryStatsRepository;
    private final CpuStatsRepository cpuStatsRepository;
    private static final Logger logger = Logger.getLogger(WorkerService.class.getName());
    DockerClientService dockerClientService = new DockerClientService();
    DockerClient dockerClient  = dockerClientService.InstantiatDockerClient();
    public WorkerService(WorkerRepository workerRepository
            ,WorkerStatisticsRepository workerStatisticsRepository
            ,CpuStatsRepository cpuStatsRepository
            ,NetworkStatsRepository networkStatsRepository
            ,MemoryStatsRepository memoryStatsRepository) {
        this.workerRepository = workerRepository;
        this.workerStatisticsRepository = workerStatisticsRepository;
        this.cpuStatsRepository = cpuStatsRepository;
        this.memoryStatsRepository = memoryStatsRepository;
        this.networkStatsRepository = networkStatsRepository;
    }

    public Worker createWorker(WorkerDTO workerDTO) throws Exception {
        try {
            // create container using DockerClient
            List<ExposedPort> exposedPorts = new ArrayList<>();
            List<Port> ports = new ArrayList<>();
            Worker worker = new Worker();

            for (Port port: workerDTO.getPorts()) {
                ExposedPort exposedPort;
                if (port.getType().equals("udp")) {
                    exposedPort = ExposedPort.udp(port.getPublicPort());
                } else {
                    exposedPort = ExposedPort.tcp(port.getPublicPort());
                }

                Port p =  new Port();
                p.setIp(port.getIp());
                p.setPrivatePort(port.getPrivatePort());
                p.setPublicPort(port.getPublicPort());
                p.setType(port.getType());
                p.setWorker(worker);
                ports.add(p);

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
            worker.setName(workerDTO.getName());
            worker.setStatus(WorkerStatus.created);
            worker.setStatusDescription(workerDTO.getStatusDescription());
            worker.setImageName(workerDTO.getImageName());
            worker.setLabels(workerDTO.getLabels());
            worker.setCommand(workerDTO.getCommand());
            worker.setContainerId(containerResponse.getId());
            worker.setPorts(ports);

            // create an empty WorkerStatistics object and associate it with the worker
            WorkerStatistics workerStats = new WorkerStatistics();
            workerStats.setWorker(worker);
            worker.setStatistics(workerStats);

            CpuStats cpuStats = new CpuStats();
            cpuStats.setWorkerStatistics(workerStats);
            workerStats.setCpuStats(cpuStats);

            MemoryStats memoryStats = new MemoryStats();
            memoryStats.setWorkerStatistics(workerStats);
            workerStats.setMemoryStats(memoryStats);

            NetworkStats networkStats = new NetworkStats();
            networkStats.setWorkerStatistics(workerStats);
            workerStats.setNetworkStats(networkStats);


            worker = workerRepository.save(worker);
            return  worker;
        } catch (Exception e) {
            throw new Exception("Failed to create worker", e);
        }
    }

    public List<WorkerDTO> listWorkers(int pageNo, int pageSize) {
        Pageable paging = PageRequest.of(pageNo, pageSize);
        Page<Worker> pagedResult = workerRepository.findAll(paging);

        List<Worker> workers = pagedResult.toList();
        List<WorkerDTO> workerDTOs = new ArrayList<>();
        for (Worker worker : workers) {
            WorkerDTO workerDTO = convertToDTO(worker);
            workerDTOs.add(workerDTO);
        }
        return workerDTOs;
    }


    @Scheduled(fixedDelay = 60000) // Collect stats every 60 seconds
    public void collectWorkerStats() {
        // for every worker update it statistics
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
                WorkerStatistics workerStats = worker.getStatistics();

                CpuStats cpuStats =  workerStats.getCpuStats();
                cpuStats.setTotalUsage(stats.getCpuStats().getCpuUsage().getTotalUsage());
                cpuStats.setUsageInKernelmode(stats.getCpuStats().getCpuUsage().getUsageInKernelmode());
                cpuStats.setUsageInUsermode(stats.getCpuStats().getCpuUsage().getUsageInUsermode());
                cpuStats.setSystemCpuUsage(stats.getCpuStats().getSystemCpuUsage());
                cpuStats.setOnlineCpus(stats.getCpuStats().getOnlineCpus());

                cpuStatsRepository.save(cpuStats);

                MemoryStats memoryStats = workerStats.getMemoryStats();
                memoryStats.setFailcnt(stats.getMemoryStats().getFailcnt());
                memoryStats.setUsage(stats.getMemoryStats().getUsage());
                memoryStats.setMaxUsage(stats.getMemoryStats().getMaxUsage());
                if (stats.getMemoryStats().getStats() != null ) {
                    memoryStats.setCache(stats.getMemoryStats().getStats().getCache());
                    memoryStats.setRss(stats.getMemoryStats().getStats().getRss());
                    memoryStats.setUnevictable(stats.getMemoryStats().getStats().getUnevictable());
                    memoryStats.setMappedFile(stats.getMemoryStats().getStats().getMappedFile());
                    memoryStats.setSwap(stats.getMemoryStats().getStats().getSwap());
                    memoryStats.setActiveFile(stats.getMemoryStats().getStats().getActiveFile());
                }

                memoryStatsRepository.save(memoryStats);

                NetworkStats networkStats = workerStats.getNetworkStats();
                if (stats.getNetworks() != null && stats.getNetworks().get("eth0") != null) {
                    networkStats.setRxBytes(stats.getNetworks().get("eth0").getRxBytes());
                    networkStats.setRxDropped(stats.getNetworks().get("eth0").getRxDropped());
                    networkStats.setRxErrors(stats.getNetworks().get("eth0").getRxErrors());
                    networkStats.setRxPackets(stats.getNetworks().get("eth0").getRxPackets());
                    networkStats.setTxBytes(stats.getNetworks().get("eth0").getTxBytes());
                    networkStats.setTxDropped(stats.getNetworks().get("eth0").getTxDropped());
                    networkStats.setTxErrors(stats.getNetworks().get("eth0").getTxErrors());
                    networkStats.setTxPackets(stats.getNetworks().get("eth0").getTxPackets());
                }

                networkStatsRepository.save(networkStats);

                // Save the updated entities
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

            // convert worker entity to workerDTO
            WorkerDTO workerDTO = convertToDTO(worker);

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

    public void updateWorkerStatus(String workerName, WorkerStatus status) {
        // Find the worker by name
        Worker worker = workerRepository.findByName(workerName)
                .orElseThrow(() -> new RuntimeException("Worker not found with name: " + workerName));;

        // Update the worker's status
        worker.setStatus(status);

        // Save the changes to the database
        workerRepository.save(worker);
    }

    public void listenForContainerEvents() {
        dockerClient.eventsCmd().exec(new ResultCallback.Adapter<com.github.dockerjava.api.model.Event>() {
            @Override
            public void onNext(com.github.dockerjava.api.model.Event event) {
                // Handle the event here
                 if (event.getType().equals("container")) {
                     switch (event.getAction()) {
                        case "create":
                            // Create worker entity
                            Worker worker = new Worker();
                            worker.setName(event.getActor().getAttributes().get("name"));
                            worker.setContainerId(event.getId());
                            worker.setImageName(event.getActor().getAttributes().get("image"));
                            worker.setStatus(WorkerStatus.created);
                            // Save to the database
                            workerRepository.save(worker);
                            break;
                        case "stop":
                            updateWorkerStatus(event.getActor().getAttributes().get("name"), WorkerStatus.exited);
                            break;
                        case "unpause":
                        case "restart":
                        case "start":
                            updateWorkerStatus(event.getActor().getAttributes().get("name"), WorkerStatus.running);
                            break;
                        case "destroy":
                        case "die":
                        case "kill":
                            updateWorkerStatus(event.getActor().getAttributes().get("name"), WorkerStatus.dead);
                            break;
                        case "pause":
                            updateWorkerStatus(event.getActor().getAttributes().get("name"), WorkerStatus.paused);
                            break;
                        default:
                           break;
                    }
                }
            }

            @Override
            public void onError(Throwable throwable) {
                listenForContainerEvents();
            }

        });
    }

    public WorkerStatistics getWorkerStatistics(String workerId) {
        Optional<Worker> optionalWorker = workerRepository.findById(workerId);
        if (!optionalWorker.isPresent()) {
            return null;
        }
        Worker worker = optionalWorker.get();
        WorkerStatistics workerStatistics = worker.getStatistics();
        if (workerStatistics == null) {
            return null;
        }

        return workerStatistics;
    }

    public WorkerDTO convertToDTO(Worker worker) {
        WorkerDTO workerDTO = new WorkerDTO();
        workerDTO.setId(worker.getId());
        workerDTO.setName(worker.getName());
        workerDTO.setStatus(worker.getStatus().toString());
        workerDTO.setImageName(worker.getImageName());
        workerDTO.setStatusDescription(worker.getStatusDescription());
        workerDTO.setLabels(worker.getLabels());
        workerDTO.setCommand(worker.getCommand());
        workerDTO.setPorts(worker.getPorts());

        return workerDTO;
    }

}
