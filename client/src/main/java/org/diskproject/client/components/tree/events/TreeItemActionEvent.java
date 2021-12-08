package org.diskproject.client.components.tree.events;

import org.diskproject.client.components.tree.TreeAction;
import org.diskproject.client.components.tree.TreeNode;

import com.google.gwt.event.shared.GwtEvent;

public class TreeItemActionEvent extends GwtEvent<TreeItemActionHandler> {

  public static Type<TreeItemActionHandler> TYPE = new Type<TreeItemActionHandler>();

  private final TreeAction action;
  private final TreeNode node;

  public TreeItemActionEvent(TreeNode node, TreeAction action) {
    this.node = node;
    this.action = action;
  }

  @Override
  public Type<TreeItemActionHandler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(TreeItemActionHandler handler) {
    handler.onAction(this);
  }

  public TreeNode getItem() {
    return this.node;
  }
  
  public TreeAction getAction() {
    return this.action;
  }
}
