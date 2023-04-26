package edu.isi.kcap.diskproject.shared.classes.question;

import java.util.List;

public class QuestionVariable {
  String id;
  String variableName;
  List<VariableOption> options;
  String optionsQuery;
  Double minCardinality, maxCardinality;
  String representation;
  String explanation, explanationQuery;
  // Possible subtypes are BoundingBox, TimeInterval and UserInput
  String subType;
  // For UserInputs:
  String inputDatatype;
  // For BoundingBox:
  QuestionVariable minLat, minLng, maxLat, maxLng;

  public QuestionVariable() {
  }

  public QuestionVariable(String id, String varName) {
    this.id = id;
    this.variableName = varName;
  }

  public QuestionVariable(String id, String varName, String optionsQuery) {
    this.id = id;
    this.variableName = varName;
    this.optionsQuery = optionsQuery;
  }

  public QuestionVariable(String id, String varname, List<VariableOption> options) {
    this.id = id;
    this.variableName = varname;
    this.options = options;
  }

  public String toString() {
    String text = "- " + this.variableName + " (" + this.id + ")";
    if (this.optionsQuery != null)
      text = text + " [" + this.optionsQuery + "]";
    return text;
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

  public String getOptionsQuery() {
    return optionsQuery;
  }

  public void setOptionsQuery(String constraints) {
    this.optionsQuery = constraints;
  }

  public List<VariableOption> getOptions() {
    return this.options;
  }

  public void setOptions(List<VariableOption> options) {
    this.options = options;
  }

  public void setSubType(String type) {
    this.subType = type;
  }

  public String getSubType() {
    return this.subType;
  }

  public void setInputDatatype(String type) {
    this.inputDatatype = type;
  }

  public String getInputDatatype() {
    return this.inputDatatype;
  }

  public void setBoundingBoxVariables(QuestionVariable minLat, QuestionVariable maxLat, QuestionVariable minLng,
      QuestionVariable maxLng) {
    this.minLat = minLat;
    this.maxLat = maxLat;
    this.minLng = minLng;
    this.maxLng = maxLng;
  }

  public void setMinLat(QuestionVariable minLatVar) {
    this.minLat = minLatVar;
  }

  public QuestionVariable getMinLat() {
    return this.minLat;
  }

  public void setMinLng(QuestionVariable minLngVar) {
    this.minLng = minLngVar;
  }

  public QuestionVariable getMinLng() {
    return this.minLng;
  }

  public void setMaxLat(QuestionVariable maxLatVar) {
    this.maxLat = maxLatVar;
  }

  public QuestionVariable getMaxLat() {
    return this.maxLat;
  }

  public void setMaxLng(QuestionVariable maxLngVar) {
    this.maxLng = maxLngVar;
  }

  public QuestionVariable getMaxLng() {
    return this.maxLng;
  }

  public void setRepresentation(String representation) {
    this.representation = representation;
  }

  public String getRepresentation() {
    return this.representation;
  }

  public void setExplanation(String explanation) {
    this.explanation = explanation;
  }

  public String getExplanation() {
    return this.explanation;
  }

  public void setExplanationQuery(String exQuery) {
    this.explanationQuery = exQuery;
  }

  public String getExplanationQuery() {
    return this.explanationQuery;
  }

  public void setMinCardinality(double value) {
    this.minCardinality = value;
  }

  public double getMinCardinality() {
    return this.minCardinality;
  }

  public void setMaxCardinality(double value) {
    this.maxCardinality = value;
  }

  public double getMaxCardinality() {
    return this.maxCardinality;
  }
}