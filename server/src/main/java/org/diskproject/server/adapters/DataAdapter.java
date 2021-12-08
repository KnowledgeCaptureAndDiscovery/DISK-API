package org.diskproject.server.adapters;

import java.util.List;
import java.util.Map;

import org.diskproject.shared.classes.loi.LineOfInquiry;

public abstract class DataAdapter {
    private String endpointUrl, name, username, password, prefix, namespace;

    public DataAdapter (String URI, String name) {
        this.endpointUrl = URI;
        this.name = name;
        this.username = null;
        this.password = null;
    }

    public DataAdapter (String URI, String name, String username, String password) {
        this.endpointUrl = URI;
        this.name = name;
        this.username = username;
        this.password = password;
    }
    
    public String getEndpointUrl () {
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
    
    public String getNamespace () {
        return this.namespace;
    }
    
    public String toString () {
        return "[" + this.name + "] " + this.username + "@" + this.endpointUrl;
    }

    public abstract List<DataResult> query (String queryString);

    //This data query must return two variable names:
    static public String VARURI = "uri";
    static public String VARLABEL = "label";
    public abstract List<DataResult> queryOptions (String varname, String constraintQuery);

    // file -> hash
    public abstract Map<String, String> getFileHashes (List<String> dsurls);

    // Check that a LOI is correctly configured for this adapter
    public abstract boolean validateLOI (LineOfInquiry loi, Map<String, String> values);
}