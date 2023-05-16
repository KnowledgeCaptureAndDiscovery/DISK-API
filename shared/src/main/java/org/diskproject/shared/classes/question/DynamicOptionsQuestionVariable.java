package org.diskproject.shared.classes.question;

public class DynamicOptionsQuestionVariable extends QuestionVariable {
  String optionsQuery;

  public DynamicOptionsQuestionVariable(String id, String varName) {
    super(id, varName);
    this.subType = QuestionSubtype.DYNAMIC_OPTIONS;
  }

  public String getOptionsQuery() {
    return optionsQuery;
  }

  public void setOptionsQuery(String constraints) {
	this.optionsQuery = constraints;
  }
}