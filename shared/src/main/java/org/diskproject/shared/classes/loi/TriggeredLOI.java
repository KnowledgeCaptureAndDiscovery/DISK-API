package org.diskproject.shared.classes.loi;

import java.util.Collections;
import java.util.List;

import org.diskproject.shared.classes.DISKResource;
import org.diskproject.shared.classes.common.Status;
import org.diskproject.shared.classes.hypothesis.Goal;
import org.diskproject.shared.classes.util.GUID;
import org.diskproject.shared.classes.workflow.WorkflowInstantiation;

public class TriggeredLOI extends LineOfInquiry implements Comparable<TriggeredLOI> {
    Status status;
    LineOfInquiry parentLoi;
    Goal parentGoal;
    DataQueryResult queryResults;
    List<WorkflowInstantiation> workflows, metaWorkflows;

    public TriggeredLOI (DISKResource src) {
        super(src);
    }

    public TriggeredLOI() {
    }

    public TriggeredLOI(LineOfInquiry loi, String hypothesisId) {
        super(GUID.randomId("TriggeredLOI"), "Triggered: " + loi.getName(), loi.getDescription());
    }

    public String toString() {
        Collections.sort(this.workflows);
        Collections.sort(this.metaWorkflows);
        return (this.parentGoal != null ? this.parentGoal.getId() : "") + "-"
                + (this.parentLoi != null ? this.parentGoal.getId() : "") + "-"
                + this.getWorkflowSeeds() + "-"
                + this.getMetaWorkflowSeeds();
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