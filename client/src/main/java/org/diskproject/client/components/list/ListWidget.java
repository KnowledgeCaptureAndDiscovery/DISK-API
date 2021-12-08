package org.diskproject.client.components.list;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.diskproject.client.components.list.events.HasListHandlers;
import org.diskproject.client.components.list.events.ListItemActionEvent;
import org.diskproject.client.components.list.events.ListItemActionHandler;
import org.diskproject.client.components.list.events.ListItemSelectionEvent;
import org.diskproject.client.components.list.events.ListItemSelectionHandler;

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

public class ListWidget extends IronFlexLayout implements HasListHandlers {
  private HandlerManager handlerManager;
  private List<ListNode> nodes;
  private Map<String, ListNode> nodemap;
  private List<ListAction> actions;
  private IronFlexLayout buttons;
  private ListNode actionNode;
  
  public ListWidget(String html) {
    handlerManager = new HandlerManager(this);
    nodes = new ArrayList<ListNode>();
    nodemap = new HashMap<String, ListNode>();
    actions = new ArrayList<ListAction>();
    
    buttons = new IronFlexLayout();
    buttons.addStyleName("action-buttons");
  }

  public void addNode(final ListNode node) {
    final PaperItem item = node.getItem();
    this.add(item);
    item.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        fireEvent(new ListItemSelectionEvent(node));
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
    
    nodes.add(node);
    nodemap.put(node.getId(), node);
  }
  
  @Override
  public void clear() {
    super.clear();
    nodes.clear();
    nodemap.clear();
  }
  
  public void removeNode(ListNode node) {
    ListNode n = nodemap.get(node.getId());
    if(n != null) {
      nodes.remove(n);      
      nodemap.remove(n.getId());
      n.getItem().removeFromParent();
    }
  }
  
  public void updateNode(String oldid, ListNode node) {
    nodemap.remove(oldid);
    nodemap.put(node.getId(), node);
    node.updateItem();
  }

  public ListNode getNode(String id) {
    return nodemap.get(id);
  }
  
  public List<ListNode> getNodes() {
    return nodes;
  }

  public void setNodes(List<ListNode> nodes) {
    this.nodes = nodes;
  }

  public List<ListAction> getActions() {
    return actions;
  }

  public void setActions(List<ListAction> actions) {
    this.actions = actions;
    this.setActionMenu();
  }
  
  public void addDeleteAction() {
    ListAction deleteAction = new ListAction("delete", null, 
        "icons:cancel", "red-button delete-action");
    this.actions.add(deleteAction);
    this.setActionMenu();
  }
  
  public void addCustomAction(String id, String text, String icon, String style) {
    ListAction action = new ListAction(id, text, icon, style);
    this.actions.add(action); 
    this.setActionMenu();
  }
  
  public void hideActionMenu () {
	  buttons.setVisible(false);
  }
  
  public void showActionMenu () {
	  buttons.setVisible(true);
  }
  
  private void setActionMenu() {
    buttons.clear();
    for(final ListAction action : this.actions) { 
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
            fireEvent(new ListItemActionEvent(actionNode, action));
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
  public HandlerRegistration addListItemSelectionHandler(
      ListItemSelectionHandler handler) {
    return handlerManager.addHandler(ListItemSelectionEvent.TYPE, handler);
  }

  @Override
  public HandlerRegistration addListItemActionHandler(
      ListItemActionHandler handler) {
    return handlerManager.addHandler(ListItemActionEvent.TYPE, handler);
  }
}
