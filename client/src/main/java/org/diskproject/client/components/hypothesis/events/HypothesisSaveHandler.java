package org.diskproject.client.components.hypothesis.events;

import com.google.gwt.event.shared.EventHandler;

public interface HypothesisSaveHandler extends EventHandler {
  void onSave(HypothesisSaveEvent event);
}
