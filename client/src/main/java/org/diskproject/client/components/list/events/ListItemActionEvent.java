package org.diskproject.client.components.list.events;

import org.diskproject.client.components.list.ListAction;
import org.diskproject.client.components.list.ListNode;

import com.google.gwt.event.shared.GwtEvent;

public class ListItemActionEvent extends GwtEvent<ListItemActionHandler> {

  public static Type<ListItemActionHandler> TYPE = new Type<ListItemActionHandler>();

  private final ListAction action;
  private final ListNode node;

  public ListItemActionEvent(ListNode node, ListAction action) {
    this.node = node;
    this.action = action;
  }

  @Override
  public Type<ListItemActionHandler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(ListItemActionHandler handler) {
    handler.onAction(this);
  }

  public ListNode getItem() {
    return this.node;
  }
  
  public ListAction getAction() {
    return this.action;
  }
}
