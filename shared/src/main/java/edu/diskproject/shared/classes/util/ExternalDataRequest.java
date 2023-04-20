package org.diskproject.shared.classes.util;

public class ExternalDataRequest {
    String source, dataId;

    public ExternalDataRequest () {
    }
    
    public ExternalDataRequest (String source, String data) {
        this.source = source;
        this.dataId = data;
    }

    public String getSource () {
        return this.source;
    }

    public String getDataId () {
        return this.dataId;
    }

    public void setSource (String source) {
        this.source = source;
    }

    public void setDataId (String dataId) {
        this.dataId = dataId;
    }
}
