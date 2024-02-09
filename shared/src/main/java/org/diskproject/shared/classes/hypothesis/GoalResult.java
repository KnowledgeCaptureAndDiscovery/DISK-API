package org.diskproject.shared.classes.hypothesis;

public class GoalResult {
    String confidenceType, confidenceValue;

    public GoalResult() {
    }

    public GoalResult (String type, String value) {
        this.confidenceType = type;
        this.confidenceValue = value;
    }

    public String getConfidenceType() {
        return confidenceType;
    }

    public void setConfidenceType(String confidenceType) {
        this.confidenceType = confidenceType;
    }

    public String getConfidenceValue() {
        return confidenceValue;
    }

    public void setConfidenceValue(String confidenceValue) {
        this.confidenceValue = confidenceValue;
    }
}
