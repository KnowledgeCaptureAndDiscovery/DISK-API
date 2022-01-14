

package org.diskproject.client.application.terminology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.diskproject.client.application.ApplicationSubviewImpl;
import org.diskproject.client.components.list.ListNode;
import org.diskproject.client.components.loader.Loader;
import org.diskproject.client.components.triples.TripleInput;
import org.diskproject.client.rest.AppNotification;
import org.diskproject.client.rest.DiskREST;
import org.diskproject.client.Config;
import org.diskproject.client.Utils;
import org.diskproject.shared.classes.common.Graph;
import org.diskproject.shared.classes.common.Triple;
import org.diskproject.shared.classes.common.Value;
import org.diskproject.shared.classes.util.KBConstants;
import org.diskproject.shared.classes.vocabulary.Individual;
import org.diskproject.shared.classes.vocabulary.Type;
import org.diskproject.shared.classes.vocabulary.Vocabulary;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.TableSectionElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.vaadin.polymer.Polymer;
import com.vaadin.polymer.elemental.Function;

public class MyTerminologyView extends ApplicationSubviewImpl implements
		MyTerminologyPresenter.MyView {

	String userid;
	String domain;
	boolean editmode;

	int loadcount = 0;

	@UiField Loader loader;
	@UiField HTMLPanel form;
	//@UiField ListWidget datalist;
	@UiField TripleInput triples;
	
	@UiField InputElement inputName;
	@UiField ListBox inputType;

	@UiField CheckBox toggleTriples;
	List<Triple> loadedTriples;

	@UiField TableSectionElement table;
	@UiField TableRowElement inputRow;
	private static Map<String, String> tableLabel;
	private static Map<String, String> tableType;

	Vocabulary vocab;

	Graph graph;

	@UiHandler("toggleTriples")
	void onClickToggleTriples(ClickEvent event) {
		boolean show = toggleTriples.getValue();
		triples.setVisible(show);
	}

	interface Binder extends UiBinder<Widget, MyTerminologyView> {
	}

	@Inject
	public MyTerminologyView(Binder binder) {
		initWidget(binder.createAndBindUi(this));
	}


	void loadVocabularies() {
		loadcount = 0;
		triples.loadVocabulary("bio", KBConstants.OMICSURI(), vocabLoaded);
		triples.loadVocabulary("neuro", KBConstants.NEUROURI(), vocabLoaded);
		triples.loadVocabulary("hyp", KBConstants.HYPURI(), vocabLoaded);
		triples.loadVocabulary("disk", KBConstants.DISKURI(), vocabLoaded);
	}

	private Callback<String, Throwable> vocabLoaded = new Callback<String, Throwable>() {
		public void onSuccess(String result) {
			loadcount++;
			if (loadcount == 4) {
				String[] prefixes = {"bio", "neuro", "hyp", "disk", ""};
				vocab = new Vocabulary();
				for (String p: prefixes)
					vocab.mergeWith(triples.getVocabulary(p));
				/*vocab.mergeWith(triples.getVocabulary("bio"));
				vocab.mergeWith(triples.getVocabulary("neuro"));
				vocab.mergeWith(triples.getVocabulary("hyp"));
				vocab.mergeWith(triples.getVocabulary("disk"));
				vocab.mergeWith(triples.getVocabulary(""));*/
				vocab.refreshChildren();

				
				inputType.clear();
				inputType.addItem("- None -", "");
				for (String p: prefixes) {
					Set<String> keys = triples.getVocabulary(p).getTypes().keySet();
					for (String k: keys) {
						//String id = triples.getVocabulary(p).getTypes().get(k).getName();
						String label = triples.getVocabulary(p).getTypes().get(k).getLabel();
						inputType.addItem("[" + p + "] " + label, k);
					}
				}

				if (graph != null)
					showAssertions();
			}
		}

		public void onFailure(Throwable reason) {
		}
	};

	@Override
	public void initializeParameters(String userid, String domain,
			String[] params, boolean edit, SimplePanel sidebar,
			SimplePanel toolbar) {

		clear();

		 if(this.userid != userid || this.domain != domain) {
		      this.userid = userid;
		      this.domain = domain;
		      DiskREST.setDomain(domain);
		      DiskREST.setUsername(userid);
		      this.loadVocabularies();
		    }
		    this.loadAssertions();
		    
		    this.setHeader(toolbar);    
		    this.setSidebar(sidebar);
		  }

	private void clear() {
		loader.setVisible(false);
		//datalist.setVisible(false);
		form.setVisible(false);
	}

	void loadAssertions() {
		loader.setVisible(true);
		triples.setVisible(false);
		DiskREST.listAssertions(new Callback<Graph, Throwable>() {
		  @Override
		  public void onSuccess(final Graph result) {
			loader.setVisible(false);
			form.setVisible(true);
			graph = result;
			if (graph != null) {
				loadedTriples = graph.getTriples();
				loadTableData(loadedTriples);
				if (loadcount == 4) showAssertions();
			}
		  }
		  @Override
		  public void onFailure(Throwable reason) {
			loader.setVisible(false);
			AppNotification.notifyFailure(reason.getMessage());
		  }      
		});
	}

	private void showAssertions() {
		Polymer.ready(triples.getElement(), new Function<Object, Object>() {
			@Override
			public Object call(Object arg) {
				// Show triples in the editor
				if (graph != null) {
					triples.setVisible(true);
					triples.setValue(graph.getTriples());
				}

				// Show data list
				//datalist.clear();
				showDataList(); 

				loader.setVisible(false);
				//datalist.setVisible(true);
				
				if (!toggleTriples.getValue()) {
					triples.setVisible(false);
				}

				return null;
			}
		});
	}

	private void loadTableData (List<Triple> G) {
		tableLabel = new HashMap<String, String>();
		tableType = new HashMap<String, String>();
		for (Triple t: G) {
			String s = t.getSubject().toString();
			String p = t.getPredicate();
			String o = t.getObject().getValue().toString();
			if (p.contentEquals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
				tableType.put(s, o);
			} else if (p.contentEquals("http://www.w3.org/2000/01/rdf-schema#label")) {
				tableLabel.put(s, o);
			}
			GWT.log(s + "  " + p + "  " + t.getObject().toString());
		}
		renderTable(tableLabel,tableType);
	}

	private void renderTable (Map<String, String> labels, Map<String, String> types) {
		// CLEAN TABLE 
		table.removeAllChildren();
		
		// NEW ELEMENTS
		Set<String> ids = labels.keySet();
		for (String id: ids) {
			TableRowElement row = Document.get().createTRElement();
			TableCellElement label = Document.get().createTDElement();
			TableCellElement info = Document.get().createTDElement();
			TableCellElement type = Document.get().createTDElement();
			label.setInnerText(tableLabel.get(id));
			String t = types.get(id);
			if (t != null) {
				type.setInnerText(Utils.extractPrefix(t));
				info.setInnerText("is a");
			}
			row.appendChild(label);
			row.appendChild(info);
			row.appendChild(type);
			table.appendChild(row);
		}
		
		table.appendChild(inputRow);
	}

	private void showDataList() {
		List<Individual> datasets = new ArrayList<Individual>();
		String parentTypeId = KBConstants.DISKNS() + "Data";
		Type topDataType = vocab.getType(parentTypeId);
		for (Type subtype : vocab.getSubTypes(topDataType)) {
			datasets.addAll(vocab.getIndividualsOfType(subtype));
		}
		for (Individual dataset : datasets) {
			String dname = "<b>" + dataset.getName() + "</b>";
			dname += " ( " + dataset.getType().replaceAll("^.*#", "") + " )";
			ListNode node = new ListNode(dataset.getId(), new HTML(dname));
			node.setIcon("icons:list");
			node.setIconStyle("blue");
			//datalist.addNode(node);
		}
	}

	@UiHandler("savebutton")
	void onSaveButtonClicked(ClickEvent event) {
		this.graph.setTriples(triples.getTriples());
		// GWT.log(graph.getTriples().toString());
		if (!this.triples.validate()) {
			AppNotification.notifyFailure("Please fix errors before saving");
			return;
		}
		AppNotification.notifyLoading("Saving data and running queries");
		DiskREST.updateAssertions(graph, new Callback<Void, Throwable>() {
			@Override
			public void onSuccess(Void result) {
				AppNotification.stopShowing();
				AppNotification.notifySuccess("Saved", 1000);
			}

			@Override
			public void onFailure(Throwable reason) {
				AppNotification.stopShowing();
				AppNotification.notifyFailure("Could not save: "
						+ reason.getMessage());
			}
		});
	}

  static String toVarName (String stdname) {
	  String[] parts = stdname.split(" ");
	  String endString = "";
	  Boolean first = true;
	  for (String p: parts) {
		  if (first) {
			  endString += p;
			  first = false;
		  } else {
			  endString += p.substring(0, 1).toUpperCase() + p.substring(1).toLowerCase();
		  }
	  }
	  return endString;
  }

	@UiHandler("addterm")
	void onAddTermButtonClicked(ClickEvent event) {
		String name = inputName.getValue();
		String id = toVarName(name);
		if (id == "" || name == "") {
		    AppNotification.notifyFailure("You must add a name to any new term.");
			return;
		}

		String fullid = Config.getServerURL() + "/" + Config.getWingsUserid() + "/" + Config.getWingsDomain() +"/assertions#" + id;
		List<Triple> allt = triples.getTriples();
		for (Triple t: allt) {
			if (t.getSubject() == fullid) {
				AppNotification.notifyFailure("You can not use the same identifier.");
				return;
			}
		}
		
		// Adding labels and types to table.
		List<Triple> mergedList = triples.getTriples();
		Value v = new Value(name,"http://www.w3.org/2001/XMLSchema#string");
		mergedList.add(new Triple(fullid, "http://www.w3.org/2000/01/rdf-schema#label",v,null));
		String selectedType = inputType.getSelectedValue();
		if (selectedType != null && selectedType.length() > 1) {
			v = new Value(selectedType);
			mergedList.add(new Triple(fullid, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",v,null));
		}

		loadTableData(mergedList);
		triples.setValue(mergedList);
		
		
		// Clear inputs
		inputName.setValue("");
		inputType.setValue(0,"");
	}

	@UiHandler("discardbutton")
	void onDiscardButtonClicked(ClickEvent event) {
		if (loadedTriples != null) {
			triples.setValue(loadedTriples);
		    loadTableData(loadedTriples);
		}
	}

	private void setHeader(SimplePanel toolbar) {
		// Set Toolbar header
		toolbar.clear();
		String title = "<h3>My Terminology</h3>";
		String icon = "icons:chrome-reader-mode";

		HTML div = new HTML("<nav><div class='layout horizontal center'>"
				+ "<iron-icon class='blue' icon='" + icon + "'/></div></nav>");
		div.getElement().getChild(0).getChild(0)
				.appendChild(new HTML(title).getElement());
		toolbar.add(div);
	}

	private void setSidebar(SimplePanel sidebar) {
		// TODO: Modify sidebar
	}
}
