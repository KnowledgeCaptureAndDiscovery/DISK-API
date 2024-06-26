package org.diskproject.shared.classes.workflow;

import org.diskproject.shared.classes.common.Endpoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WorkflowSeed implements Comparable<WorkflowSeed> {
    String id, link, description, name;
    List<VariableBinding> parameters, inputs, outputs;
    Endpoint source;

    public WorkflowSeed (WorkflowSeed src) {
        this.id = src.getId();
        this.link = src.getLink();
        this.name = src.getName();
        this.description = src.getDescription();
        this.source = src.getSource();
        //FIXME
        this.parameters = new ArrayList<VariableBinding>(src.getParameters());
        this.inputs = new ArrayList<VariableBinding>(src.getInputs());
        this.outputs = new ArrayList<VariableBinding>(src.getOutputs());
    }

    public WorkflowSeed () {
    }
    
    public String getLink() {
        return link;
    }
    public void setLink(String link) {
        this.link = link;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public List<VariableBinding> getParameters() {
        return parameters;
    }
    public void setParameters(List<VariableBinding> parameters) {
        this.parameters = parameters;
    }
    public List<VariableBinding> getInputs() {
        return inputs;
    }
    public void setInputs(List<VariableBinding> inputs) {
        this.inputs = inputs;
    }
    public Endpoint getSource() {
        return source;
    }
    public void setSource(Endpoint source) {
        this.source = source;
    }

    public List<VariableBinding> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<VariableBinding> outputs) {
        this.outputs = outputs;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    @Override
    public String toString() {
        String description = source.toString() + "|" + this.getLink() + "{";
        int i = 0;
        if (parameters != null) {
            Collections.sort(parameters);
            for (VariableBinding param : parameters) {
                if (i > 0)
                    description += ", ";
                description += param.getVariable() + " = " + param.getBinding();
                i++;
            }
        }
        if (inputs != null) {
            Collections.sort(inputs);
            for (VariableBinding inp : inputs) {
                if (i > 0)
                    description += ", ";
                description += inp.getVariable() + " = " + inp.getBinding();
                i++;
            }
        }
        if (outputs != null) {
            Collections.sort(outputs);
            for (VariableBinding ou : outputs) {
                if (i > 0)
                    description += ", ";
                description += ou.getVariable() + " = " + ou.getBinding();
                i++;
            }
        }
        description += "}";
        return description;
    }

    @Override
    public int compareTo(WorkflowSeed o) {
        return this.toString().compareTo(o.toString());
    }
}
