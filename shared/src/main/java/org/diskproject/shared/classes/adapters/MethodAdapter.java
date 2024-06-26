package org.diskproject.shared.classes.adapters;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.diskproject.shared.classes.workflow.WorkflowVariable;
import org.diskproject.shared.classes.workflow.Execution;
import org.diskproject.shared.classes.workflow.VariableBinding;
import org.diskproject.shared.classes.workflow.WorkflowTemplate;

public abstract class MethodAdapter {
    private String name;
    private String endpointUrl;
    private String username;
    private String password;
    private String description;
    private Float version; 
    private String id;

    public static class FileAndMeta {
        public byte[] data;
        public String contentType;
        public FileAndMeta (byte[] d, String t) {
            data = d;
            contentType = t;
        }
    }

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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String toString () {
        return "[" + this.name + "] " + (this.username != null ? this.username + "@" : "") + this.endpointUrl;
    }
    
	public abstract List<WorkflowTemplate> getWorkflowList();

	public abstract List<WorkflowVariable> getWorkflowVariables(String id);

	public abstract String getWorkflowId(String id);

	public abstract String getWorkflowUri(String id);

	public abstract String getWorkflowLink(String id);

	public abstract String getDataUri (String id);

	public abstract List<String> areFilesAvailable (Set<String> fileList, String dType);

	public abstract String addData (String url, String name, String dType) throws Exception;

	public abstract Map<String, WorkflowVariable> getWorkflowInputs (String id);

	public abstract List<String> runWorkflow (String wfId, List<VariableBinding> vBindings, Map<String, WorkflowVariable> inputVariables);

	public abstract Execution getRunStatus (String runId);

	public abstract FileAndMeta fetchData (String dataId);

	public abstract Map<String, String> getRunVariableBindings (String runId);

	public abstract boolean registerData (String dataid, String type);

    // Test connection with source
    public abstract boolean ping ();
}