package org.diskproject.shared.classes;

public class DISKResource {
    String id, name, description, dateCreated, dateModified, author, notes;
    
    public DISKResource () {}

    public DISKResource (String id, String name, String description) {
        this.id = id;
        this.name = name;
	    this.description = description;
    }

    public DISKResource (String id, String name, String description, String notes,
            String author, String dateCreated, String dateModified) {
        this.id = id;
        this.name = name;
        this.notes = notes;
        this.author = author;
	    this.description = description;
        this.dateCreated = dateCreated;
        this.dateModified = dateModified;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getAuthor() {
        return this.author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDateCreated() {
        return this.dateCreated;
    }

    public void setDateCreated(String date) {
        this.dateCreated = date;
    }

    public String getDateModified() {
        return dateModified;
    }

    public void setDateModified(String date) {
        this.dateModified = date;
    }

}
