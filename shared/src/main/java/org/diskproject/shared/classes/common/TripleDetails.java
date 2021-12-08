package org.diskproject.shared.classes.common;

public class TripleDetails {
  double confidenceValue;
  String triggeredLOI;
  
  public TripleDetails(double confidenceValue, String triggeredLOI)
  {
	  this.confidenceValue = confidenceValue;
	  this.triggeredLOI = triggeredLOI;
  }
  
  public TripleDetails(){}
  
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

}
