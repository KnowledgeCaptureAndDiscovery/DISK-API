package org.diskproject.shared.classes.loi;

import org.diskproject.shared.classes.common.Endpoint;

public class DataQueryResult extends DataQueryTemplate {
    private String query, results;

    public DataQueryResult(DataQueryTemplate src) {
        super(src);
    }

    public DataQueryResult(String template, Endpoint endpoint) {
        super(template, endpoint);
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getResults() {
        return results;
    }

    public void setResults(String results) {
        this.results = results;
    }

}
