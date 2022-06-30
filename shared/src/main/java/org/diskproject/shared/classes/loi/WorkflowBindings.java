package org.diskproject.shared.classes.loi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.diskproject.shared.classes.workflow.VariableBinding;
import org.diskproject.shared.classes.workflow.WorkflowRun;

public class WorkflowBindings implements Comparable<WorkflowBindings>{
  String source;          // This is the ID of the source
  String workflow;
  String workflowLink;
  List<VariableBinding> bindings, parameters, optionalParameters;
  WorkflowRun run;
  MetaWorkflowDetails meta;

  public WorkflowBindings() {
    bindings = new ArrayList<VariableBinding>();
    parameters = new ArrayList<VariableBinding>();
    optionalParameters = new ArrayList<VariableBinding>();
    run = new WorkflowRun();
    meta = new MetaWorkflowDetails();
  }

  public WorkflowBindings(String workflow, String workflowLink) {
    this.workflow = workflow;
    this.workflowLink = workflowLink;
    bindings = new ArrayList<VariableBinding>();
    parameters = new ArrayList<VariableBinding>();
    optionalParameters = new ArrayList<VariableBinding>();
    run = new WorkflowRun();
    meta = new MetaWorkflowDetails();
  }

  public WorkflowBindings(String workflow, String workflowLink,
          List<VariableBinding> bindings,
          List<VariableBinding> parameters,
          List<VariableBinding> optionalParameters) {
    this.workflow = workflow;
    this.workflowLink = workflowLink;
    this.bindings = bindings;
    this.parameters = parameters;
    this.optionalParameters = optionalParameters;
    run = new WorkflowRun();
    meta = new MetaWorkflowDetails();
  }

  public String getSource () {
    return this.source;
  }

  public void setSource (String source) {
    this.source = source;
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

  public WorkflowRun getRun() {
    return run;
  }

  public void setRun(WorkflowRun run) {
    this.run = run;
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
  
  public List<VariableBinding> getParameters () {
	  return parameters;
  }
  
  public void setParameters (List<VariableBinding> params) {
	  this.parameters = params;
  }
  
  public void addParameter (VariableBinding param) {
    this.parameters.add(param);
  }
  
  public List<VariableBinding> getOptionalParameters () {
	  return optionalParameters;
  }
  
  public void setOptionalParameters (List<VariableBinding> optionalParameters) {
	this.optionalParameters = optionalParameters;
  }
  
  public void addOptionalParameter (VariableBinding binding) {
	  this.optionalParameters.add(binding);
  }
  
  public List<String> getSparqlVariables () {
    List<String> vars = new ArrayList<String>();
    for (VariableBinding b: bindings) {
      for (String v: b.getBindingAsArray()) {
        vars.add(v);
      }
    }
    return vars;
  }

  public List<String> getSparqlParameters () {
    List<String> vars = new ArrayList<String>();
    for (VariableBinding b: parameters) {
      for (String v: b.getBindingAsArray()) {
        vars.add(v);
      }
    }
    return vars;
  }

  public List<String> getCollectionVariables () {
    List<String> vars = new ArrayList<String>();
    for (VariableBinding b: bindings) {
      if (b.isCollection()) {
        for (String v: b.getBindingAsArray()) {
          vars.add(v);
        }
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
    String description = "{";
    for (VariableBinding vBinding : bindings) {
      if (i > 0) description += ", ";
      description += vBinding.getVariable() + " = " + vBinding.getBinding();
      i++;
    }
    for (VariableBinding vBinding : parameters) {
      if (i > 0) description += ", ";
      description += vBinding.getVariable() + " = " + vBinding.getBinding();
      i++;
    }
    for (VariableBinding vBinding : optionalParameters) {
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
 