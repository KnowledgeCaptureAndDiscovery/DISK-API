package org.diskproject.shared.classes.question;

public class VariableOption {
    String value, label, comment;

    public VariableOption (String value, String label) {
        this.value = value;
        this.label = label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public void setValue (String value) {
        this.value = value;
    }

    public String getValue () {
        return this.value;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getComment () {
        return this.comment;
    }
}
