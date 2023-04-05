package ai.openfabric.api.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "cpu_stats")
public class CpuStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "of-uuid")
    @GenericGenerator(name = "of-uuid", strategy = "ai.openfabric.api.model.IDGenerator")

    public String id;
    private Long totalUsage;
    @ElementCollection
    private List<Long> percpuUsage;
    private Long usageInKernelmode;
    private Long usageInUsermode;
    private Long systemCpuUsage;
    private Long onlineCpus;

    @OneToOne(mappedBy = "cpuStats", cascade = CascadeType.ALL)
    @JsonBackReference
    private WorkerStatistics workerStatistics;
}
