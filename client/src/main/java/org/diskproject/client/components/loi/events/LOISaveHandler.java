package org.diskproject.client.components.loi.events;

import com.google.gwt.event.shared.EventHandler;

public interface LOISaveHandler extends EventHandler {
  void onSave(LOISaveEvent event);
}
