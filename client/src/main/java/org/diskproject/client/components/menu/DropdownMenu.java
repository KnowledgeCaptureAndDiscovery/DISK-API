package org.diskproject.client.components.menu;

import java.util.List;

import org.diskproject.client.components.js.JSFunctions;
import org.diskproject.client.components.menu.events.HasMenuHandlers;
import org.diskproject.client.components.menu.events.MenuSelectionEvent;
import org.diskproject.client.components.menu.events.MenuSelectionHandler;

import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.vaadin.polymer.Polymer;
import com.vaadin.polymer.iron.widget.event.IronSelectEvent;
import com.vaadin.polymer.iron.widget.event.IronSelectEventHandler;
import com.vaadin.polymer.paper.PaperDropdownMenuElement;
import com.vaadin.polymer.paper.PaperListboxElement;
import com.vaadin.polymer.paper.widget.PaperDropdownMenu;
import com.vaadin.polymer.paper.widget.PaperListbox;

public class DropdownMenu extends PaperDropdownMenu 
    implements HasMenuHandlers {
  private HandlerManager handlerManager;

  List<String> items;
  String value;
  PaperListbox listbox;
  
  public DropdownMenu(String html) {
    super(html);
    this.initialize();
  }

  public DropdownMenu() {
    super();
    this.initialize();
  }
  
  private void initialize() {
    listbox = new PaperListbox();
    listbox.addStyleName("dropdown-content");
    listbox.addIronSelectHandler(new IronSelectEventHandler() {
      @Override
      public void onIronSelect(IronSelectEvent event) {
        fireEvent(new MenuSelectionEvent(null));
      }
    });   
    this.addListbox((PaperDropdownMenuElement)this.getElement(), 
        (PaperListboxElement) listbox.getElement());
    handlerManager = new HandlerManager(this);
  }
  
  private native void addListbox(PaperDropdownMenuElement menu, 
      PaperListboxElement listbox) /*-{
    $wnd.Polymer.dom(menu).appendChild(listbox);
  }-*/;
  
  public void setItems(List<String> items) {
    JSFunctions.setMenuItems((PaperListboxElement)listbox.getElement(), 
        Polymer.asJsArray(items));
    this.items = items;
  }
  
  @Override
  public String getValue() {
    String index = (String) listbox.getSelected();
    if(index == null)
      return null;
    
    return this.items.get(Integer.parseInt(index));    
  }
  
  @Override
  public void setValue(String val) {
    if(val == null) {
      listbox.setSelected(null);
    }
    else {
      Integer index = this.items.indexOf(val);
      listbox.setSelected(index+"");
    }
  }
  
  @Override
  public void fireEvent(GwtEvent<?> event) {
    handlerManager.fireEvent(event);
  }
  
  @Override
  public HandlerRegistration addMenuSelectionHandler(MenuSelectionHandler handler) {
    return handlerManager.addHandler(MenuSelectionEvent.TYPE, handler);
  }
}
