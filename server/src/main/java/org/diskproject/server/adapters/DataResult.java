package org.diskproject.server.adapters;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DataResult {
    private Set<String> varNames;
    private Map<String, String> values;
    private Map<String, String> names;

    public DataResult () {
        this.varNames = new HashSet<String>();
        this.values = new HashMap<String, String>();
        this.names = new HashMap<String, String>();
    }
    
    public void addValue (String varName, String value) {
        if (!this.varNames.contains(varName)) {
            this.varNames.add(varName);
        }
        this.values.put(varName, value);
    }

    public void addValue (String varName, String value, String name) {
        this.addValue(varName, value);
        String decodedName = name;
        try {
            decodedName = URLDecoder.decode(name, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            decodedName = name;
        }
        this.names.put(varName, decodedName);
    }
    
    public String getValue (String varName) {
        return this.varNames.contains(varName) ? this.values.get(varName) : null;
    }
    
    public Set<String> getVariableNames () {
        return this.varNames;
    }
    
    public boolean isLiteral (String varName) {
        return this.varNames.contains(varName) && this.values.get(varName) != null && this.names.get(varName) == null;
    }
    
    public String getName (String varName) {
        return this.varNames.contains(varName) ? this.names.get(varName) : null;
    }
    
    public String toString () {
        String repr = Integer.toString(this.varNames.size()) + " variables: ";
        for (String varname: this.varNames) {
            repr += varname + " ";
        }
        repr += "\n";
        for (String varname: this.varNames) {
            repr += "  " + varname + ": " + getValue(varname);
            if (!isLiteral(varname))
                repr += " (" +getName(varname) + ")";
            repr += "\n";
        }

        return repr;
    }
}
