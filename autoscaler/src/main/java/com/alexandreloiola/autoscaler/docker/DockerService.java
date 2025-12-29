package com.alexandreloiola.autoscaler.docker;

public interface DockerService {
    int countBackendInstances();
    void scaleUp();
    void scaleDown();
}
