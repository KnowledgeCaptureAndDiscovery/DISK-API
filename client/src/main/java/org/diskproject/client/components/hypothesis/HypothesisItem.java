package org.diskproject.client.components.hypothesis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.diskproject.client.components.searchpanel.SearchableItem;
import org.diskproject.client.place.NameTokens;
import org.diskproject.client.rest.AppNotification;
import org.diskproject.client.rest.DiskREST;
import org.diskproject.shared.classes.common.TreeItem;
import org.diskproject.shared.classes.loi.TriggeredLOI;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.LabelElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.vaadin.polymer.paper.widget.PaperIconButton;

public class HypothesisItem extends SearchableItem {
	interface Binder extends UiBinder<Widget, HypothesisItem> {};
	private static Binder uiBinder = GWT.create(Binder.class);
	
	@UiField DivElement title, description, info, executions;
	@UiField PaperIconButton editButton, deleteButton;
	@UiField LabelElement tloiLabel;
	private String strTitle, strDescription, strAuthor, strDate;
	private String id;
	private static String username, domain;
	private Map<String, ExecutionList> executionLists;
	
	public HypothesisItem(String id) {
		initWidget(uiBinder.createAndBindUi(this)); 
		executionLists = new HashMap<String, ExecutionList>();
		this.id = id;
		//This is necessary as we are not adding this elements in the html.
		super.onAttach();
	}
	
	public static void setUsenameAndDomain (String username, String domain) {
		HypothesisItem.username = username;
		HypothesisItem.domain = domain;
		ExecutionList.setUsenameAndDomain(username, domain);
	}

	public String getTextRepresentation() {
		String repr = "";
		if (strTitle != null) repr += strTitle;
		if (strDescription != null) repr += strDescription;
		if (strAuthor != null) repr += strAuthor;
		return repr;
	}
	
	public String getCreationDate () {
	    return this.strDate;
	}
	
	public String getId () {
	    return this.id;
	}
	
	public void setTitle (String newTitle) {
		strTitle = newTitle;
		title.setInnerText(newTitle);
	}
	
	public void setDescription (String desc) {
		strDescription =  desc;
		description.setInnerText(desc);
	}
	
	public void setInfo (String author, String date) {
		strAuthor = author;
		strDate = date;
		SpanElement authSpan = SpanElement.as(DOM.createSpan());
		SpanElement dateSpan = SpanElement.as(DOM.createSpan());
		authSpan.setInnerText(author);
		dateSpan.setInnerText(date);
		info.removeAllChildren();
		info.appendChild(authSpan);
		info.appendChild(dateSpan);
	}
	
	public void load (TreeItem item) {
		setTitle(item.getName());
		setDescription(item.getDescription());
		tloiLabel.setInnerText("No line of inquiry matches this hypothesis."); //FIXME: change this message

		setInfo(item.getAuthor(), item.getCreationDate());
	}

	public void addExecutionList (String loiid, List<TriggeredLOI> tloilist) {
		if (tloilist == null || tloilist.size() < 1) {
			tloiLabel.setInnerText("No line of inquiry matches this hypothesis.");
			return;
		}
		if (!executionLists.containsKey(loiid)) {
			ExecutionList n = new ExecutionList(this.getId(), loiid);
			executionLists.put(loiid, n);
			executions.appendChild(n.getElement());
		}
		ExecutionList l = executionLists.get(loiid);
		//FIXME: get infor from LOI
		l.setTitle(tloilist.get(0).getName().replace("Triggered: ", ""));
		l.setTitle(tloilist.get(0).getDescription());
		l.setList(tloilist);
		tloiLabel.setInnerText("This hypothesis has " + executionLists.size() + " matching lines of inquiry:");
	}

	@UiHandler("editButton")
	void onEditButtonClicked(ClickEvent event) {
		String token = NameTokens.hypotheses + "/" + HypothesisItem.username +"/" + HypothesisItem.domain + "/" + this.id;
		History.newItem(token); 
	}

	@UiHandler("deleteButton")
	void onDelButtonClicked(ClickEvent event) {
	    HypothesisItem me = this;
		if (Window.confirm("Are you sure you want to delete " + this.strTitle)) {
          DiskREST.deleteHypothesis(this.id, new Callback<Void, Throwable>() {
            @Override
            public void onFailure(Throwable reason) {
              AppNotification.notifyFailure(reason.getMessage());
            }
            @Override
            public void onSuccess(Void result) {
              AppNotification.notifySuccess("Deleted", 500);
              //TODO: do some kind of update to remove this element
              me.setVisible(false);
            }
          });
        }
	}
}
