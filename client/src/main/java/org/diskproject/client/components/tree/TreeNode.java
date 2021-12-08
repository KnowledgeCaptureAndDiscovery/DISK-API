package org.diskproject.client.components.tree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HTML;
import com.vaadin.polymer.iron.widget.IronFlexLayout;
import com.vaadin.polymer.iron.widget.IronIcon;
import com.vaadin.polymer.paper.widget.PaperItem;

public class TreeNode implements Comparable<TreeNode> {
  String id;
  String name;
  String description;
  String creationDate;
  String author;
  Object data;
  String type;
  HTML content;

  PaperItem item;
  IronFlexLayout childrenSection;

  IronIcon icon, collapser;
  String iconName, expandedIconName;

  boolean expanded;

  TreeNode parent;
  List<TreeNode> children;
  Map<String, TreeNode> nodemap;

  public TreeNode(String id, String name, String description, String date, String author) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.creationDate = date;
    this.author = author;
    this.setContent(name, description);
    this.initialize();
  }

  public TreeNode(String id, HTML content) {
    this.id = id;
    this.content = content;
    this.initialize();
  }

  private void setContent(String name, String description) {
    String html = "<div class='name'>" + this.name + "</div>";
    html += "<div class='description'>"+this.description+"</div>";
    html += "<div class='footer' style='display: flex;justify-content: space-between;'>";
    html += "<span><b>Creation date:</b> ";
    //html += (this.creationDate != null) ? this.creationDate : "None specified";
    if (this.creationDate != null) {
      String[] hotfix = this.creationDate.split(" ");
      if (hotfix.length == 2)
    	  html += hotfix[1] + " " + hotfix[0];
      else
    	  html += this.creationDate;
    } else {
      html += "None specified";
    }
    	
    html += "</span><span><b>Author:</b> ";
    html += (this.author != null) ? this.author : "None specified";
    html += "</span></div>";

    if(this.content != null)
      this.content.setHTML(html);
    else
      this.content = new HTML(html);
    this.content.addStyleName("treenode");
  }

  private void initialize() {
    this.icon = new IronIcon();
    this.children = new ArrayList<TreeNode>();
    this.nodemap = new HashMap<String, TreeNode>();
    
    this.childrenSection = new IronFlexLayout();
    this.childrenSection.addStyleName("pad");
    
    this.collapser = new IronIcon();
    this.collapser.addStyleName("collapser");
    this.collapser.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        event.stopPropagation();
        expanded = !expanded;
        if(expanded)
          childrenSection.removeStyleName("hidden");
        else
          childrenSection.addStyleName("hidden");
        setIcons();
      }
    });
    
    this.item = new PaperItem();
    this.updateItem();    
  }

  public IronIcon getIcon() {
    return icon;
  }

  public void setIconStyle(String style) {
    this.icon.addStyleName(style);
  }

  public void setIcon(String iconstr) {
    this.iconName = iconstr;
    this.icon.setIcon(this.iconName);
  }

  public void setExpandedIcon(String iconstr) {
    this.expandedIconName = iconstr;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name, boolean update) {
    this.name = name;
    if(update)
      this.setContent(name, this.description);
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description, boolean update) {
    this.description = description;
    if(update)
      this.setContent(this.name, description);
  }

  public Object getData() {
    return data;
  }

  public void setData(Object data) {
    this.data = data;
  }
  
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
    item.addStyleName(this.type);
  }

  public PaperItem getItem() {
    return this.item;
  }

  public PaperItem updateItem() {
    if(this.item == null)
      return null;
    
    item.clear();
    item.add(collapser);
    item.add(icon);
    item.add(content);
    this.setIcons();

    if(this.type != null)
      item.addStyleName(this.type);
    
    return this.item;
  }

  public IronFlexLayout updateChildrenSection() {
    childrenSection.clear();
    if(expanded)
      childrenSection.removeStyleName("hidden");
    else
      childrenSection.addStyleName("hidden");
    for(TreeNode childnode : this.getChildren()) {
      childnode.updateChildrenSection();
      childrenSection.add(childnode.getItem());
      childrenSection.add(childnode.getChildrenSection());
    }
    this.setIcons();
    return childrenSection;
  }

  public IronFlexLayout getChildrenSection() {
    return this.childrenSection;
  }

  public TreeNode findNode(String id) {
    if(this.id.equals(id))
      return this;
    
    for(TreeNode child : this.children) {
      TreeNode node = child.findNode(id);
      if(node != null)
        return node;
    }
    return null;
  }

  public boolean removeNode(String id) {
    if(this.id.equals(id)) {
      this.item.removeFromParent();
      this.childrenSection.removeFromParent();
      return true;
    }
    for(TreeNode child : this.children) {
      if(child.removeNode(id)) {
        this.children.remove(child);
        this.nodemap.remove(child.getId());
      }
    }
    return false;
  }

  public boolean isExpanded() {
    return expanded;
  }

  public void setExpanded(boolean expanded) {
    this.expanded = expanded;
  }

  public void addChild(TreeNode child) {
    this.children.add(child);
    this.nodemap.put(child.getId(), child);
    child.setParent(this);
  }

  public List<TreeNode> getChildren() {
    return children;
  }

  public void setChildren(List<TreeNode> children) {
    this.children = children;
  }

  public TreeNode getParent() {
    return parent;
  }

  public void setParent(TreeNode parent) {
    this.parent = parent;
  }

  private void setIcons() {
    if(this.children.size() == 0) {
      collapser.setIcon("icons:a");
      collapser.addStyleName("transparent");
      icon.setIcon(this.iconName);
    }
    else {
      collapser.removeStyleName("transparent");
      if(this.isExpanded()) {
        collapser.setIcon("icons:expand-more");
        icon.setIcon(this.expandedIconName);
      }
      else {
        collapser.setIcon("icons:chevron-right");
        icon.setIcon(this.iconName);
      }
    }
  }  

  @Override
  public int compareTo(TreeNode node) {
    return id.compareTo(node.getId());
  }
}
