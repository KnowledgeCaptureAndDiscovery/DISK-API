package org.diskproject.shared.classes.workflow;

import java.util.List;
import java.util.Map;

import org.diskproject.shared.classes.common.Status;
import org.diskproject.shared.classes.common.Value.Type;

public class WorkflowRun {
  public static class RuntimeInfo {
    public Status status;
    public String log;
    public int startTime, endTime;

    public String toString() {
      String txt = "Status: " + status.toString();
      if (startTime > 0) {
        txt += " (" + startTime;
        if (endTime > 0)
          txt += " - " + endTime;
        txt += ")";
      }
      if (log != null && !log.equals(""))
        txt += "\n LOG : " + log;
      return txt;
    }
  }

  public static class RunBinding { // This is equivalent to Variable Binding
    public String id, datatype, value;
    public Type type;
  }

  String id, link;
  List<RuntimeInfo> steps;
  RuntimeInfo execution;
  Map<String, RunBinding> input, output;

  public WorkflowRun(){
    this.execution = new RuntimeInfo();
  }

  public void setAsPending () {
    if (this.execution == null)
      this.execution = new RuntimeInfo();
    this.execution.status = Status.PENDING;
  }

  public String toString () {
    String txt = "[WorkflowRun: " + this.id;
    if (link != null) txt += "\n Link=" + link;
    if (execution != null) {
      txt += "\n " + execution.toString();
    }

    if (input != null && input.size() > 0) {
      txt += "\n Inputs (" + input.size() + ") =";
      for (String k: input.keySet()) {
        RunBinding b = input.get(k);
        if (b.type == Type.LITERAL) {
          txt += "\n    - " + k + " [L]: "  + b.value + " " + b.datatype;
        } else {
          txt += "\n    - " + k + " [U]:"  + b.id;
        }
      }
    }

    if (output != null && output.size() > 0) {
      txt += "\n Outputs (" + output.size() + ") =";
      for (String k: output.keySet()) {
        RunBinding b = output.get(k);
        if (b.type == Type.LITERAL) {
          txt += "\n    - " + k + " [L]: "  + b.value + " " + b.datatype;
        } else {
          txt += "\n    - " + k + " [U]:"  + b.id;
        }
      }
    }

    if (steps != null && steps.size() > 0) {
      txt += "\n Steps (" + output.size() + ") =";
      for (RuntimeInfo r: steps) {
        txt += "\n   " + r.toString();
      }
    }
    txt += "]";
    return txt;
  }
  
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getLink() {
    return link;
  }

  public void setLink(String link) {
    this.link = link;
  }

  public void setExecutionInfo (RuntimeInfo info) {
    this.execution = info;
  }

  public RuntimeInfo getExecutionInfo () {
    return this.execution;
  }

  public void setStepsInfo (List<RuntimeInfo> steps) {
    this.steps = steps;
  }

  public List<RuntimeInfo> getStepsInfo () {
    return this.steps;
  }

  public void setInputs (Map<String, RunBinding> input) {
    this.input = input;
  }

  public Map<String, RunBinding> getInputs () {
    return this.input;
  }

  public void setOutputs (Map<String, RunBinding> output) {
    this.output = output;
  }

  public Map<String, RunBinding> getOutputs () {
    return this.output;
  }
}
