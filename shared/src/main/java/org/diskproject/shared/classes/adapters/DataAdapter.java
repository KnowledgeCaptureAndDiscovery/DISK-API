package org.diskproject.shared.classes.adapters;

import java.util.List;
import java.util.Map;

public abstract class DataAdapter {
    private String endpointUrl, name, username, password, description;
    private String prefix, namespace, prefixResolution;

    public DataAdapter(String URI, String name) {
        this.endpointUrl = URI;
        this.name = name;
        this.username = null;
        this.password = null;
    }

    public DataAdapter(String URI, String name, String username, String password) {
        this.endpointUrl = URI;
        this.name = name;
        this.username = username;
        this.password = password;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String desc) {
        this.description = desc;
    }

    public String getEndpointUrl() {
        return this.endpointUrl;
    }

    public String getName ( ) {
        return this.name;
    }
    
    protected String getUsername () {
        return this.username;
    }

    protected String getPassword () {
        return this.password;
    }
    
    public void setPrefix (String prefix, String namespace) {
        this.prefix = prefix;
        this.namespace = namespace;
    }
    
    public String getPrefix () {
        return this.prefix;
    }

    public void setPrefixResolution (String prefixResolution) {
        this.prefixResolution = prefixResolution;
    }
    
    public String getPrefixResolution () {
        return this.prefixResolution;
    }
    
    public String getNamespace () {
        return this.namespace;
    }
    
    public String toString () {
        return "[" + this.name + "] " + (this.username != null ? this.username + "@" : "") + this.endpointUrl;
    }

    public abstract List<DataResult> query (String queryString) throws Exception;
    public abstract void queryCSV(String queryString) throws Exception;

    //This data query must return two variable names:
    static public String VARURI = "uri";
    static public String VARLABEL = "label";
    public abstract List<DataResult> queryOptions (String varname, String constraintQuery) throws Exception;

    // file -> hash
    public abstract Map<String, String> getFileHashes (List<String> dsUrls) throws Exception;

    // Test connection with source
    public abstract boolean ping ();
    
    public abstract Map<String,String> getFileHashesByETag(List<String> dsUrls) throws Exception;

}