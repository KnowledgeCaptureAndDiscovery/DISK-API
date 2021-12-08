package org.diskproject.client.components.list;

import com.google.gwt.user.client.ui.HTML;
import com.vaadin.polymer.iron.widget.IronIcon;
import com.vaadin.polymer.paper.widget.PaperItem;

public class ListNode implements Comparable<ListNode> {
  String id;
  String name;
  String description;
  Object data;
  String type;
  HTML content;
  String creationDate, author;
  
  PaperItem item;
  IronIcon icon;
  String iconName;
  
  public ListNode(String id, String name, String description, String date, String author) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.icon = new IronIcon();
    this.creationDate = date;
    this.author = author;
    this.setContent(name,  description);
  }
  
  public ListNode(String id, HTML content) {
    this.id = id;
    this.content = content;
    this.icon = new IronIcon();
  }
  
  public void setContent(String name, String description) {
    String html = "<div class='name'>" + this.name + "</div>";
    html += "<div class='description'>"+this.description+"</div>";
    html += "<div class='footer' style='display: flex;justify-content: space-between;'>";
    html += "<span><b>Creation date:</b> ";
    html += (this.creationDate != null) ? this.creationDate : "None specified";
    html += "</span><span><b>Author:</b> ";
    html += (this.author != null) ? this.author : "None specified";
    html += "</span></div>";
    
    if(this.content != null)
      this.content.setHTML(html);
    else
      this.content = new HTML(html);
    this.content.addStyleName("listnode");
  }

  public void setFullContent(String html) {
    if(this.content != null)
      this.content.setHTML(html);
    else
      this.content = new HTML(html);
  }
  
  public IronIcon getIcon() {
    return icon;
  }

  public void setIconStyle(String style) {
    this.icon.addStyleName(style);
  }
  
  public void setIcon(String iconstr) {
    this.iconName = iconstr;
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
      this.setContent(name, description);
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description, boolean update) {
    this.description = description;
    if(update)
      this.setContent(name, description);
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
    if(item != null)
      item.addStyleName(this.type);
  }

  public HTML getContent() {
    return content;
  }

  public void setContent(HTML content) {
    this.content = content;
  }

  public PaperItem getItem() {
    if(this.item != null)
      return this.item;
    
    this.item = new PaperItem();
    return this.updateItem();
  }
  
  public PaperItem updateItem() {
    if(this.item == null)
      return null;
    
    item.clear();
    if(this.iconName != null) {
      icon.setIcon(this.iconName);    
      item.add(icon);
    }
    item.add(content);
    
    if(this.type != null)
      item.setStyleName(this.type);
    
    return this.item;
  }
  
  @Override
  public int compareTo(ListNode node) {
    return id.compareTo(node.getId());
  }
}
