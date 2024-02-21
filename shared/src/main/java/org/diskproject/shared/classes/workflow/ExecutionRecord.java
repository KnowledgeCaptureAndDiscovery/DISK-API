package org.diskproject.shared.classes.workflow;

import org.diskproject.shared.classes.common.Status;

public class ExecutionRecord {
    private String log, startDate, endDate;
    private Status status;

    @Override
    public String toString () {
        String txt = "{" + (status == null ? "NULL" : status.name()) + ":[";
        txt += (startDate == null ? "NULL" : startDate) + "-";
        txt += (endDate == null ? "NULL" : endDate) + "]";
        if (log != null) txt += "\"" + log + "\"";
        return txt;
    }

    public ExecutionRecord (ExecutionRecord src) {
        this.log = src.getLog();
        this.startDate = src.getStartDate();
        this.endDate = src.getEndDate();
        this.status = src.getStatus();
    }

    public ExecutionRecord () {
    }

    public ExecutionRecord (Status status, String start, String end, String log) {
        this.status = status;
        this.startDate = start;
        this.endDate = end;
        this.log = log;
    }

    public String getLog() {
        return log;
    }
    public void setLog(String log) {
        this.log = log;
    }
    public String getStartDate() {
        return startDate;
    }
    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }
    public String getEndDate() {
        return endDate;
    }
    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }
    public Status getStatus() {
        return status;
    }
    public void setStatus(Status status) {
        this.status = status;
    }
}
