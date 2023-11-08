package org.diskproject.shared.classes.util;

import java.util.HashMap;
import java.util.Map;

public class QuestionOptionsRequest {
    String id;
    Map<String, String[]> bindings;

    public QuestionOptionsRequest () {
        this.bindings = null;
    };

    public QuestionOptionsRequest (String id, Map<String, String[]> bindings) {
        this.id = id;
        this.bindings = bindings;
    };

    public void setId (String id) {
        this.id = id;
    }

    public void setBindings (Map<String,String[]> bindings) {
        this.bindings = bindings;
    }

    public void addBinding (String name, String[] value) {
        if (this.bindings == null) this.bindings = new HashMap<String, String[]>();
        this.bindings.put(name, value);
    }

    public String getId () {
        return this.id;
    }

    public Map<String, String[]> getBindings () {
        return this.bindings;
    }
}
