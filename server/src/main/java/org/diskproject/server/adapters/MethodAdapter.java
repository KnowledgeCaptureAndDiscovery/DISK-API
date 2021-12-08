package org.diskproject.server.adapters;

import java.util.Map;

import org.diskproject.shared.classes.loi.LineOfInquiry;

public abstract class MethodAdapter {
    public MethodAdapter () {}
    
    // Check that a LOI is correctly configured for this adapter
    public abstract boolean validateLOI (LineOfInquiry loi, Map<String, String> values);
}