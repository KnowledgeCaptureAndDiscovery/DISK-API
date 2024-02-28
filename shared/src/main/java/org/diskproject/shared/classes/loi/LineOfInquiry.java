package org.diskproject.shared.classes.loi;

import java.util.List;
import org.diskproject.shared.classes.DISKResource;
import org.diskproject.shared.classes.workflow.WorkflowSeed;

public class LineOfInquiry extends LOICommon {
    DataQueryTemplate dataQueryTemplate;
    List<WorkflowSeed> workflowSeeds, metaWorkflowSeeds;

    public LineOfInquiry () {}

    public LineOfInquiry (DISKResource src) {
        super(src);
    }

    public LineOfInquiry (String id){
        super(id);
    }

    public LineOfInquiry(String id, String name, LineOfInquiry src){
        super(id,name,src);
        this.dataQueryTemplate = src.getDataQueryTemplate();
        this.workflowSeeds = src.getWorkflowSeeds();
        this.metaWorkflowSeeds = src.getMetaWorkflowSeeds();
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
}