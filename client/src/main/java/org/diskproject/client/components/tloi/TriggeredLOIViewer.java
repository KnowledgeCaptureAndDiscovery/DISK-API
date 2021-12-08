package org.diskproject.client.components.tloi;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.diskproject.client.Config;
import org.diskproject.client.application.dialog.CloseableDialog;
import org.diskproject.client.application.dialog.ShinyElement;
import org.diskproject.client.components.brain.Brain;
import org.diskproject.client.components.hypothesis.ExecutionList;
import org.diskproject.client.components.list.ListNode;
import org.diskproject.client.components.list.ListWidget;
import org.diskproject.client.components.list.events.ListItemActionEvent;
import org.diskproject.client.components.triples.SparqlInput;
import org.diskproject.client.components.triples.TripleViewer;
import org.diskproject.client.place.NameTokens;
import org.diskproject.client.rest.AppNotification;
import org.diskproject.client.rest.DiskREST;
import org.diskproject.shared.classes.common.Triple;
import org.diskproject.shared.classes.hypothesis.Hypothesis;
import org.diskproject.shared.classes.loi.LineOfInquiry;
import org.diskproject.shared.classes.loi.TriggeredLOI;
import org.diskproject.shared.classes.loi.TriggeredLOI.Status;
import org.diskproject.shared.classes.util.GUID;
import org.diskproject.shared.classes.loi.WorkflowBindings;
import org.diskproject.shared.classes.workflow.VariableBinding;
import org.diskproject.shared.classes.workflow.WorkflowRun;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.IFrameElement;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.polymer.paper.widget.PaperButton;
import com.vaadin.polymer.paper.widget.PaperDialog;
import com.vaadin.polymer.iron.widget.IronIcon;
import com.vaadin.polymer.iron.widget.event.IronOverlayClosedEvent;

public class TriggeredLOIViewer extends Composite {
  interface Binder extends UiBinder<Widget, TriggeredLOIViewer> {};
  private static Binder uiBinder = GWT.create(Binder.class);

  @UiField DivElement header, workflowButtons;
  @UiField DivElement hypothesisSection, LOISection, dataDiv, WFSection, MetaWFSection;
  @UiField Label DataLabel;
  @UiField HTMLPanel revHypothesisSection, DataSection, DataQuerySection;
  //@UiField HTMLPanel executionNarrative, dataQueryNarrative;
  @UiField TripleViewer hypothesis;
  @UiField SparqlInput dataQuery;
  @UiField ListWidget workflowlist, metaworkflowlist;
  @UiField AnchorElement hypothesisLink, loiLink;
  @UiField PaperButton downloadbutton, editBindingsButton, runButton;
  @UiField PaperDialog editBindingsDialog;
  @UiField BindingsEditor bindingseditor;

  private Status status;

  String username, domain;
  String rawcsv; //USED to save the downloadable CSV.
  String datamode = "all";
  TriggeredLOI tloi;
  Hypothesis parentHypothesis;
  LineOfInquiry parentLOI;
  Map<String, List<String>> dataRetrieved;
  @UiField CheckBox showdata, showdq, showHypothesisGraph;
  //@UiField CheckBox showExecNarrative, showDataQueryNarrative;
  
  //@UiField HTMLPanel workflowList;
  @UiField HTMLPanel narrative;

  public TriggeredLOIViewer() {
    initWidget(uiBinder.createAndBindUi(this));
  }

	private native void  nativeLog(Object obj) /*-{
    	console.log(obj);
  	}-*/;  

  public void initialize(String username, String domain) {
    this.username = username;
    this.domain = domain;
    editBindingsButton.setVisible(false);
    hypothesis.initialize(username, domain);
    hypothesis.setVisible(false);
    
    workflowlist.hideActionMenu();
    metaworkflowlist.hideActionMenu();

    DataSection.setVisible(false);
    DataQuerySection.setVisible(false);
    DataLabel.setVisible(false);
    //brainSection.getStyle().setDisplay(Display.NONE);
    //shinySection.getStyle().setDisplay(Display.NONE);
    
    //brainVisualization.setVisible(false);
  }
  
  public void enableBindingEdition () {
	  // Only show button if theres multiple files. FIXME: only works for the first wf mwf.
	  List<WorkflowBindings> wfs = tloi.getWorkflows();
	  List<WorkflowBindings> metawfs = tloi.getMetaWorkflows();

	  if (wfs.size() > 0) {
		  List<String> cv = wfs.get(0).getCollectionVariables();
		  if (cv.size() > 0) {
			  editBindingsButton.setVisible(true);
		  }
	  } else if (metawfs.size() > 0) {
		  List<String> cv = metawfs.get(0).getCollectionVariables();
		  if (cv.size() > 0) {
			  editBindingsButton.setVisible(true);
		  }
	  }
  }

  public void load(TriggeredLOI tloi) {
    this.tloi = tloi;
    status = tloi.getStatus();
    runButton.setVisible(status==null);
    
    setHeader(tloi);
    setLOILink(tloi.getName(), tloi.getLoiId(), loiLink);
    if (tloi.getExplanation() != null) {
    	DataLabel.setText(tloi.getExplanation());
    }

    // Load all workflows:
    List<WorkflowBindings> wf = tloi.getWorkflows();
    List<WorkflowBindings> mwf = tloi.getMetaWorkflows();

    if (mwf.size() == 0) {
    	MetaWFSection.getStyle().setDisplay(Display.NONE);
    } else {
    	MetaWFSection.getStyle().setDisplay(Display.INITIAL);
    }
    	
    setWorkflowsHTML(wf, workflowlist);
    //setWorkflowsHTML2(wf, workflowList);
    setWorkflowsHTML(mwf, metaworkflowlist);
    
    setHypothesisHTML(tloi.getParentHypothesisId(), hypothesisSection, hypothesis, hypothesisLink);
    setDataQueryHTML(tloi.getDataQuery(), LOISection, dataQuery);
    setRevisedHypothesesHTML(tloi.getResultingHypothesisIds(), revHypothesisSection);
    
    if (status == Status.SUCCESSFUL) {
    	//loadNarratives(tloi);
    	updateNarrative();
    	if (wf.size() > 0) loadWorkflows(wf, workflowlist);
    	if (mwf.size() > 0) loadWorkflows(mwf, workflowlist);
    }
  }
  
  private void updateNarrative () {
      if (this.tloi == null || this.parentHypothesis == null) {
          // Hide div that shows narrative
          narrative.setVisible(false);
      } else {
          WorkflowBindings wfb = null;
          for (WorkflowBindings wb: tloi.getWorkflows()) {
              wfb = wb;
              break;
          }
          for (WorkflowBindings wb: tloi.getMetaWorkflows()) {
              wfb = wb;
              break;
          }
          if (wfb != null) {
              WorkflowRun run = wfb.getRun();
              int datasets = 0;
              for (VariableBinding b: wfb.getBindings()) {
                  if (b.isCollection()) {
                      int size = b.getBindingAsArray().length;
                      if (datasets < size) datasets = size;
                  }
              }
              String html = "The hypothesis with title: <b>" + parentHypothesis.getName() + "</b> was runned";
              String status = run.getStatus();
              if (status != null) html += " <span class='" + run.getStatus() + "'>" + run.getStatus() + "</span>";
              html += " with the Line of inquiry: <b>" + tloi.getName().replace("Triggered: ", "")
                      + "</b>. The LOI triggered the workflow <b>" + wfb.getWorkflow() + "</b> on WINGS where it was tested with <b>"
                      + datasets + "</b> datasets.";
              double p = tloi.getConfidenceValue();
              if (p > 0) html += " The resulting p-value is " + (p < 0.001 ?
				        ExecutionList.exponentFormat.format(p)
				        : ExecutionList.decimalFormat.format(p)
                      );

              narrative.clear();
              narrative.add(new HTML(html));
              narrative.setVisible(true);
          } else {
              GWT.log("Needs at least one workflow bindings");
          }

      }
  }
  
  private void setHeader(TriggeredLOI tloi) {
	  String extra ="", extracls="";
	  Status status = tloi.getStatus();
	  if (status == null) {
		  extracls = " TORUN";
	  } else {
		  String icon = "icons:hourglass-empty";
		  if(status == Status.SUCCESSFUL) {
			  icon = "icons:check";
		  }
		  else if(status == Status.FAILED) {
			  icon = "icons:clear";
		  }
		  extra = " <iron-icon class='"+status+"' icon='"+icon+"' />";
		  extracls = " " +status;
	  }
	  
	  String html = "<div class='name" + extracls+ "'>" + tloi.getName() + extra +"</div>";
	  
	  if (tloi.getDescription() != null) {
		  html += "<div class='description'>" + tloi.getDescription() + "</div>";
	  }

    /* TODO: add date to tloi.
    html += "<div class='footer' style='display: flex;justify-content: space-between;'>";
    html += "<span><b>Creation date:</b> ";
    html += (this.creationDate != null) ? this.creationDate : "None specified";
    html += "</span><span><b>Author:</b> ";
    html += (this.author != null) ? this.author : "None specified";
    html += "</span></div>";*/

	  header.setInnerHTML(html);
  }

  /*private void loadNarratives (TriggeredLOI tloi) {
	  String id = tloi.getId();
      DiskREST.getTLOINarratives(id, new Callback<Map<String,String>, Throwable>() {
        @Override
        public void onSuccess(Map<String, String> response) {
          if (response != null) {
        	  executionNarrative.clear();
        	  executionNarrative.add(new HTML(response.get("execution")));
        	  onClickShowExecutionNarrative(null);
        	  dataQueryNarrative.clear();
        	  dataQueryNarrative.add(new HTML(response.get("dataquery")));
        	  onClickShowDataQueryNarrative(null);
          }
        }
        @Override
        public void onFailure(Throwable reason) {
          AppNotification.notifyFailure(reason.getMessage());
        }
	  });
  }*/

  @UiHandler("showdq")
  void onClickShowDQ(ClickEvent event) {
    boolean show = showdq.getValue();
    if (show) {
      dataQuery.setVisible(false);
      dataQuery.setValue("");
      dataQuery.setVisible(true);
      dataQuery.setValue(tloi.getDataQuery());
    }
    DataQuerySection.setVisible(show);
  }

  @UiHandler("showdata")
  void onClickShowData(ClickEvent event) {
    boolean show = showdata.getValue();
    if (show) {
      if (dataRetrieved == null) loadAndShowData();
      DataSection.setVisible(true);
      DataLabel.setVisible(true);
    } else {
      DataSection.setVisible(false);
      DataLabel.setVisible(false);
    }
  }

  @UiHandler("showHypothesisGraph")
  void onClickShowHypothesisGraph(ClickEvent event) {
    boolean show = showHypothesisGraph.getValue();
    hypothesis.setVisible(show);
  }

  /*@UiHandler("showExecNarrative")
  void onClickShowExecutionNarrative(ClickEvent event) {
    boolean show = showExecNarrative.getValue();
    executionNarrative.setVisible(show);
  }

  @UiHandler("showDataQueryNarrative")
  void onClickShowDataQueryNarrative(ClickEvent event) {
    boolean show = showDataQueryNarrative.getValue();
    dataQueryNarrative.setVisible(show);
  }*/

  private void loadAndShowData () {
    String vars = tloi.getRelevantVariables();
    String dq = tloi.getDataQuery();
    String enp = tloi.getDataSource();
    if (vars != null && dq != null && dataRetrieved == null) {
      dataDiv.setInnerText("Loading...");
      DiskREST.queryExternalStore(enp, dq, vars, new Callback<Map<String, List<String>>, Throwable>() {
        @Override
        public void onSuccess(Map<String, List<String>> response) {
          if (response != null) {
            dataRetrieved = response;
            setDataHTML(dataDiv);
          }
        }
        @Override
        public void onFailure(Throwable reason) {
          AppNotification.notifyFailure(reason.getMessage());
          dataDiv.setInnerText("An error has ocurred");
        }
      });
    }
  }


  private void showHypothesisData() {
    String vars = tloi.getRelevantVariables();
    String dq = tloi.getDataQuery();
    String enp = tloi.getDataSource();
    if (vars != null && dq != null) {
      DiskREST.queryExternalStore(enp, dq, vars, new Callback<Map<String, List<String>>, Throwable>() {
        @Override
        public void onSuccess(Map<String, List<String>> response) {
          if (response != null) {
            dataRetrieved = response;
            setDataHTML(dataDiv);
          }
        }
        @Override
        public void onFailure(Throwable reason) {
          AppNotification.notifyFailure(reason.getMessage());
        }
      });
    }
  }

  public TriggeredLOI getTLOI() {
    return this.tloi;
  }
  
  private void setDataHTML(final DivElement section) {
    if (dataRetrieved == null) {
      DataSection.setVisible(false);
      DataLabel.setVisible(false);
      downloadbutton.setVisible(false);
      return;
    }
    
    Set<String> vars = dataRetrieved.keySet();
    
    if (vars.size() == 0) {
    	section.setInnerHTML("No data retrieved.");
    	downloadbutton.setVisible(false);
    	return;
    }
    rawcsv = "";
    DataSection.setVisible(true);
    DataLabel.setVisible(true);
    downloadbutton.setVisible(true);
    
    int lines = 0;
    
    String html = "<table class=\"pure-table\"><thead><tr>";
    for (String v: vars) {
    	html += "<th><b>" + v + "</b></th>";
    	rawcsv += v + ",";
    	if (dataRetrieved.get(v).size() > lines) {
    		lines = dataRetrieved.get(v).size();
    	}
    }
    html += "</tr></thead><tbody>";
    rawcsv += "\n";
    
    for (int i = 0; i < lines; i++) {
    	html += "<tr>";
    	for (String v: vars) {
    		String url = dataRetrieved.get(v).get(i).replace("localhost:8080", "organicdatapublishing.org");
    		if (url.contains("http")) {
    			String parts[] = url.split("/");
    			String name = (parts.length>3) ?
    					((parts[parts.length-1].length() > 0) ? parts[parts.length-1] : parts[parts.length-2])
    					: url;
    			html += "<td><a href=\"" + url + "\" target=\"_blank\">" + name + "</a></td>";
    		} else {
    			html += "<td>" + url + "</td>";
    		}
    		rawcsv += dataRetrieved.get(v).get(i) + ",";
    	}
    	html += "</tr>";
    	rawcsv += "\n";
    }

    html += "</table>";
    
    section.setInnerHTML(html);
  }

  private void setDataQueryHTML(final String strDataQuery, final DivElement section, 
      final SparqlInput tv) {
    if (strDataQuery == null || strDataQuery.equals("")) {
      section.setAttribute("style", "display:none");
      return;
    }
    section.setAttribute("style", "");
    GWT.log("dataquery: " + strDataQuery);
    tv.setValue(strDataQuery);
    GWT.log(tv.getValue());
  }

  private void setLOILink(String tname, String id, AnchorElement anchor) {
    String name = tname.replace("Triggered: ", "").replace("New: ", "");
    anchor.setInnerText(name);
    anchor.setHref(this.getLOILink(id));
  }

  private void setRevisedHypothesesHTML(List<String> ids, final HTMLPanel section) {
    if(ids.size() == 0) {
      section.setVisible(false);
      return;
    }
    section.setVisible(true);
    section.clear();
    
    Label label = new Label("Revised Hypothesis");
    label.addStyleName("small-grey");
    section.add(label);
    
    for(final String id : ids) {
      HTMLPanel panel = new HTMLPanel("");
      panel.addStyleName("bordered-list padded");

      HTMLPanel anchordiv = new HTMLPanel("");
      anchordiv.addStyleName("rev-hyp-title");
      final Anchor anchor = new Anchor();
      anchordiv.add(anchor);
      
      
      final TripleViewer tv = new TripleViewer("");
      tv.initialize(username, domain);

      panel.add(anchordiv);
      panel.add(tv);
      section.add(panel);
      
      DiskREST.getHypothesis(id, 
          new Callback<Hypothesis, Throwable>() {
        public void onSuccess(Hypothesis result) {
          anchor.setHref(getHypothesisLink(id));
          if (result != null) {
			  anchor.setText(result.getName());
			  if (result.getGraph() != null) {
				tv.setDefaultNamespace(getNamespace(result.getId()));
				List<Triple> triples = result.getGraph().getTriples();
				tv.load(triples);
				//write the confidence value.
				String cv = "";
				for(final Triple t : triples) {
					if (t.getDetails() != null) {
						cv = "" + t.getDetails().getConfidenceValue();
						if (cv.length() > 4) cv = cv.substring(0, 4);
						break;
					}
				}
				if (!cv.contentEquals("")) {
					final Anchor pval = new Anchor("Confidence: " + cv);
					anchordiv.add(pval);
				}
			  }
          }
        }
        public void onFailure(Throwable reason) {}
      });
    }
  }

  private void setHypothesisHTML(final String id, final DivElement section, 
      final TripleViewer tv, final AnchorElement anchor) {
    if(id == null) {
      section.setAttribute("style", "display:none");
      return;
    }
    section.setAttribute("style", "");

    DiskREST.getHypothesis(id, new Callback<Hypothesis, Throwable>() {
      public void onSuccess(Hypothesis result) {
        anchor.setHref(getHypothesisLink(id));
        if (result.getName() != null) {
          anchor.setInnerText(result.getName());
        }
        if(result.getGraph() != null) {
          tv.setDefaultNamespace(getNamespace(id));
          tv.load(result.getGraph().getTriples());
        }
        parentHypothesis = result;
        updateNarrative();
      }
      public void onFailure(Throwable reason) {}
    });  
  }

  private String getNamespace(String id) {
    return Config.getServerURL() + "/"+username+"/"+domain + "/hypotheses/" + id + "#";
  }

  private String getHypothesisLink(String id) {
    return "#" + NameTokens.getHypotheses()+"/" + this.username+"/"+this.domain + "/" + id;
  }

  private String getLOILink(String id) {
    return "#" + NameTokens.getLOIs()+"/" + this.username+"/"+this.domain + "/" + id;
  }

  private void setWorkflowsHTML2(List<WorkflowBindings> wbindings, HTMLPanel list) {
    list.clear();
    for (WorkflowBindings bindings: wbindings) {
        WorkflowViewer wv = new WorkflowViewer(bindings);
        list.add(wv);
    }
  }

  private void setWorkflowsHTML(List<WorkflowBindings> wbindings, ListWidget list) {
    list.clear();
    for(WorkflowBindings bindings: wbindings) {
      String type = bindings.getRun().getLink() == null ? "no-run-link" : "";
      ListNode tnode = new ListNode(bindings.getWorkflow(), 
          new HTML(bindings.getHTML()));
      tnode.setIcon("icons:dashboard");
      tnode.setIconStyle("orange");
      tnode.setData(bindings);
      tnode.setType(type);
      list.addNode(tnode);
    }
  }

  private void loadWorkflows(List<WorkflowBindings> wbindings, ListWidget list) {
      list.clear();
      for (WorkflowBindings bindings: wbindings) {
		  String base = bindings.getHTML();
		  String html = base + "<div>Loading...</div>";
          ListNode node = new ListNode(bindings.getWorkflow(), new HTML(html));

          node.setIcon("icons:dashboard");
          node.setIconStyle("orange");
          node.setData(bindings);
          list.addNode(node);

          WorkflowRun run = bindings.getRun();
          String id = run.getId();
          if (id != null) {
              String[] lid = id.split("#|/");
              id = lid[lid.length - 1];
          }

		  GWT.log("Current Run:\n ID: " + run.getId());
		  DiskREST.monitorWorkflow(id, new Callback<WorkflowRun, Throwable>() {
			  @Override
			  public void onSuccess(WorkflowRun result) {
				  //Save data
				  String sdate = result.getStartDate();
				  String edate = result.getEndDate();
				  if (sdate != null) run.setStartDate(sdate);
				  if (edate != null) run.setEndDate(edate);

                  @SuppressWarnings("deprecation")
                  Element el;

				  Map<String, String> outputs = result.getOutputs();
				  if (outputs.containsKey("brain_visualization")) {
                      IronIcon iconBrain = new IronIcon();
                      iconBrain.addStyleName("inline-button");
                      iconBrain.setIcon("3d-rotation");
                      el = iconBrain.getElement();
                      Event.sinkEvents(el, Event.ONCLICK);
                      Event.setEventListener(el, new EventListener() {
                        @Override
                        public void onBrowserEvent(Event event) {
                            String brainURL = null;
                            for (String outfile: tloi.getOutputFiles()) {
                                if (outfile.contains("brain_visualization")) {
                                    brainURL = outfile;
                                    break;
                                }
                            }
                            if (brainURL != null) {
                                CloseableDialog dialog = new CloseableDialog();
                                Brain brain = Brain.get();
                                brain.loadConfigFile(brainURL);
                                dialog.setText("Brain Visualization");
                                dialog.add(brain);
                                dialog.centerAndShow();
                            } else {
                                AppNotification.notifyFailure("Could not find Brain visualization");
                            }
                        }
                      });
                      workflowButtons.appendChild(el);
				      outputs.remove("brain_visualization");
				  }
				  if (outputs.containsKey("shiny_visualization")) {
                      IronIcon iconShiny = new IronIcon();
                      iconShiny.addStyleName("inline-button");
                      iconShiny.setIcon("assessment");
                      el = iconShiny.getElement();
                      Event.sinkEvents(el, Event.ONCLICK);
                      Event.setEventListener(el, new EventListener() {
                        @Override
                        public void onBrowserEvent(Event event) {
                            String shinyURL = null;
                            for (String outfile: tloi.getOutputFiles()) {
                                if (outfile.contains("shiny_visualization")) {
                                    shinyURL = outfile;
                                    break;
                                }
                            }
                            if (shinyURL != null) {
                                CloseableDialog dialog = new CloseableDialog();
                                ShinyElement shiny =  new ShinyElement();
                                shiny.load(shinyURL);
                                dialog.setText("Shiny Visualization");
                                dialog.add(shiny);
                                dialog.centerAndShow();
                            } else {
                                AppNotification.notifyFailure("Could not find Shiny visualization");
                            }
                        }
                      });
                      workflowButtons.appendChild(el);
				      outputs.remove("shiny_visualization");
				  }
				  
				  if (outputs != null && outputs.size() > 0) run.setOutputs(outputs);
				  run.setFiles(result.getFiles());
				  
				  node.setFullContent(bindings.getHTML());
				  updateNarrative();
			  }
			  @Override
			  public void onFailure(Throwable reason) {
				  String html = base 
						  	  + "<div>An error has occurred retrieving this data from the Wings server."
						  	  + "Please try again. </div>";
				  node.setFullContent(html);
			  }
		  });
      }
  }
  
  @UiHandler({"workflowlist", "metaworkflowlist"})
  void onWorkflowListAction(ListItemActionEvent event) {
	  if(event.getAction().getId().equals("runlink")) {
		  // Get node and set loading text
		  ListNode node = event.getItem();
		  WorkflowBindings bindings = (WorkflowBindings) node.getData();
		  String base = bindings.getHTML();
		  String html = base + "<div>Loading...</div>";
		  node.setFullContent(html);

		  //Request data from wings
		  WorkflowRun run = bindings.getRun();
		  String id = run.getId();
		  if (id != null) {
			  String[] lid = id.split("#|/");
			  id = lid[lid.length - 1];
		  }

		  GWT.log("Current Run:\n  ID: " + run.getId());
		  DiskREST.monitorWorkflow(id, new Callback<WorkflowRun, Throwable>() {
			  @Override
			  public void onSuccess(WorkflowRun result) {
				  //Save data
				  String sdate = result.getStartDate();
				  String edate = result.getEndDate();
				  Map<String, String> outputs = result.getOutputs();
				  
				  if (sdate != null) run.setStartDate(sdate);
				  if (edate != null) run.setEndDate(edate);
				  if (outputs != null && outputs.size() > 0) run.setOutputs(outputs);
				  run.setFiles(result.getFiles());
				  
				  for (String key: outputs.keySet()) {
					  GWT.log(key + ": " + outputs.get(key));
				  }
				  
				  if (outputs.keySet().contains("brain_visualization")) {
					  //This workflow returns a brain visualizations:
					  //TODO: Assuming theres only one brainviz per tloi.
					  //loadBrainViz( outputs.get("brain_visualization") );
				  }

				  if (outputs.keySet().contains("shiny_visualization")) {
					  //This workflow returns a shiny visualization
					  //TODO: Assuming theres only one shiny viz per tloi.
					  String sp[] = outputs.get("shiny_visualization").split("#");
					  String id = sp[sp.length-1];
					  //loadShinyViz(id);
				  }
				  
				  node.setFullContent(bindings.getHTML());
			  }
			  @Override
			  public void onFailure(Throwable reason) {
				  String html = base 
						  	  + "<div>An error has occurred retrieving this data from the Wings server."
						  	  + "Please try again. </div>";
				  node.setFullContent(html);
			  }
		  });

		  //Window.open(bindings.getRun().getLink(), "_blank", "");
	  }
    }

  	/*private void loadShinyViz (String shinyLog) {
		DiskREST.getDataFromWingsAsJS(shinyLog, new Callback<JavaScriptObject, Throwable>() {
			@Override
			public void onSuccess(JavaScriptObject result) {
				// TODO Auto-generated method stub
				String url = getShinyURL(result);
				if (url != null && !url.equals("")) {
					GWT.log("RETURN FROM shinyLog " + url);
					shinySection.getStyle().setDisplay(Display.INITIAL);
					shinyIframe.setSrc(url);
				}
			}
			
			@Override
			public void onFailure(Throwable reason) {
				// TODO Auto-generated method stub
				
			}
		});
  		
  	}

  	private void loadBrainViz (String brainURL) {
		brainSection.getStyle().setDisplay(Display.INITIAL);
		brainPanel.clear();
        Brain brain = Brain.get();
        brain.loadConfigFile(brainURL);
        brainPanel.add(brain);
  	}*/

  	public static native void download(String name, String raw, String enc) /*-{
  		var blob = new Blob([raw], {type: enc});
        var a = document.createElement("a");
        a.style = "display: none";
        document.body.appendChild(a);
        var url = $wnd.window.URL.createObjectURL(blob);
        a.href = url;
        a.download = name;
        a.click();
        window.URL.revokeObjectURL(url);
	}-*/;

	@UiHandler("downloadbutton")
	void onSaveButtonClicked(ClickEvent event) {
		String name = (this.tloi != null) ? this.tloi.getId() + "_metadata.csv" : "metadata.csv";
		download(name, rawcsv, "text/csv;encoding:utf-8");
	}

    Set<String> getRelevantVariables () {
    	Set<String> r = new HashSet<String>();
    	ListWidget[] wfs = {workflowlist, metaworkflowlist};
    	for (ListWidget wf: wfs) {
			for (ListNode node: wf.getNodes()) {
				WorkflowBindings bindings = (WorkflowBindings) node.getData();
				for (VariableBinding vb: bindings.getBindings()) {
					r.add(vb.getVariable());
				}
			}
    	}
    	return r;
    }

	@UiHandler("runButton")
	void onRunButtonClicked(ClickEvent event) {
		tloi.setName(tloi.getName().replace("New:", "Triggered:"));
		DiskREST.addTriggeredLOI(tloi, new Callback<Void, Throwable>() {
			@Override
			public void onFailure(Throwable reason) {
				AppNotification.notifyFailure(reason.getMessage());
			}
			@Override
			public void onSuccess(Void result) {
				AppNotification.notifySuccess("Submitted.", 1000);
				String token = NameTokens.getTLOIs() + "/" + username + "/"
						+ domain + "/" + tloi.getId();
				History.newItem(token, true);
			}
		});
	}

	@UiHandler("editBindingsButton")
	void onEditBindingsButtonClicked(ClickEvent event) {
		//For the moment this works with only one workflow!
		List<WorkflowBindings> metawfs = tloi.getMetaWorkflows();
		List<WorkflowBindings> wfs = tloi.getWorkflows();
		if (metawfs.size() > 0) {
			bindingseditor.setWorkflowBindings(metawfs.get(0));
		} else if (wfs.size() > 0) {
			bindingseditor.setWorkflowBindings(wfs.get(0));
		} else {
			GWT.log("Cannot edit, workflow not found.");
		}

		editBindingsDialog.open();
	}

	@UiHandler("editBindingsDialog")
	void onDialogClose(IronOverlayClosedEvent event) {
		if(!isConfirmed(event.getPolymerEvent().getDetail()))
			return;
		
		//For the moment this works with only one workflow!
		List<WorkflowBindings> metawfs = tloi.getMetaWorkflows();
		List<WorkflowBindings> wfs = tloi.getWorkflows();
		
		List<WorkflowBindings> toSet = new ArrayList<WorkflowBindings>();
		toSet.add( bindingseditor.getWorkflowBindings() );
		
		if (metawfs.size() > 0) {
			tloi.setMetaWorkflows(toSet);
		} else if (wfs.size() > 0) {
			tloi.setWorkflows(toSet);
		} else {
			GWT.log("Cannot edit, workflow not found.");
		}
		// Clear some properties
		tloi.setResultingHypothesisIds(new ArrayList<String>());
		tloi.setConfidenceValue(0);
		tloi.setStatus(null);
		tloi.setAuthor(null);
		tloi.setDateCreated(null);
		tloi.setId(GUID.randomId("TriggeredLOI"));
		load(tloi);
	}

	private native boolean isConfirmed(Object obj) /*-{
    	return obj.confirmed;
  	}-*/;  
}
