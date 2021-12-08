package org.diskproject.client.components.tree;

import java.util.ArrayList;
import java.util.List;

import org.diskproject.client.components.tree.events.HasTreeHandlers;
import org.diskproject.client.components.tree.events.TreeItemActionEvent;
import org.diskproject.client.components.tree.events.TreeItemActionHandler;
import org.diskproject.client.components.tree.events.TreeItemSelectionEvent;
import org.diskproject.client.components.tree.events.TreeItemSelectionHandler;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.InlineHTML;
import com.vaadin.polymer.PolymerWidget;
import com.vaadin.polymer.iron.widget.IronFlexLayout;
import com.vaadin.polymer.iron.widget.IronIcon;
import com.vaadin.polymer.paper.widget.PaperButton;
import com.vaadin.polymer.paper.widget.PaperIconButton;
import com.vaadin.polymer.paper.widget.PaperItem;

public class TreeWidget extends IronFlexLayout implements HasTreeHandlers {
  private HandlerManager handlerManager;
  private TreeNode root;
  private boolean showRoot;
  
  private List<TreeAction> actions;
  private IronFlexLayout buttons;
  private TreeNode actionNode;
  
  public TreeWidget(String html) {
    handlerManager = new HandlerManager(this);
    showRoot = false;
    actions = new ArrayList<TreeAction>();
    
    buttons = new IronFlexLayout();
    buttons.addStyleName("action-buttons");
  }
  
  public void removeNode(TreeNode node) {
    this.root.removeNode(node.getId());
    if(node.getParent() != null && node.getParent() != root) {
      node.getParent().updateChildrenSection();
      node.getParent().updateItem();
    }
  }
  
  public void updateNode(String oldid, TreeNode node) {
    if(node.getParent() != null) {
      node.nodemap.remove(oldid);
      node.nodemap.put(node.getId(), node);
    }
    node.updateItem();
  }

  public TreeNode getNode(String id) {
    return this.root.findNode(id);
  }
  
  public TreeNode getRoot() {
    return this.root;
  }

  public void setRoot(TreeNode root) {
    this.root = root;
    this.clear();
    
    root.updateChildrenSection();
    if(showRoot) {
      this.add(root.getItem());
      this.add(root.getChildrenSection());
    }
    else {
      for(TreeNode n : root.getChildren()) {
        this.add(n.getItem());
        this.add(n.getChildrenSection());
      }
    }
  }

  public void addNode(TreeNode parent, final TreeNode node) {
    parent.addChild(node);
    final PaperItem item = node.getItem();
    
    item.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        fireEvent(new TreeItemSelectionEvent(node));
      }
    });
    
    item.addDomHandler(new MouseOverHandler() {
      @Override
      public void onMouseOver(MouseOverEvent event) {
        actionNode = node;
        item.add(buttons);
      }
    }, MouseOverEvent.getType());
    
    item.addDomHandler(new MouseOutHandler() {
      @Override
      public void onMouseOut(MouseOutEvent event) {
        actionNode = null;
        buttons.removeFromParent();
      }
    }, MouseOutEvent.getType()); 
    
  }

  public List<TreeAction> getActions() {
    return actions;
  }

  public void setActions(List<TreeAction> actions) {
    this.actions = actions;
    this.setActionMenu();
  }
  
  public void addDeleteAction() {
    TreeAction deleteAction = new TreeAction("delete", null, 
        "icons:cancel", "red-button delete-action");
    this.actions.add(deleteAction);
    this.setActionMenu();
  }
  
  public void addCustomAction(String id, String text, String icon, String style) {
    TreeAction action = new TreeAction(id, text, icon, style);
    this.actions.add(action); 
    this.setActionMenu();
  }
  
  private void setActionMenu() {
    buttons.clear();
    for(final TreeAction action : this.actions) { 
      PolymerWidget button;
      if(action.getText() == null) {
        PaperIconButton ib = new PaperIconButton();
        ib.setTitle(action.getId().toUpperCase());
        ib.setIcon(action.getIcon());
        ib.addStyleName(action.getIconStyle());
        button = ib;
      }
      else {
        PaperButton pb = new PaperButton();
        pb.addStyleName(action.getIconStyle());
        if(action.getIcon() != null) {
          IronIcon icon = new IronIcon();
          icon.setIcon(action.getIcon());
          pb.add(icon);
        }
        pb.add(new InlineHTML(action.getText()));
        button = pb;
      }
      
      button.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          event.stopPropagation();
          if(actionNode != null)
            fireEvent(new TreeItemActionEvent(actionNode, action));
        }
      });  
      buttons.add(button);
    }
  }
  
  @Override
  public void fireEvent(GwtEvent<?> event) {
    handlerManager.fireEvent(event);
  }
  
  @Override
  public HandlerRegistration addTreeItemSelectionHandler(
      TreeItemSelectionHandler handler) {
    return handlerManager.addHandler(TreeItemSelectionEvent.TYPE, handler);
  }

  @Override
  public HandlerRegistration addTreeItemActionHandler(
      TreeItemActionHandler handler) {
    return handlerManager.addHandler(TreeItemActionEvent.TYPE, handler);
  }
}
