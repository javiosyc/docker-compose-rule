/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.execution;

import static java.util.stream.Collectors.toList;

import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.configuration.ShutdownStrategy;
import com.palantir.docker.compose.connection.ContainerName;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shuts down containers as fast as possible, without giving them time to finish
 * IO or clean up any resources.
 */
public class AggressiveShutdownStrategy implements ShutdownStrategy {

    private static final Logger log = LoggerFactory.getLogger(AggressiveShutdownStrategy.class);

    @Override
    public void shutdown(DockerComposeRule rule) throws IOException, InterruptedException {
        List<ContainerName> runningContainers = rule.dockerCompose().ps();

        log.info("Shutting down {}", runningContainers.stream().map(ContainerName::semanticName).collect(toList()));
        removeContainersCatchingErrors(rule, runningContainers);
        removeNetworks(rule);
    }

    private void removeContainersCatchingErrors(DockerComposeRule rule, List<ContainerName> runningContainers) throws IOException, InterruptedException {
        try {
            removeContainers(rule, runningContainers);
        } catch (DockerExecutionException exception) {
            log.error("Error while trying to remove containers", exception);
        }
    }

    private void removeContainers(DockerComposeRule rule, List<ContainerName> running) throws IOException, InterruptedException {
        List<String> rawContainerNames = running.stream()
                .map(ContainerName::rawName)
                .collect(toList());

        rule.docker().rm(rawContainerNames);
        removeNetworks(rule);
        log.debug("Finished shutdown");
    }

    private void removeNetworks(DockerComposeRule rule) throws IOException, InterruptedException {
        rule.dockerCompose().down();
    }

}
