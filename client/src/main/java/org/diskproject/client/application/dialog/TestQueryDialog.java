package org.diskproject.client.application.dialog;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.diskproject.client.components.triples.SparqlInput;
import org.diskproject.client.rest.DiskREST;
import org.diskproject.shared.classes.util.KBConstants;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.core.client.Callback;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

public class TestQueryDialog extends DialogBox { //implements ClickHandler {
    interface Binder extends UiBinder<Widget, TestQueryDialog> {};
    private static Binder uiBinder = GWT.create(Binder.class);

    private String dataQuery, requestedVariables;
    @UiField SparqlInput sparqlQuery, sparqlVariables;
    @UiField DivElement resultContainer;
    @UiField ListBox dataSource;

    public TestQueryDialog() {
        setWidget(uiBinder.createAndBindUi(this));
        setText("Test data query");
        setAnimationEnabled(false);
        setModal(true);
        //setWidth("780px")
        //setHeight("90vh");
        //initialize();
    }

    private void initialize () { // FIXME: Load all vocabularies
        sparqlQuery.loadVocabulary("bio", KBConstants.OMICSURI(), new Callback<String, Throwable>() {
            @Override
            public void onSuccess(String result) {
                sparqlQuery.setValue(dataQuery);
                GWT.log("B");
            }
            @Override
            public void onFailure(Throwable reason) {
                GWT.log("C");
            }
        });
    }
    
    public void setDataSourceList(Map<String, String> nameToEndpoint) {
        dataSource.clear();
        for (String name: nameToEndpoint.keySet()) {
            dataSource.addItem(name.replace("_", " "), nameToEndpoint.get(name));
        }
    }

    public void showAndCenter () {
        show();
        center();
    }

    public void setDataQuery (String dq) {
        dataQuery = dq;
        sparqlQuery.setValue(dataQuery);
    }

    public void setVariables (String variables) {
        requestedVariables = variables;
        sparqlVariables.setValue(requestedVariables);
    }
    
    public void setEndpoint (String endpointName) {
        if (endpointName != null) {
        	int dataLen = dataSource.getItemCount();
        	for (int i = 0; i < dataLen; i++) {
        		String curname = dataSource.getItemText(i);
        		if (curname.equals(endpointName)) {
        			dataSource.setSelectedIndex(i);
        			break;
        		}
        	}
        }
        
    }

    @UiHandler("cancelButton")
    void cancelButtonClicked(ClickEvent event) {
        hide();
    }

    @UiHandler("sendButton")
    void sendButtonClicked(ClickEvent event) {
        /*if (!sparqlQuery.validate()) {
          return;
        }*/

        String query = sparqlQuery.getValue();
        String variables = sparqlVariables.getValue();
        String endpoint = dataSource.getSelectedValue();
        resultContainer.setInnerHTML("Loading...");
        DiskREST.queryExternalStore(endpoint, query, variables, new Callback<Map<String, List<String>>, Throwable>() {
            @Override
            public void onSuccess(Map<String, List<String>> result) {
                renderResults(result);
            }
            @Override
            public void onFailure(Throwable reason) {
                resultContainer.setInnerHTML("An error occurred while executing the query. Please try again.");
            }
        });
    }

    private void renderResults (Map<String, List<String>> results) {
        if (results != null) {
            Set<String> vars = results.keySet();
            if (vars != null && vars.size() > 0) {
                int lines = 0;
                String html = "<table class=\"pure-table\"><thead><tr>";
                for (String v: vars) {
                    html += "<th><b>" + v + "</b></th>";
                    if (results.get(v).size() > lines) {
                        lines = results.get(v).size();
                    }
                }
                html += "</tr></thead><tbody>";

                for (int i = 0; i < lines; i++) {
                    html += "<tr>";
                    for (String v: vars) {
                        String url = results.get(v).get(i).replace(
                            "http://localhost:8080/enigma_new/index.php/Special:URIResolver/",
                            "http://organicdatapublishing.org/enigma_new/index.php/");
                        if (url.contains("http")) {
                            String parts[] = url.split("/");
                            String name = (parts.length>3) ?
                                    ((parts[parts.length-1].length() > 0) ? parts[parts.length-1] : parts[parts.length-2])
                                    : url;
                            html += "<td><a href=\"" + url + "\" target=\"_blank\">" + name + "</a></td>";
                        } else {
                            html += "<td>" + url + "</td>";
                        }
                    }
                    html += "</tr>";
                }
                html += "</table>";
                resultContainer.setInnerHTML("<table style=\"width:100%\">" + html + "</table>");
                setHeight("90vh");
                center();
                return;
            }
        }
        resultContainer.setInnerHTML("No results found. Please check your query and try again.");
    }
}