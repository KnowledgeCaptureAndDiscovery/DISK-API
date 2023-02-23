package org.diskproject.shared.classes.hypothesis;

import java.util.List;

import org.diskproject.shared.classes.common.Graph;
import org.diskproject.shared.classes.workflow.VariableBinding;

public class Hypothesis {
  String id;
  String name;
  String description;
  String dateCreated;
  String dateModified;
  String author;
  String notes;
  String questionId;
  List<VariableBinding> questionBindings;
  Graph graph; // The actual hypothesis.
  //For revised hypotheses
  String parentId;
  
  public void setQuestionId (String q) {
    this.questionId = q;
  }
  
  public String getQuestionId () {
    return this.questionId;
  }

  public Hypothesis (String id, String name, String description, String parentId, Graph graph){
	  this.id = id;
	  this.name = name;
	  this.description = description;
	  this.parentId = parentId;
	  this.graph = graph;
  }

  public Hypothesis(){}
  
  public List<VariableBinding> getQuestionBindings () {
	  return this.questionBindings;
  }
  
  public void setQuestionBindings (List<VariableBinding> bindings) {
	  this.questionBindings = bindings;
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

  public String getParentId() {
    return parentId;
  }

  public void setParentId(String parentId) {
    this.parentId = parentId;
  }

  public Graph getGraph() {
    return graph;
  }

  public void setGraph(Graph graph) {
    this.graph = graph;
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

  public void setDateModified (String date) {
	  this.dateModified = date;
  }

  public String getDateModified () {
	  return dateModified;
  }
}
