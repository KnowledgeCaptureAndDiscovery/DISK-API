package org.diskproject.server.threads;

import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.diskproject.shared.classes.adapters.MethodAdapter;
import org.diskproject.shared.classes.common.Status;
import org.diskproject.shared.classes.common.Value.Type;
import org.diskproject.shared.classes.loi.TriggeredLOI;
import org.diskproject.shared.classes.workflow.Execution;
import org.diskproject.shared.classes.workflow.ExecutionRecord;
import org.diskproject.shared.classes.workflow.VariableBinding;
import org.diskproject.shared.classes.workflow.WorkflowInstantiation;
import org.diskproject.shared.classes.workflow.WorkflowRun;
import org.diskproject.shared.classes.workflow.WorkflowRun.RunBinding;
import org.diskproject.shared.classes.workflow.WorkflowRun.RuntimeInfo;

public class MonitorThread implements Runnable {
    ThreadManager manager;
    boolean metamode;
    TriggeredLOI tloi;
    List<String> runList;                   // runList = [runId1, runId2, ...]
    Map<String, Execution> runInfo;       // runInfo[runId1] = WorkflowRun
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
        //wf.addRun(run); // This replaces old run. FIXME
        Integer index = this.runToIndex.get(run.getExternalId());
        List<WorkflowInstantiation> list = this.metamode ? this.tloi.getMetaWorkflows() : this.tloi.getWorkflows();
        list.set(index, wf); // Replaces run in the wf list
        if (this.metamode) this.tloi.setMetaWorkflows(list);
        else this.tloi.setWorkflows(list);

        // Process outputs
        if (run.getStatus() == Status.SUCCESSFUL) {
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
            Execution updatedRun = workflowRunToExecution(methodAdapter.getRunStatus(runId));

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
            manager.schedule(this, 20, TimeUnit.SECONDS);
        }
    }

    private Execution workflowRunToExecution (WorkflowRun run) {
        if (run == null)
            return null;
        Execution e = new Execution(run.getId());
        if (run.getLink() != null)
            e.setLink(run.getLink());

        RuntimeInfo info = run.getExecutionInfo();
        if (info != null) {
            if (info.status != null) e.setStatus(info.status);
            if (info.log != null) e.setLog(info.log.endsWith("\n") ? info.log.substring(0,info.log.length()-1) : info.log );
            if (info.startTime > 0)  e.setStartDate(String.valueOf(info.startTime));
            if (info.endTime > 0)  e.setEndDate(String.valueOf(info.endTime));
        }
        List<RuntimeInfo> stepsInfo = run.getStepsInfo();
        List<ExecutionRecord> steps = new ArrayList<ExecutionRecord>();
        if (stepsInfo != null && stepsInfo.size() > 0) {
            for (RuntimeInfo rInfo: stepsInfo) {
                ExecutionRecord step = new ExecutionRecord();
                if (rInfo.status != null) step.setStatus(rInfo.status);
                if (rInfo.log != null) step.setLog(rInfo.log);
                if (rInfo.startTime > 0)  step.setStartDate(String.valueOf(rInfo.startTime));
                if (rInfo.endTime > 0)  step.setEndDate(String.valueOf(rInfo.endTime));
            }
        }
        e.setSteps(steps);
        Map<String, RunBinding> inputs = run.getInputs();
        List<VariableBinding> newInputBindings = new ArrayList<VariableBinding>();
        if (inputs != null && inputs.size() > 0) {
            for (String name: inputs.keySet()) {
                VariableBinding newVarBinding = new VariableBinding();
                RunBinding runBinding = inputs.get(name);
                newVarBinding.setVariable(name);
                if (runBinding.id != null && runBinding.type == Type.URI) {
                    newVarBinding.setSingleBinding(runBinding.id);
                    newVarBinding.setType(VariableBinding.BindingTypes.DISK_DATA.name());
                } else if (runBinding.value != null) {
                    newVarBinding.setSingleBinding(runBinding.value);
                    newVarBinding.setType(VariableBinding.BindingTypes.DEFAULT.name());
                }
                if (runBinding.datatype != null) newVarBinding.setDatatype(runBinding.datatype);
            }
        }
        e.setInputs(newInputBindings);

        Map<String, RunBinding> outputs = run.getOutputs();
        List<VariableBinding> newOutputBindings = new ArrayList<VariableBinding>();
        if (outputs != null && outputs.size() > 0) {

        }
        e.setInputs(newOutputBindings);

        return e;
    }
}