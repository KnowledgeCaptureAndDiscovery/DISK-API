package org.diskproject.client.components.tree.events;

import com.google.gwt.event.shared.EventHandler;

public interface TreeItemSelectionHandler extends EventHandler {
  void onSelection(TreeItemSelectionEvent event);
}
