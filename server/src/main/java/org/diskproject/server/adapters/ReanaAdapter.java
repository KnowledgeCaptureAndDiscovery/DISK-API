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
import org.diskproject.server.adapters.Reana.ApiSchema.ReanaSpecification.Inputs;
import org.diskproject.server.adapters.Reana.ApiSchema.ReanaSpecification.Outputs;
import org.diskproject.server.adapters.Reana.ApiSchema.ReanaWorkflow;
import org.diskproject.server.adapters.Reana.ApiSchema.ResponseGetSpecification;
import org.diskproject.server.adapters.Reana.ApiSchema.ResponseRunStatus;
import org.diskproject.server.adapters.Reana.ApiSchema.ResponseRunStatus.Progress;
import org.diskproject.shared.classes.adapters.MethodAdapter;
import org.diskproject.shared.classes.loi.TriggeredLOI.Status;
import org.diskproject.shared.classes.workflow.Variable;
import org.diskproject.shared.classes.workflow.VariableBinding;
import org.diskproject.shared.classes.workflow.Workflow;
import org.diskproject.shared.classes.workflow.WorkflowRun;

public class ReanaAdapter extends MethodAdapter {
    private static final int CONNECT_TIMEOUT = 1000 * 2;
    private static final int READ_TIMEOUT = 1000 * 2;
    private ApiClient apiClient;
    private ResponseRunStatus runStatus;

    /**
     * Method Adapter for Reana https://reanahub.io/
     * 
     * @param adapterName Name of the adapter: reana
     * @param apiUrl      REANA API URL
     * @param username    REANA username (leave blank for token access)
     * @param password    REANA password (if you are using token access, use the
     *                    token)
     */
    public ReanaAdapter(String adapterName, String apiUrl, String username, String password) {
        super(adapterName, apiUrl, username, password);
        this.status.put(Status.QUEUED, "queued");
        this.status.put(Status.FAILED, "failed");
        this.status.put(Status.RUNNING, "running");
        this.status.put(Status.SUCCESSFUL, "finished");
        this.apiClient = new ApiClient(apiUrl, password, username);
    }

    public String handleCollectionParameter(String value) {
        if (value.startsWith("[") && value.endsWith("]")) {
            String delimiter = "";
            String array[] = value.replace("[", "").replace("]", "").split(",");
            StringBuilder sb = new StringBuilder();
            for (String item : array)
                sb.append(item.toString()).append(delimiter);
            return sb.toString();
        } else {
            return value;
        }
    }

    /**
     * Create the parameters for the REANA workflow using DISK variables binding
     * 
     * @param variableBindings
     * @param inputVariables
     * @return
     */
    public Map<String, String> createInputParameters(List<VariableBinding> variableBindings,
            Map<String, Variable> inputVariables) {
        /**
         * Convert variable bindings to json
         */
        Map<String, String> inputParameters = new HashMap<String, String>();
        for (String variableKey : inputVariables.keySet()) {
            Variable variable = inputVariables.get(variableKey);
            for (VariableBinding variableBinding : variableBindings) {
                if (variableBinding.getVariable().equals(variable.getName())) {
                    String value = handleCollectionParameter(variableBinding.getBinding());
                    inputParameters.put(variableKey, value);
                }
            }
        }
        return inputParameters;
    }

    /**
     * @param url
     * @param name
     * @param dType
     * @return String
     * @throws Exception
     */
    @Override
    public String addData(String url, String name, String dType) throws Exception {
        return null;
    }

    /**
     * @param url
     * @param name
     * @return String
     * @throws IOException
     */
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

    /**
     * @param workflowName
     * @param workflowId
     * @param variableBindings
     * @param inputVariables
     * @return String
     * @throws Exception
     */
    /*
     * (non-Javadoc)
     * 
     * @see
     * org.diskproject.shared.classes.adapters.MethodAdapter#runWorkflow(java.lang.
     * String, java.util.List, java.util.Map)
     */
    @Override
    public String runWorkflow(String workflowId, String workflowLink, List<VariableBinding> variableBindings,
            Map<String, Variable> inputVariables) throws Exception {

        Map<String, String> inputParameters = createInputParameters(variableBindings, inputVariables);
        this.apiClient.startWorkflow(workflowId, inputParameters, null);
        return workflowId;
    }

    /**
     * @return boolean
     */
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

    /**
     * @return List<Workflow>
     */
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
                Workflow workflow = new Workflow(workflowReana.id, workflowReana.name, url, this.getName());
                workflowDisk.add(workflow);
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        return workflowDisk;
    }

    /**
     * @param id
     * @return List<Variable>
     * @throws Exception
     */
    public List<Variable> getWorkflowVariables(String id) throws Exception {
        ResponseGetSpecification responseSpecification = this.apiClient.getSpecification(id);
        ReanaSpecification specification = responseSpecification.specification;
        return getVariables(specification);
    }

    /**
     * @param id
     * @return String
     */
    @Override
    public String getWorkflowId(String id) {
        return this.getEndpointUrl() + "/api/workflows/" + id;
    }

    /**
     * @param id
     * @return String
     */
    @Override
    public String getWorkflowUri(String id) {
        return this.getEndpointUrl() + "/api/workflows/" + id;
    }

    /**
     * @param id
     * @return String
     */
    @Override
    public String getWorkflowLink(String id) {
        return this.getEndpointUrl() + "/api/workflows/" + id;
    }

    /**
     * @param runId
     * @return WorkflowRun
     * @throws Exception
     */
    @Override
    public WorkflowRun getRunStatus(String runId) throws Exception {
        // Auto-generated method stub
        ResponseRunStatus reanaRun = this.apiClient.getRunStatus(runId);
        WorkflowRun run = new WorkflowRun();
        Progress progress = reanaRun.progress;
        if (reanaRun.id == null) {
            run.setStatus("failed");
            throw new Exception("Run not found");
        }
        run.setId(reanaRun.id);
        run.setStatus(reanaRun.status);
        run.setStartDate(progress.getRun_started_at());
        run.setEndDate(progress.getRun_finished_at());

        ReanaSpecification specification = this.apiClient.getSpecification(runId).specification;
        if (run.getStatus().equals("finished")) {

            Map<String, String> outputs = convertReanaOutputs(specification);
            Map<String, String> inputs = convertReanaInputs(specification);
            run.setOutputs(outputs);
            run.setFiles(inputs);
        }

        return run;
    }

    /**
     * @param dataUrl
     * @return String
     * @throws Exception
     */
    @Override
    public String fetchData(String dataUrl) throws Exception {
        return this.apiClient.downloadData(dataUrl);
    }

    /**
     * @param runId
     * @return Map<String, String>
     */
    @Override
    public Map<String, String> getRunVariableBindings(String runId) {
        // Auto-generated method stub
        return null;
    }

    /**
     * @param id
     * @return String
     */
    @Override
    public String getDataUri(String dataId, String runId) {
        String requestUrl = String.format(this.apiClient.API_WORKFLOW_DOWNLOAD_FILE, this.apiClient.url, runId, dataId);
        return requestUrl;
    }

    /**
     * @param fileList
     * @param dType
     * @return List<String>
     */
    @Override
    public List<String> areFilesAvailable(Set<String> fileList, String dType) {
        return new ArrayList<String>();
    }

    /**
     * @param specification
     * @return List<Variable>
     * @throws Exception
     */
    public List<Variable> getVariables(ReanaSpecification specification) throws Exception {
        List<Variable> variables = new ArrayList<Variable>();
        Inputs reanaInputs = specification.inputs;
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

    /**
     * @param id
     * @return String
     * @throws Exception
     */
    public String duplicateWorkflow(String id) throws Exception {
        ReanaSpecification oldSpecification = this.apiClient.getSpecification(id).specification;
        runStatus = this.apiClient.getRunStatus(id);
        String newWorkflowId = this.apiClient.createWorkflow(oldSpecification, runStatus.getName());
        return newWorkflowId;
    }

    /**
     * @param id
     * @param name
     * @return String
     * @throws Exception
     */
    public String duplicateWorkflow(String id, String name) throws Exception {
        ReanaSpecification oldSpecification = this.apiClient.getSpecification(id).specification;
        if (name == null) {
            String[] nameArray = id.split("\\.");
            name = nameArray[0];
        }
        runStatus = this.apiClient.getRunStatus(id);
        String newWorkflowId = this.apiClient.createWorkflow(oldSpecification, name);
        return newWorkflowId;
    }

    /**
     * @param specification
     * @return Map<String, String>
     * @throws Exception
     */
    Map<String, String> convertReanaOutputs(ReanaSpecification specification)
            throws Exception {
        Map<String, String> outputs = new HashMap<String, String>();
        Outputs reanaRunOutputs = specification.outputs;
        for (String output : reanaRunOutputs.getFiles()) {
            Path path = Paths.get(output);
            path.getFileName();
            outputs.put(path.getFileName().toString(), output);
        }
        return outputs;
    }

    /**
     * @param specification
     * @return Map<String, String>
     * @throws Exception
     */
    private Map<String, String> convertReanaInputs(ReanaSpecification specification) throws Exception {
        Map<String, String> inputs = new HashMap<String, String>();
        Inputs reanaRunInputs = specification.inputs;
        for (String input : reanaRunInputs.getFiles()) {
            inputs.put(input, input);
        }
        return inputs;
    }

    /**
     * @param url
     * @param workSpaceId
     * @param name
     * @param dType
     * @return String
     * @throws Exception
     */
    @Override
    public String addData(String url, String workSpaceId, String name, String dType) throws Exception {
        String filePath = downloadData(url, name);
        String remoteUrl = this.apiClient.addFileWorkspace(workSpaceId, filePath, name);
        return remoteUrl;
    }

    @Override
    public Map<String, Variable> getWorkflowInputs(String id) throws Exception {
        // TODO Auto-generated method stub
        ResponseGetSpecification specification = this.apiClient.getSpecification(id);
        ReanaSpecification reanaSpecification = specification.specification;
        Map<String, Variable> variables = getVariablesDiskFormat(reanaSpecification);
        return variables;
    }

    /*
     * Get the list of variables using the Disk format
     * 
     * @param specification
     * 
     * @return List<Variable>
     * 
     * @throws Exception
     */
    public Map<String, Variable> getVariablesDiskFormat(ReanaSpecification specification) throws Exception {
        Map<String, Variable> variables2 = new HashMap<>();
        Inputs reanaInputs = specification.inputs;
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
            variables2.put(key, variable);
        });
        return variables2;
    }
}
