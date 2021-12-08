package org.diskproject.client.components.menu.events;

import com.google.gwt.event.shared.HandlerRegistration;

public interface HasMenuHandlers {
  public HandlerRegistration addMenuSelectionHandler(
      MenuSelectionHandler handler);
}
