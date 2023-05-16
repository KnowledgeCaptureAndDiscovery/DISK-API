package org.diskproject.shared.classes.question;

public class TimeIntervalQuestionVariable extends DynamicOptionsQuestionVariable {
  QuestionVariable startTime, endTime, timeType;

  public TimeIntervalQuestionVariable(String id, String varName) {
    super(id, varName);
    this.subType = QuestionSubtype.TIME_INTERVAL;
  }

  public void setTimeType (QuestionVariable type) {
    this.timeType = type;
  }

  public QuestionVariable getTimeType () {
    return this.timeType;
  }

  public void setStartTime (QuestionVariable time) {
    this.startTime = time;
  }

  public QuestionVariable getStartTime () {
    return this.startTime;
  }

  public void setEndTime (QuestionVariable time) {
    this.endTime = time;
  }

  public QuestionVariable getEndTime () {
    return this.endTime;
  }
}