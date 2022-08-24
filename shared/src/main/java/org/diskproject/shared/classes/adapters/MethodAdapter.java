package org.diskproject.shared.classes.adapters;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.diskproject.shared.classes.workflow.Variable;
import org.diskproject.shared.classes.workflow.VariableBinding;
import org.diskproject.shared.classes.workflow.Workflow;
import org.diskproject.shared.classes.workflow.WorkflowRun;

public abstract class MethodAdapter {
    private String name;
    private String endpointUrl;
    private String inventoryUrl;


    private String username;
    private String password;
    private String description;
    private Float version; 

    public MethodAdapter (String adapterName, String url) {
        this.name = adapterName;
        this.endpointUrl = url;
    }
    
    public MethodAdapter (String adapterName, String url, String username, String password) {
        this.name = adapterName;
        this.endpointUrl = url;
        this.username = username;
        this.password = password;
    }

    public MethodAdapter (String adapterName, String url, String username, String password, String inventory) {
        this.name = adapterName;
        this.endpointUrl = url;
        this.username = username;
        this.password = password;
        this.inventoryUrl = inventory;
    }

    public Float getVersion () {
        return this.version;
    }

    public void setVersion (Float v) {
        this.version = v;
    }

    public String getDescription () {
        return this.description;
    }

    public void setDescription (String desc) {
        this.description = desc;
    }
    
    public String getName () {
        return this.name;
    }
    
    public String getEndpointUrl () {
        return this.endpointUrl;
    }
    
    protected String getUsername () {
        return this.username;
    }
    
    protected String getPassword () {
        return this.password;
    }

    public String toString () {
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

	public abstract String getDataUri (String id);

	/**
     * If the method adapter needs to store the data, check if it is already stored.
	 * @param fileList A list of files to check.
	 * @param dType The RDF file type, this is used by Wings to determine the file type.
	 * @return
	 */
	public abstract List<String> areFilesAvailable (Set<String> fileList, String dType);

	public abstract String addData (String url, String name, String dType) throws Exception;

	public abstract String addData (String url, String workSpaceId, String name, String dType) throws Exception;
	/**
     * Return a Map of the variable name and variable for a given workflow run.
	 * @param id
	 * @return
	 */
	public abstract Map<String, Variable> getWorkflowInputs (String id);

	/**
     * Run the workflow.
	 * @param wfId Workflow ID
	 * @param vBindings An array of VariableBindings from the Disk System. A VariableBinding is a pair of a variable name and a value.
	 * @param inputVariables A map of input variables from the Workflow System. The key is the variable name and the value is the variable.
	 * @return
	 * @throws Exception
	 */
	public abstract String runWorkflow (String wfId, List<VariableBinding> vBindings, Map<String, Variable> inputVariables) throws Exception;

	public abstract WorkflowRun getRunStatus (String runId) throws Exception;

	public abstract String fetchData (String dataId);

	public abstract Map<String, String> getRunVariableBindings (String runId);

    // Test connection with source
    public abstract boolean ping ();
}