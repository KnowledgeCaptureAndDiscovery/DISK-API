package org.diskproject.shared.classes.loi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.diskproject.shared.classes.util.GUID;
import org.diskproject.shared.classes.workflow.VariableBinding;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class TriggeredLOI implements Comparable<TriggeredLOI> {
  public static enum Status {
    QUEUED, RUNNING, FAILED, SUCCESSFUL
  };
  
  String id;
  String name;
  String description;
  Status status;

  String loiId;
  String parentHypothesisId;
  List<String> resultingHypothesisIds;
  List<WorkflowBindings> workflows;
  List<WorkflowBindings> metaWorkflows;

  double confidenceValue;
  List<String> inputFiles, outputFiles;

  String author;
  String notes;
  String dateCreated;
  String dateModified;
  String dataQuery;
  String relevantVariables;
  String explanation;
  String dataSource;

  public TriggeredLOI() {
    workflows = new ArrayList<WorkflowBindings>();
    metaWorkflows = new ArrayList<WorkflowBindings>();
    resultingHypothesisIds = new ArrayList<String>();
    inputFiles = new ArrayList<String>();
    outputFiles = new ArrayList<String>();
  }

  public TriggeredLOI(String id,
		  String name,
		  String description,
		  String dataSource,
		  Status status,
		  String loiId,
		  String parentHypothesisId,
		  List<String> resultingHypothesisIds,
		  List<WorkflowBindings> workflows,
		  List<WorkflowBindings> metaWorkflows) {
	  this.id = id;
	  this.name = name;
	  this.description = description;
	  this.dataSource = dataSource;
	  this.status = status;
	  this.loiId = loiId;
	  this.parentHypothesisId = parentHypothesisId;
	  this.resultingHypothesisIds = resultingHypothesisIds;
	  this.workflows = workflows;
	  this.metaWorkflows = metaWorkflows;

	  inputFiles = new ArrayList<String>();
	  outputFiles = new ArrayList<String>();
	  }

  public TriggeredLOI(LineOfInquiry loi, String hypothesisId) {
    this.id = GUID.randomId("TriggeredLOI");
    this.loiId = loi.getId();
    this.name = "Triggered: " + loi.getName();
    this.description = loi.getDescription();
    this.dataSource = loi.getDataSource();
    this.parentHypothesisId = hypothesisId;
    workflows = new ArrayList<WorkflowBindings>();
    metaWorkflows = new ArrayList<WorkflowBindings>();
    resultingHypothesisIds = new ArrayList<String>();
    inputFiles = new ArrayList<String>();
    outputFiles = new ArrayList<String>();
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

  public String getExplanation () {
	  return this.explanation;
  }

  public void setExplanation (String explanation) {
	  this.explanation = explanation;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getRelevantVariables() {
    return relevantVariables;
  }

  public void setRelevantVariables(String v) {
    this.relevantVariables = v;
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

  public String getLoiId() {
    return loiId;
  }

  public void setLoiId(String loiId) {
    this.loiId = loiId;
  }

  public String getParentHypothesisId() {
    return parentHypothesisId;
  }

  public void setParentHypothesisId(String parentHypothesisId) {
    this.parentHypothesisId = parentHypothesisId;
  }

  public List<String> getResultingHypothesisIds() {
    return resultingHypothesisIds;
  }

  public void setResultingHypothesisIds(List<String> resultingHypothesisIds) {
    this.resultingHypothesisIds = resultingHypothesisIds;
  }
  
  public void addResultingHypothesisId(String resultingHypothesisId) {
    this.resultingHypothesisIds.add(resultingHypothesisId);
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

  //FIXME: This is the way it looks in the list.
  @JsonIgnore
  public String getHeaderHTML() {
    String extra ="", extracls="";
    if(status != null) {
      String icon = "icons:hourglass-empty";
      if(status == Status.SUCCESSFUL) {
        icon = "icons:check";
      }
      else if(status == Status.FAILED) {
        icon = "icons:clear";
      }
      extra = " <iron-icon class='"+status+"' icon='"+icon+"' />";
      extracls = " " +status;
    } else {
      extracls = " TORUN";
    }

    String html = "<div class='name" + extracls+ "'>"+ name + extra +"</div>";
    html += "<div class='description'>";
    if(description != null)
      html += description;
    html += "</div>";

    return html;
  }

  public String toString() {
    Collections.sort(this.workflows);
    Collections.sort(this.metaWorkflows);
    return this.getLoiId() + "-" 
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

  public void setInputFiles (List<String> inputs) {
	  this.inputFiles = inputs;
  }

  public void setOutputFiles (List<String> outputs) {
	  this.outputFiles = outputs;
  }
  
  public List<String> getInputFiles () {
	  return this.inputFiles;
  }
  
  public List<String> getOutputFiles () {
	  return this.outputFiles;
  }

  public void addInputFile (String input) {
	  this.inputFiles.add(input);
  }

  public void addOutputFile (String output) {
	  this.outputFiles.add(output);
  }
}