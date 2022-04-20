package org.diskproject.shared.classes.loi;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LineOfInquiry {
  String id;
  String name;
  String description;
  String hypothesisQuery;
  String dataQuery;
  List<WorkflowBindings> workflows;
  List<WorkflowBindings> metaWorkflows;
  String notes;
  String author;
  String dateCreated;
  String dateModified;
  String relevantVariables; //To show on the table.
  String explanation; //To describe the table
  String dataSource;
  String question;

  public LineOfInquiry() {
    this.workflows = new ArrayList<WorkflowBindings>();
    this.metaWorkflows = new ArrayList<WorkflowBindings>();
  }

  public LineOfInquiry(String id, String name, String description){
	  this.id = id;
	  this.name = name;
	  this.description = description;
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
  }
  
  public void setDataSource (String ds) {
	  this.dataSource = ds;
  }
  
  public String getDataSource () {
	  return this.dataSource;
  }

  public void setQuestion (String q) {
    this.question = q;
  }
  
  public String getQuestion () {
    return this.question;
  }

  public String getExplanation () {
	  return this.explanation;
  }
  
  public void setExplanation (String e) {
	  this.explanation = e;
  }
  

  public Set<String> getAllWorkflowVariables () {
    Set<String> allVars = new HashSet<String>();
    List<WorkflowBindings> wfs = new ArrayList<WorkflowBindings>(workflows);
    wfs.addAll(metaWorkflows);
    for (WorkflowBindings wb: wfs) {
      List<String> vars = wb.getSparqlVariables();
      for (String v: vars) {
        allVars.add(v);
      }
    }
    return allVars;
  }

  public Set<String> getAllWorkflowParameters () {
    Set<String> allVars = new HashSet<String>();
    List<WorkflowBindings> wfs = new ArrayList<WorkflowBindings>(workflows);
    wfs.addAll(metaWorkflows);
    for (WorkflowBindings wb: wfs) {
      List<String> vars = wb.getSparqlParameters();
      for (String v: vars) {
        allVars.add(v);
      }
    }
    return allVars;
  }

  public Set<String> getAllWorkflowCollectionVariables () {
    Set<String> allVars = new HashSet<String>();
    List<WorkflowBindings> wfs = new ArrayList<WorkflowBindings>(workflows);
    wfs.addAll(metaWorkflows);
    for (WorkflowBindings wb: wfs) {
      List<String> vars = wb.getCollectionVariables();
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
  
  public void addWorkflow(WorkflowBindings workflowid) {
    this.workflows.add(workflowid);
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
  
  public String getRelevantVariables () {
	  return this.relevantVariables;
  }
  
  public void setRelevantVariables (String v) {
	  this.relevantVariables = v;
  }
}
