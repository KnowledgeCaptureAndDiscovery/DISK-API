package edu.isi.kcap.diskproject.shared.classes.common;

public class TreeItem {
  String id;
  String name;
  String description;
  String dateCreated;
  String dateModified;
  String author;
  String parentId;
  String question;

  public TreeItem() {
  }

  public TreeItem(String id, String name, String description, String parentId, String creationDate, String author) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.parentId = parentId;
    this.dateCreated = creationDate;
    this.author = author;
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

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getParentId() {
    return parentId;
  }

  public void setParentId(String parentId) {
    this.parentId = parentId;
  }

  public void setCreationDate(String date) {
    this.dateCreated = date;
  }

  public String getCreationDate() {
    return dateCreated;
  }

  public void setDateModified(String date) {
    this.dateModified = date;
  }

  public String getDateModified() {
    return dateModified;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public String getAuthor() {
    return this.author;
  }

  public String getQuestion() {
    return this.question;
  }

  public void setQuestion(String id) {
    this.question = id;
  }
}
