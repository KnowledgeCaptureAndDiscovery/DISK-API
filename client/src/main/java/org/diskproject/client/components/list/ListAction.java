package org.diskproject.client.components.list;

public class ListAction {
  String id;
  String text;
  String icon;
  String iconStyle;

  public ListAction(String id, String text, String icon, String iconStyle) {
    this.id = id;
    this.text = text;
    this.icon = icon;
    this.iconStyle = iconStyle;
  }
  
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public String getIconStyle() {
    return iconStyle;
  }

  public void setIconStyle(String iconStyle) {
    this.iconStyle = iconStyle;
  }

}
