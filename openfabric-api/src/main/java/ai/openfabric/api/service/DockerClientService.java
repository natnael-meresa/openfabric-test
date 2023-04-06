package ai.openfabric.api.service;

import ai.openfabric.api.config.DockerConfig;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DockerClientService {

    @Value("${docker.host}")
    private String dockerHost = "tcp://localhost:2375";
    @Value("${docker.tls-verify}")
    private Boolean dockerTlsVerify = false;
    @Value("${docker.cert-path}")
    private String dockerCertPath = "/home/user/.docker";


    public DockerClient InstantiatDockerClient () {

        DockerClientConfig dockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerHost)
                .withDockerTlsVerify(dockerTlsVerify)
                .withDockerCertPath(dockerCertPath)
                .build();


        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(dockerClientConfig.getDockerHost())
                .sslConfig(dockerClientConfig.getSSLConfig())
                .build();

        return DockerClientImpl.getInstance(dockerClientConfig, httpClient);
    }
}
