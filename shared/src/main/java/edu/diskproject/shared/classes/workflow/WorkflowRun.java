package edu.diskproject.shared.classes.workflow;

import java.util.HashMap;
import java.util.Map;

public class WorkflowRun {
  String id;
  String link;
  String status;

  Map<String, String> outputs;
  Map<String, String> files;
  String startDate, endDate;

  public WorkflowRun() {
  }

  public WorkflowRun(String id, String link, String status) {
    this.id = id;
    this.link = link;
    this.status = status;
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

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public void setStartDate(String date) {
    this.startDate = date;
  }

  public String getStartDate() {
    return this.startDate;
  }

  public void setEndDate(String date) {
    this.endDate = date;
  }

  public String getEndDate() {
    return this.endDate;
  }

  public void setOutputs(Map<String, String> outputs) {
    this.outputs = outputs;
  }

  public Map<String, String> getOutputs() {
    return outputs;
  }

  public void addOutput(String name, String link) {
    if (outputs == null)
      outputs = new HashMap<String, String>();
    outputs.put(name, link);
  }

  public void addFile(String name, String link) {
    if (files == null)
      files = new HashMap<String, String>();
    files.put(name, link);
  }

  public Map<String, String> getFiles() {
    return files;
  }

  public void setFiles(Map<String, String> files) {
    if (files != null)
      this.files = files;
  }
}
