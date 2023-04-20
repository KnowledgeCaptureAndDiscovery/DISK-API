package edu.diskproject.shared.classes.loi;

public class MetaWorkflowDetails {
  String hypothesis;
  String revisedHypothesis;

  public MetaWorkflowDetails() {
  }

  public MetaWorkflowDetails(String hypothesis, String revisedHypothesis) {
    this.hypothesis = hypothesis;
    this.revisedHypothesis = revisedHypothesis;
  }

  public String getHypothesis() {
    return hypothesis;
  }

  public void setHypothesis(String hypothesis) {
    this.hypothesis = hypothesis;
  }

  public String getRevisedHypothesis() {
    return revisedHypothesis;
  }

  public void setRevisedHypothesis(String revisedHypothesis) {
    this.revisedHypothesis = revisedHypothesis;
  }

}
