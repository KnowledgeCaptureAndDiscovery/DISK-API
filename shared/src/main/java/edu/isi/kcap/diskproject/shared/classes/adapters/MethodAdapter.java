package edu.isi.kcap.diskproject.shared.classes.adapters;

import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.isi.kcap.diskproject.shared.classes.workflow.Variable;
import edu.isi.kcap.diskproject.shared.classes.workflow.VariableBinding;
import edu.isi.kcap.diskproject.shared.classes.workflow.Workflow;
import edu.isi.kcap.diskproject.shared.classes.workflow.WorkflowRun;

public abstract class MethodAdapter {
    private String name;
    private String endpointUrl;
    private String username;
    private String password;
    private String description;
    private Float version;

    public MethodAdapter(String adapterName, String url) {
        this.name = adapterName;
        this.endpointUrl = url;
    }

    public MethodAdapter(String adapterName, String url, String username, String password) {
        this.name = adapterName;
        this.endpointUrl = url;
        this.username = username;
        this.password = password;
    }

    public Float getVersion() {
        return this.version;
    }

    public void setVersion(Float v) {
        this.version = v;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String desc) {
        this.description = desc;
    }

    public String getName() {
        return this.name;
    }

    public String getEndpointUrl() {
        return this.endpointUrl;
    }

    protected String getUsername() {
        return this.username;
    }

    protected String getPassword() {
        return this.password;
    }

    public String toString() {
        return "[" + this.name + "] " + (this.username != null ? this.username + "@" : "") + this.endpointUrl;
    }

    public abstract List<Workflow> getWorkflowList();

    public abstract List<Variable> getWorkflowVariables(String id);

    public abstract String getWorkflowId(String id);

    public abstract String getWorkflowUri(String id);

    public abstract String getWorkflowLink(String id);

    public abstract String getDataUri(String id);

    public abstract List<String> areFilesAvailable(Set<String> fileList, String dType);

    public abstract String addData(String url, String name, String dType) throws Exception;

    public abstract Map<String, Variable> getWorkflowInputs(String id);

    public abstract String runWorkflow(String wfId, List<VariableBinding> vBindings,
            Map<String, Variable> inputVariables);

    public abstract WorkflowRun getRunStatus(String runId);

    public abstract byte[] fetchData(String dataId);

    public abstract Map<String, String> getRunVariableBindings(String runId);

    // Test connection with source
    public abstract boolean ping();
}