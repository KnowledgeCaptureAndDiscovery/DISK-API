package org.diskproject.shared.classes.common;

public class Triple {
  String subject;
  String predicate;
  Value object;

  public Triple(String subject, String predicate, Value object)
  {
	  this.subject = subject;
	  this.predicate = predicate;
	  this.object = object;
  }
  
  public Triple(){}
  
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
  
  public String toString() {
    return "<"+this.subject+"> <"+this.predicate+"> "+this.object;
  }
}
