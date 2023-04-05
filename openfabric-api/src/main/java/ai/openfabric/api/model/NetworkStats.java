package ai.openfabric.api.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name = "network_stats")
public class NetworkStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "of-uuid")
    @GenericGenerator(name = "of-uuid", strategy = "ai.openfabric.api.model.IDGenerator")
    public String id;

    private Long rxBytes;
    private Long rxDropped;
    private Long rxErrors;
    private Long rxPackets;
    private Long txBytes;
    private Long txDropped;
    private Long txErrors;
    private Long txPackets;

    @OneToOne(mappedBy = "networkStats")
    @JsonBackReference
    private WorkerStatistics workerStatistics;
}
