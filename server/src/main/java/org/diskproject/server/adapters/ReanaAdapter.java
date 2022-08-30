package org.diskproject.server.adapters;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.diskproject.server.adapters.Reana.ApiClient;
import org.diskproject.server.adapters.Reana.ApiClient.ResponseListWorkflow;
import org.diskproject.server.adapters.Reana.ApiSchema.ReanaSpecification;
import org.diskproject.server.adapters.Reana.ApiSchema.ReanaWorkflow;
import org.diskproject.server.adapters.Reana.ApiSchema.ResponseRunStatus;
import org.diskproject.server.adapters.Reana.ApiSchema.ReanaSpecification.Inputs;
import org.diskproject.server.adapters.Reana.ApiSchema.ReanaSpecification.Outputs;
import org.diskproject.server.adapters.Reana.ApiSchema.ResponseRunStatus.Progress;
import org.diskproject.shared.classes.adapters.MethodAdapter;
import org.diskproject.shared.classes.workflow.Variable;
import org.diskproject.shared.classes.workflow.VariableBinding;
import org.diskproject.shared.classes.workflow.Workflow;
import org.diskproject.shared.classes.workflow.WorkflowRun;

public class ReanaAdapter extends MethodAdapter {
    private static final int CONNECT_TIMEOUT = 60;
    private static final int READ_TIMEOUT = 60;
    private ApiClient apiClient;
    private ResponseRunStatus runStatus;

    /**
     * Method Adapter for Reana https://reanahub.io/
     * @param adapterName Name of the adapter: reana
     * @param apiUrl REANA API URL
     * @param username REANA username (leave blank for token access)
     * @param password REANA password (if you are using token access, use the token)
     * @param inventory Deprecated
     */
    public ReanaAdapter(String adapterName, String apiUrl, String username, String password, String inventory) {
        super(adapterName, apiUrl, username, password, inventory);
        this.apiClient = new ApiClient(apiUrl, password, username);
    }

    /**
     * Create the parameters for the REANA workflow using DISK variables binding
     * @param variableBindings
     * @param inputVariables
     * @return
     */
    private Map<String, String> createInputParameters(List<VariableBinding> variableBindings,
            Map<String, Variable> inputVariables) {
        /**
         * Convert variable bindings to json
         */
        Map<String, String> inputParameters = new HashMap<String, String>();
        for (String variableKey : inputVariables.keySet()) {
            Variable variable = inputVariables.get(variableKey);
            for (VariableBinding variableBinding : variableBindings) {
                if (variableBinding.getVariable().equals(variable.getName())) {
                    inputParameters.put(variableKey, variableBinding.getBinding());
                }
            }
        }
        return inputParameters;
    }

    @Override
    public String addData(String url, String name, String dType) throws Exception {
        return null;
    }

    public String downloadData(String url, String name) throws IOException {
        File directory = File.createTempFile("tmp", "");
        if (!directory.delete() || !directory.mkdirs()) {
            System.err.println("Could not create temporary directory " + directory);
            return null;
        }
        File file = new File(directory.getAbsolutePath() + "/" + name);
        FileUtils.copyURLToFile(
                new URL(url),
                file,
                CONNECT_TIMEOUT,
                READ_TIMEOUT);
        return file.getAbsolutePath();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.diskproject.shared.classes.adapters.MethodAdapter#runWorkflow(java.lang.
     * String, java.util.List, java.util.Map)
     */
    @Override
    public String runWorkflow(String workflowName, String workflowId, List<VariableBinding> variableBindings,
            Map<String, Variable> inputVariables) throws Exception {

        Map<String, String> inputParameters = createInputParameters(variableBindings, inputVariables);
        this.apiClient.startWorkflow(workflowId, inputParameters, null);
        return workflowId;
    }

    @Override
    public boolean ping() {
        String requestUrl = this.getEndpointUrl() + "/api/ping";
        try {
            this.apiClient.performHTTPGet(requestUrl, "application/json");
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.diskproject.shared.classes.adapters.MethodAdapter#getWorkflowList()
     */
    @Override
    public List<Workflow> getWorkflowList() {
        List<Workflow> workflowDisk = new ArrayList<Workflow>();
        try {
            ResponseListWorkflow workflowResponse = this.apiClient.getWorkflows();
            for (ReanaWorkflow workflowReana : workflowResponse.items) {
                String url = this.getEndpointUrl() + "/api/workflows/" + workflowReana.id;
                Workflow workflow = new Workflow(url, workflowReana.id, url, this.getName());
                workflowDisk.add(workflow);
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        return workflowDisk;
    }

    public List<Variable> getWorkflowVariables(String id) throws Exception {
        ReanaSpecification specification = this.apiClient.getSpecification(id);
        return convertReanaVariable(specification);
    }

    @Override
    public String getWorkflowId(String id) {
        // Auto-generated method stub
        return id;
    }

    @Override
    public String getWorkflowUri(String id) {
        // Auto-generated method stub
        return id;
    }

    @Override
    public String getWorkflowLink(String id) {
        // Auto-generated method stub
        return id;
    }

    @Override
    public Map<String, Variable> getWorkflowInputs(String id) {
        // Auto-generated method stub
        return null;

    }

    @Override
    public WorkflowRun getRunStatus(String runId) throws Exception {
        // Auto-generated method stub
        ResponseRunStatus reanaRun = this.apiClient.getRunStatus(runId);
        WorkflowRun run = new WorkflowRun();
        Progress progress = reanaRun.progress;
        run.setId(reanaRun.id);
        run.setStatus(reanaRun.status);
        run.setStartDate(progress.getRun_started_at());
        run.setEndDate(progress.getRun_finished_at());

        ReanaSpecification specification = this.apiClient.getSpecification(runId);
        if (run.getStatus() == "finished") {
            run.setOutputs(convertReanaOutputs(specification));
            run.setFiles(convertReanaInputs(specification));
        }

        return run;
    }

    @Override
    public String fetchData(String dataId) {
        // Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, String> getRunVariableBindings(String runId) {
        // Auto-generated method stub
        return null;
    }

    @Override
    public String getDataUri(String id) {
        return null;
    }

    @Override
    public List<String> areFilesAvailable(Set<String> fileList, String dType) {
        return new ArrayList<String>();
    }

    public List<Variable> convertReanaVariable(ReanaSpecification specification) throws Exception {
        List<Variable> variables = new ArrayList<Variable>();
        Inputs reanaInputs = specification.getSpecification().getInputs();
        ArrayList<String> reanaFiles = reanaInputs.getFiles();
        reanaInputs.getParameters().forEach((key, value) -> {
            Variable variable = new Variable();
            variable.setName(key);
            variable.setInput(true);
            if (reanaFiles.contains(value)) {
                variable.setType("File");
                variable.setParam(false);
            } else {
                variable.setName(key.toString());
                // TODO: the spec doesn't contain information about the type
                variable.setType("Parameter");
                variable.setParam(true);
            }

            variables.add(variable);
        });
        return variables;
    }

    public String duplicateWorkflow(String id) throws Exception {
        ReanaSpecification oldSpecification = this.apiClient.getSpecification(id);
        runStatus = this.apiClient.getRunStatus(id);
        String newWorkflowId = this.apiClient.createWorkflow(oldSpecification, runStatus.getName());
        return newWorkflowId;
    }

    public String duplicateWorkflow(String id, String name) throws Exception {
        ReanaSpecification oldSpecification = this.apiClient.getSpecification(id);
        runStatus = this.apiClient.getRunStatus(id);
        String newWorkflowId = this.apiClient.createWorkflow(oldSpecification, name);
        return newWorkflowId;
    }

    Map<String, String> convertReanaOutputs(ReanaSpecification specification)
            throws Exception {
        Map<String, String> outputs = new HashMap<String, String>();
        Outputs reanaRunOutputs = specification.getSpecification().getOutputs();
        for (String output : reanaRunOutputs.getFiles()) {
            Path path = Paths.get(output);
            path.getFileName();
            outputs.put(path.getFileName().toString(), output);
        }
        return outputs;
    }

    private Map<String, String> convertReanaInputs(ReanaSpecification specification) throws Exception {
        Map<String, String> inputs = new HashMap<String, String>();
        Inputs reanaRunInputs = specification.getSpecification().getInputs();
        for (String input : reanaRunInputs.getFiles()) {
            inputs.put(input, input);
        }
        return inputs;
    }

    @Override
    public String addData(String url, String workSpaceId, String name, String dType) throws Exception {
        String filePath = downloadData(url, name);
        String remoteUrl = this.apiClient.addFileWorkspace(workSpaceId, filePath);
        return remoteUrl;
    }
}
