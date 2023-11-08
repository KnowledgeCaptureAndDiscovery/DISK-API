package org.diskproject.shared.classes.loi;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.diskproject.shared.classes.DISKResource;

public class LineOfInquiry extends DISKResource {
    public static class UpdateStatus {
        public static final int
                ON_DATA_UPDATE = 1,
                ON_METHOD_UPDATE = 2,
                MANUAL = 4;
    }

    // Data query:
    String dataQuery, dataQueryExplanation;
    // Data source:
    String dataSource, dataSourceURL;
    // Preview table:
    String tableVariables, tableDescription;
    // Methods
    List<WorkflowBindings> workflows, metaWorkflows;
    // Linking with the hypothesis (question)
    String hypothesisQuery, questionId;
    int updateCondition = UpdateStatus.ON_DATA_UPDATE;

    public LineOfInquiry() {
        this.workflows = new ArrayList<WorkflowBindings>();
        this.metaWorkflows = new ArrayList<WorkflowBindings>();
    }

    public LineOfInquiry(String id, String name, String description){
        super(id,name,description);
        this.workflows = new ArrayList<WorkflowBindings>();
        this.metaWorkflows = new ArrayList<WorkflowBindings>();
    }

    public LineOfInquiry(String id, String name, String description, String dataSource,
		    String hypothesisQuery, String dataQuery, List<WorkflowBindings> workflows,
		    List<WorkflowBindings> metaWorkflows){
        super(id,name,description);
	    this.dataSource = dataSource;
	    this.hypothesisQuery = hypothesisQuery;
	    this.dataQuery = dataQuery;
	    this.workflows = workflows;
	    this.metaWorkflows = metaWorkflows;
    }

    public void setDataSource (String ds) {
	    this.dataSource = ds;
    }

    public String getDataSource () {
	    return this.dataSource;
    }

    public void setDataSourceURL (String url) {
	    this.dataSourceURL = url;
    }

    public String getDataSourceURL () {
	    return this.dataSourceURL;
    }

    public Set<String> getAllWorkflowVariables () {
        Set<String> allVars = new HashSet<String>();
        List<WorkflowBindings> wfs = new ArrayList<WorkflowBindings>(workflows);
        wfs.addAll(metaWorkflows);
        for (WorkflowBindings wb: wfs) {
            List<String> vars = wb.getAllVariables();
            for (String v: vars) {
                allVars.add(v);
            }
        }
        return allVars;
    }

    public Set<String> getAllWorkflowNonCollectionVariables () {
        Set<String> allVars = new HashSet<String>();
        List<WorkflowBindings> wfs = new ArrayList<WorkflowBindings>(workflows);
        wfs.addAll(metaWorkflows);
        for (WorkflowBindings wb: wfs) {
            List<String> vars = wb.getNonCollectionVariables();
            for (String v: vars) {
                allVars.add(v);
            }
        }
        return allVars;
    }

    public String getDataQuery() {
        return dataQuery;
    }

    public void setDataQuery(String dataQuery) {
        this.dataQuery = dataQuery;
    }

    public List<WorkflowBindings> getWorkflows() {
        return workflows;
    }

    public void setWorkflows(List<WorkflowBindings> workflows) {
        this.workflows = workflows;
    }

    public void addWorkflow(WorkflowBindings workflowId) {
        this.workflows.add(workflowId);
    }

    public List<WorkflowBindings> getMetaWorkflows() {
        return metaWorkflows;
    }

    public void setMetaWorkflows(List<WorkflowBindings> metaWorkflows) {
        this.metaWorkflows = metaWorkflows;
    }

    public void addMetaWorkflow(WorkflowBindings metaWorkflowId) {
        this.metaWorkflows.add(metaWorkflowId);
    }

    public String getTableVariables () {
	    return this.tableVariables;
    }

    public void setTableVariables (String v) {
	    this.tableVariables = v;
    }

    public String getTableDescription () {
	    return this.tableDescription;
    }

    public void setTableDescription (String v) {
	    this.tableDescription = v;
    }

    public String getDataQueryExplanation () {
	    return this.dataQueryExplanation;
    }

    public void setDataQueryExplanation (String v) {
	    this.dataQueryExplanation = v;
    }

    public void setUpdateCondition (int b) {
        this.updateCondition = b;
    }

    public int getUpdateCondition () {
        return this.updateCondition;
    }

    public void setQuestionId (String q) {
        this.questionId = q;
    }

    public String getQuestionId () {
        return this.questionId;
    }

    public void setHypothesisQuery(String query) {
        this.hypothesisQuery = query;
    }

    public String getHypothesisQuery() {
        return hypothesisQuery;
    }
}