package org.diskproject.client.components.loi;

import java.util.ArrayList;
import java.util.List;

import org.diskproject.client.components.list.ListNode;
import org.diskproject.client.components.list.ListWidget;
import org.diskproject.client.components.list.events.ListItemActionEvent;
import org.diskproject.client.components.list.events.ListItemSelectionEvent;
import org.diskproject.shared.classes.loi.MetaWorkflowDetails;
import org.diskproject.shared.classes.loi.WorkflowBindings;
import org.diskproject.shared.classes.workflow.VariableBinding;
import org.diskproject.shared.classes.workflow.Workflow;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.LabelElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.polymer.iron.widget.event.IronOverlayClosedEvent;
import com.vaadin.polymer.paper.widget.PaperDialog;
import com.vaadin.polymer.paper.widget.PaperIconButton;

public class LOIWorkflowList extends Composite {
  interface Binder extends UiBinder<Widget, LOIWorkflowList> {};
  private static Binder uiBinder = GWT.create(Binder.class);

  ListNode update;
  
  @UiField WorkflowBindingsEditor bindingseditor;
  @UiField PaperDialog workflowdialog;
  @UiField ListWidget workflowlist;
  @UiField PaperIconButton addwflowbutton;
  @UiField LabelElement label;
  
  LOIWorkflowList source;
  
  public @UiConstructor LOIWorkflowList(boolean metamode, String label) {
    initWidget(uiBinder.createAndBindUi(this)); 
    
    workflowlist.addCustomAction("link", null, "icons:link", "blue-button");
    workflowlist.addDeleteAction();
    
    bindingseditor.setMetamode(metamode);
    this.label.setInnerText(label);
  }

  public void setWorkflowList(List<Workflow> list) {
    bindingseditor.setWorkflowList(list);
  }
  
  public void updateSourceWorkflows() {
    if(this.source != null) {
      List<String> metalist = new ArrayList<String>();
      for(WorkflowBindings bindings : source.getBindingsList()) {
        metalist.add(bindings.getWorkflow());
      }
      bindingseditor.setSourceWorkflows(metalist);        
    }
  }
  
  public void setWorkflowSource(LOIWorkflowList source) {
    this.source = source;
  }
  
  public List<WorkflowBindings> getBindingsList() {
    List<WorkflowBindings> bindings = new ArrayList<WorkflowBindings>();
    for(ListNode node : workflowlist.getNodes()) {
      bindings.add((WorkflowBindings)node.getData());
    }
    return bindings;
  }
  
  public void loadBindingsList(List<WorkflowBindings> bindingslist) {
    workflowlist.clear();
    if(bindingslist == null)
      return;
    for(WorkflowBindings bindings: bindingslist) {
      this.addWorkflowBindingsToList(bindings, null);
    }
  }
  
  @UiHandler("workflowlist")
  void onWorkflowListItemSelected(ListItemSelectionEvent event) {
    WorkflowBindings bindings = (WorkflowBindings)event.getItem().getData();
    update = event.getItem();    
    this.updateSourceWorkflows();
    bindingseditor.loadWorkflowBindings(bindings);
    workflowdialog.open();
  }
  
  @UiHandler("workflowlist")
  void onWorkflowListItemAction(ListItemActionEvent event) {
    ListNode node = event.getItem();
    if(event.getAction().getId().equals("delete")) {
      if(workflowlist.getNode(node.getId()) != null)
        workflowlist.removeNode(node);
    }
    else if(event.getAction().getId().equals("link")) {
      String link = ((WorkflowBindings)node.getData()).getWorkflowLink();
      Window.open(link, "_blank", "");
    }
  }
  
  @UiHandler("addwflowbutton")
  void onAddWorkflowButtonClicked(ClickEvent event) {
    update = null;
    this.updateSourceWorkflows();
    bindingseditor.loadWorkflowBindings(null);
    workflowdialog.open();
  }
  
  @UiHandler("workflowdialog")
  void onDialogClose(IronOverlayClosedEvent event) {
    if(!isConfirmed(event.getPolymerEvent().getDetail()))
      return;

    WorkflowBindings bindings = bindingseditor.getWorkflowBindings();
    if(bindings == null)
      return;
    
    this.addWorkflowBindingsToList(bindings, update);
  }
  
  private void addWorkflowBindingsToList(WorkflowBindings bindings, 
      ListNode tnode) {
    String id = bindings.getWorkflow();
    //String html = bindings.getHTML();
    String html = createWorkflowBindingsBox(bindings);

    if(tnode == null) {
      tnode = new ListNode(id, new HTML(html));
      tnode.setIcon("dashboard");
      tnode.setIconStyle("green");
      tnode.setData(bindings);
      workflowlist.addNode(tnode);      
    }
    else {
      String oldid = tnode.getId();
      tnode.setId(id);
      tnode.getContent().setHTML(html);
      tnode.setData(bindings);
      workflowlist.updateNode(oldid, tnode);
    }    
  }
  
  private String createWorkflowBindingsBox (WorkflowBindings bindings) {
      String id = bindings.getWorkflow();
      String html = "<div class='name'>"+ id +"</div>";
      html += "<div class='description workflow-description'>";

      List<VariableBinding> parameters = bindings.getParameters();
      if (parameters != null && parameters.size() > 0) {
          html += "<span><b>Parameters:</b></span><span><ul style=\"margin: 0\">";
          for (VariableBinding param: parameters)
              html += "<li><b>" + param.getVariable() + " = </b>" + param.getBinding() + "</li>" ;
          html += "</ul></span>";
      }
      
      List<VariableBinding> varBindings = bindings.getBindings();
      if (varBindings != null && varBindings.size() > 0) {
          html += "<span><b>Variable Bindings:</b></span><span><ul style=\"margin: 0\">";
          for (VariableBinding varB: varBindings)
              html += "<li><b>" + varB.getVariable() + " = </b>" + varB.getBinding() + "</li>" ;
          html += "</ul></span>";
      }
      
      MetaWorkflowDetails meta = bindings.getMeta();
      if (meta != null && (meta.getHypothesis() != null || meta.getRevisedHypothesis() != null)) {
          html += "<span></span> <span><ul style=\"margin: 0\">";
          if (meta.getHypothesis() != null) {
              html += "<li><b>" + meta.getHypothesis() + "</b>: [Hypothesis]</li>";
          }
          if (meta.getRevisedHypothesis() != null) {
              html += "<li><b>" + meta.getRevisedHypothesis() + "</b>: [Revised Hypothesis]</li>";
          }
          html += "</ul></span>";
      }
      
      html += "</div>";
      return html;
  }
  
  private native boolean isConfirmed(Object obj) /*-{
    return obj.confirmed;
  }-*/;  

}
