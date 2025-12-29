package com.alexandreloiola.autoscaler.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Service;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Log4j2
@Component
public class DockerServiceImpl implements DockerService {

    @Value("${autoscaler.docker.service-name:upscaler_backend}")
    private String serviceName;

    @Value("${autoscaler.docker.min-replicas:10}")
    private int minReplicas;

    @Value("${autoscaler.docker.max-replicas:1}")
    private int maxReplicas;

    private final DockerClient dockerClient;

    public DockerServiceImpl(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    @Override
    public void scaleUp() {
        long current = getCurrentReplicas();

        if (current >= maxReplicas) {
            log.debug("Scale up skipped. Service '{}' already at max replicas ({})", serviceName, current);
            return;
        }

        long target = current + 1;
        log.info("Scaling UP service '{}' from {} to {} replicas", serviceName, current, target);

        updateReplicas(target);
    }

    @Override
    public void scaleDown() {
        long current = getCurrentReplicas();

        if (current <= minReplicas) {
            log.debug("Scale down skipped. Service '{}' already at min replicas ({})", serviceName, current);
            return;
        }

        long target = current - 1;
        log.info("Scaling DOWN service '{}' from {} to {} replicas", serviceName, current, target);

        updateReplicas(target);
    }

    @Override
    public int countBackendInstances() {
        return (int) getCurrentReplicas();
    }

    private long getCurrentReplicas() {
        Service service = getService();
        return service.getSpec()
                .getMode()
                .getReplicated()
                .getReplicas();
    }

    private void updateReplicas(long replicas) {
        Service service = getService();

        var spec = service.getSpec();
        spec.getMode()
                .getReplicated()
                .withReplicas((int) replicas);

        dockerClient.updateServiceCmd(service.getId(), spec)
                .withVersion(service.getVersion().getIndex())
                .exec();

        log.debug("Docker service '{}' updated to {} replicas", serviceName, replicas);
    }

    private Service getService() {
        List<Service> services = dockerClient.listServicesCmd()
                .withNameFilter(List.of(serviceName))
                .exec();

        if (services.isEmpty()) {
            log.error("Docker service '{}' not found", serviceName);
            throw new IllegalStateException("Service not found: " + serviceName);
        }

        return services.get(0);
    }
}