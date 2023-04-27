package edu.isi.kcap.diskproject.server.util;

import java.util.HashMap;
import java.util.Map;

import edu.isi.kcap.ontapi.KBAPI;
import edu.isi.kcap.ontapi.KBObject;

public class KBCache {
    private Map<String, KBObject> properties;
    private Map<String, KBObject> classes;

    public KBCache(KBAPI kb) {
        this.properties = new HashMap<String, KBObject>();
        this.classes = new HashMap<String, KBObject>();

        for (KBObject classObj : kb.getAllClasses())
            if (classObj != null && classObj.getName() != null)
                this.classes.put(classObj.getName(), classObj);

        for (KBObject propObj : kb.getAllObjectProperties())
            if (propObj != null && propObj.getName() != null)
                this.properties.put(propObj.getName(), propObj);

        for (KBObject propObj : kb.getAllDatatypeProperties())
            if (propObj != null && propObj.getName() != null)
                this.properties.put(propObj.getName(), propObj);
    }

    public KBObject getClass(String name) {
        return this.classes.get(name);
    }

    public KBObject getProperty(String name) {
        return this.properties.get(name);
    }
}