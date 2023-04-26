package org.diskproject.shared.classes.vocabulary;

public class Entity {
  String id;
  String name;
  String label;
  String description;
  
  String parent;
  
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }
  
  public String getName() {
    return this.name;
  }
  
  public void setName(String name) {
    this.name = name;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String desc) {
    this.description = desc;
  }
  
  @Override
  public int hashCode() {
    return (id + "_" + label).hashCode();
  }
}
