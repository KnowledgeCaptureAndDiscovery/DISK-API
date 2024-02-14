package org.diskproject.shared.classes.common;

public class Endpoint {
    private String name, url;
    private String id;

    public Endpoint() {}

    public Endpoint(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public Endpoint(String name, String url, String id) {
        this.name = name;
        this.url = url;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return (this.id != null ? "(" + this.id + ")" : "") + "[" + this.name + "|" + this.url + "]";
    }
}