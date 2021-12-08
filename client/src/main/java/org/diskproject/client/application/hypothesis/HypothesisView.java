package org.diskproject.client.application.hypothesis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.diskproject.client.Config;
import org.diskproject.client.Utils;
import org.diskproject.client.application.ApplicationSubviewImpl;
import org.diskproject.client.components.hypothesis.HypothesisEditor;
import org.diskproject.client.components.hypothesis.HypothesisItem;
import org.diskproject.client.components.hypothesis.events.HypothesisSaveEvent;
import org.diskproject.client.components.loader.Loader;
import org.diskproject.client.components.searchpanel.SearchPanel;
import org.diskproject.client.components.tloi.TriggeredLOIViewer;
import org.diskproject.client.components.tree.TreeNode;
import org.diskproject.client.components.tree.TreeWidget;
import org.diskproject.client.components.tree.events.TreeItemActionEvent;
import org.diskproject.client.components.tree.events.TreeItemSelectionEvent;
import org.diskproject.client.place.NameTokens;
import org.diskproject.client.rest.AppNotification;
import org.diskproject.client.rest.DiskREST;
import org.diskproject.shared.classes.common.Graph;
import org.diskproject.shared.classes.common.TreeItem;
import org.diskproject.shared.classes.hypothesis.Hypothesis;
import org.diskproject.shared.classes.loi.TriggeredLOI;
import org.diskproject.shared.classes.loi.WorkflowBindings;
import org.diskproject.shared.classes.util.GUID;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.vaadin.polymer.Polymer;
import com.vaadin.polymer.elemental.Function;
import com.vaadin.polymer.iron.widget.IronIcon;
import com.vaadin.polymer.paper.widget.PaperButton;
import com.vaadin.polymer.paper.widget.PaperFab;

public class HypothesisView extends ApplicationSubviewImpl 
  implements HypothesisPresenter.MyView {

  String userid;
  String domain;
  boolean editmode;
  boolean addmode;
  
  @UiField TreeWidget tree;
  @UiField Loader loader;
  @UiField HypothesisEditor form;
  @UiField PaperFab addicon;
  @UiField HTMLPanel matchlist;
  @UiField HTMLPanel description;
  @UiField HTMLPanel retryDiv, emptyDiv;
  @UiField AnchorElement retryLink, addLink;
  @UiField DivElement notloi;
  
  @UiField DialogBox helpDialog;
  @UiField SearchPanel searchPanel;
  
  ListBox varList;
  Map<String, List<CheckBox>> checkMap;
  WorkflowBindings selectedWorkflow;
  
  List<TreeItem> hypothesisList;
  List<TriggeredLOI> tloilist; 
  List<TriggeredLOI> matches; 

  interface Binder extends UiBinder<Widget, HypothesisView> {
  }

  @Inject
  public HypothesisView(Binder binder) {
    initWidget(binder.createAndBindUi(this));
    tree.addCustomAction("query", null, "icons:explore", 
        "green-button query-action");
    tree.addDeleteAction();

    HypothesisView me = this;
    Event.sinkEvents(retryLink, Event.ONCLICK);
    Event.setEventListener(retryLink, new EventListener() {
      @Override
      public void onBrowserEvent(Event event) {
        me.showHypothesisList();
      }
    });
    Event.sinkEvents(addLink, Event.ONCLICK);
    Event.setEventListener(addLink, new EventListener() {
        @Override
        public void onBrowserEvent(Event event) {
            me.onAddIconClicked(null);
        }
    });
  }

  @Override
  public void initializeParameters(String userid, String domain, 
      String[] params, boolean edit, 
      SimplePanel sidebar, SimplePanel toolbar) {
    
    clear();
    
    if(this.userid != userid || this.domain != domain) {
      this.userid = userid;
      this.domain = domain;
      DiskREST.setDomain(domain);
      DiskREST.setUsername(userid);
      form.initialize(userid, domain);
      HypothesisItem.setUsenameAndDomain(userid, domain);
    }
    
    this.setHeader(toolbar);    
    this.setSidebar(sidebar);
    
    if(params.length == 0) {
      this.showHypothesisList();
    }
    else if(params.length == 1) {
      this.showHypothesis(params[0]);
    }
    else if(params.length == 2 && params[1].equals("query")) {
      this.showHypothesisMatches(params[0]);
    }
  }

  private void clear() {
	notloi.removeAttribute("visible");
	retryDiv.setVisible(false);
	emptyDiv.setVisible(false);
    loader.setVisible(false);
    form.setVisible(false);
    tree.setVisible(false);
    searchPanel.setVisible(false);
    description.setVisible(false);
    addicon.setVisible(false);
    matchlist.setVisible(false);
    addmode = false;
  }
  
  private void showErrorWhileLoading() {
    clear();
    retryDiv.setVisible(true);
  }

  private void showHypothesisList() {
    loader.setVisible(true);
    // This can be a problem, the server is not handling concurrency correctly.
    // Will make this sequential, but multiple users can make this error to happen.
    DiskREST.listHypotheses(new Callback<List<TreeItem>, Throwable>() {
      @Override
      public void onSuccess(List<TreeItem> result) {
        if (result != null) {
          hypothesisList = result;
          //generateHypothesisTree();
          generateHypothesisItems();
        } else {
          AppNotification.notifyFailure("Error loading hypothesis");
          showErrorWhileLoading();
        }
      }
      @Override
      public void onFailure(Throwable reason) {
        loader.setVisible(false);   
        AppNotification.notifyFailure(reason.getMessage());
        showErrorWhileLoading();
      }
    });
    DiskREST.listTriggeredLOIs(new Callback<List<TriggeredLOI>, Throwable>() {
      @Override
      public void onSuccess(List<TriggeredLOI> result) {
        if (result != null) {
          tloilist = result;
          if (hypothesisList != null) addExecutions(tloilist);
        } else {
          AppNotification.notifyFailure("Error loading trigered lines of inquiry");
          showErrorWhileLoading();
        }
      }
      @Override
      public void onFailure(Throwable reason) {
        loader.setVisible(false);   
        AppNotification.notifyFailure(reason.getMessage());
        GWT.log("Failed", reason);
      }
    });
  }
  
  private void generateHypothesisItems () {
      clear();
      addicon.setVisible(true);
      description.setVisible(true);
      searchPanel.setVisible(true);
      
      if (hypothesisList.size() == 0) {
          emptyDiv.setVisible(true);
      } else {
          for (TreeItem hyp: hypothesisList) {
              HypothesisItem item = new HypothesisItem(hyp.getId());
              String parentid = hyp.getParentId();
              if (parentid == null || parentid.equals("")) {
                  item.load(hyp);
                  searchPanel.addItem(hyp.getId(), item);
              }
          }
          if (tloilist != null) addExecutions(tloilist);
      }
  }

    private void addExecutions (List<TriggeredLOI> tlois) {
        for (TreeItem hyp: hypothesisList) {
            String hid = hyp.getId();
            HypothesisItem item = (HypothesisItem) searchPanel.getItem(hid);

            List<TriggeredLOI> allexec = tlois.stream()
                    .filter(p -> p.getParentHypothesisId() != null && p.getParentHypothesisId().equals(hid))
                    .collect(Collectors.toList());
            
            Map<String, List<TriggeredLOI>> groups = new HashMap<String, List<TriggeredLOI>>();
            for (TriggeredLOI exec: allexec) {
                String loiid = exec.getLoiId();
                if (!groups.containsKey(loiid)) groups.put(loiid, new ArrayList<TriggeredLOI>());
                List<TriggeredLOI> g = groups.get(loiid);
                g.add(exec);
            }
            
            for (String loiid: groups.keySet()) {
                item.addExecutionList(loiid, groups.get(loiid));
            }
        }
    }

  private void showHypothesis(final String id) {
    loader.setVisible(true);
    Polymer.ready(form.getElement(), new Function<Object, Object>() {
      @Override
      public Object call(Object o) {
        DiskREST.getHypothesis(id, new Callback<Hypothesis, Throwable>() {
          @Override
          public void onSuccess(Hypothesis result) {
            loader.setVisible(false);
            form.setVisible(true);
            form.setNamespace(getNamespace(result.getId()));
            form.load(result);
          }
          @Override
          public void onFailure(Throwable reason) {
            loader.setVisible(false);
            AppNotification.notifyFailure(reason.getMessage());
          }
        });
        return null;
      }
    });
  }

 private void showHypothesisMatches(final String id) {
   loader.setVisible(true);   
    DiskREST.queryHypothesis(id, 
        new Callback<List<TriggeredLOI>, Throwable>() {
      @Override
      public void onSuccess(List<TriggeredLOI> result) {
        loader.setVisible(false);
        matchlist.setVisible(true);
        matches = result;
        showTriggeredLOIOptions(result);
      }
      @Override
      public void onFailure(Throwable reason) {
        loader.setVisible(false);
        AppNotification.notifyFailure(reason.getMessage());
      }
    });
  }

  private void showTriggeredLOIOptions(List<TriggeredLOI> tlois) {
    matchlist.clear();
    
    if (tlois.size() == 0) {
    	notloi.setAttribute("visible", "");
    	return;
    }
    
    Collections.sort(tlois, Utils.tloiSorter);
    
    for(final TriggeredLOI tloi : tlois) {
      final HTMLPanel panel = new HTMLPanel("");
      panel.setStyleName("bordered-section");
      
      if (tloi.getStatus() == null) { 
    	  //TLOI has not been executed so add the button
		  HTMLPanel buttonPanel = new HTMLPanel("");
		  buttonPanel.setStyleName("floating-right-button");      
		  PaperButton button = new PaperButton();
		  IronIcon icon = new IronIcon();
		  icon.setIcon("build");
		  button.add(icon);
		  button.add(new InlineHTML("Run this line of inquiry"));
		  button.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
			  triggerMatchedLOI(tloi);
			  matchlist.remove(panel);
			}
		  });
		  buttonPanel.add(button);
		  //panel.add(buttonPanel);            
		  
		  GWT.log("TLOI name= " + tloi.getName());
		  tloi.setName( tloi.getName().replace("Triggered", "New") );
		  
      }

      final TriggeredLOIViewer tviewer = new TriggeredLOIViewer();
      tviewer.initialize(userid, domain);
      tviewer.load(tloi);
      tviewer.enableBindingEdition();
      panel.add(tviewer);
      
      matchlist.add(panel);
    }
  }

  /*void updateDialogContent (List<WorkflowBindings> workflows) {
	  dialogContent.clear();
	  varList = null;
	  checkMap = null;
	  if (workflows.size() == 0) return;

	  varList = new ListBox();
	  checkMap = new HashMap<>();
	  WorkflowBindings wf = workflows.get(0); //FIXME
	  selectedWorkflow = wf;
	  for (VariableBinding b: wf.getBindings()) {
		  varList.addItem(b.getVariable());
		  dialogContent.add(varList);
		  String binds = b.getBinding().replace("]", "").replace("[", "");
		  List<CheckBox> cblist = new ArrayList<CheckBox>();
		  checkMap.put(b.getVariable(), cblist);
		  for (String bind: binds.split(",")) {
			  CheckBox cb = new CheckBox(bind);
			  cb.setValue(true);
			  cb.setStyleName("block");
			  dialogContent.add(cb); //FIXME
			  cblist.add(cb);
		  }
	  }
  }*/

  void triggerMatchedLOI(final TriggeredLOI tloi) {
	tloi.setName(tloi.getName().replace("New:", "Triggered:"));
    DiskREST.addTriggeredLOI(tloi, new Callback<Void, Throwable>() {
      @Override
      public void onFailure(Throwable reason) {
        AppNotification.notifyFailure(reason.getMessage());
      }
      @Override
      public void onSuccess(Void result) {
        AppNotification.notifySuccess("Submitted.", 1000);
        String token = NameTokens.getTLOIs()+"/" + userid+"/"
            + domain + "/" + tloi.getId();
        History.newItem(token, true);
      }
    });
  }

  @UiHandler("addicon")
  void onAddIconClicked(ClickEvent event) {
    tree.setVisible(false);
    searchPanel.setVisible(false);
    description.setVisible(false);
    addicon.setVisible(false);
    form.setVisible(true);        
    addmode = true;
    
    String id = GUID.randomId("Hypothesis");

    Hypothesis hypothesis = new Hypothesis();
    hypothesis.setId(id);
    hypothesis.setGraph(new Graph());
    form.setNamespace(this.getNamespace(id));    
    form.load(hypothesis);
    
    History.newItem(this.getHistoryToken(NameTokens.hypotheses, id), false);
  }

  @UiHandler("form")
  void onHypothesisFormSave(HypothesisSaveEvent event) {
    Hypothesis hypothesis = event.getHypothesis();
    if(this.addmode) {
      DiskREST.addHypothesis(hypothesis, new Callback<Void, Throwable>() {
        @Override
        public void onSuccess(Void result) {
          AppNotification.notifySuccess("Saved", 500);
        }        
        @Override
        public void onFailure(Throwable reason) {
          AppNotification.notifyFailure("Could not save: " + reason.getMessage()); 
        }
      });
    } else {
      DiskREST.updateHypothesis(hypothesis, new Callback<Void, Throwable>() {
        @Override
        public void onSuccess(Void result) {
          AppNotification.notifySuccess("Updated", 500);
        }        
        @Override
        public void onFailure(Throwable reason) {
          AppNotification.notifyFailure("Could not save: " + reason.getMessage()); 
        }
      });      
    }
  }

  @UiHandler("tree")
  void onTreeItemSelected(TreeItemSelectionEvent event) {
    TreeNode node = event.getItem();
    String token = this.getHistoryToken(node.getType(), node.getId());
    History.newItem(token); 
  }

  @UiHandler("tree")
  void onTreeItemAction(TreeItemActionEvent event) {
    final TreeNode node = event.getItem();
    if(event.getAction().getId().equals("delete")) {
      if(Window.confirm("Are you sure you want to delete " + node.getName())) {
        if(node.getType().equals(NameTokens.hypotheses)) {
          HypothesisView me = this;
          DiskREST.deleteHypothesis(node.getId(), new Callback<Void, Throwable>() {
            @Override
            public void onFailure(Throwable reason) {
              AppNotification.notifyFailure(reason.getMessage());
            }
            @Override
            public void onSuccess(Void result) {
              AppNotification.notifySuccess("Deleted", 500);
              me.showHypothesisList();
              tree.removeNode(node);
            }
          });
        }
        else if(node.getType().equals(NameTokens.tlois)) {
          DiskREST.deleteTriggeredLOI(node.getId(), new Callback<Void, Throwable>() {
            @Override
            public void onFailure(Throwable reason) {
              AppNotification.notifyFailure(reason.getMessage());
            }
            @Override
            public void onSuccess(Void result) {
              AppNotification.notifySuccess("Deleted", 500);
              tree.removeNode(node);
            }
          });
        }
      }
    }
    else if(event.getAction().getId().equals("query")) {
      String token = 
          this.getHistoryToken(NameTokens.hypotheses, node.getId()) + "/query";
      History.newItem(token);
    }
  }

  /*@UiHandler("dialogOkButton")
  void onOkButtonClicked(ClickEvent event) {
	  String var = varList.getSelectedValue();
	  List<String> bindings = new ArrayList<String>();
	  
	  for (CheckBox cb: checkMap.get(var)) {
		  if (cb.getValue()) {
			  bindings.add(cb.getText());
		  }
	  }
	  String strb = "[" + String.join(",", bindings) + "]";
	  strb = strb.replace(" ", "").replace(",", ", ");
	  for (VariableBinding b: selectedWorkflow.getBindings()) {
		  b.setBinding(strb);
	  }
	  
	  showTriggeredLOIOptions(matches);
	  dialog.hide();
  }

  @UiHandler("dialogCancelButton")
  void onCancelButtonClicked(ClickEvent event) {
	  dialog.hide();
  }*/

  private void setHeader(SimplePanel toolbar) {
    // Set Toolbar header
    toolbar.clear();
    String title = "<h3>Hypotheses</h3>";
    String icon = "icons:help";

    HTML div = new HTML("<nav><div class='layout horizontal center'>"
        + "<iron-icon class='orange' icon='" + icon + "'/></div></nav>");
    div.getElement().getChild(0).getChild(0).appendChild(new HTML(title).getElement());
    
    toolbar.add(div);    
  }

  private void setSidebar(SimplePanel sidebar) {
    // TODO: Modify sidebar
  }

  private String getHistoryToken(String type, String id) {    
    return type+"/" + this.userid+"/"+this.domain + "/" + id;    
  }

  private String getNamespace(String id) {
    return Config.getServerURL() + "/"+userid+"/"+domain + "/hypotheses/" + id + "#";
  }

  @UiHandler("helpicon")
  void onHelpIconClicked(ClickEvent event) {
	  helpDialog.center();
	  helpDialog.setWidth("800px");
	  helpDialog.center();
  }

  @UiHandler("closeDialog")
  void onCloseButtonClicked(ClickEvent event) {
	  helpDialog.hide();
  }
}
