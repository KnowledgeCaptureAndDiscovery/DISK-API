package org.diskproject.shared.classes.workflow;

import org.diskproject.shared.classes.common.Status;
import org.diskproject.shared.classes.hypothesis.GoalResult;

import java.util.List;

public class Execution extends ExecutionRecord {
    private String externalId;
    private GoalResult result;
    private List<ExecutionRecord> steps;
    private List<VariableBinding> inputs, outputs;

    public Execution(ExecutionRecord src) {
        super(src);
    }

    public Execution(Status status, String start, String end, String log) {
        super(status, start, end, log);
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
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