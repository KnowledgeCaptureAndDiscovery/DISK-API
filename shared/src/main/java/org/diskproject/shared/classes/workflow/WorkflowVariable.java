package org.diskproject.shared.classes.workflow;

import java.util.List;

public class WorkflowVariable {
	  String name;
	  List<String> type;
	  int dimensionality;
	  boolean param, input, output;

	  public WorkflowVariable(){}
	  
	  public WorkflowVariable(String name, List<String> type, int dimensionality, boolean param, boolean input){
		  this.name = name;
		  this.type = type;
		  this.dimensionality = dimensionality;
		  this.param = param;
		  this.input = input;
		  this.output = false;
	  }
	  
	  public String toString(){
		  return name+": [type: "+type+", dimensionality: "+dimensionality+", param: "+param+", input: "+input+"]";
	  }

	  public void setOutput (boolean b) {
		this.param = false;
		this.output = b;
		this.input = !b;
	  }

	  public String getName() {
	    return name;
	  }

	  public void setName(String name) {
	    this.name = name;
	  }

	  public List<String> getType() {
	    return type;
	  }

	  public void setType(List<String> type) {
	    this.type = type;
	  }

	  public int getDimensionality() {
	    return dimensionality;
	  }

	  public void setDimensionality(int dimensionality) {
	    this.dimensionality = dimensionality;
	  }

	  public boolean isParam() {
	    return param;
	  }

	  public void setParam(boolean param) {
	    this.param = param;
	  }

	  public boolean isInput() {
	    return input;
	  }

	  public void setInput(boolean input) {
	    this.input = input;
	  }

	  public boolean isOutput() {
	    return output;
	  }
	}