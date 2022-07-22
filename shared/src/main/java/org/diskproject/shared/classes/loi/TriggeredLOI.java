package org.diskproject.shared.classes.loi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.diskproject.shared.classes.util.GUID;
import org.diskproject.shared.classes.workflow.VariableBinding;

public class TriggeredLOI implements Comparable<TriggeredLOI> {
  public static enum Status {
    QUEUED, RUNNING, FAILED, SUCCESSFUL
  };
  
  String id, name, description, author;
  String dataSource;
  Status status;
  String parentLoiId, parentHypothesisId;
  List<WorkflowBindings> workflows, metaWorkflows;

  double confidenceValue;
  String errorMessage;
  String notes;
  String dateCreated, dateModified;
  String dataQuery, dataQueryExplanation;
  String tableVariables, tableDescription;

  public TriggeredLOI() {
    workflows = new ArrayList<WorkflowBindings>();
    metaWorkflows = new ArrayList<WorkflowBindings>();
  }

  public TriggeredLOI(String id,
		  String name,
		  String description,
		  String dataSource,
		  Status status,
      String errorMessage,
		  String loiId,
		  String parentHypothesisId,
		  List<WorkflowBindings> workflows,
		  List<WorkflowBindings> metaWorkflows) {
	  this.id = id;
	  this.name = name;
	  this.description = description;
	  this.dataSource = dataSource;
	  this.status = status;
	  this.parentLoiId = loiId;
	  this.parentHypothesisId = parentHypothesisId;
	  this.workflows = workflows;
	  this.metaWorkflows = metaWorkflows;
	}

  public TriggeredLOI(LineOfInquiry loi, String hypothesisId) {
    this.id = GUID.randomId("TriggeredLOI");
    this.parentLoiId = loi.getId();
    this.name = "Triggered: " + loi.getName();
    this.description = loi.getDescription();
    this.dataSource = loi.getDataSource();
    this.parentHypothesisId = hypothesisId;
    String tableVariables = loi.getTableVariables();
    String tableExplanation = loi.getTableDescription();
    String dataQueryExplanation = loi.getDataQueryExplanation();
    String errorMessage = "";
    if (tableVariables != null) this.tableVariables = tableVariables;
    if (tableExplanation != null) this.tableDescription = tableExplanation;
    if (dataQueryExplanation != null) this.dataQueryExplanation = dataQueryExplanation;
    workflows = new ArrayList<WorkflowBindings>();
    metaWorkflows = new ArrayList<WorkflowBindings>();
  }

  public void copyWorkflowBindings(List<WorkflowBindings> fromlist,
      List<WorkflowBindings> tolist) {
    for(WorkflowBindings from : fromlist) {
      WorkflowBindings to = new WorkflowBindings();
      to.setWorkflow(from.getWorkflow());
      to.setMeta(from.getMeta());
      to.setBindings(new ArrayList<VariableBinding>(from.getBindings()));
      tolist.add(to);
    }
  }
  
  public void setErrorMessage(String errorMessage){
    this.errorMessage = errorMessage;
  }

  public String getErrorMessage(){
    return this.errorMessage;
  }
  
  public void setConfidenceValue (double cv) {
	  this.confidenceValue = cv;
  }
  
  public double getConfidenceValue () {
	  return this.confidenceValue;
  }

  public void setDataSource (String ds) {
	  this.dataSource = ds;
  }

  public String getDataSource () {
	  return this.dataSource;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setDataQuery (String dq) {
    this.dataQuery = dq;
  }

  public String getDataQuery () {
    return this.dataQuery;
  }

  public String getTableDescription () {
	  return this.tableDescription;
  }

  public void setTableDescription (String explanation) {
	  this.tableDescription = explanation;
  }

  public String getDataQueryExplanation () {
	  return this.dataQueryExplanation;
  }

  public void setDataQueryExplanation (String explanation) {
	  this.dataQueryExplanation = explanation;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getTableVariables() {
    return tableVariables;
  }

  public void setTableVariables(String v) {
    this.tableVariables = v;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Status getStatus() {
    return status;
  }
  
  public String getNotes () {
	  return this.notes;
  }
  
  public void setNotes (String notes) {
	  this.notes = notes;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public String getParentLoiId() {
    return parentLoiId;
  }

  public void setParentLoiId(String loiId) {
    this.parentLoiId = loiId;
  }

  public String getParentHypothesisId() {
    return parentHypothesisId;
  }

  public void setParentHypothesisId(String parentHypothesisId) {
    this.parentHypothesisId = parentHypothesisId;
  }

  public List<WorkflowBindings> getWorkflows() {
    return workflows;
  }

  public void setWorkflows(List<WorkflowBindings> workflows) {
    this.workflows = workflows;
  }

  public List<WorkflowBindings> getMetaWorkflows() {
    return metaWorkflows;
  }

  public void setMetaWorkflows(List<WorkflowBindings> metaWorkflows) {
    this.metaWorkflows = metaWorkflows;
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
 
  public void setDateCreated(String date) {
	  this.dateCreated = date;
  }
 
  public void setAuthor (String author) {
	  this.author = author;
  }
 
  public String getDateCreated () {
	  return this.dateCreated;
  }
 
  public String getAuthor () {
	  return this.author;
  }

  public void setDateModified(String date) {
	  this.dateModified = date;
  }
 
  public String getDateModified () {
	  return dateModified;
  }
}