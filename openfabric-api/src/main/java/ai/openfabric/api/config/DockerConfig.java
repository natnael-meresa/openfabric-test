package ai.openfabric.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
@Component
@ConfigurationProperties(prefix = "docker")
@Data
public class DockerConfig {
    private String host;
    private boolean tlsVerify;
    private String certPath;
}
