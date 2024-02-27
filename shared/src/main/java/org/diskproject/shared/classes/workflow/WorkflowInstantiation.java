package org.diskproject.shared.classes.workflow;

import org.diskproject.shared.classes.common.Status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

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

    @JsonIgnore
    public void addOrReplaceExecution (Execution e) {
        if (this.executions == null)
            this.executions = new ArrayList<Execution>();
        List<Execution> newExec = new ArrayList<Execution>();
        boolean added = false;
        for (Execution cur: this.executions) {
            if (cur.getExternalId().equals(e.getExternalId())) {
                newExec.add(e);
                added = true;
            } else {
                newExec.add(cur);
            }
        }
        if (!added) {
            newExec.add(e);
        }
        this.setExecutions(newExec);
    }

    @Override
    public String toString() {
        String txt = super.toString() + " {data=[";
        if (dataBindings != null) {
            int i = 0;
            Collections.sort(dataBindings);
            for (VariableBinding dat : dataBindings) {
                if (i > 0)
                    txt += ", ";
                txt += dat.getVariable() + " = " + dat.getBinding();
                i++;
            }
            txt += "]}";
        }
        return txt;
    }
}
