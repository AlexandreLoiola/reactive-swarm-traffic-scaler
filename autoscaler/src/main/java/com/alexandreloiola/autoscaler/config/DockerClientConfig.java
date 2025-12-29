package com.alexandreloiola.autoscaler.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration

public class DockerClientConfig {
    @Bean
    public DockerClient dockerClient() {
        DefaultDockerClientConfig config =
                DefaultDockerClientConfig.createDefaultConfigBuilder()
                        .withDockerHost("unix:///var/run/docker.sock")
                        .build();

        DockerHttpClient httpClient =
                new ApacheDockerHttpClient.Builder()
                        .dockerHost(config.getDockerHost())
                        .build();
        return DockerClientBuilder.getInstance(config)
                .withDockerHttpClient(httpClient)
                .build();
    }
}