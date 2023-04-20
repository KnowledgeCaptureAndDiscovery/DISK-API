package edu.diskproject.shared.classes.vocabulary;

import java.util.ArrayList;
import java.util.List;

public class Type extends Entity {
  String parent;
  List<String> children;

  public Type() {
    this.children = new ArrayList<String>();
  }

  public String getParent() {
    return parent;
  }

  public void setParent(String parent) {
    this.parent = parent;
  }

  public List<String> getChildren() {
    return children;
  }

  public void setChildren(ArrayList<String> children) {
    this.children = children;
  }

  public void addChild(String child) {
    this.children.add(child);
  }
}
