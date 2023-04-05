package ai.openfabric.api.dto;

import ai.openfabric.api.model.Port;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class WorkerDTO {
    private String id;
    private String name;
    private List<Port> ports;
    private String status;
    private String imageName;
    private String statusDescription;
    public Map<String, String> labels;
    public String command;
}
