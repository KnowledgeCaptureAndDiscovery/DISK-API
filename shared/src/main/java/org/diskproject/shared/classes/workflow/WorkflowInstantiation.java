package org.diskproject.shared.classes.workflow;

import org.diskproject.shared.classes.common.Status;

import java.util.List;

public class WorkflowInstantiation extends WorkflowSeed {
    Status status;
    List<VariableBinding> dataBindings;
    List<Execution> executions;

    public WorkflowInstantiation () {
    }

    public WorkflowInstantiation (WorkflowSeed src) {
        super(src);
    }

    public Status getStatus() {
        return status;
    }
    public void setStatus(Status status) {
        this.status = status;
    }
    public List<VariableBinding> getDataBindings() {
        return dataBindings;
    }
    public void setDataBindings(List<VariableBinding> dataBindings) {
        this.dataBindings = dataBindings;
    }
    public List<Execution> getExecutions() {
        return executions;
    }
    public void setExecutions(List<Execution> executions) {
        this.executions = executions;
    }
}
