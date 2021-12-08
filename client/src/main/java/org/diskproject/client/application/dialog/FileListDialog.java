package org.diskproject.client.application.dialog;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

public class FileListDialog extends DialogBox { //implements ClickHandler {
    interface Binder extends UiBinder<Widget, FileListDialog> {};
    private static Binder uiBinder = GWT.create(Binder.class);

    private List<String> list;
    @UiField HTMLPanel listContainer, listHeader;

    public FileListDialog() {
        setWidget(uiBinder.createAndBindUi(this));

        setAnimationEnabled(false);
        setModal(true);
        setText("File list");

        //setWidth("780px")
        //setHeight("90vh");
        //initialize();
    }
    
    public void setName (String name) {
        setText(name);
    }
    
    public void setHeader (String header) {
        listHeader.add(new HTML(header));
    }
    
    public void setFileList (List<String> filelist) {
        this.list = filelist;
        this.update();
    }
    
    private void update () {
        listContainer.clear();
        for (String file: this.list) {
			AnchorElement link = AnchorElement.as(DOM.createAnchor());
			String id = file.replaceAll("^.*#", "");
			String name = id.replaceAll("^SHA[a-zA-Z0-9]{6}_", "")
			                .replaceAll("\\-[a-z0-9]{25}$", "")
			                .replaceAll("_", " ");
			if (name.equals("age vs allele") || name.equals("forest plot"))
			    name += ".pdf";

			String dl = "https://enigma-disk.wings.isi.edu/wings-portal/users/admin/test/data/fetch?data_id="
			          + file.replace(":", "%3A").replace("#", "%23");
			link.setInnerText(name);
			link.setHref(dl);
			link.setClassName("link-element");
			link.setTarget("_blank");
            listContainer.getElement().appendChild(link);
        }
    }

    @UiHandler("closeButton")
    void onCloseButtonClicked(ClickEvent event) {
        hide();
    }
}