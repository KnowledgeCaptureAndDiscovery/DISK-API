package org.diskproject.shared.classes.common;

public class TripleDetails {
  double confidenceValue;
  String triggeredLOI;
  String confidenceType;

  public TripleDetails(double confidenceValue, String triggeredLOI, String confidenceType) {
    this.confidenceValue = confidenceValue;
    this.triggeredLOI = triggeredLOI;
    this.confidenceType = confidenceType;
  }

  public TripleDetails() {
  }

  public double getConfidenceValue() {
    return confidenceValue;
  }

  public void setConfidenceValue(double confidenceValue) {
    this.confidenceValue = confidenceValue;
  }

  public String getTriggeredLOI() {
    return triggeredLOI;
  }

  public void setTriggeredLOI(String triggeredLOI) {
    this.triggeredLOI = triggeredLOI;
  }

  public void setConfidenceType(String value) {
    this.confidenceType = value;
  }

  public String getConfidenceType() {
    return confidenceType;
  }

}
