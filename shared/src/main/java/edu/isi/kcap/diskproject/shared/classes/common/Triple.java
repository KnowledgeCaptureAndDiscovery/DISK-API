package edu.isi.kcap.diskproject.shared.classes.common;

public class Triple {
  String subject;
  String predicate;
  Value object;
  TripleDetails details;

  public Triple(String subject, String predicate, Value object, TripleDetails details) {
    this.subject = subject;
    this.predicate = predicate;
    this.object = object;
    this.details = details;
  }

  public Triple() {
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String getPredicate() {
    return predicate;
  }

  public void setPredicate(String predicate) {
    this.predicate = predicate;
  }

  public Value getObject() {
    return object;
  }

  public void setObject(Value object) {
    this.object = object;
  }

  public TripleDetails getDetails() {
    return details;
  }

  public void setDetails(TripleDetails details) {
    this.details = details;
  }

  public String toString() {
    return "<" + this.subject + "> <" + this.predicate + "> " + this.object;
  }
}
