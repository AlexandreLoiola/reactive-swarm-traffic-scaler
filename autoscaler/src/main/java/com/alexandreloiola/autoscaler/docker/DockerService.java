package com.alexandreloiola.autoscaler.docker;

public interface DockerService {
    int countBackendInstances();
    void scaleUp(int instanceDelta);
    void scaleDown(int instanceDelta);
}
