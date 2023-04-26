package org.diskproject.shared.classes.loi;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LineOfInquiry {
  public static enum UpdateStatus {
    ON_DATA_UPDATE, ON_METHOD_UPDATE, BOTH, DISABLED
  };

  // Metadata
  String id, name, description, notes;
  // PROV
  String author, dateCreated, dateModified;
  // Data query
  String dataSource, dataQuery, dataQueryExplanation;
  String tableVariables, tableDescription;
  // Methods
  List<WorkflowBindings> workflows, metaWorkflows;
  // Linking with the hypothesis (question)
  String hypothesisQuery, questionId;
  UpdateStatus active;

  public LineOfInquiry() {
    this.workflows = new ArrayList<WorkflowBindings>();
    this.metaWorkflows = new ArrayList<WorkflowBindings>();
  }

  public LineOfInquiry(String id, String name, String description){
	  this.id = id;
	  this.name = name;
	  this.description = description;
    this.active = UpdateStatus.ON_DATA_UPDATE;
  }

  public LineOfInquiry(String id,
		  String name, 
		  String description, 
		  String dataSource,
		  String hypothesisQuery, 
		  String dataQuery, 
		  List<WorkflowBindings> workflows, 
		  List<WorkflowBindings> metaWorkflows){
	  this.id = id;
	  this.name = name;
	  this.description = description;
	  this.dataSource = dataSource;
	  this.hypothesisQuery = hypothesisQuery;
	  this.dataQuery = dataQuery;
	  this.workflows = workflows;
	  this.metaWorkflows = metaWorkflows;
    this.active = UpdateStatus.ON_DATA_UPDATE;
  }

  public void setDataSource (String ds) {
	  this.dataSource = ds;
  }

  public String getDataSource () {
	  return this.dataSource;
  }

  public void setQuestionId (String q) {
    this.questionId = q;
  }
  
  public String getQuestionId () {
    return this.questionId;
  }

  public String getExplanation () {
	  return this.dataQueryExplanation;
  }
  
  public void setExplanation (String e) {
	  this.dataQueryExplanation = e;
  }
  

  public Set<String> getAllWorkflowVariables () {
    Set<String> allVars = new HashSet<String>();
    List<WorkflowBindings> wfs = new ArrayList<WorkflowBindings>(workflows);
    wfs.addAll(metaWorkflows);
    for (WorkflowBindings wb: wfs) {
      List<String> vars = wb.getAllVariables();
      for (String v: vars) {
        allVars.add(v);
      }
    }
    return allVars;
  }

  public Set<String> getAllWorkflowNonCollectionVariables () {
    Set<String> allVars = new HashSet<String>();
    List<WorkflowBindings> wfs = new ArrayList<WorkflowBindings>(workflows);
    wfs.addAll(metaWorkflows);
    for (WorkflowBindings wb: wfs) {
      List<String> vars = wb.getNonCollectionVariables();
      for (String v: vars) {
        allVars.add(v);
      }
    }
    return allVars;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public void setDescription(String description) {
    this.description = description;
  }  

  public String getHypothesisQuery() {
    return hypothesisQuery;
  }

  public void setHypothesisQuery(String query) {
    this.hypothesisQuery = query;
  }

  public String getDataQuery() {
    return dataQuery;
  }

  public void setDataQuery(String dataQuery) {
    this.dataQuery = dataQuery;
  }
  
  public List<WorkflowBindings> getWorkflows() {
    return workflows;
  }

  public void setWorkflows(List<WorkflowBindings> workflows) {
    this.workflows = workflows;
  }
  
  public void addWorkflow(WorkflowBindings workflowId) {
    this.workflows.add(workflowId);
  }
  
  public List<WorkflowBindings> getMetaWorkflows() {
    return metaWorkflows;
  }

  public void setMetaWorkflows(List<WorkflowBindings> metaWorkflows) {
    this.metaWorkflows = metaWorkflows;
  }
  
  public void addMetaWorkflow(WorkflowBindings metaWorkflowid) {
    this.metaWorkflows.add(metaWorkflowid);
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
	  return this.dateModified;
  }
  
  public String getTableVariables () {
	  return this.tableVariables;
  }
  
  public void setTableVariables (String v) {
	  this.tableVariables = v;
  }
  
  public String getTableDescription () {
	  return this.tableDescription;
  }
  
  public void setTableDescription (String v) {
	  this.tableDescription = v;
  }

  public String getDataQueryExplanation () {
	  return this.dataQueryExplanation;
  }
  
  public void setDataQueryExplanation (String v) {
	  this.dataQueryExplanation = v;
  }

  public void setActive (UpdateStatus b) {
    this.active = b;
  }

  public UpdateStatus getActive () {
    return this.active;
  }
}
