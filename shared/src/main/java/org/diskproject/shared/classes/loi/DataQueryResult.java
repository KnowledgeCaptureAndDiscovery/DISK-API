package org.diskproject.shared.classes.loi;

public class DataQueryResult extends DataQueryTemplate {
    private String query, results;

    public DataQueryResult(DataQueryTemplate src) {
        super(src);
    }

    public DataQueryResult (DataQueryTemplate src, String query, String results) {
        super(src);
        this.query = query;
        this.results = results;
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
