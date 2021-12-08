package org.diskproject.client.components.tloi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.diskproject.shared.classes.loi.WorkflowBindings;
import org.diskproject.shared.classes.workflow.VariableBinding;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.dom.client.SpanElement;

public class BindingsEditor extends Composite {
  interface Binder extends UiBinder<Widget, BindingsEditor> {};
  private static Binder uiBinder = GWT.create(Binder.class);
 
  @UiField HTMLPanel varsection;
  @UiField SpanElement  workflowName;

  List<CheckBox> orderedOptions;

  Map<String, HTMLPanel> variablePanels;
  
  WorkflowBindings bindings;

  public BindingsEditor() {
    initWidget(uiBinder.createAndBindUi(this));  
    variablePanels = new HashMap<String, HTMLPanel>();
  }

  public void setWorkflowBindings(WorkflowBindings bindings) {
    this.bindings = bindings;
    clearUI();
    if(bindings != null) {
        updateTable();
        workflowName.setInnerText(bindings.getWorkflow());
    }
  }

  private void updateTable () {
    if (bindings != null) {
      List<VariableBinding> allVarBindings = bindings.getBindings();
      int nCols = allVarBindings.size(), nRows = 0, index = -1;

      for (int i = 0; i < nCols; i++) {
        VariableBinding var = allVarBindings.get(i);
        if (var.isCollection()) {
          String[] values = var.getBindingAsArray();
          if (values.length > nRows) {
              nRows = values.length;
              index = i;
          }
        }
      }
      
      if (index < 0) return; //TODO: show some error.

      orderedOptions = new ArrayList<CheckBox>();
      
      //Only works when the number of elements for collection is the same.
      //First order collections first to have some id. //FIXME: use more than one value to generate ID
      List<VariableBinding> ordered = new ArrayList<VariableBinding>();
      ordered.add(allVarBindings.get(index));
      for (int i = 0; i < nCols; i++) {
        if (i != index) ordered.add(allVarBindings.get(i));
      }

      FlexTable table = new FlexTable();
      //Add headers
      for (int i = 0; i < nCols; i++) {
          table.setHTML(0, i+1, "<b>" + ordered.get(i).getVariable() + "</b>");
      }
      
      for (int i = 0; i < nRows; i++) {
          for (int j=0; j < nCols; j++) {
              VariableBinding vb = ordered.get(j);
              String value;
              if (vb.isCollection()) {
                  value = vb.getBindingAsArray()[i];
              } else {
                  value = vb.getBinding();
              }
              table.setHTML(i + 1, j + 1, value.replaceAll("^SHA[a-zA-Z0-9]{6}_", ""));
              if (j == 0) {
                  CheckBox item = new CheckBox();
                  item.setValue(true);
                  table.setWidget(i+1, j, item);
                  orderedOptions.add(item);
              }
          }
      }
      
      varsection.add(table);
    }
  }

  private void clearUI () {
    varsection.clear();
    variablePanels = new HashMap<String, HTMLPanel>();
  }

  public WorkflowBindings getWorkflowBindings() {
    List<Integer> indexes = new ArrayList<>();
    for (int i = orderedOptions.size() - 1; i >= 0; i--) {
        CheckBox cur = orderedOptions.get(i);
        if (!cur.getValue())
            indexes.add(i);
    }
      
      
    if (indexes.size() == 0) return bindings;
    
    List<VariableBinding> newVarBindings = new ArrayList<VariableBinding>();
    for (VariableBinding origBinding: bindings.getBindings()) {
    	String varname = origBinding.getVariable();
    	if (origBinding.isCollection()) {
    	    String[] values = origBinding.getBindingAsArray();
    	    int len = values.length;
    		String newVal = "[";
    		boolean first = true;
    		for (int i = 0; i < len; i++) {
    			if (!indexes.contains(i)) {
    				if (!first) newVal += ", ";
    				newVal += values[i];
    				first = false;
    			}
    		}
    		newVal += "]";
    		GWT.log(newVal);
    		newVarBindings.add(new VariableBinding(varname, newVal));
    	} else {
    		newVarBindings.add(origBinding);
    	}
    }
    bindings.setBindings(newVarBindings);
    
    return bindings;
  }  
}
