package com.cp.compiler.executions.go;

import com.cp.compiler.executions.Execution;
import com.cp.compiler.models.Language;
import com.cp.compiler.models.WellKnownTemplates;
import com.cp.compiler.templates.EntrypointFileGenerator;
import com.cp.compiler.utilities.StatusUtil;
import io.micrometer.core.instrument.Counter;
import lombok.SneakyThrows;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * The type Go execution.
 */
public class GoExecution extends Execution {
    
    /**
     * Instantiates a new Go Execution.
     *
     * @param sourceCodeFile     the source code
     * @param inputFile          the inputFile file
     * @param expectedOutputFile the expected output file
     * @param timeLimit          the time limit
     * @param memoryLimit        the memory limit
     * @param executionCounter   the execution counter
     */
    protected GoExecution(MultipartFile sourceCodeFile,
                          MultipartFile inputFile,
                          MultipartFile expectedOutputFile,
                          int timeLimit,
                          int memoryLimit,
                          Counter executionCounter,
                          EntrypointFileGenerator entryPointFileGenerator) {
        super(sourceCodeFile, inputFile, expectedOutputFile, timeLimit, memoryLimit, Language.GO, executionCounter, entryPointFileGenerator);
    }
    
    @SneakyThrows
    @Override
    protected void createEntrypointFile() {
        final var commandPrefix = "./exec";
        final var executionCommand = getInputFile() == null
                ? commandPrefix + "\n"
                : commandPrefix + " < " + getInputFile().getOriginalFilename() + "\n";
    
        Map<String, String> attributes = Map.of(
                "rename", "false",
                "compile", "true",
                "fileName", Language.GO.getSourceCodeFileName(),
                "defaultName", Language.GO.getSourceCodeFileName(),
                "timeLimit", String.valueOf(getTimeLimit()),
                "compilationCommand", Language.GO.getCompilationCommand() +  " -o exec " + Language.GO.getSourceCodeFileName(),
                "compilationErrorStatusCode", String.valueOf(StatusUtil.COMPILATION_ERROR_STATUS),
                "memoryLimit", String.valueOf(getMemoryLimit()),
                "executionCommand", executionCommand);
    
        String content = getEntrypointFileGenerator()
                .createEntrypointFile(WellKnownTemplates.ENTRYPOINT_TEMPLATE, attributes);
    
        try(OutputStream os = new FileOutputStream(getPath() + "/entrypoint.sh")) {
            os.write(content.getBytes(), 0, content.length());
        }
    }
}
