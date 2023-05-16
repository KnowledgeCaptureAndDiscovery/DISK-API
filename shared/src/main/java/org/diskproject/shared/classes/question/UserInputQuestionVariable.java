package org.diskproject.shared.classes.question;

public class UserInputQuestionVariable extends QuestionVariable {
  String inputDatatype;

  public UserInputQuestionVariable(String id, String varName) {
    super(id, varName);
    this.subType = QuestionSubtype.USER_INPUT;
  }

  public void setInputDatatype (String type) {
    this.inputDatatype = type;
  }

  public String getInputDatatype () {
    return this.inputDatatype;
  }
}