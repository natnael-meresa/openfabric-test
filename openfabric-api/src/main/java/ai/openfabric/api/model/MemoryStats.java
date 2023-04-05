package ai.openfabric.api.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name = "memory_stats")
public class MemoryStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "of-uuid")
    @GenericGenerator(name = "of-uuid", strategy = "ai.openfabric.api.model.IDGenerator")
    public String id;

    private Long failcnt;
    private Long usage;
    private Long maxUsage;
    private Long cache;
    private Long rss;
    private Long unevictable;
    private Long mappedFile;
    private Long swap;
    private Long activeFile;

    @OneToOne(mappedBy = "memoryStats")
    @JsonBackReference
    private WorkerStatistics workerStatistics;
}

