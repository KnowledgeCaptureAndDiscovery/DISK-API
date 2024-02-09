package org.diskproject.shared.classes.hypothesis;

import java.util.List;

import org.diskproject.shared.classes.DISKResource;
import org.diskproject.shared.classes.common.Graph;
import org.diskproject.shared.classes.workflow.VariableBinding;

public class Hypothesis extends DISKResource {
  // The graph (for server) and question bindings (for UI)
  String questionId;
  List<VariableBinding> questionBindings;
  Graph graph; // The actual hypothesis.

  public Hypothesis (String id, String name, String description, String parentId, Graph graph){
    super(id,name,description);
	  this.graph = graph;
  }

  public Hypothesis(){}
  
  public void setQuestionId (String q) {
    this.questionId = q;
  }
  
  public String getQuestionId () {
    return this.questionId;
  }

  public void setQuestionBindings (List<VariableBinding> bindings) {
	  this.questionBindings = bindings;
  }
  
  public List<VariableBinding> getQuestionBindings () {
	  return this.questionBindings;
  }
  
  public void setGraph(Graph graph) {
    this.graph = graph;
  }

  public Graph getGraph() {
    return graph;
  }
}
