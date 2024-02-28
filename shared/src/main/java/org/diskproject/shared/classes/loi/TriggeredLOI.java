package org.diskproject.shared.classes.loi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.diskproject.shared.classes.DISKResource;
import org.diskproject.shared.classes.common.Status;
import org.diskproject.shared.classes.hypothesis.Goal;
import org.diskproject.shared.classes.workflow.WorkflowInstantiation;

public class TriggeredLOI extends LOICommon implements Comparable<TriggeredLOI> {
    Status status;
    LineOfInquiry parentLoi;
    Goal parentGoal;
    DataQueryResult queryResults;
    List<WorkflowInstantiation> workflows, metaWorkflows;

    public TriggeredLOI () {}

    public TriggeredLOI (DISKResource src) {
        super(src);
    }

    public TriggeredLOI(LineOfInquiry loi, Goal goal) {
        super("", "Triggered: " + loi.getName(), loi);
        this.status = Status.PENDING;
        this.parentGoal = goal;
        this.parentLoi = loi;
        this.workflows = new ArrayList<WorkflowInstantiation>();
        this.metaWorkflows = new ArrayList<WorkflowInstantiation>();
    }

    public String toString() {
        if (this.workflows != null) Collections.sort(this.workflows);
        if (this.metaWorkflows != null) Collections.sort(this.metaWorkflows);
        return (this.parentGoal != null ? this.parentGoal.getId() : "") + "-"
                + (this.parentLoi != null ? this.parentLoi.getId() : "") + "-"
                + (this.getWorkflows() != null ? this.getWorkflows() : "NULL") + "-"
                + (this.getMetaWorkflows() != null ? this.getMetaWorkflows() : "NULL");
    }

    public int compareTo(TriggeredLOI o) {
        return this.toString().compareTo(o.toString());
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LineOfInquiry getParentLoi() {
        return parentLoi;
    }

    public void setParentLoi(LineOfInquiry parentLoi) {
        this.parentLoi = parentLoi;
    }

    public Goal getParentGoal() {
        return parentGoal;
    }

    public void setParentGoal(Goal parentGoal) {
        this.parentGoal = parentGoal;
    }

    public DataQueryResult getQueryResults() {
        return queryResults;
    }

    public void setQueryResults(DataQueryResult queryResults) {
        this.queryResults = queryResults;
    }

    public List<WorkflowInstantiation> getWorkflows() {
        return workflows;
    }

    public void setWorkflows(List<WorkflowInstantiation> workflows) {
        this.workflows = workflows;
    }

    public List<WorkflowInstantiation> getMetaWorkflows() {
        return metaWorkflows;
    }

    public void setMetaWorkflows(List<WorkflowInstantiation> metaWorkflows) {
        this.metaWorkflows = metaWorkflows;
    }
}