package org.diskproject.client.components.list.events;

import com.google.gwt.event.shared.EventHandler;

public interface ListItemSelectionHandler extends EventHandler {
  void onSelection(ListItemSelectionEvent event);
}
