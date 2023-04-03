package ai.openfabric.api.model;


import lombok.Getter;
import lombok.Setter;
import org.checkerframework.common.aliasing.qual.Unique;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Entity()
@Getter
@Setter
public class Worker extends Datable implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "of-uuid")
    @GenericGenerator(name = "of-uuid", strategy = "ai.openfabric.api.model.IDGenerator")
    public String id;

    @Unique
    public String name;

    @Enumerated(EnumType.STRING)
    private WorkerStatus status;

    public String imageName;

    public String statusDescription;

    @OneToMany(mappedBy = "worker", cascade = CascadeType.ALL)
    private List<Port> ports;

    @ElementCollection
    public Map<String, String> labels;

    public String command;

    @Unique
    public String containerId;

    @OneToOne(mappedBy = "worker", cascade = CascadeType.ALL, orphanRemoval = true)
    private WorkerStatistics statistics;

}
