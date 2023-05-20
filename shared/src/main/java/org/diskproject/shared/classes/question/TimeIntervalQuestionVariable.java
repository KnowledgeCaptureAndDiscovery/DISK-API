package org.diskproject.shared.classes.question;

public class TimeIntervalQuestionVariable extends DynamicOptionsQuestionVariable {
  UserInputQuestionVariable startTime, endTime;
  StaticOptionsQuestionVariable timeType;

  public TimeIntervalQuestionVariable(String id, String varName) {
    super(id, varName);
    this.subType = QuestionSubtype.TIME_INTERVAL;
  }

  public void setTimeType (StaticOptionsQuestionVariable type) {
    this.timeType = type;
  }

  public StaticOptionsQuestionVariable getTimeType () {
    return this.timeType;
  }

  public void setStartTime (UserInputQuestionVariable time) {
    this.startTime = time;
  }

  public UserInputQuestionVariable getStartTime () {
    return this.startTime;
  }

  public void setEndTime (UserInputQuestionVariable time) {
    this.endTime = time;
  }

  public UserInputQuestionVariable getEndTime () {
    return this.endTime;
  }
}