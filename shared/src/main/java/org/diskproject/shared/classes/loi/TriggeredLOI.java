package org.diskproject.shared.classes.loi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.diskproject.shared.classes.util.GUID;
import org.diskproject.shared.classes.workflow.VariableBinding;
import org.diskproject.shared.classes.workflow.WorkflowRun.Status;

public class TriggeredLOI extends LineOfInquiry implements Comparable<TriggeredLOI> {
  Status status;
  double confidenceValue;
  String confidenceType;
  String parentLoiId, parentHypothesisId;
  String queryResults;

  public TriggeredLOI() {}

  public TriggeredLOI(String id, String name, String description, String dataSource, Status status,
		  String loiId, String parentHypothesisId, List<WorkflowBindings> workflows,
      List<WorkflowBindings> metaWorkflows) {
    super(id,name,description,dataSource,"","",workflows,metaWorkflows);
	  this.parentLoiId = loiId;
	  this.parentHypothesisId = parentHypothesisId;

	  this.dataSource = dataSource;
	  this.status = status;
	  this.workflows = workflows;
	  this.metaWorkflows = metaWorkflows;
	}

  public TriggeredLOI(LineOfInquiry loi, String hypothesisId) {
    super(GUID.randomId("TriggeredLOI"), "Triggered: " + loi.getName(), loi.getDescription());
    this.parentLoiId = loi.getId();
    this.parentHypothesisId = hypothesisId;
    this.dataSource = loi.getDataSource();
    String tableVariables = loi.getTableVariables();
    String tableExplanation = loi.getTableDescription();
    String dataQueryExplanation = loi.getDataQueryExplanation();
    if (tableVariables != null) this.tableVariables = tableVariables;
    if (tableExplanation != null) this.tableDescription = tableExplanation;
    if (dataQueryExplanation != null) this.dataQueryExplanation = dataQueryExplanation;
  }

  public void copyWorkflowBindings(List<WorkflowBindings> fromlist,
      List<WorkflowBindings> toList) {
    for(WorkflowBindings from : fromlist) {
      WorkflowBindings to = new WorkflowBindings();
      to.setWorkflow(from.getWorkflow());
      to.setMeta(from.getMeta());
      to.setBindings(new ArrayList<VariableBinding>(from.getBindings()));
      toList.add(to);
    }
  }

  public String toString() {
    Collections.sort(this.workflows);
    Collections.sort(this.metaWorkflows);
    return this.getParentLoiId() + "-" 
        + this.getParentHypothesisId() + "-" 
        + this.getWorkflows() + "-" 
        + this.getMetaWorkflows();
  }

  public int compareTo(TriggeredLOI o) {
    return this.toString().compareTo(o.toString());
  }

  public String getParentLoiId() {
    return parentLoiId;
  }

  public String getParentHypothesisId() {
    return parentHypothesisId;
  }

  public void setParentLoiId(String v) {
    this.parentLoiId = v;
  }

  public void setParentHypothesisId(String v) {
    this.parentHypothesisId = v;
  }
  
  public void setConfidenceValue (double cv) {
	  this.confidenceValue = cv;
  }
  
  public double getConfidenceValue () {
	  return this.confidenceValue;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public void setConfidenceType(String type) {
    this.confidenceType = type;
  }

  public String getConfidenceType() {
    return confidenceType;
  }

  public String getQueryResults() {
    return queryResults;
  }

  public void setQueryResults(String raw) {
    this.queryResults = raw;
  }
}