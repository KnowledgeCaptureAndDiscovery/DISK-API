package org.diskproject.shared.classes.hypothesis;

import org.diskproject.shared.classes.workflow.VariableBinding;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class GoalResult {
    String confidenceType;
    Double confidenceValue;
    List<VariableBinding> extras;

    public GoalResult() {
        extras = new ArrayList<VariableBinding>();
    }

    public GoalResult (String type, Double value) {
        this.confidenceType = type;
        this.confidenceValue = value;
    }

    public String getConfidenceType() {
        return confidenceType;
    }

    public void setConfidenceType(String confidenceType) {
        this.confidenceType = confidenceType;
    }

    public Double getConfidenceValue() {
        return confidenceValue;
    }

    public void setConfidenceValue(Double confidenceValue) {
        this.confidenceValue = confidenceValue;
    }

    public List<VariableBinding> getExtras() {
        return extras;
    }

    public void setExtras(List<VariableBinding> extras) {
        this.extras = extras;
    }

    @JsonIgnore
    public void addValue(VariableBinding newBinding) {
        if (extras == null)
            extras = new ArrayList<VariableBinding>();
        this.extras.add(newBinding);
    }

    @Override
    public String toString () {
        String txt = confidenceType + " = " + confidenceValue;
        if (extras != null) for (VariableBinding b: extras)
            txt += "\n " + b.getVariable() + ": " + b.getBinding();
        return txt;
    }
}
