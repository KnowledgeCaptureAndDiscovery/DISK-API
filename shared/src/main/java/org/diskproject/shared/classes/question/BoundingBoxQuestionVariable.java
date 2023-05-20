package org.diskproject.shared.classes.question;

public class BoundingBoxQuestionVariable extends DynamicOptionsQuestionVariable {
  UserInputQuestionVariable minLat, minLng, maxLat, maxLng;

  public BoundingBoxQuestionVariable(String id, String varName) {
    super(id, varName);
    this.subType = QuestionSubtype.BOUNDING_BOX;
  }

  public void setBoundingBoxVariables (UserInputQuestionVariable minLat, UserInputQuestionVariable maxLat, UserInputQuestionVariable minLng, UserInputQuestionVariable maxLng) {
    this.minLat = minLat;
    this.maxLat = maxLat;
    this.minLng = minLng;
    this.maxLng = maxLng;
  }

  public void setMinLat (UserInputQuestionVariable minLatVar) {
    this.minLat = minLatVar;
  }

  public UserInputQuestionVariable getMinLat () {
    return this.minLat;
  }

  public void setMinLng (UserInputQuestionVariable minLngVar) {
    this.minLng = minLngVar;
  }

  public UserInputQuestionVariable getMinLng () {
    return this.minLng;
  }

  public void setMaxLat (UserInputQuestionVariable maxLatVar) {
    this.maxLat = maxLatVar;
  }

  public UserInputQuestionVariable getMaxLat () {
    return this.maxLat;
  }

  public void setMaxLng (UserInputQuestionVariable maxLngVar) {
    this.maxLng = maxLngVar;
  }

  public UserInputQuestionVariable getMaxLng () {
    return this.maxLng;
  }
}