package org.diskproject.shared.classes.workflow;

import org.diskproject.shared.classes.hypothesis.GoalResult;

import java.util.List;

public class Execution extends ExecutionRecord {
    private String externalId, link;
    private GoalResult result;
    private List<ExecutionRecord> steps;
    private List<VariableBinding> inputs, outputs;

    @Override
    public String toString () {
        String txt = "execution={\n  id: " + (externalId == null ? "null" : externalId);
        txt += ",\n  link: " + (link == null ? "null" : link);
        txt += ",\n  result: " + (result == null ? "null" : result.toString());
        txt += ",\n  info: " + super.toString().replaceAll("\n", "\n  ");
        txt += ",\n  inputs: [";
        if (inputs != null) for (VariableBinding vb: inputs) {
            txt += "\n    " + vb.toString();
        }
        txt += "\n  ],\n  outputs: [";
        if (outputs != null) for (VariableBinding vb: outputs) {
            txt += "\n    " + vb.toString();
        }
        txt += "\n  ],\n  steps: [";
        if (steps != null) for (ExecutionRecord er: steps) {
            txt += "\n    " + er.toString().replaceAll("\n", "\n    ");
        }
        return txt + "\n  ]\n}";
    }

    public Execution (String id) {
        this.externalId = id;
    }

    public Execution(ExecutionRecord src) {
        super(src);
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public GoalResult getResult() {
        return result;
    }

    public void setResult(GoalResult result) {
        this.result = result;
    }

    public List<ExecutionRecord> getSteps() {
        return steps;
    }

    public void setSteps(List<ExecutionRecord> steps) {
        this.steps = steps;
    }

    public List<VariableBinding> getInputs() {
        return inputs;
    }

    public void setInputs(List<VariableBinding> inputs) {
        this.inputs = inputs;
    }

    public List<VariableBinding> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<VariableBinding> outputs) {
        this.outputs = outputs;
    }
}