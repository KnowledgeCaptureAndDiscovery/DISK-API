package org.diskproject.shared.classes.loi;

import java.util.ArrayList;
import java.util.List;

import org.diskproject.shared.classes.DISKResource;
import org.diskproject.shared.classes.question.Question;
import org.diskproject.shared.classes.workflow.WorkflowSeed;

public class LineOfInquiry extends DISKResource {
    public static class UpdateConditions {
        public static final int
                ON_DATA_UPDATE = 1,
                ON_METHOD_UPDATE = 2,
                MANUAL = 4;
    }
    int updateCondition = UpdateConditions.ON_DATA_UPDATE;
    String goalQuery; //Simple query to get the graph.
    Question question;
    DataQueryTemplate dataQueryTemplate;
    // Methods
    List<WorkflowSeed> workflowSeeds, metaWorkflowSeeds;

    public LineOfInquiry (DISKResource src) {
        super(src);
    }

    public LineOfInquiry (String id){
        this.setId(id);
    }

    public LineOfInquiry() {
        this.workflowSeeds = new ArrayList<WorkflowSeed>();
        this.metaWorkflowSeeds = new ArrayList<WorkflowSeed>();
    }

    public LineOfInquiry(String id, String name, String description){
        super(id,name,description);
        this.workflowSeeds = new ArrayList<WorkflowSeed>();
        this.metaWorkflowSeeds = new ArrayList<WorkflowSeed>();
    }

    public DataQueryTemplate getDataQueryTemplate () {
        return dataQueryTemplate;
    }

    public void setDataQueryTemplate(DataQueryTemplate dataQuery) {
        this.dataQueryTemplate = dataQuery;
    }

    public List<WorkflowSeed> getWorkflowSeeds() {
        return workflowSeeds;
    }

    public void setWorkflowSeeds(List<WorkflowSeed> workflows) {
        this.workflowSeeds = workflows;
    }

    public void addWorkflow(WorkflowSeed workflowId) {
        this.workflowSeeds.add(workflowId);
    }

    public List<WorkflowSeed> getMetaWorkflowSeeds() {
        return metaWorkflowSeeds;
    }

    public void setMetaWorkflowSeeds(List<WorkflowSeed> metaWorkflows) {
        this.metaWorkflowSeeds = metaWorkflows;
    }

    public void addMetaWorkflow(WorkflowSeed metaWorkflowId) {
        this.metaWorkflowSeeds.add(metaWorkflowId);
    }

    public void setUpdateCondition (int b) {
        this.updateCondition = b;
    }

    public int getUpdateCondition () {
        return this.updateCondition;
    }

    public String getGoalQuery() {
        return goalQuery;
    }

    public void setGoalQuery(String goalQuery) {
        this.goalQuery = goalQuery;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }
}