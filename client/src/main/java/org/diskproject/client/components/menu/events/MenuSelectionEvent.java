package org.diskproject.client.components.menu.events;

import com.google.gwt.event.shared.GwtEvent;

public class MenuSelectionEvent extends GwtEvent<MenuSelectionHandler> {

  public static Type<MenuSelectionHandler> TYPE = new Type<MenuSelectionHandler>();

  private final String value;

  public MenuSelectionEvent(String value) {
    this.value = value;
  }

  @Override
  public Type<MenuSelectionHandler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(MenuSelectionHandler handler) {
    handler.onMenuSelection(this);
  }

  public String getValue() {
    return this.value;
  }
}
