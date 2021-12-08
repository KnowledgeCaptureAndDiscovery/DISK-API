package org.diskproject.client.components.hypothesis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.diskproject.client.Utils;
import org.diskproject.client.application.dialog.CloseableDialog;
import org.diskproject.client.application.dialog.FileListDialog;
import org.diskproject.client.application.dialog.Narrative;
import org.diskproject.client.application.dialog.ShinyElement;
import org.diskproject.client.components.brain.Brain;
import org.diskproject.client.components.searchpanel.SearchableItem;
import org.diskproject.client.rest.AppNotification;
import org.diskproject.client.rest.DiskREST;
import org.diskproject.shared.classes.common.TreeItem;
import org.diskproject.shared.classes.loi.TriggeredLOI;
import org.diskproject.shared.classes.loi.TriggeredLOI.Status;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableSectionElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.polymer.iron.widget.IronIcon;
import com.vaadin.polymer.paper.widget.PaperSpinner;

@SuppressWarnings("deprecation")
public class ExecutionList extends SearchableItem {
	interface Binder extends UiBinder<Widget, ExecutionList> {};
	private static Binder uiBinder = GWT.create(Binder.class);

	private static String username, domain;
	public static NumberFormat decimalFormat = NumberFormat.getFormat("#0.000");
	public static NumberFormat exponentFormat = NumberFormat.getFormat("#0.0E0");

	
	@UiField DivElement title, description;
	@UiField TableSectionElement tloilist;
	@UiField FocusPanel toggle;
	@UiField HTMLPanel tableContainer;
	@UiField PaperSpinner updating;

	List<TriggeredLOI> list;
	private String parentHypothesisId, parentLOIId;
	
	public ExecutionList(String hypid, String loiid) {
		initWidget(uiBinder.createAndBindUi(this)); 
		super.onAttach();
	    tableContainer.setVisible(false);
	    updating.setVisible(false);
	    parentHypothesisId = hypid;
	    parentLOIId = loiid;
	}
	
	public static void setUsenameAndDomain (String username, String domain) {
		ExecutionList.username = username;
		ExecutionList.domain = domain;
	}
	
	public void setTitle (String newTitle) {
		title.setInnerText(newTitle);
	}
	
	public void setDescription (String desc) {
		description.setInnerText(desc);
	}
	
	public void load (TreeItem item) {
		//item.get
		setTitle(item.getName());
		setDescription(item.getDescription());
	}

	public void setList(List<TriggeredLOI> tloilist) {
		list = tloilist;
		updateTable();
	}
	
	private void updateTable () {
		List<TriggeredLOI> ordered = list;//FIXME!
	    Collections.sort(ordered, Utils.orderTLOIs);
        Element el = null; //Pointer to pseudo elements

		tloilist.removeAllChildren();
		int i = 1;
		for (TriggeredLOI tloi: ordered) {
			TableRowElement row = TableRowElement.as(DOM.createTR());

			//row number
			TableCellElement n = TableCellElement.as(DOM.createTD());
			n.setInnerText(Integer.toString(i));
			i += 1;

			//Date
			TableCellElement date = TableCellElement.as(DOM.createTD());
			
			AnchorElement link = AnchorElement.as(DOM.createAnchor());
			String[] dsp = tloi.getDateCreated().split(" ");
			if (dsp.length == 2)
			    link.setInnerText(dsp[1] + " " + dsp[0]);
			else 
			    link.setInnerText(tloi.getDateCreated());
			    
            //FIXME: this is different on the server
			link.setHref("/disk-portal/#tlois/" + ExecutionList.username + "/" + ExecutionList.domain + "/" + tloi.getId());
			//date.setInnerText(tloi.getDateCreated());
			date.appendChild(link);
			
			//status
			TableCellElement st = TableCellElement.as(DOM.createTD());
			Status curStatus = tloi.getStatus();
			if (curStatus == Status.SUCCESSFUL) {
				st.setInnerText("SUCCESSFUL");
				st.getStyle().setColor("green");
			} else if (curStatus == Status.QUEUED) {
				st.setInnerText("QUEUED");
				//st.getStyle().setColor("yellow");
			} else if (curStatus == Status.RUNNING) {
				st.setInnerText("RUNNING");
				//st.getStyle().setColor("yellow");
			} else if (curStatus == Status.FAILED) {
				st.setInnerText("FAILED");
				st.getStyle().setColor("red");
			}
			
			//inputs
			TableCellElement in = TableCellElement.as(DOM.createTD());
			in.setInnerText(Integer.toString(tloi.getInputFiles().size()));
			IronIcon iconInputList = new IronIcon();
			iconInputList.addStyleName("inline-button");
			iconInputList.setIcon("description");
			el = iconInputList.getElement();
			Event.sinkEvents(el, Event.ONCLICK);
            Event.setEventListener(el, new EventListener() {
              @Override
              public void onBrowserEvent(Event event) {
                  FileListDialog dialog = new FileListDialog();
                  dialog.setFileList(tloi.getInputFiles());
                  dialog.setName("Input files:");
                  String header = "<b>" + tloi.getName().replaceAll("Triggered: ", "") + "</b>:";
                  dialog.setHeader("Inputs used for " + header);
                  dialog.show();
                  dialog.center();
              }
            });
			in.appendChild(el);

			// Analyze outputs
			TableCellElement out = TableCellElement.as(DOM.createTD());
			boolean hasShinyURL = false, hasBrainURL = false;
			if (curStatus == Status.SUCCESSFUL) {
				// Count files excluding system-related files
			    int nouts = 0;
				for (String outfile: tloi.getOutputFiles()) {
				    String outname = outfile.replaceAll("^.*#", "");
				    if (outname.startsWith("shiny_visualization")) {
				        hasShinyURL = true;
				    } else if (outname.startsWith("brain_visualization")) {
				        hasBrainURL = true;
				    } else if (outname.startsWith("p_val") || outname.startsWith("pval") || outname.startsWith("p_value")) {
				        //Do nothing
				    } else {
				        nouts += 1;
				    }
				}
				out.setInnerText(Integer.toString(nouts));

				if (nouts != 0) {
                    // Add list file dialog.
                    IronIcon iconOutputList = new IronIcon();
                    iconOutputList.addStyleName("inline-button");
                    iconOutputList.setIcon("description");
                    el = iconOutputList.getElement();
                    Event.sinkEvents(el, Event.ONCLICK);
                    Event.setEventListener(el, new EventListener() {
                      @Override
                      public void onBrowserEvent(Event event) {
                          List<String> filtered = new ArrayList<String>();
                          for (String outfile: tloi.getOutputFiles()) {
                              if (!(outfile.contains("shiny_visualization") || outfile.contains("brain_visualization")
                                  || outfile.contains("pval") || outfile.contains("p_val") || outfile.contains("p_value"))) {
                                  filtered.add(outfile);
                              }
                          }

                          FileListDialog dialog = new FileListDialog();
                          dialog.setFileList(filtered);
                          dialog.setName("Output files:");
                          String header = "<b>" + tloi.getName().replaceAll("Triggered: ", "") + "</b>:";
                          dialog.setHeader("Outputs used for " + header);
                          dialog.show();
                          dialog.center();
                      }
                    });
                    out.appendChild(el);
				}
			} else if (curStatus == Status.FAILED) {
				out.setInnerText("-");
			} else {
				out.setInnerText("...");
			}
			
			//p value
			TableCellElement pval = TableCellElement.as(DOM.createTD());
			if (curStatus == Status.SUCCESSFUL) {
			    double confidence = tloi.getConfidenceValue();
				pval.setInnerText(
				        confidence != 0 && confidence < 0.001 ?
				        ExecutionList.exponentFormat.format(confidence)
				        : ExecutionList.decimalFormat.format(confidence));
			} else if (curStatus == Status.FAILED) {
				pval.setInnerText("-");
			} else {
				pval.setInnerText("...");
			}

			//Buttons at the end
			TableCellElement options = TableCellElement.as(DOM.createTD());
			
			if (hasShinyURL) {
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
                options.appendChild(el);
			}

			if (hasBrainURL) {
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
                options.appendChild(el);
			}

			if (curStatus == Status.SUCCESSFUL) {
                IronIcon iconNarrative = new IronIcon();
                iconNarrative.addStyleName("inline-button");
                iconNarrative.setIcon("assignment");
                el = iconNarrative.getElement();
                Event.sinkEvents(el, Event.ONCLICK);
                Event.setEventListener(el, new EventListener() {
                    @Override
                    public void onBrowserEvent(Event event) {
                        CloseableDialog dialog = new CloseableDialog();
                        Narrative narrativeEl = new Narrative(tloi);
                        dialog.setText("Execution narrative");
                        dialog.add(narrativeEl);
                        dialog.centerAndShow();
                    }
                });

                options.appendChild(el);
			}

			IronIcon iconDelete = new IronIcon();
			iconDelete.addStyleName("delete-button");
			iconDelete.setIcon("delete");
            el = iconDelete.getElement();
			Event.sinkEvents(el, Event.ONCLICK);
            Event.setEventListener(el, new EventListener() {
                @Override
                public void onBrowserEvent(Event event) {
                    String id = tloi.getId();
                    if (id != null) {
                        if (Window.confirm("Are you sure you want to delete " + tloi.getName())) {
                            updating.setVisible(true);
                            DiskREST.deleteTriggeredLOI(id, new Callback<Void, Throwable>() {
                                @Override
                                public void onFailure(Throwable reason) {
                                    updating.setVisible(false);
                                    AppNotification.notifyFailure(reason.getMessage());
                                }
                                @Override
                                public void onSuccess(Void result) {
                                    updating.setVisible(false);
                                    list.remove(tloi);
                                    updateTable();
                                }
                            });
                        }
                    }
                }
            });

			options.appendChild(el);
			
			row.appendChild(n);
			row.appendChild(date);
			row.appendChild(st);
			row.appendChild(in);
			row.appendChild(out);
			row.appendChild(pval);
			row.appendChild(options);
			tloilist.appendChild(row);
		}
	};

	@UiHandler("toggle")
	void onToggleClicked(ClickEvent event) {
	    tableContainer.setVisible(!tableContainer.isVisible());
	}

	@UiHandler("updateButton")
	void onUpdateButtonClicked(ClickEvent event) {
	    updating.setVisible(true);
	    GWT.log("Checking " + this.parentHypothesisId + " - " + this.parentLOIId);
	    DiskREST.getNewExecution(parentHypothesisId, parentLOIId, new Callback<List<TriggeredLOI>, Throwable>() {
	        @Override
	        public void onSuccess (List<TriggeredLOI> result) {
	            updating.setVisible(false);
	            GWT.log("Success!");
	            if (result == null) GWT.log(" NULL");
	            else {
	                //list.add(result);
	                list = result;
	                updateTable();
	            }
	        }
	        @Override
	        public void onFailure (Throwable reason) {
	            updating.setVisible(false);
	            GWT.log("On update:", reason);
	            AppNotification.notifyFailure(reason.toString());
	        }
        });
	}

}
