package org.diskproject.shared.classes.util;

import org.diskproject.shared.classes.common.Endpoint;
import org.diskproject.shared.classes.workflow.WorkflowTemplate;

public class WorkflowTemplateResponse {
    String id, name, link;
    Endpoint source;

    public WorkflowTemplateResponse (WorkflowTemplate wf, Endpoint source) {
        this.id = wf.getId();
        this.name = wf.getName();
        this.link = wf.getLink();
        this.source = source;
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

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public Endpoint getSource() {
        return source;
    }

    public void setSource(Endpoint source) {
        this.source = source;
    }
}