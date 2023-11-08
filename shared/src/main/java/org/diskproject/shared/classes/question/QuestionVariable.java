package org.diskproject.shared.classes.question;

import java.util.List;

import org.diskproject.shared.classes.common.Triple;

public class QuestionVariable {
  public static enum QuestionSubtype {
    NONE, USER_INPUT, DYNAMIC_OPTIONS, STATIC_OPTIONS, BOUNDING_BOX, TIME_INTERVAL,
  };

  String id, variableName;
  Double minCardinality, maxCardinality;
  boolean conjunction; // Uses multiple values as AND when is set to true; otherwise uses OR. (When cardinality > 1);
  String representation, explanation;
  List<Triple> patternFragment;
  QuestionSubtype subType;

  public QuestionVariable () {
  }

  public QuestionVariable (String id, String varName) {
	  this.id = id;
	  this.variableName = varName;
    this.subType = QuestionSubtype.NONE;
    this.conjunction = false;
  }

  public String toString () {
	  return "Question Variable: " + this.variableName + " (" + this.id + ")";
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getVariableName() {
    return variableName;
  }

  public void setVariableName(String name) {
    this.variableName = name;
  }

  public void setSubType (QuestionSubtype type) {
    this.subType = type;
  }

  public QuestionSubtype getSubType () {
    return this.subType;
  }

  public void setRepresentation (String representation) {
    this.representation = representation;
  }

  public String getRepresentation () {
    return this.representation;
  }

  public void setExplanation (String explanation) {
    this.explanation = explanation;
  }

  public String getExplanation () {
    return this.explanation;
  }

  public void setMinCardinality (double value) {
    this.minCardinality = value;
  }

  public double getMinCardinality () {
    return this.minCardinality;
  }

  public void setMaxCardinality (double value) {
    this.maxCardinality = value;
  }

  public double getMaxCardinality () {
    return this.maxCardinality;
  }

  public void setPatternFragment (List<Triple> value) {
    this.patternFragment = value;
  }

  public List<Triple> getPatternFragment () {
    return this.patternFragment;
  }

  public void setConjunction (boolean b) {
    this.conjunction = b;
  }

  public boolean getConjunction () {
    return this.conjunction;
  }
}