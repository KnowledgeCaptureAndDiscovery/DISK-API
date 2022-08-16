package org.diskproject.shared.classes.question;

public class QuestionVariable {
  String id;
  String varname;
  String constraints;
  String[] fixedOptions;
  Integer minCardinality;
  Integer maxCardinality;

  public QuestionVariable () {
  }

  public QuestionVariable (String id, String varname, String constraints) {
	  this.id = id;
	  this.varname = varname;
	  this.constraints = constraints;
    this.maxCardinality = 1;
    this.minCardinality = 1;
  }

  public String toString () {
	  String text = "- " + this.varname + " (" + this.id + ")";
	  if (this.constraints != null) text = text + " [" + this.constraints + "]";
	  return text;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getVarName() {
    return varname;
  }

  public void setVarName(String name) {
    this.varname = name;
  }

  public String getConstraints() {
    return constraints;
  }

  public void setConstraints(String constraints) {
	this.constraints = constraints;
  }

  public String[] getFixedOptions() {
    return this.fixedOptions;
  }

  public void setFixedOptions(String[] options) {
    this.fixedOptions = options;
  }

  public Integer getMinCardinality() {
    return minCardinality;
  }

  public void setMinCardinality(Integer minCardinality) {
    this.minCardinality = minCardinality;
  }

  public Integer getMaxCardinality() {
    return maxCardinality;
  }

  public void setMaxCardinality(Integer maxCardinality) {
    this.maxCardinality = maxCardinality;
  }
}
