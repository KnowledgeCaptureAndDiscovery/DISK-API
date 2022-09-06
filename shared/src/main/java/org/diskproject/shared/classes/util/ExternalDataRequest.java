package org.diskproject.shared.classes.util;

public class ExternalDataRequest {
    String source, dataId, runId;

    public ExternalDataRequest(String source, String dataId, String runId) {
        this.source = source;
        this.dataId = dataId;
        this.runId = runId;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

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
