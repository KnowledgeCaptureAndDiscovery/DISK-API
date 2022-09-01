package org.diskproject.shared.classes.adapters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.diskproject.shared.classes.loi.TriggeredLOI.Status;
import org.diskproject.shared.classes.workflow.Variable;
import org.diskproject.shared.classes.workflow.VariableBinding;
import org.diskproject.shared.classes.workflow.Workflow;
import org.diskproject.shared.classes.workflow.WorkflowRun;

public abstract class MethodAdapter {
    public HashMap<Status, String> status = new HashMap<Status, String>();
    private String name;
    private String endpointUrl;
    private String inventoryUrl;

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

    public String getInventoryUrl() {
        return inventoryUrl;
    }

    public void setInventoryUrl(String inventoryUrl) {
        this.inventoryUrl = inventoryUrl;
    }

    /**
     * Returns a list of available methods for the adapter.
     * 
     * @return
     */
    public abstract List<Workflow> getWorkflowList();

    /**
     * Return the list of variable of a given workflow.
     * A variable can be a parameter or a input
     * 
     * @param id
     * @return
     * @throws Exception
     */
    public abstract List<Variable> getWorkflowVariables(String id) throws Exception;

    public abstract String getWorkflowId(String id);

    public abstract String getWorkflowUri(String id);

    public abstract String getWorkflowLink(String id);

    public abstract String getDataUri(String id);

    /**
     * If the method adapter needs to store the data, check if it is already stored.
     * 
     * @param fileList A list of files to check.
     * @param dataType    The RDF file type, this is used by Wings to determine the
     *                 file type.
     * @return
     */
    public abstract List<String> areFilesAvailable(Set<String> fileList, String dataType);

    public abstract String addData(String url, String name, String dataType) throws Exception;

    /**
     * Upload a remote file to the method adapter.
     * Used by the REANA adapter.
     * 
     * @param url         The URL of the file to upload
     * @param workSpaceId The workspace id where the file will be uploaded
     * @param name        The name of the file to upload
     * @param dataType    The data type of the file to upload
     * @return
     * @throws Exception
     */
    public abstract String addData(String url, String workSpaceId, String name, String dataType) throws Exception;

    /**
     * Return a Map of the variable name and variable for a given workflow run.
     * 
     * @param id
     * @return
     * @throws Exception
     */
    public abstract Map<String, Variable> getWorkflowInputs(String id) throws Exception;

    /**
     * Run the workflow.
     * 
     * @param workflowName   Workflow ID
     * @param vBindings      An array of VariableBindings from the Disk System. A
     *                       VariableBinding is a pair of a variable name and a
     *                       value.
     * @param inputVariables A map of input variables from the Workflow System. The
     *                       key is the variable name and the value is the variable.
     * @return
     * @throws Exception
     */
    public abstract String runWorkflow(String workflowName, String workflowLink, List<VariableBinding> vBindings,
            Map<String, Variable> inputVariables) throws Exception;

    public abstract WorkflowRun getRunStatus(String runId) throws Exception;

    public abstract String fetchData(String dataId);

    public abstract Map<String, String> getRunVariableBindings(String runId);

    public abstract String duplicateWorkflow(String workflowId, String newName) throws Exception;

    // Test connection with source
    public abstract boolean ping();
}