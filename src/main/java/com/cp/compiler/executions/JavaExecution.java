package com.cp.compiler.executions;

import com.cp.compiler.models.Language;
import com.cp.compiler.wellknownconstants.WellKnownFiles;
import com.cp.compiler.wellknownconstants.WellKnownTemplates;
import com.cp.compiler.templates.EntrypointFileGenerator;
import com.cp.compiler.utils.StatusUtil;
import io.micrometer.core.instrument.Counter;
import lombok.Getter;
import lombok.SneakyThrows;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * The type Java execution.
 */
@Getter
public class JavaExecution extends Execution {

    /**
     * Instantiates a new Java execution.
     *
     * @param sourceCode         the source code
     * @param inputFile          the input file
     * @param expectedOutputFile the expected output file
     * @param timeLimit          the time limit
     * @param memoryLimit        the memory limit
     * @param executionCounter   the execution counter
     */
    public JavaExecution(MultipartFile sourceCode,
                         MultipartFile inputFile,
                         MultipartFile expectedOutputFile,
                         int timeLimit,
                         int memoryLimit,
                         Counter executionCounter,
                         EntrypointFileGenerator entryPointFileGenerator) {
        super(sourceCode, inputFile, expectedOutputFile, timeLimit, memoryLimit, executionCounter, entryPointFileGenerator);
    }
    
    @SneakyThrows
    @Override
    protected void createEntrypointFile() {
        // This case is a bit different, Java file name must be the same as the name of the class
        // So we will keep the name of the file as it's sent by the user.
        var fileName = getSourceCodeFile().getOriginalFilename();
        final var prefixName = fileName.substring(0, fileName.length() - 5); // remove ".java"
        final var commandPrefix = "java " + prefixName;
        final var executionCommand = getInputFile() == null
                ? commandPrefix + "\n"
                : commandPrefix + " < " + getInputFile().getOriginalFilename() + "\n";
    
        Map<String, String> attributes = Map.of(
                "rename", "true",
                "compile", "true",
                "defaultName", Language.JAVA.getSourceCodeFileName(),
                "fileName", fileName,
                "timeLimit", String.valueOf(getTimeLimit()),
                "compilationCommand", Language.JAVA.getCompilationCommand() + " " + fileName,
                "compilationErrorStatusCode", String.valueOf(StatusUtil.COMPILATION_ERROR_STATUS),
                "memoryLimit", String.valueOf(getMemoryLimit()),
                "executionCommand", executionCommand);
        
        String content = getEntrypointFileGenerator()
                .createEntrypointFile(WellKnownTemplates.ENTRYPOINT_TEMPLATE, attributes);
        
        try(OutputStream os = new FileOutputStream(getPath() + "/" + WellKnownFiles.ENTRYPOINT_FILE_NAME)) {
            os.write(content.getBytes(), 0, content.length());
        }
    }

    @Override
    public Language getLanguage() {
        return Language.JAVA;
    }
}