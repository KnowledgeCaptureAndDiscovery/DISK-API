package org.diskproject.client.components.tree.events;

import com.google.gwt.event.shared.HandlerRegistration;

public interface HasTreeHandlers {
  public HandlerRegistration addTreeItemSelectionHandler(
      TreeItemSelectionHandler handler);
  public HandlerRegistration addTreeItemActionHandler(
      TreeItemActionHandler handler);  
}
