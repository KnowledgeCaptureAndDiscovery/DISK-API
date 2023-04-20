package edu.diskproject.shared.classes.vocabulary;

public class Property extends Entity {
  String domain;
  String range;

  public String getDomain() {
    return domain;
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }

  public String getRange() {
    return range;
  }

  public void setRange(String range) {
    this.range = range;
  }

}
