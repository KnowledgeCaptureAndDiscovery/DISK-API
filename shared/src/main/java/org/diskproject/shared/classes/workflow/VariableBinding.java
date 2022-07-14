package org.diskproject.shared.classes.workflow;


import java.io.Serializable;

public class VariableBinding implements Serializable, Comparable<VariableBinding> {
  private static final long serialVersionUID = -847994634505985728L;
  
  String variable;
  String binding;
   
  public VariableBinding(String v, String b) {
	  variable = v;
	  binding = b;
  }
  
  public VariableBinding(){}
  
  public String getVariable() {
    return variable;
  }

  public void setVariable(String variable) {
    this.variable = variable;
  }

  public String getBinding() {
    return binding;
  }
  
  public String[] getBindingAsArray () {
	String b = getBinding().replace("]", "").replace("[", "").replaceAll(" ", "");
    return b.split(",");
  }
  
  public boolean isCollection () {
    return (binding.charAt(0) == '[' && binding.charAt(binding.length()-1) == ']');
  }

  public void setBinding(String binding) {
    this.binding = binding;
  }

  public String toString() {
    return variable+" = "+binding;
  }

  public int compareTo(VariableBinding o) {
    return this.toString().compareTo(o.toString());
  }
}
