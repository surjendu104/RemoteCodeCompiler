package com.cp.compiler.services;

import com.cp.compiler.exceptions.*;
import com.cp.compiler.models.ProcessOutput;
import com.cp.compiler.wellknownconstants.WellKnownMetrics;
import com.cp.compiler.utils.CmdUtils;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * This class provides Docker utilities that are used by the compiler
 *
 * @author Zakaria Maaraki
 */
@Slf4j
@Service
public class ContainerServiceDefault implements ContainerService {
    
    /**
     * The constant BUILD_TIMEOUT.
     */
    public static final int BUILD_TIMEOUT = 5 * 60000; // 5 minutes
    
    /**
     * The constant COMMAND_TIMEOUT.
     */
    public static final int COMMAND_TIMEOUT = 2000;
    
    /**
     * The constant EXECUTION_PATH_ENV_VARIABLE.
     */
    public static final String EXECUTION_PATH_ENV_VARIABLE = "EXECUTION_PATH";
    
    /**
     * The constant SOURCE_CODE_ENV_VARIABLE.
     */
    public static final String SOURCE_CODE_FILE_NAME_ENV_VARIABLE = "SOURCE_CODE_FILE_NAME";

    private final MeterRegistry meterRegistry;

    private Timer buildTimer;

    private Timer runTimer;
    
    /**
     * Instantiates a new Container service.
     *
     * @param meterRegistry the meter registry
     */
    public ContainerServiceDefault(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    /**
     * Init.
     */
    @PostConstruct
    public void init() {
        buildTimer = meterRegistry.timer(WellKnownMetrics.CONTAINER_BUILD_TIMER, "container", "docker");
        runTimer = meterRegistry.timer(WellKnownMetrics.CONTAINER_RUN_TIMER, "container", "docker");
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String buildImage(String contextPath, String imageName, String dockerfileName) {
        // TODO Refactor by using vavr.Try
        return buildTimer.record(() -> {
            String dockerfilePath = contextPath + "/" + dockerfileName;
            String[] buildCommand =
                    new String[]{"docker", "image", "build", "-f", dockerfilePath, "-t", imageName, contextPath};
            return executeContainerCommand(buildCommand, BUILD_TIMEOUT);
        });
    }
    
    /**
     * Run an instance of an image
     * @param imageName the image name
     * @param timeout   the timeout after which the container will be destroyed
     * @return ProcessOutput
     */
    @Override
    public ProcessOutput runContainer(String imageName, long timeout, float maxCpus) {
        return runTimer.record(() -> {
            try {
                var cpus = "--cpus=" + maxCpus;
                String[] dockerCommand = new String[]{"docker", "run", cpus, "--rm", imageName};
                return CmdUtils.executeProcess(dockerCommand, timeout);
            } catch(ProcessExecutionTimeoutException processExecutionTimeoutException) {
                // TLE
                throw new ContainerOperationTimeoutException(processExecutionTimeoutException.getMessage());
            } catch(ProcessExecutionException processExecutionException) {
                throw new ContainerFailedDependencyException(processExecutionException.getMessage());
            }
        });
    }
    
    @Override
    public ProcessOutput runContainer(
            String imageName,
            long timeout,
            String volumeMounting,
            String executionPath,
            String sourceCodeFileName) {
        
        return runTimer.record(() -> {
            log.info("Execution path {}", executionPath);
            try {
                String[] dockerCommand =
                        new String[]{
                                "docker",
                                "run",
                                "-v", volumeMounting,
                                "-e", EXECUTION_PATH_ENV_VARIABLE + "=" + executionPath,
                                "-e", SOURCE_CODE_FILE_NAME_ENV_VARIABLE + "=" + sourceCodeFileName,
                                "--rm",
                                imageName};
                return CmdUtils.executeProcess(dockerCommand, timeout);
            } catch(ProcessExecutionTimeoutException processExecutionTimeoutException) {
                throw new ContainerOperationTimeoutException(processExecutionTimeoutException.getMessage());
            } catch(ProcessExecutionException processExecutionException) {
                throw new ContainerFailedDependencyException(processExecutionException.getMessage());
            }
        });
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getRunningContainers() {
        String[] command = {"docker", "ps"};
        return executeContainerCommand(command, COMMAND_TIMEOUT);
    }
    
    @Override
    public String getContainersStats() {
        String[] command = {"docker", "stats", "--no-stream"};
        return executeContainerCommand(command, COMMAND_TIMEOUT);
    }
    
    @Override
    public String getAllContainersStats() {
        String[] command = {"docker", "stats", "--no-stream", "--all"};
        return executeContainerCommand(command, COMMAND_TIMEOUT);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getImages() {
        String[] command = {"docker", "images"};
        return executeContainerCommand(command, COMMAND_TIMEOUT);
    }
    
    @Override
    public String deleteImage(String imageName) {
        String[] command = {"docker", "rmi", "-f", imageName};
        return executeContainerCommand(command, COMMAND_TIMEOUT);
    }
    
    @Override
    public boolean isUp() {
        String[] command = {"docker", "ps"};
        ProcessOutput processOutput = CmdUtils.executeProcess(command, COMMAND_TIMEOUT);
        return processOutput.getStdErr().isEmpty();
    }
    
    @Override
    public String getContainerizationName() {
        return "Docker";
    }
    
    private String executeContainerCommand(String[] command, long timeout) {
        try {
            ProcessOutput processOutput = CmdUtils.executeProcess(command, timeout);
            if (!processOutput.getStdErr().isEmpty()) {
                throw new ContainerFailedDependencyException("Fatal error: " + processOutput.getStdErr());
            }
            return processOutput.getStdOut();
        } catch (ProcessExecutionException e) {
            throw new ContainerFailedDependencyException(e.getMessage());
        } catch (ProcessExecutionTimeoutException e) {
            throw new ContainerOperationTimeoutException(e.getMessage());
        }
    }
}
