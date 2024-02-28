package org.diskproject.server.threads;

import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.diskproject.shared.classes.adapters.MethodAdapter;
import org.diskproject.shared.classes.common.Status;
import org.diskproject.shared.classes.loi.TriggeredLOI;
import org.diskproject.shared.classes.workflow.Execution;
import org.diskproject.shared.classes.workflow.WorkflowInstantiation;

public class MonitorThread implements Runnable {
    ThreadManager manager;
    boolean metamode;
    TriggeredLOI tloi;
    List<String> runList;                   // runList = [runId1, runId2, ...]
    Map<String, Execution> runInfo;         // runInfo[runId1] = WorkflowRun
    Map<String, WorkflowInstantiation> runToWf;  // runToWf[runId1] = WorkflowBindings
    Map<String, Integer> runToIndex;        // runToIndex[runId1] = 0,1...

    public MonitorThread (ThreadManager manager, TriggeredLOI tloi, boolean metamode) {
        this.tloi = tloi;
        this.manager = manager;
        this.metamode = metamode;

        this.runInfo = new HashMap<String, Execution>();
        this.runToWf = new HashMap<String, WorkflowInstantiation>();
        this.runToIndex = new HashMap<String, Integer>();
        this.runList = new ArrayList<String>();

        Integer index = 0;
        for (WorkflowInstantiation curWorkflow : (metamode ? this.tloi.getMetaWorkflows() : this.tloi.getWorkflows())) {
            for (Execution run : curWorkflow.getExecutions()) {
                Status st = run.getStatus();
                String runId = run.getExternalId();
                if (st == Status.PENDING || st == Status.QUEUED || st == Status.RUNNING) {
                    runList.add(runId);
                    runInfo.put(runId, run);
                    runToWf.put(runId, curWorkflow);
                    runToIndex.put(runId, index);
                }
            }
            index += 1;
        }
    }

    private boolean needsMonitoring (Execution run) {
        return !(run.getStatus() == Status.SUCCESSFUL || run.getStatus() == Status.FAILED);
    }

    private Execution getNextPendingRun () {
        for (String runId: this.runList) {
            Execution run = this.runInfo.get(runId);
            if (needsMonitoring(run) && run.getExternalId() != null)
                return run;
        }
        return null;
    }

    private Status getOverallStatus () {
        for (String runId: this.runList) {
            Execution run = this.runInfo.get(runId);
            Status status = run.getStatus();
            if (status == Status.FAILED)
                return Status.FAILED;
            if (status != Status.SUCCESSFUL) //If all of them are pending then the tloi should be pending too.
                return Status.RUNNING;
        }
        return Status.SUCCESSFUL;
    }

    private void updateRun (WorkflowInstantiation wf, Execution run) {
        this.runInfo.replace(run.getExternalId(), run); // Updates status on the run list
        wf.addOrReplaceExecution(run);
        Integer index = this.runToIndex.get(run.getExternalId());
        List<WorkflowInstantiation> list = this.metamode ? this.tloi.getMetaWorkflows() : this.tloi.getWorkflows();
        list.set(index, wf); // Replaces run in the wf list
        if (this.metamode) this.tloi.setMetaWorkflows(list);
        else this.tloi.setWorkflows(list);

        // Process outputs
        if (run.getStatus() == Status.SUCCESSFUL) {
            updateWorkflowStatus();
            this.manager.processFinishedRun(this.tloi, wf, run, metamode);
        }
    }

    @Override
    public void run() {
        System.out.println("[M] Running monitoring thread");
        Execution pendingRun = this.getNextPendingRun();
        if (pendingRun == null || pendingRun.getExternalId() == null) {
            System.out.println("[M] No more pending runs.");
        } else {
            WorkflowInstantiation wf = runToWf.get(pendingRun.getExternalId());
            MethodAdapter methodAdapter = this.manager.getMethodAdapters().getMethodAdapterByEndpoint(wf.getSource());
            if (methodAdapter == null) {
                System.out.println("[M] Error: Method adapter not found: " + wf.getSource());
                return;
            }

            String runId = pendingRun.getExternalId().replaceAll("^.*#", "");
            Execution updatedRun = methodAdapter.getRunStatus(runId);

            // If we cannot get the status but the run was pending, it means that the run is in the WINGS queue.
            if (updatedRun == null || updatedRun.getStatus() == null) {
                System.out.println("[E] Cannot get status for " + tloi.getId() + " - RUN " + runId);
                if (pendingRun.getStatus() == Status.PENDING) { // In queue
                    updatedRun = pendingRun;
                } else {
                    System.out.println("[E] This should not happen");
                    return;
                }
            }
            updateRun(wf, updatedRun);
        }

        Status status = getOverallStatus();
        this.tloi.setStatus(status);
        manager.updateTLOI(tloi);

        if (status == Status.SUCCESSFUL || status == Status.FAILED) {
            if (status == Status.SUCCESSFUL) {
                if (metamode) {
                    System.out.println("[M] " + this.tloi.getId() + " was successfully executed.");
                } else {
                    System.out.println("[M] Starting metamode after " + this.runList.size() + " runs.");
                    this.manager.executeTLOI(tloi, true);
                }
            } else if (status == Status.FAILED) {
                if (metamode) {
                    System.out.println("[M] " + this.tloi.getId() + " was executed with errors.");
                } else {
                    System.out.println("[M] " + this.tloi.getId() + " will not run metamode. Some runs failed.");
                }
            }
        } else {
            System.out.println("[M] " + this.tloi.getId() + " still pending.");
            manager.schedule(this, 20, TimeUnit.SECONDS);
        }
    }

    private void updateWorkflowStatus() {
        List<WorkflowInstantiation> list = metamode ? tloi.getMetaWorkflows() : tloi.getWorkflows();
        for (WorkflowInstantiation inst: list) {
            inst.setStatus(Status.SUCCESSFUL);
            for (Execution e: inst.getExecutions()) {
                if (e.getStatus() != Status.SUCCESSFUL) {
                    inst.setStatus(e.getStatus());
                    break;
                }
            }
        }
    }
}