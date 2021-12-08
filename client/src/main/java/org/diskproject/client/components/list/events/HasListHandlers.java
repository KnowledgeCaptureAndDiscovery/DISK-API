package org.diskproject.client.components.list.events;

import com.google.gwt.event.shared.HandlerRegistration;

public interface HasListHandlers {
  public HandlerRegistration addListItemSelectionHandler(
      ListItemSelectionHandler handler);
  public HandlerRegistration addListItemActionHandler(
      ListItemActionHandler handler);  
}
