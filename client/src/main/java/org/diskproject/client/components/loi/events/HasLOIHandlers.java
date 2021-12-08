package org.diskproject.client.components.loi.events;

import com.google.gwt.event.shared.HandlerRegistration;

public interface HasLOIHandlers {
  public HandlerRegistration addLOISaveHandler(
      LOISaveHandler handler);
}
