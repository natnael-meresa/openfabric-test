package ai.openfabric.api.controller;

import ai.openfabric.api.dto.WorkerDTO;
import ai.openfabric.api.service.WorkerService;
import com.github.dockerjava.api.model.Container;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${node.api.path}/worker")
public class WorkerController {

    @Autowired
    WorkerService workerService;

    @PostMapping(path = "/hello")
    public @ResponseBody String hello(@RequestBody String name) {
        return "Hello!" + name;
    }

    @GetMapping(path = "/list")
    public @ResponseBody List<Container> list() {
        return workerService.listWorkers();
    }

    @PostMapping
    public ResponseEntity<String> createWorker(@RequestBody WorkerDTO workerDTO) {
        try {
            workerService.createWorker(workerDTO);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create worker: " + e.getMessage());
        }
    }

    @GetMapping("/{workerId}")
    public ResponseEntity<WorkerDTO> getWorker(@PathVariable String id) {
        WorkerDTO workerDTO = workerService.getWorkerById(id);
        if (workerDTO == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(workerDTO);
    }


    @PostMapping("/{id}/start")
    public ResponseEntity<?> startWorker(@PathVariable String containerID) {
        workerService.startWorker(containerID);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/stop")
    public ResponseEntity<?> stopWorker(@PathVariable String containerID) {
        workerService.stopWorker(containerID);
        return ResponseEntity.ok().build();
    }


}
