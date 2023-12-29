package com.cp.compiler.services.ux;

import com.cp.compiler.contract.RemoteCodeCompilerResponse;
import com.cp.compiler.contract.testcases.TestCaseResult;
import com.cp.compiler.executions.Execution;
import com.cp.compiler.executions.ExecutionFactory;
import com.cp.compiler.contract.RemoteCodeCompilerRequest;
import com.cp.compiler.contract.problems.Problem;
import com.cp.compiler.contract.problems.ProblemExecution;
import com.cp.compiler.contract.testcases.TestCase;
import com.cp.compiler.models.Verdict;
import com.cp.compiler.repositories.problems.ProblemsRepository;
import com.cp.compiler.services.api.CompilerFacade;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.LinkedHashMap;

@Service
public class ExecutionServiceDefault implements ExecutionService {
    
    private CompilerFacade compiler;
    private ProblemsRepository problemsRepository;
    
    public ExecutionServiceDefault(CompilerFacade compiler, ProblemsRepository problemsRepository) {
        this.compiler = compiler;
        this.problemsRepository = problemsRepository;
    }
    
    @Override
    public ResponseEntity execute(ProblemExecution problemExecution) throws IOException {
    
        Problem problem = problemsRepository.getProblemById(problemExecution.getProblemId());
        
        RemoteCodeCompilerRequest request = createRequest(problem, problemExecution);
        
        Execution execution = ExecutionFactory.createExecution(
                request.getSourcecodeFile(),
                request.getConvertedTestCases(),
                request.getTimeLimit(),
                request.getMemoryLimit(),
                request.getLanguage());
    
        ResponseEntity responseEntity = compiler.compile(execution, false, null, null);
    
        makeExpectedOutputHiddenIfTheResponseWasNotAccepted(responseEntity);
        
        return responseEntity;
    }
    
    private void makeExpectedOutputHiddenIfTheResponseWasNotAccepted(ResponseEntity responseEntity) {
        if (responseEntity.getBody() instanceof RemoteCodeCompilerResponse) {
            var response = (RemoteCodeCompilerResponse) responseEntity.getBody();
            
            if (response.getVerdict().equals(Verdict.ACCEPTED.getStatusResponse())) {
                return;
            }
            
            int index = 0;
            for (TestCaseResult testCaseResult : response.getTestCasesResult().values()) {
                if (index++ < 2) {
                    continue;
                }
                testCaseResult.setExpectedOutput("**Hidden**");
                testCaseResult.setOutputDiff("**Hidden**");
            }
        }
    }
    
    private RemoteCodeCompilerRequest createRequest(Problem problem, ProblemExecution problemExecution) {
        
        var testCases = new LinkedHashMap<String, TestCase>();
        int index = 0;
        
        for (TestCase testCase : problem.getTestCases()) {
            testCases.put(String.valueOf(index++), testCase);
        }
        
        return new RemoteCodeCompilerRequest(
                problemExecution.getSourceCode(),
                problemExecution.getLanguage(),
                problem.getTimeLimit(),
                problem.getMemoryLimit(),
                testCases);
    }
}
