package org.diskproject.shared.classes.loi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.diskproject.shared.classes.workflow.VariableBinding;
import org.diskproject.shared.classes.workflow.WorkflowRun;

public class WorkflowBindings implements Comparable<WorkflowBindings>{
  String source;          // This is the ID of the method source
  String workflow;
  String workflowLink;
  String description;
  List<VariableBinding> bindings;
  MetaWorkflowDetails meta;
  Map<String, WorkflowRun> runs;

  public WorkflowBindings() {
    bindings = new ArrayList<VariableBinding>();
    runs = new HashMap<String, WorkflowRun>();
    meta = new MetaWorkflowDetails();
  }

  public WorkflowBindings(String workflow, String workflowLink) {
    this.workflow = workflow;
    this.workflowLink = workflowLink;
    bindings = new ArrayList<VariableBinding>();
    runs = new HashMap<String, WorkflowRun>();
    meta = new MetaWorkflowDetails();
  }

  public WorkflowBindings(String workflow, String workflowLink, List<VariableBinding> bindings) {
    this.workflow = workflow;
    this.workflowLink = workflowLink;
    this.bindings = bindings;
    runs = new HashMap<String, WorkflowRun>();
    meta = new MetaWorkflowDetails();
  }

  public String getSource () {
    return this.source;
  }

  public void setSource (String source) {
    this.source = source;
  }

  public String getDescription () {
    return this.description;
  }

  public void setDescription (String description) {
    this.description = description;
  }

  public String getWorkflow() {
    return workflow;
  }

  public void setWorkflow(String workflow) {
    this.workflow = workflow;
  }

  public String getWorkflowLink() {
    return workflowLink;
  }

  public void setWorkflowLink(String workflowLink) {
    this.workflowLink = workflowLink;
  }

  public Map<String, WorkflowRun> getRuns() {
    return runs;
  }

  public void setRuns(Map<String, WorkflowRun> runs) {
    this.runs = runs;
  }

  public void addRun (WorkflowRun run) {
    if (this.runs == null)
      this.runs = new HashMap<String, WorkflowRun>();
    if (this.runs.containsKey(run.getId()))
      this.runs.remove(run.getId());
    this.runs.put(run.getId(), run);
  }

  public WorkflowRun getRun (String runId) {
    return this.runs.get(runId);
  }

  public List<VariableBinding> getBindings() {
    return bindings;
  }

  public void setBindings(List<VariableBinding> bindings) {
    this.bindings = bindings;
  }
  
  public void addBinding(VariableBinding binding) {
    this.bindings.add(binding);
  }
  
  public List<String> getAllVariables () {
    List<String> vars = new ArrayList<String>();
    for (VariableBinding b: bindings) {
      for (String v: b.getBindingAsArray()) {
        vars.add(v);
      }
    }
    return vars;
  }

  public List<String> getNonCollectionVariables () {
    List<String> vars = new ArrayList<String>();
    for (VariableBinding b: bindings) {
      if (!b.isCollection()) {
        for (String v: b.getBindingAsArray()) {
          vars.add(v);
        }
      }
    }
    return vars;
  }

  public List<String> getVariableBindings(String variable) {
    List<String> bindings = new ArrayList<String>();
    for(VariableBinding vb : this.bindings) {
      if(vb.getVariable().equals(variable))
        bindings.add(vb.getBinding());
    }
    return bindings;
  }
  
  public List<String> getBindingVariables(String binding) {
    List<String> variables = new ArrayList<String>();
    for(VariableBinding vb : this.bindings) {
      if(vb.getVariable().equals(binding))
        variables.add(vb.getVariable());
    }
    return variables;
  }

  public String toString() {
    Collections.sort(bindings);
    int i=0;
    String description = "["+source+"]{";
    for (VariableBinding vBinding : bindings) {
      if (i > 0) description += ", ";
      description += vBinding.getVariable() + " = " + vBinding.getBinding();
      i++;
    }
    if (this.meta.getHypothesis() != null) {
      if (i > 0) description += ", ";
      description += this.meta.getHypothesis() + " = [Hypothesis]";
      i++;
    }
    if (this.meta.getRevisedHypothesis() != null) {
      if (i > 0) description += ", ";
      description += this.meta.getRevisedHypothesis() + " = [Revised Hypothesis]";
      i++;
    }
    description += "}";
    return description;
  }
  
  public MetaWorkflowDetails getMeta() {
    return meta;
  }

  public void setMeta(MetaWorkflowDetails meta) {
    this.meta = meta;
  }

  public int compareTo(WorkflowBindings o) {
    return this.toString().compareTo(o.toString());
  }
}
 