package edu.isi.kcap.diskproject.shared.classes.workflow;

public class Workflow {
  String id, name, link, source;

  public Workflow(String id, String name, String link, String source) {
    this.id = id;
    this.name = name;
    this.link = link;
    this.source = source;
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

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }
}
