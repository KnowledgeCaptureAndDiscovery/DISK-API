package edu.isi.kcap.diskproject.shared.classes.workflow;

import java.io.Serializable;

public class VariableBinding implements Serializable, Comparable<VariableBinding> {
  private static final long serialVersionUID = -847994634505985728L;

  String variable;
  String binding;
  String type;

  public VariableBinding(String v, String b) {
    variable = v;
    binding = b;
  }

  public VariableBinding(String v, String b, String t) {
    variable = v;
    binding = b;
    type = t;
  }

  public VariableBinding() {
  }

  public String getVariable() {
    return variable;
  }

  public void setVariable(String variable) {
    this.variable = variable;
  }

  public String getBinding() {
    return binding;
  }

  public void setBinding(String binding) {
    this.binding = binding;
  }

  public String getType() {
    return type;
  }

  public void setType(String t) {
    this.type = t;
  }

  public String[] getBindingAsArray() {
    String b = getBinding().replace("]", "").replace("[", "").replaceAll(" ", "");
    return b.split(",");
  }

  public boolean isCollection() {
    return (binding.charAt(0) == '[' && binding.charAt(binding.length() - 1) == ']');
  }

  public String toString() {
    return variable + " = " + binding;
  }

  public int compareTo(VariableBinding o) {
    return this.toString().compareTo(o.toString());
  }
}
