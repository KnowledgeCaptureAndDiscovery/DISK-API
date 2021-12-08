package org.diskproject.client.components.list.events;

import com.google.gwt.event.shared.EventHandler;

public interface ListItemActionHandler extends EventHandler {
  void onAction(ListItemActionEvent event);
}
