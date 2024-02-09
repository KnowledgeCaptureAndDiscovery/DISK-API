package org.diskproject.shared.classes.workflow;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class VariableBinding implements Serializable, Comparable<VariableBinding> {
  private static final long serialVersionUID = -847994634505985728L;

  public static enum BindingTypes {
    DEFAULT, FREEFORM, DATA_QUERY, QUERY_VARIABLE, WORKFLOW_VARIABLE, DISK_DATA, // Inputs
    DROP, SAVE, PROCESS // Outputs
  }
  
  String variable;        // Variable NAME
  List<String> binding;   // Binding VALUE
  boolean isArray;        // This binding is an array, if false bindings list will only have one value.
  BindingTypes type;      // This is for the type of selector used to set this binding.
  String datatype;        // Binding datatype. This one should be an xsd value. Does not require to be defined on the ontology TODO
  //String filetype;        // When datatype=anyURI and bindings is some url file. //FIXME: is this necessary? datatype could store this value

  public VariableBinding(){}
   
  public VariableBinding (String v, String b) {
	  variable = v;
    binding = new ArrayList<String>();
    binding.add(b);
    isArray = false;
  }

  public VariableBinding (String v, String b, String t) {
	  variable = v;
    binding = new ArrayList<String>();
    binding.add(b);
    datatype = t;
    isArray = false;
  }

  public VariableBinding (String v, List<String> b) {
	  variable = v;
    binding = b;
    isArray = true;
  }

  public VariableBinding (String v, List<String> b, String t) {
	  variable = v;
    binding = b;
    datatype = t;
    isArray = true;
  }
  
  public String getVariable() {
    return variable;
  }

  public void setVariable(String variable) {
    this.variable = variable;
  }

  public boolean isArray() {
    return isArray;
  }

  public void setIsArray (boolean b) {
    this.isArray = b;
  }

  public String getBinding () {
    return isArray || binding.size() == 0 ? null : binding.get(0);
  }

  public void setBinding (String binding) {
    if (!isArray) {
      this.binding = new ArrayList<String>();
      this.binding.add(binding);
    }
  }

  public List<String> getBindings () {
    return isArray ? this.binding : null;
  }

  public void setBindings (List<String> bindings) {
    if (isArray) {
      this.binding = bindings;
    }
  }
  
  public String getDatatype () {
    return datatype;
  }

  public void setDatatype (String t) {
    this.datatype = t;
  }

  /*public String getFiletype () {
    return filetype;
  }

  public void setFiletype (String t) {
    this.filetype = t;
  }*/

  public BindingTypes getType () {
    return type;
  }

  public void setType (BindingTypes t) {
    this.type = t;
  }

  public void setType (String t) {
    //TODO: check how the types are stored on the RDF
    this.type = null;
  }

  public String toString () {
    return variable+" = "+binding;
  }

  public int compareTo (VariableBinding o) {
    return this.toString().compareTo(o.toString());
  }
}
