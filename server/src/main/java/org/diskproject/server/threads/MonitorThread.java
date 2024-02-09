package org.diskproject.server.threads;

import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.diskproject.shared.classes.adapters.MethodAdapter;
import org.diskproject.shared.classes.loi.TriggeredLOI;
import org.diskproject.shared.classes.workflow.WorkflowInstantiation;
import org.diskproject.shared.classes.workflow.WorkflowRun;
import org.diskproject.shared.classes.workflow.WorkflowRun.RuntimeInfo;
import org.diskproject.shared.classes.workflow.WorkflowRun.Status;

public class MonitorThread implements Runnable {
    ThreadManager manager;
    boolean metamode;
    TriggeredLOI tloi;
    List<String> runList;                   // runList = [runId1, runId2, ...]
    Map<String, WorkflowRun> runInfo;       // runInfo[runId1] = WorkflowRun
    Map<String, WorkflowInstantiation> runToWf;  // runToWf[runId1] = WorkflowBindings
    Map<String, Integer> runToIndex;        // runToIndex[runId1] = 0,1...

    public MonitorThread (ThreadManager manager, TriggeredLOI tloi, boolean metamode) {
        this.tloi = tloi;
        this.manager = manager;
        this.metamode = metamode;

        this.runInfo = new HashMap<String, WorkflowRun>();
        this.runToWf = new HashMap<String, WorkflowInstantiation>();
        this.runToIndex = new HashMap<String, Integer>();
        this.runList = new ArrayList<String>();

        Integer index = 0;
        for (WorkflowInstantiation curWorkflow : (metamode ? this.tloi.getMetaWorkflows() : this.tloi.getWorkflows())) {
            //FIXME:
            //for (WorkflowRun run : curWorkflow.getRuns().values()) {
            //    RuntimeInfo exec = run.getExecutionInfo();
            //    Status st = exec.status;
            //    String runId = run.getId();
            //    if (st == Status.PENDING || st == Status.QUEUED || st == Status.RUNNING) {
            //        runList.add(runId);
            //        runInfo.put(runId, run);
            //        runToWf.put(runId, curWorkflow);
            //        runToIndex.put(runId, index);
            //    }
            //}
            index += 1;
        }
    }

    private boolean needsMonitoring (RuntimeInfo run) {
        return !(run.status == Status.SUCCESSFUL || run.status == Status.FAILED);
    }

    private WorkflowRun getNextPendingRun () {
        for (String runId: this.runList) {
            WorkflowRun run = this.runInfo.get(runId);
            if (needsMonitoring(run.getExecutionInfo()) && run.getId() != null)
                return run;
        }
        return null;
    }

    private Status getOverallStatus () {
        for (String runId: this.runList) {
            WorkflowRun run = this.runInfo.get(runId);
            Status status = run.getExecutionInfo().status;
            if (status == Status.FAILED)
                return Status.FAILED;
            if (status != Status.SUCCESSFUL) //If all of them are pending then the tloi should be pending too.
                return Status.RUNNING;
        }
        return Status.SUCCESSFUL;
    }

    private void updateRun (WorkflowInstantiation wf, WorkflowRun run) {
        this.runInfo.replace(run.getId(), run); // Updates status on the run list
        //wf.addRun(run); // This replaces old run. FIXME
        Integer index = this.runToIndex.get(run.getId());
        List<WorkflowInstantiation> list = this.metamode ? this.tloi.getMetaWorkflows() : this.tloi.getWorkflows();
        list.set(index, wf); // Replaces run in the wf list
        if (this.metamode) this.tloi.setMetaWorkflows(list);
        else this.tloi.setWorkflows(list);

        // Process outputs
        if (run.getExecutionInfo().status == Status.SUCCESSFUL) {
            this.manager.processFinishedRun(this.tloi, wf, run, metamode);
        }
    }

    @Override
    public void run() {
        System.out.println("[M] Running monitoring thread");
        WorkflowRun pendingRun = this.getNextPendingRun();
        if (pendingRun == null || pendingRun.getId() == null) {
            System.out.println("[M] No more pending runs.");
            return;
        }
        WorkflowInstantiation wf = runToWf.get(pendingRun.getId());
        MethodAdapter methodAdapter = this.manager.getMethodAdapters().getMethodAdapterByName(wf.getSource().getName());
        if (methodAdapter == null) {
            System.out.println("[M] Error: Method adapter not found: " + wf.getSource());
            return;
        }

        String runId = pendingRun.getId().replaceAll("^.*#", "");
        WorkflowRun updatedRun = methodAdapter.getRunStatus(runId);

        RuntimeInfo oldExec = pendingRun.getExecutionInfo();
        RuntimeInfo newExec = updatedRun != null ? updatedRun.getExecutionInfo() : null;
        // If we cannot get the status but the run was pending, it means that the run is in the WINGS queue.
        if (newExec == null || newExec.status == null) {
            System.out.println("[E] Cannot get status for " + tloi.getId() + " - RUN " + runId);
            if (oldExec.status == Status.PENDING) { // In queue
                updatedRun = pendingRun;
            } else {
                System.out.println("[E] This should not happen");
                //???
                return;
            }
        }
        updateRun(wf, updatedRun);

        Status status = getOverallStatus();
        //this.tloi.setStatus(status); FIXME
        manager.updateTLOI(tloi);

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
        } else {
            System.out.println("[M] " + this.tloi.getId() + " still pending.");
            manager.schedule(this, 10, TimeUnit.SECONDS);
        }
    }
}