package org.diskproject.client.components.menu.events;

import com.google.gwt.event.shared.EventHandler;

public interface MenuSelectionHandler extends EventHandler {
  void onMenuSelection(MenuSelectionEvent event);
}
