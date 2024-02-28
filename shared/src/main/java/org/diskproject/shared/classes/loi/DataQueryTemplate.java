package org.diskproject.shared.classes.loi;

import org.diskproject.shared.classes.common.Endpoint;

import java.util.List;

public class DataQueryTemplate {
    private Endpoint endpoint;
    private String template, description, footnote;
    private List<String> variablesToShow;

    public DataQueryTemplate () {}

    public DataQueryTemplate (DataQueryTemplate src) {
        this.endpoint = src.getEndpoint();
        this.template = src.getTemplate();
        this.description = src.getDescription();
        this.variablesToShow = src.getVariablesToShow();
        this.footnote = src.getFootnote();
    }

    public DataQueryTemplate (String template, Endpoint endpoint) {
        this.template = template;
        this.endpoint = endpoint;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String query) {
        this.template = query;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getVariablesToShow() {
        return variablesToShow;
    }

    public void setVariablesToShow(List<String> variablesToShow) {
        this.variablesToShow = variablesToShow;
    }

    public String getFootnote() {
        return footnote;
    }

    public void setFootnote(String footnote) {
        this.footnote = footnote;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
    }
}
