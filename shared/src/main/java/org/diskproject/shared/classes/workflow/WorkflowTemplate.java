package org.diskproject.shared.classes.workflow;

public class WorkflowTemplate {
  String id, name, link, sourceName;

  public WorkflowTemplate (String id, String name, String link, String source) {
    this.id = id;
    this.name = name;
    this.link = link;
    this.sourceName = source;
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

  public String getLink() {
    return link;
  }

  public void setLink(String link) {
    this.link = link;
  }

  public String getSourceName() {
    return sourceName;
  }

  public void setSourceName(String sourceName) {
    this.sourceName = sourceName;
  }
}