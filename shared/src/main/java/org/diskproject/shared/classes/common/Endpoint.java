package org.diskproject.shared.classes.common;

public class Endpoint {
    private String name, url;

    public Endpoint() {}

    public Endpoint(String name, String url) {
        this.name = name;
        this.url = url;
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

    @Override
    public String toString() {
        return "[" + this.name + "|" + this.url + "]";
    }
}