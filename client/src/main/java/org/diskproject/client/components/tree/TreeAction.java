package org.diskproject.client.components.tree;

public class TreeAction {
  String id;
  String text;
  String icon;
  String iconStyle;

  public TreeAction(String id, String text, String icon, String iconStyle) {
    super();
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
