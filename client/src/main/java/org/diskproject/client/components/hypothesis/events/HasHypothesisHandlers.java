package org.diskproject.client.components.hypothesis.events;

import com.google.gwt.event.shared.HandlerRegistration;

public interface HasHypothesisHandlers {
  public HandlerRegistration addHypothesisSaveHandler(
      HypothesisSaveHandler handler);
}
