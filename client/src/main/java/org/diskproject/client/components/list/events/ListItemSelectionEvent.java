package org.diskproject.client.components.list.events;

import org.diskproject.client.components.list.ListNode;
import com.google.gwt.event.shared.GwtEvent;

public class ListItemSelectionEvent extends GwtEvent<ListItemSelectionHandler> {

  public static Type<ListItemSelectionHandler> TYPE = new Type<ListItemSelectionHandler>();

  private final ListNode node;

  public ListItemSelectionEvent(ListNode node) {
    this.node = node;
  }

  @Override
  public Type<ListItemSelectionHandler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(ListItemSelectionHandler handler) {
    handler.onSelection(this);
  }

  public ListNode getItem() {
    return this.node;
  }
}
