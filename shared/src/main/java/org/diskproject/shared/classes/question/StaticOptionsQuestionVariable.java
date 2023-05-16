package org.diskproject.shared.classes.question;

import java.util.List;

public class StaticOptionsQuestionVariable extends QuestionVariable {
  List<VariableOption> options;

  public StaticOptionsQuestionVariable(String id, String varName) {
    super(id, varName);
    this.subType = QuestionSubtype.STATIC_OPTIONS;
  }

  public List<VariableOption> getOptions() {
    return this.options;
  }

  public void setOptions(List<VariableOption> options) {
    this.options = options;
  }
}