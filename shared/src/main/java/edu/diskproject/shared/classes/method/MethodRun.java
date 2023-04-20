package org.diskproject.shared.classes.method;

import java.util.List;

public class MethodRun {
    private String id;
    private Method method;
    private List<MethodInput> inputs;
    private List<MethodOutput> outputs;
    
    public MethodRun (String id, Method method, List<MethodInput> inputs, List<MethodOutput> outputs) {
        this.id = id;
        this.method = method;
        this.inputs = inputs;
        this.outputs = outputs;
    }
    
    public void setMethod (Method method) {
        this.method = method;
    }
    
    public void setInputs (List<MethodInput> bindings) {
        this.inputs = bindings;
    }

    public void setOutputs (List<MethodOutput> bindings) {
        this.outputs = bindings;
    }
    
    public String getId () {
        return this.id;
    }
    
    public Method getMethod () {
        return this.method;
    }
    
    public List<MethodInput> getInputs () {
        return this.inputs;
    }
    
    public List<MethodOutput> getOutputs () {
        return this.outputs;
    }
}