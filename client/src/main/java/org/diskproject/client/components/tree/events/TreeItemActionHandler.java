package org.diskproject.client.components.tree.events;

import com.google.gwt.event.shared.EventHandler;

public interface TreeItemActionHandler extends EventHandler {
  void onAction(TreeItemActionEvent event);
}
