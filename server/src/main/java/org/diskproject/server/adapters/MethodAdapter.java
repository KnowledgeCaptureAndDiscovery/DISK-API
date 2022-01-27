package org.diskproject.server.adapters;

import java.util.Map;

import org.diskproject.shared.classes.loi.LineOfInquiry;

public abstract class MethodAdapter {
    private String name;
    private String endpointUrl;
    private String username;
    private String password;
    
    public MethodAdapter (String adapterName, String url, String username, String password) {
        this.name = adapterName;
        this.endpointUrl = url;
        this.username = username;
        this.password = password;
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
    
    // Check that a LOI is correctly configured for this adapter
    public abstract boolean validateLOI (LineOfInquiry loi, Map<String, String> values);
}