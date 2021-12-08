package org.diskproject.client.application.dialog;

import java.util.Map;

import org.diskproject.client.rest.DiskREST;
import org.diskproject.shared.classes.loi.TriggeredLOI;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

public class Narrative extends Composite {
    interface Binder extends UiBinder<Widget, Narrative> {};
    private static Binder uiBinder = GWT.create(Binder.class);
    
    @UiField HTMLPanel container;

    public Narrative (TriggeredLOI tloi) {
        initWidget(uiBinder.createAndBindUi(this)); 
        nativeLog(tloi);

        /*String loiname = tloi.getName().replace("Triggered: ", "");
        String all = "The Hypothesis with title: <b>" + "HYP NAME"
                   + "</b> was runned <span class=\"" + tloi.getStatus() + "\">" 
                   + tloi.getStatus() + "</span>"
                   + " with the Line of Inquiry: <b>" + loiname
                   + "</b>. The LOI triggered the <a target=\"_blank\" href=\"" //+ wf.getWorkflowLink() 
                   + "\">workflow on WINGS</a>"
                   + " where it was tested with the following datasets:<ul>" + "[DATASETS]"
                   + "</ul>The resulting p-value is " 
                   + ExecutionList.decimalFormat.format(tloi.getConfidenceValue()) + ".";
        
        container.clear();
        container.getElement().setInnerHTML(all);*/
        
        load(tloi.getId());
    }
    
    public void load (String tloiid) {
        DiskREST.getTLOINarratives(tloiid, new Callback<Map<String,String>, Throwable>() {
            @Override
            public void onSuccess (Map<String, String> result) {
                container.clear();
                container.getElement().setInnerHTML(result.get("execution"));
            }
            
            @Override
            public void onFailure (Throwable reason) {
                // TODO Auto-generated method stub
            }
        });
    }
    
    private native void  nativeLog(Object obj) /*-{
        console.log(obj);
    }-*/;
}