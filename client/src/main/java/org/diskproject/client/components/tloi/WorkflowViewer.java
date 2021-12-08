package org.diskproject.client.components.tloi;

import java.util.List;

import org.diskproject.shared.classes.loi.WorkflowBindings;
import org.diskproject.shared.classes.workflow.VariableBinding;
import org.diskproject.shared.classes.workflow.WorkflowRun;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.polymer.iron.widget.IronIcon;

public class WorkflowViewer extends Composite {
    interface Binder extends UiBinder<Widget, WorkflowViewer> {};
    private static Binder uiBinder = GWT.create(Binder.class);
   
    @UiField Label workflowName, workflowDate;
    @UiField IronIcon statusIcon;
    @UiField HTMLPanel parametersPanel, outputsPanel;
    @UiField HTMLPanel parameterList;
   
    WorkflowBindings bindings;
  
    public WorkflowViewer (WorkflowBindings bindings) {
      initWidget(uiBinder.createAndBindUi(this));  
      this.bindings = bindings;
      initialize();
    }
    
    private void initialize () {
        String name = bindings.getWorkflow();
        WorkflowRun run = bindings.getRun();
        if (name != null) workflowName.setText(name);
        if (run != null) {
            String status = run.getStatus();
            if(status != null) {
                if (status.equals("SUCCESS")) {
                    statusIcon.setIcon("icons:check");
                } else if(status.equals("FAILURE")) {
                    statusIcon.setIcon("icons:clear");
                } else {
                    statusIcon.setIcon("icons:hourglass-empty");
                }
                statusIcon.addStyleName(status);
            } else {
                statusIcon.setIcon("icons:hourglass-empty");
            }
            
            String startDate = run.getStartDate();
            String endDate = run.getEndDate();
            if (startDate != null) {
                String datetext = "The run started at " + startDate;
                if (endDate != null) datetext += " and ended at " + endDate;
                workflowDate.setText(datetext);
                workflowDate.setVisible(true);
            } else {
                workflowDate.setVisible(false);
            }
        } else {
            GWT.log("Run == null");
            statusIcon.setIcon("icons:hourglass-empty");
        }
        
        fillAttributeValueList(bindings.getParameters(), parameterList);

        // check if theres some variable binding collection.
        int maxlen = 0;
        for (VariableBinding vb: bindings.getBindings()) {
            if (vb.isCollection()) {
                int curlen = vb.getBindingAsArray().length;
                maxlen = maxlen < curlen ? curlen : maxlen;
            }
        }
    }
    
    private void fillAttributeValueList (List<VariableBinding> vb, HTMLPanel panel) {
        if (vb != null && vb.size() > 0) {
            panel.clear();
            for (VariableBinding cur: vb) {
                Label varname = new Label(cur.getVariable() + " =");
                Label value = new Label(cur.getBinding());
                varname.addStyleName("bold");
                varname.addStyleName("inline");
                value.addStyleName("inline");
                FlowPanel cont = new FlowPanel();
                cont.add(varname);
                cont.add(value);
                parameterList.add(cont);
            }
            panel.setVisible(true);
        } else {
            panel.setVisible(false);
        }
    }
  
}
