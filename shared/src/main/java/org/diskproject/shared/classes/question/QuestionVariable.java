package org.diskproject.shared.classes.question;

public class QuestionVariable {
  String id;
  String varname;
  String constraints; //FIXME: change name to something more intuive
  String[] fixedOptions; //FIXME: MAP {[id, name], [id, name]}
  Number cardinalityMax, cardinalityMin;

  public QuestionVariable () {
  }

  public QuestionVariable (String id, String varname, String constraints) {
	  this.id = id;
	  this.varname = varname;
	  this.constraints = constraints;
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
}
