package edu.isi.kcap.diskproject.shared.classes.method;

public class MethodBinding {
    private MethodParameter parameter;
    private String value;

    public MethodBinding(MethodParameter parameter, String value) {
        this.parameter = parameter;
        this.value = value;
    }

    public void setParameter(MethodParameter parameter) {
        this.parameter = parameter;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public MethodParameter getParameter() {
        return this.parameter;
    }

    public String getValue() {
        return this.value;
    }
}
