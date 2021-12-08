package org.diskproject.client.components.tree.events;

import org.diskproject.client.components.tree.TreeNode;
import com.google.gwt.event.shared.GwtEvent;

public class TreeItemSelectionEvent extends GwtEvent<TreeItemSelectionHandler> {

  public static Type<TreeItemSelectionHandler> TYPE = new Type<TreeItemSelectionHandler>();

  private final TreeNode node;

  public TreeItemSelectionEvent(TreeNode node) {
    this.node = node;
  }

  @Override
  public Type<TreeItemSelectionHandler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(TreeItemSelectionHandler handler) {
    handler.onSelection(this);
  }

  public TreeNode getItem() {
    return this.node;
  }
}
