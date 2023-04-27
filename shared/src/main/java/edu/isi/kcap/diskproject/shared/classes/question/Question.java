package edu.isi.kcap.diskproject.shared.classes.question;

import java.util.ArrayList;
import java.util.List;

public class Question {
  String id;
  String name;
  String template;
  String pattern;
  String constraint;
  QuestionCategory category;
  List<QuestionVariable> variables;

  public Question() {
  }

  public Question(String id, String name, String template, String pattern, List<QuestionVariable> vars) {
    this.id = id;
    this.name = name;
    this.template = template;
    this.pattern = pattern;
    if (vars != null) {
      this.variables = vars;
    } else {
      this.variables = new ArrayList<QuestionVariable>();
    }
  }

  public String toString() {
    String text = "NAME: " + this.name + "\tID: " + this.id + "\n" +
        "TEMPLATE: " + this.template + "\n" +
        "PATTERN: " + this.pattern + "\n";
    if (this.variables != null && this.variables.size() > 0) {
      for (QuestionVariable qv : this.variables) {
        text = text + " " + qv.toString() + "\n";
      }
    }
    return text;
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

  public String getTemplate() {
    return template;
  }

  public void setTemplate(String template) {
    this.template = template;
  }

  public String getPattern() {
    return pattern;
  }

  public void setPattern(String pattern) {
    this.pattern = pattern;
  }

  public List<QuestionVariable> getVariables() {
    return this.variables;
  }

  public void setVariables(List<QuestionVariable> vars) {
    this.variables = vars;
  }

  public void addVariable(QuestionVariable var) {
    this.variables.add(var);
  }

  public void setConstraint(String query) {
    this.constraint = query;
  }

  public String getConstraint() {
    return this.constraint;
  }

  public void setCategory(QuestionCategory cat) {
    this.category = cat;
  }

  public QuestionCategory getCategory() {
    return this.category;
  }
}