package org.diskproject.shared.classes.workflow;

import java.net.MalformedURLException;
import java.net.URL;

public class Workflow {
  String id, name, link, source;

  /**
   * Workflows define the set of computational tasks and dependencies needed to carry out in silico experiments
   * @param id a unique identified, it must be a url
   * @param name a name for the workflow
   * @param link a public link to the workflow
   * @param source the source of the workflow
   * @throws MalformedURLException
   */
  public Workflow (String id, String name, String link, String source) throws MalformedURLException {
    new URL(id);
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

  public String getSource () {
    return source;
  }

  public void setSource (String source) {
    this.source = source;
  }
}
