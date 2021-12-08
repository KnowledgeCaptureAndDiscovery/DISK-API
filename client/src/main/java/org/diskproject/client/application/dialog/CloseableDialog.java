package org.diskproject.client.application.dialog;

import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.EventListener;
import com.vaadin.polymer.iron.widget.IronIcon;

@SuppressWarnings("deprecation")
public class CloseableDialog extends DialogBox {
    public CloseableDialog() {
        super();
        setGlassEnabled(true);
        
        IronIcon closeIcon = new IronIcon();
        closeIcon.setIcon("close");
        Element closeElement = closeIcon.getElement();
        Event.sinkEvents(closeElement, Event.ONCLICK);
        Event.setEventListener(closeElement, new EventListener() {
          @Override
          public void onBrowserEvent(Event event) {
              hide();
          }
        });
        closeIcon.setStyle("position: absolute; right: 8px; top: 4px; cursor: pointer;");
        
        try {
             getElement().getFirstChild().getFirstChild().getFirstChild().getFirstChild().getChild(1).getFirstChild().appendChild(closeElement);
        } catch (Exception e) {
             getElement().appendChild(closeElement);
        }
         
    }
    
    public void centerAndShow () {
        center();
        show();
    }
}