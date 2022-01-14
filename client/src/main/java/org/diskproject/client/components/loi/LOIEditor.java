package org.diskproject.client.components.loi;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.diskproject.client.application.dialog.TestQueryDialog;
import org.diskproject.client.components.loi.events.HasLOIHandlers;
import org.diskproject.client.components.loi.events.LOISaveEvent;
import org.diskproject.client.components.loi.events.LOISaveHandler;
import org.diskproject.client.components.question.QuestionSelector;
import org.diskproject.client.components.triples.SparqlInput;
import org.diskproject.client.rest.AppNotification;
import org.diskproject.client.rest.DiskREST;
import org.diskproject.shared.classes.loi.LineOfInquiry;
import org.diskproject.shared.classes.util.KBConstants;
import org.diskproject.shared.classes.workflow.Workflow;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.polymer.paper.PaperInputElement;
import com.vaadin.polymer.paper.PaperTextareaElement;

public class LOIEditor extends Composite implements HasLOIHandlers {
    private HandlerManager handlerManager;

    interface Binder extends UiBinder<Widget, LOIEditor> {};

    String userid, domain;

    boolean editmode;
    boolean metamode;
    int loadcount=0;

    @UiField PaperInputElement name, metaVariables;
    @UiField PaperTextareaElement description, notes, explanation;
    @UiField SparqlInput hypothesisQuery;
    @UiField SparqlInput dataQuery;
    @UiField LOIWorkflowList workflowlist, metaworkflowlist;
    @UiField QuestionSelector questionSelector;
    @UiField ListBox dataSource;

    LineOfInquiry loi;

    private Map<String, String> nameToEndpoint, endpointToName;

    private static Binder uiBinder = GWT.create(Binder.class);

    public LOIEditor() {
        initWidget(uiBinder.createAndBindUi(this));
        handlerManager = new HandlerManager(this);    
    }

    public void initialize(String userid, String domain) {
        this.userid = userid;
        this.domain = domain;
        this.loadVocabularies();
        this.loadWorkflows();
        this.questionSelector.setParent(this);
        this.loadDataOptions();
    }

    private void loadDataOptions () {
        dataSource.clear();
        dataSource.addItem("Loading...", "");

        DiskREST.getEndpoints(new Callback<Map<String,String>, Throwable>() {
        	@Override
            public void onSuccess(Map<String, String> result) {
                nameToEndpoint = result;
                endpointToName = new HashMap<String, String>();
                for (String name: result.keySet()) {
                	endpointToName.put(result.get(name), name.replace("_", " "));
                }
                updateDataOptions();
                
                //Add endpoints to vocabulary
            }

            @Override
            public void onFailure(Throwable reason) {
                AppNotification.notifyFailure(reason.getMessage());
                dataSource.clear();
                dataSource.addItem("Error!", "");
            }
        });
    }
    
    private void updateDataOptions () {
    	if (nameToEndpoint == null)
    		return;

    	dataSource.clear();
    	String selected = this.loi != null ? this.loi.getDataSource() : null;
    	int selectedIndex = -1, 
    		count = 0;

    	for (String name: nameToEndpoint.keySet()) {
    		String url = nameToEndpoint.get(name);
    		dataSource.addItem(name.replace("_", " "), url);
    		if (selected != null && selected.equals(url)) 
    			selectedIndex = count;
    		count += 1;
    	}

    	if (selectedIndex >= 0)
    		dataSource.setSelectedIndex(selectedIndex);
    }


    public void load(LineOfInquiry loi) {
        this.loi = loi;
        name.setValue(loi.getName());
        description.setValue(loi.getDescription());
        notes.setValue(loi.getNotes());
        explanation.setValue(loi.getExplanation());

        String ds = loi.getDataSource();
        if (ds != null && endpointToName != null && endpointToName.containsKey(ds)) {
        	int dataLen = dataSource.getItemCount();
        	String selectedName = endpointToName.get(ds);
        	
        	for (int i = 0; i < dataLen; i++) {
        		String curname = dataSource.getItemText(i);
        		if (curname.equals(selectedName)) {
        			dataSource.setSelectedIndex(i);
        			break;
        		}
        	}
        }

        if(loi.getHypothesisQuery() != null && loadcount==7) {
            hypothesisQuery.setValue(loi.getHypothesisQuery());
        }
        if(loi.getDataQuery() != null && loadcount==7) {
            dataQuery.setValue(loi.getDataQuery());    
        }
        if (loi.getRelevantVariables() != null) {
            metaVariables.setValue(loi.getRelevantVariables());
        } else {
            metaVariables.setValue("");
        }
        workflowlist.loadBindingsList(loi.getWorkflows());
        metaworkflowlist.loadBindingsList(loi.getMetaWorkflows());  
        String q = loi.getQuestion();
        if (q != null) questionSelector.setQuestion(q);
    }

    public void setNamespace(String ns) {
        hypothesisQuery.setDefaultNamespace(ns);
        dataQuery.setDefaultNamespace(ns);
    }

    private void loadVocabularies() {
        loadcount=0;
        hypothesisQuery.loadVocabulary("bio", KBConstants.OMICSURI(), vocabLoaded);
        hypothesisQuery.loadVocabulary("neuro", KBConstants.NEUROURI(), vocabLoaded);
        hypothesisQuery.loadVocabulary("hyp", KBConstants.HYPURI(), vocabLoaded);
        hypothesisQuery.loadVocabulary("disk", KBConstants.DISKURI(), vocabLoaded);

        dataQuery.loadVocabulary("bio", KBConstants.OMICSURI(), vocabLoaded);
        dataQuery.loadVocabulary("neuro", KBConstants.NEUROURI(), vocabLoaded);
        dataQuery.loadVocabulary("hyp", KBConstants.HYPURI(), vocabLoaded);
    }

    private Callback<String, Throwable> vocabLoaded = 
        new Callback<String, Throwable>() {
            public void onSuccess(String result) {
                loadcount++;
                if (loi != null && loi.getHypothesisQuery() != null && loadcount==7) {
                    hypothesisQuery.setValue(loi.getHypothesisQuery());
                }
                if (loi != null && loi.getDataQuery() != null && loadcount==7) {
                    dataQuery.setValue(loi.getDataQuery());
                }
            }
            public void onFailure(Throwable reason) {}
        };

    private void loadWorkflows() {
        DiskREST.listWorkflows(new Callback<List<Workflow>, Throwable>() {
        	@Override
            public void onSuccess(List<Workflow> result) {
                Collections.sort(result, new Comparator<Workflow>() {
                	@Override
                    public int compare(Workflow o1, Workflow o2) {
                        return o1.getName().compareTo(o2.getName());
                    }
                });
                workflowlist.setWorkflowList(result);
                metaworkflowlist.setWorkflowList(result);
                metaworkflowlist.setWorkflowSource(workflowlist);
            }      
            @Override
            public void onFailure(Throwable reason) {
                AppNotification.notifyFailure(reason.getMessage());
            }
        });
    }

    @UiHandler("savebutton")
	void onSaveButtonClicked(ClickEvent event) {   
		boolean ok1 = this.name.validate();
		boolean ok2 = this.description.validate();
		boolean ok3 = this.hypothesisQuery.validate();
		boolean ok4 = true;// || this.dataQuery.validate();
		String db = this.dataSource.getSelectedValue();

		if(!ok1 || !ok2 || !ok3 || !ok4 || db == null) {
			AppNotification.notifyFailure("Please fix errors before saving");
			return;
		}

		loi.setDescription(description.getValue());
		loi.setDataSource(db);
		loi.setNotes(notes.getValue());
		loi.setExplanation(explanation.getValue());
		loi.setName(name.getValue());
		loi.setHypothesisQuery(hypothesisQuery.getValue());
		loi.setDataQuery(dataQuery.getValue());
		loi.setWorkflows(workflowlist.getBindingsList());
		loi.setMetaWorkflows(metaworkflowlist.getBindingsList());
		loi.setRelevantVariables(metaVariables.getValue());
		loi.setQuestion(questionSelector.getSelectedQuestion());

		fireEvent(new LOISaveEvent(loi));
	}

    @UiHandler("testbutton")
	void onTestButtonClicked(ClickEvent event) {   

		/** TODO: new test */
		String wfvars = "";
		TestQueryDialog dialog = new TestQueryDialog();
		dialog.show();
		dialog.setDataSourceList(nameToEndpoint);
		dialog.setDataQuery(loi.getDataQuery());
		dialog.setVariables(wfvars != "" ? wfvars : "*");
		dialog.setEndpoint(dataSource.getSelectedItemText());
		dialog.center();
	}  

    @Override
	public void fireEvent(GwtEvent<?> event) {
		handlerManager.fireEvent(event);
	}

    @Override
	public HandlerRegistration addLOISaveHandler(
			LOISaveHandler handler) {
		return handlerManager.addHandler(LOISaveEvent.TYPE, handler);
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

    public void setHypothesis (String hypothesis) {
        hypothesisQuery.setStringValue(hypothesis);

    }

}
