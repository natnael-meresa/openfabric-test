package ai.openfabric.api.model;

import lombok.Getter;
import lombok.Setter;
import org.checkerframework.common.aliasing.qual.Unique;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Entity()
@Getter
@Setter
public class Port {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "of-uuid")
    @GenericGenerator(name = "of-uuid", strategy = "ai.openfabric.api.model.IDGenerator")
    public String id;

    public String ip;

    public int privatePort;

    @Unique
    public int publicPort;

    public String type;

    @ManyToOne()
    @JoinColumn(name = "worker_id")
    private Worker worker;
}
