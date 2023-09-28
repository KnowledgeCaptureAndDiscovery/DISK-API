package org.diskproject.server.threads;

import java.util.List;
import java.util.Map;

import org.diskproject.shared.classes.adapters.MethodAdapter;
import org.diskproject.shared.classes.loi.LineOfInquiry;
import org.diskproject.shared.classes.loi.TriggeredLOI;
import org.diskproject.shared.classes.loi.WorkflowBindings;
import org.diskproject.shared.classes.workflow.VariableBinding;
import org.diskproject.shared.classes.workflow.WorkflowRun;
import org.diskproject.shared.classes.workflow.WorkflowRun.Status;
import org.diskproject.shared.classes.workflow.WorkflowVariable;

public class ExecutionThread implements Runnable {
    ThreadManager manager;
    boolean metamode;
    TriggeredLOI tloi;
    LineOfInquiry loi;
    
    public ExecutionThread (ThreadManager manager, TriggeredLOI tloi, LineOfInquiry loi, boolean metamode) {
        this.manager = manager;
        this.tloi = tloi;
        this.loi = loi;
        this.metamode = metamode;
    }

    @Override
    public void run() {
        if (this.metamode)
            System.out.println("[R] Running execution thread on META mode");
        else
            System.out.println("[R] Running execution thread");

        List<WorkflowBindings> workflowList = this.metamode ? tloi.getMetaWorkflows() : tloi.getWorkflows();
        Status currentStatus = Status.RUNNING;

        // Start workflows from tloi
        for (WorkflowBindings curWorkflow : workflowList) {
            MethodAdapter methodAdapter = this.manager.getMethodAdapters().getMethodAdapterByName(curWorkflow.getSource());
            if (methodAdapter == null) {
                currentStatus = Status.FAILED;
                break;
            }
            if (this.metamode) {
                // TODO: Here we should map workflow outputs to metaworkflow inputs.
            }

            // Get workflow input details
            Map<String, WorkflowVariable> inputVariables = methodAdapter.getWorkflowInputs(curWorkflow.getWorkflow());
            List<VariableBinding> inputBindings = curWorkflow.getBindings();
            this.printWorkflowRun(curWorkflow, inputBindings);
            List<String> runIds = methodAdapter.runWorkflow(curWorkflow.getWorkflow(), inputBindings, inputVariables);

            if (runIds != null) {
                System.out.println("[R] " + runIds.size() + " Workflows send: ");
                for (String rid : runIds) {
                    WorkflowRun run = new WorkflowRun();
                    run.setId(rid);
                    run.setAsPending();
                    System.out.println("[R]   ID: " + rid);
                    curWorkflow.addRun(run);
                }
            } else {
                currentStatus = Status.FAILED;
                System.out.println("[R] Error: Could not run workflow");
            }
        }
        tloi.setStatus(currentStatus);
        manager.updateTLOI(tloi);

        // Start monitoring
        if (currentStatus == Status.RUNNING) {
            manager.monitorTLOI(tloi, loi, metamode);
        } else {
            System.out.println("[E] Finished: Something when wrong.");
        }
    }
    
    public void printWorkflowRun(WorkflowBindings wf, List<VariableBinding> inputBindings) {
        // Execute workflow
        System.out.println("[R] Executing " + wf.getWorkflow() + " with " + inputBindings.size() + " parameters:");
        for (VariableBinding v : inputBindings) {
            String[] l = v.isCollection() ? v.getBindingAsArray() : null;
            int i = 0;
            if (l != null) {
                System.out.println("[R] - " + v.getVariable() + ": ");
                for (String b : l) {
                    System.out.println("[R]    " + String.valueOf(i) + ") " + b);
                    i++;
                }
            } else {
                System.out.println("[R] - " + v.getVariable() + ": " + v.getBinding());
            }
        }
    }
}
