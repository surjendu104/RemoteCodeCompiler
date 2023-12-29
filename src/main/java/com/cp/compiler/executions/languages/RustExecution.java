package com.cp.compiler.executions.languages;

import com.cp.compiler.executions.Execution;
import com.cp.compiler.executions.ExecutionFactory;
import com.cp.compiler.models.testcases.TransformedTestCase;
import com.cp.compiler.contract.Language;
import lombok.Getter;
import lombok.val;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * The type Rust execution.
 *
 * @author Zakaria Maaraki
 */
@Getter
public class RustExecution extends Execution {
    
    /**
     * Instantiates a new Rust execution.
     *
     * @param sourceCode  the source code
     * @param testCases   the test cases
     * @param timeLimit   the time limit
     * @param memoryLimit the memory limit
     */
    public RustExecution(MultipartFile sourceCode,
                         List<TransformedTestCase> testCases,
                         int timeLimit,
                         int memoryLimit) {
        super(sourceCode, testCases, timeLimit, memoryLimit, ExecutionFactory.getExecutionType(Language.RUST));
    }
    
    @Override
    public Map<String, String> getParameters(String inputFileName) {
        val commandPrefix = "./main";
        val executionCommand = inputFileName == null
                ? commandPrefix + "\n"
                : commandPrefix + " < " + inputFileName + "\n";
    
        return Map.of(
                "timeLimit", String.valueOf(getTimeLimit()),
                "memoryLimit", String.valueOf(getMemoryLimit()),
                "executionCommand", executionCommand);
    }
    
    @Override
    protected void copyLanguageSpecificFilesToExecutionDirectory() throws IOException {
        // Empty
    }
    
    @Override
    public Language getLanguage() {
        return Language.RUST;
    }
}
