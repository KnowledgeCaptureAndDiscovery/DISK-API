package org.diskproject.client.components.loi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.diskproject.client.rest.AppNotification;
import org.diskproject.client.rest.DiskREST;
import org.diskproject.shared.classes.loi.WorkflowBindings;
import org.diskproject.shared.classes.workflow.Variable;
import org.diskproject.shared.classes.workflow.VariableBinding;
import org.diskproject.shared.classes.workflow.Workflow;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.polymer.Polymer;
import com.vaadin.polymer.PolymerWidget;
import com.vaadin.polymer.paper.widget.PaperIconButton;
import com.vaadin.polymer.paper.widget.PaperInput;
import com.vaadin.polymer.vaadin.widget.VaadinComboBox;
import com.vaadin.polymer.vaadin.widget.event.ValueChangedEvent;

public class WorkflowBindingsEditor extends Composite {
  interface Binder extends UiBinder<Widget, WorkflowBindingsEditor> {};
  private static Binder uiBinder = GWT.create(Binder.class);
 
  @UiField HTMLPanel varsection, workflowstoberunsection, metasection, varbindings, workflowstoberunbindings, paramssection, parambindings, optionalbindings;
  @UiField VaadinComboBox workflowmenu, hypothesismenu, revhypothesismenu;
  @UiField PaperIconButton addbindingbutton, addworkflowstoberunbutton, addparambutton, addoptionalbutton;

  boolean metamode;
  List<String> sourceworkflows;
  
  Map<String, Workflow> workflowcache;
  Map<String, Variable> variablecache;
  Map<String, List<Variable>> workflowVariablesCache = new HashMap<String, List<Variable>>();
  
  WorkflowBindings bindings;

  public WorkflowBindingsEditor() {
    initWidget(uiBinder.createAndBindUi(this));  
  }

  public void setMetamode(boolean metamode) {
    this.metamode = metamode;
  }

  public void setSourceWorkflows(List<String> workflows) {
    this.sourceworkflows = workflows;
    Collections.sort(sourceworkflows);
  }

  public void setWorkflowList(List<Workflow> list) {
    workflowcache = new HashMap<String, Workflow>();
    for(Workflow w : list)
      workflowcache.put(w.getName(), w);
    List<String> names = new ArrayList<String>();
    for(String wflowName : workflowcache.keySet()) {
      names.add(wflowName);
    }
    Collections.sort(names);
    workflowmenu.setItems(Polymer.asJsArray(names));
    clearVariableBindingsUI();
  }

  public void loadWorkflowBindings(WorkflowBindings bindings) {
    this.bindings = bindings;
    workflowmenu.setValue(null);  
    if(bindings != null) {
      workflowmenu.setValue(bindings.getWorkflow());
    }
  }

  private void clearVariableBindingsUI() {
    varbindings.clear();
    parambindings.clear();
    optionalbindings.clear();
    workflowstoberunbindings.clear();
    metasection.setVisible(false);
    workflowstoberunsection.setVisible(false);
    //this.setMetaSection(null);
    addbindingbutton.setVisible(false);
    addparambutton.setVisible(false);
    addoptionalbutton.setVisible(false);
    addworkflowstoberunbutton.setVisible(false);
  }

  private void setBindingsUI() {
    if(metamode) {
      metasection.setVisible(true);
      workflowstoberunsection.setVisible(true);
      addworkflowstoberunbutton.setVisible(true);
      this.setMetaSection(bindings);
    }
    
    if(bindings == null)
      return;
    
    for(VariableBinding vbinding : bindings.getBindings()) {
      if( metamode && sourceworkflows.contains(vbinding.getBinding()) ) {
        this.addVariableBinding(vbinding.getVariable(), vbinding.getBinding(), true);
        continue;
      }
      this.addVariableBinding(vbinding.getVariable(), vbinding.getBinding(), false);
    }
    
    for (VariableBinding param: bindings.getParameters()) {
      this.addParameter(param.getVariable(), param.getBinding());
    }

    for (VariableBinding optional: bindings.getOptionalParameters()) {
      this.addOptionalParameter(optional.getVariable(), optional.getBinding());
    }
  }

  private void setMetaSection(WorkflowBindings bindings) {
    List<String> names = new ArrayList<String>();
    List<String> outnames = new ArrayList<String>();
    for(String varname : variablecache.keySet()) {
      if(variablecache.get(varname).isInput())
        names.add(varname);
      else
        outnames.add(varname);
    }
    hypothesismenu.setItems(Polymer.asJsArray(names));
    revhypothesismenu.setItems(Polymer.asJsArray(outnames));
    if(bindings != null) {
      hypothesismenu.setValue(bindings.getMeta().getHypothesis());
      revhypothesismenu.setValue(bindings.getMeta().getRevisedHypothesis());
    }
    else {
      hypothesismenu.setValue(null);
      revhypothesismenu.setValue(null);
    }
  }

  private void addVariableBinding(String varid, String binding, boolean previousWorkflowFlag) {
    final HTMLPanel el = new HTMLPanel("");
    el.setStyleName("varbindings-row");
    
    VaadinComboBox varmenu = new VaadinComboBox();
    varmenu.setLabel("Workflow Variable");
    varmenu.addStyleName("no-label varbindings-cell");
    List<String> names = new ArrayList<String>();
    for(String varname : variablecache.keySet()) {
      if(variablecache.get(varname).isInput())
        names.add(varname);
    }
    varmenu.setItems(Polymer.asJsArray(names));
    if(varid != null)
      varmenu.setValue(varid);
    
    PolymerWidget bindwidget = null;
    if(previousWorkflowFlag) {
      VaadinComboBox bindinput = new VaadinComboBox();
      
      bindinput.setItems(Polymer.asJsArray(sourceworkflows));
      bindinput.setLabel("Previous workflow run");
      bindinput.addStyleName("no-label varbindings-cell");
      if(binding != null)
        bindinput.setValue(binding);
      
      bindwidget = bindinput;
    }
    else {
      PaperInput bindinput = new PaperInput();
      bindinput.setLabel("Binding");
      bindinput.setNoLabelFloat(true);
      bindinput.addStyleName("no-label varbindings-cell");
      if(binding != null)
        bindinput.setValue(binding);
      bindwidget = bindinput;
    }
    
    PaperIconButton delbutton = new PaperIconButton();
    delbutton.setStyleName("smallicon red-button");
    delbutton.setIcon("cancel");
    
    el.add(varmenu);
    el.add(bindwidget);
    el.add(delbutton);

    if(previousWorkflowFlag)
      workflowstoberunbindings.add(el);
    else
      varbindings.add(el);
    
    delbutton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        el.removeFromParent();
      }
    });
  }

  private void addParameter(String varid, String binding) {
    final HTMLPanel el = new HTMLPanel("");
    el.setStyleName("varbindings-row");
    
    VaadinComboBox varmenu = new VaadinComboBox();
    varmenu.setLabel("Workflow Variable");
    varmenu.addStyleName("no-label varbindings-cell");
    List<String> names = new ArrayList<String>();
    for(String varname : variablecache.keySet()) {
      if(variablecache.get(varname).isInput())
        names.add(varname);
    }
    varmenu.setItems(Polymer.asJsArray(names));
    if(varid != null)
      varmenu.setValue(varid);
    
    PaperInput bindinput = new PaperInput();
    bindinput.setLabel("Binding");
    bindinput.setNoLabelFloat(true);
    bindinput.addStyleName("no-label varbindings-cell");
    if(binding != null)
      bindinput.setValue(binding);
    
    PaperIconButton delbutton = new PaperIconButton();
    delbutton.setStyleName("smallicon red-button fixtop");
    delbutton.setIcon("cancel");
    
    el.add(varmenu);
    el.add(bindinput);
    el.add(delbutton);

    parambindings.add(el);
    
    delbutton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        el.removeFromParent();
      }
    });
  }

  private void addOptionalParameter(String varid, String binding) {
    final HTMLPanel el = new HTMLPanel("");
    el.setStyleName("varbindings-row");
    
    VaadinComboBox varmenu = new VaadinComboBox();
    varmenu.setLabel("Workflow Variable");
    varmenu.addStyleName("no-label varbindings-cell");
    List<String> names = new ArrayList<String>();
    for(String varname : variablecache.keySet()) {
      if(variablecache.get(varname).isInput())
        names.add(varname);
    }
    varmenu.setItems(Polymer.asJsArray(names));
    if(varid != null)
      varmenu.setValue(varid);
    
    PaperInput bindinput = new PaperInput();
    bindinput.setLabel("Binding");
    bindinput.setNoLabelFloat(true);
    bindinput.addStyleName("no-label varbindings-cell");
    if(binding != null)
      bindinput.setValue(binding);
    
    PaperIconButton delbutton = new PaperIconButton();
    delbutton.setStyleName("smallicon red-button fixtop");
    delbutton.setIcon("cancel");
    
    el.add(varmenu);
    el.add(bindinput);
    el.add(delbutton);

    optionalbindings.add(el);
    
    delbutton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        el.removeFromParent();
      }
    });
  }

  @UiHandler("addparambutton")
  void onAddParamClicked(ClickEvent event) {
    this.addParameter(null, null);
  }

  @UiHandler("addoptionalbutton")
  void onAddOptionalClicked(ClickEvent event) {
    this.addOptionalParameter(null, null);
  }
  
  @UiHandler("addbindingbutton")
  void onAddBindingClicked(ClickEvent event) {
    this.addVariableBinding(null, null, false);
  }

  @UiHandler("addworkflowstoberunbutton")
  void onAddPrevWorkflowsClicked(ClickEvent event) {
    this.addVariableBinding(null, null, true);
  }
  
  void showWorkflowVariablesMenu(String workflowid) {
    List<Variable> variables = workflowVariablesCache.get(workflowid);
    clearVariableBindingsUI();
    addbindingbutton.setVisible(true);
    addparambutton.setVisible(true);
    addoptionalbutton.setVisible(true);
    if(metamode) {
      metasection.setVisible(true);
      workflowstoberunsection.setVisible(true);
      addworkflowstoberunbutton.setVisible(true);
    }
    variablecache = new HashMap<String, Variable>();
    for(Variable v : variables)
      variablecache.put(v.getName(), v);
    setBindingsUI();
  }
  
  @UiHandler("workflowmenu")
  void onWorkflowMenuSelected(ValueChangedEvent event) {
    String workflowid = workflowmenu.getValue();
    clearVariableBindingsUI();
    if(workflowid == null || workflowid.equals("")) {
      return;
    }
    
    // Use Cache here to fetch workflow variables only once
    if(workflowVariablesCache.containsKey(workflowid)) {
      this.showWorkflowVariablesMenu(workflowid);
    }
    else {
      DiskREST.getWorkflowVariables(workflowid, 
          new Callback<List<Variable>, Throwable>() {
        @Override
        public void onFailure(Throwable reason) {
          AppNotification.notifyFailure(reason.getMessage());
        }
        @Override
        public void onSuccess(List<Variable> result) {
          workflowVariablesCache.put(workflowid, result);
          showWorkflowVariablesMenu(workflowid);
        }
      });
    }
  }
  
  @UiHandler("workflowlink")
  void onWorkflowLinkClicked(ClickEvent event) {
    if(workflowmenu.getValue() != null) {
      Workflow workflow = workflowcache.get(workflowmenu.getValue());
      if(workflow != null && workflow.getLink() != null)
        Window.open(workflow.getLink(), "_blank", "");
    }
  }
  
  public WorkflowBindings getWorkflowBindings() {
    WorkflowBindings bindings = new WorkflowBindings();
    Workflow workflow = workflowcache.get(workflowmenu.getValue());
    bindings.setWorkflow(workflow.getName());
    bindings.setWorkflowLink(workflow.getLink());
    
    List<VariableBinding> vbindings = new ArrayList<VariableBinding>();
    for(int i=0; i<varbindings.getWidgetCount(); i++) {
      HTMLPanel row = (HTMLPanel) varbindings.getWidget(i);
      VaadinComboBox cb = (VaadinComboBox) row.getWidget(0);
      String varname = cb.getValue();
      
      VariableBinding vbinding = new VariableBinding();
      vbinding.setVariable(varname);
      PaperInput in = (PaperInput) row.getWidget(1);
      vbinding.setBinding(in.getValue());

      vbindings.add(vbinding);
    }
    
    List<VariableBinding> parameters = new ArrayList<VariableBinding>();
    for (int i = 0; i < parambindings.getWidgetCount(); i++) {
      HTMLPanel row = (HTMLPanel) parambindings.getWidget(i);
      VaadinComboBox cb = (VaadinComboBox) row.getWidget(0);
      String varname = cb.getValue();

      PaperInput in = (PaperInput) row.getWidget(1);
      
      VariableBinding vbinding = new VariableBinding(varname, in.getValue());

      parameters.add(vbinding);
    }

    List<VariableBinding> optionalParameters = new ArrayList<VariableBinding>();
    for (int i = 0; i < optionalbindings.getWidgetCount(); i++) {
      HTMLPanel row = (HTMLPanel) optionalbindings.getWidget(i);
      VaadinComboBox cb = (VaadinComboBox) row.getWidget(0);
      String varname = cb.getValue();

      PaperInput in = (PaperInput) row.getWidget(1);
      
      VariableBinding vbinding = new VariableBinding(varname, in.getValue());

      optionalParameters.add(vbinding);
    }

    if( metamode ) {
      for(int i=0; i<workflowstoberunbindings.getWidgetCount(); i++) {
        HTMLPanel row = (HTMLPanel) workflowstoberunbindings.getWidget(i);
        VaadinComboBox cb = (VaadinComboBox) row.getWidget(0);
        String varname = cb.getValue();
        
        VariableBinding vbinding = new VariableBinding();
        vbinding.setVariable(varname);
        VaadinComboBox vb = (VaadinComboBox) row.getWidget(1);
        vbinding.setBinding(vb.getValue());

        vbindings.add(vbinding);
      }
    }

    bindings.setBindings(vbindings);
    bindings.setParameters(parameters);
    bindings.setOptionalParameters(optionalParameters);
    
    if(metamode) {
      bindings.getMeta().setHypothesis(hypothesismenu.getValue());
      bindings.getMeta().setRevisedHypothesis(revhypothesismenu.getValue());
    }
    
    return bindings;
  }  
}
