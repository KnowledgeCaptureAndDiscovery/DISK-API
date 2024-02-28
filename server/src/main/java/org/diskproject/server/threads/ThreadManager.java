package org.diskproject.server.threads;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.diskproject.server.managers.MethodAdapterManager;
import org.diskproject.server.repository.DiskRepository;
import org.diskproject.shared.classes.adapters.MethodAdapter;
import org.diskproject.shared.classes.common.Status;
import org.diskproject.shared.classes.loi.LineOfInquiry;
import org.diskproject.shared.classes.loi.TriggeredLOI;
import org.diskproject.shared.classes.workflow.Execution;
import org.diskproject.shared.classes.workflow.VariableBinding;
import org.diskproject.shared.classes.workflow.WorkflowInstantiation;
import org.diskproject.shared.classes.workflow.WorkflowSeed;
import org.diskproject.shared.classes.workflow.WorkflowVariable;

public class ThreadManager {
    protected MethodAdapterManager methodAdapters;
    private DiskRepository disk;
    protected ScheduledExecutorService monitor;
    protected ExecutorService executor;

    public ThreadManager (MethodAdapterManager methodAdapters, DiskRepository disk) {
        this.disk = disk;
        this.methodAdapters = methodAdapters;
        this.executor = Executors.newFixedThreadPool(2);
        this.monitor = Executors.newScheduledThreadPool(0);
    }

    public void shutdownExecutors () {
        if (monitor  != null) monitor.shutdownNow();
        if (executor != null) executor.shutdownNow();
    }

    public MethodAdapterManager getMethodAdapters () {
        return methodAdapters;
    }

    public void schedule (MonitorThread thread, int time, TimeUnit unit) {
        this.monitor.schedule(thread, time, unit);
    }

    public void executeTLOI (TriggeredLOI tloi) {
        this.executeTLOI(tloi, false);
    }

    public void executeTLOI (TriggeredLOI tloi, Boolean meta) {
        //Continue here, we do not need the loi anymore.
        LineOfInquiry loi = disk.getLOI(tloi.getParentLoi().getId());
        if (meta) {
            tloi.setStatus(Status.RUNNING);
            this.addMetaBindings(tloi);
            this.updateTLOI(tloi);
        }
        ExecutionThread workflowThread = new ExecutionThread(this, tloi, loi, meta);
        executor.execute(workflowThread);
    }

    public void monitorTLOI (TriggeredLOI tloi, LineOfInquiry loi, Boolean metamode) {
        MonitorThread monitorThread = new MonitorThread(this, tloi, metamode);
        monitor.schedule(monitorThread, 15, TimeUnit.SECONDS);
    }

    public void updateTLOI (TriggeredLOI tloi) {
        String localId = tloi.getId().replaceAll("^.*\\/", "");
        disk.updateTriggeredLOI(localId, tloi);
    }

    public void processFinishedRun (TriggeredLOI tloi, WorkflowSeed wf, Execution run, boolean meta) {
        disk.processWorkflowOutputs(wf, run, meta);
    }

    private boolean addMetaBindings (TriggeredLOI tloi) {
        List<String> dates = new ArrayList<String>();
        Map<String, List<String>> files = new HashMap<String, List<String>>();
            System.out.println("[M] Adding data to metaworkflow");
        boolean allOk = true;
        //Get all 
        String thisParentLoiId = tloi.getParentLoi().getId();
        String thisParentGoalId = tloi.getParentGoal().getId();
        for (TriggeredLOI cur: disk.listTriggeredLOIs()) {
            String parentLoiId = cur.getParentLoi().getId();
            String parentGoalId = cur.getParentGoal().getId();
            if (thisParentGoalId.equals(parentGoalId) && thisParentLoiId.equals(parentLoiId)) {
                //TLOIs that match both, LOI & Hypothesis
                for (WorkflowInstantiation wf: cur.getWorkflows()) {
                    for (Execution run: wf.getExecutions()) {
                        for (VariableBinding out: run.getOutputs()) {
                            //FIXME: continue here.
                            //if (!files.containsKey(outputName)) {
                            //    files.put(outputName, new ArrayList<String>());
                            //}
                            //List<String> list = files.get(outputName);
                            //list.add(out.id.replaceAll("^.*#", ""));
                        }
                        dates.add(String.valueOf(run.getEndDate()));
                    }
                }
            }
        }

        /*for (WorkflowSeed wf: tloi.getMetaWorkflowSeeds()) {
            MethodAdapter adapter = this.methodAdapters.getMethodAdapterByName(wf.getSource().getName());
            List<WorkflowVariable> vars = adapter.getWorkflowVariables(wf.getId());
             Check this also TODO
            for (VariableBinding vb: wf.getBindings()) {
                String binding = vb.getBinding();
                if (binding.equals("_RUN_DATE_")) {
                    vb.setBinding("[" + String.join(",", dates) + "]");
                } else {
                    if (binding.startsWith("[") && binding.endsWith("]")) {
                        binding = binding.substring(1, binding.length() -1);
                    }
                    for (String outName: files.keySet()) {
                        if (binding.equals("!" + outName)) {
                            vb.setBinding("[" + String.join(",", files.get(outName)) + "]");
                            String type = null;
                            for (WorkflowVariable wv: vars) {
                                if (vb.getVariable().equals(wv.getName()) && wv.getType().size() > 0) {
                                    type = wv.getType().get(0);
                                }
                            }
                            System.out.println("type: " + type);
                            // Upload files:
                            for (String dataid: files.get(outName)) {
                                if (!adapter.registerData(dataid, type));
                                    allOk = false;
                            }
                        }
                    }
                }
            }
        }*/

        return allOk;
    }
}