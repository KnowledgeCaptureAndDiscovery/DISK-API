package org.diskproject.client.application.dialog;

import org.diskproject.client.rest.DiskREST;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.IFrameElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.polymer.paper.widget.PaperSpinner;

public class ShinyElement extends Composite {
    interface Binder extends UiBinder<Widget, ShinyElement> {};
    private static Binder uiBinder = GWT.create(Binder.class);
    
    private String curConfig;

    @UiField IFrameElement iframe;
	@UiField PaperSpinner loading;

    public ShinyElement() {
		initWidget(uiBinder.createAndBindUi(this)); 
    }
    
    public void load (String shinyConfigURL) {
        String URL = shinyConfigURL.replaceAll("^.*#", "");
        if (curConfig == null || !curConfig.equals(URL)) {
            curConfig = URL;
            loading.setVisible(true);
            DiskREST.getDataFromWingsAsJS(URL, new Callback<JavaScriptObject, Throwable>() {
                @Override
                public void onSuccess(JavaScriptObject result) {
                    String shinyURL = getShinyURL(result);
                    if (shinyURL != null && !shinyURL.equals("")) {
                        iframe.setSrc(shinyURL);
                        loading.setVisible(false);
                    }
                }
                
                @Override
                public void onFailure(Throwable reason) {
                    // TODO Auto-generated method stub
                    loading.setVisible(false);
                }
            });
        }
    }

  	private static native String getShinyURL(JavaScriptObject shinyobj) /*-{
  		return shinyobj && shinyobj.url ? shinyobj.url : "";
	}-*/;
}