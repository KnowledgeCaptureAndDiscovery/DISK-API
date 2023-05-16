package org.diskproject.shared.classes.question;

public class BoundingBoxQuestionVariable extends DynamicOptionsQuestionVariable {
  QuestionVariable minLat, minLng, maxLat, maxLng;

  public BoundingBoxQuestionVariable(String id, String varName) {
    super(id, varName);
    this.subType = QuestionSubtype.BOUNDING_BOX;
  }

  public void setBoundingBoxVariables (QuestionVariable minLat, QuestionVariable maxLat, QuestionVariable minLng, QuestionVariable maxLng) {
    this.minLat = minLat;
    this.maxLat = maxLat;
    this.minLng = minLng;
    this.maxLng = maxLng;
  }

  public void setMinLat (QuestionVariable minLatVar) {
    this.minLat = minLatVar;
  }

  public QuestionVariable getMinLat () {
    return this.minLat;
  }

  public void setMinLng (QuestionVariable minLngVar) {
    this.minLng = minLngVar;
  }

  public QuestionVariable getMinLng () {
    return this.minLng;
  }

  public void setMaxLat (QuestionVariable maxLatVar) {
    this.maxLat = maxLatVar;
  }

  public QuestionVariable getMaxLat () {
    return this.maxLat;
  }

  public void setMaxLng (QuestionVariable maxLngVar) {
    this.maxLng = maxLngVar;
  }

  public QuestionVariable getMaxLng () {
    return this.maxLng;
  }
}