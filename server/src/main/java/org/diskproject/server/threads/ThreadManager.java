package org.diskproject.server.threads;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.diskproject.server.adapters.MethodAdapterManager;
import org.diskproject.server.repository.DiskRepository;
import org.diskproject.shared.classes.adapters.MethodAdapter;
import org.diskproject.shared.classes.loi.LineOfInquiry;
import org.diskproject.shared.classes.loi.TriggeredLOI;
import org.diskproject.shared.classes.loi.WorkflowBindings;
import org.diskproject.shared.classes.workflow.VariableBinding;
import org.diskproject.shared.classes.workflow.WorkflowRun;
import org.diskproject.shared.classes.workflow.WorkflowVariable;
import org.diskproject.shared.classes.workflow.WorkflowRun.RunBinding;
import org.diskproject.shared.classes.workflow.WorkflowRun.Status;

public class ThreadManager {
    private static String USERNAME = "admin";
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
        LineOfInquiry loi = disk.getLOI(USERNAME, tloi.getParentLoiId());
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
        disk.updateTriggeredLOI(USERNAME, tloi.getId(), tloi);
    }

    public void processFinishedRun (TriggeredLOI tloi, WorkflowBindings wf, WorkflowRun run, boolean meta) {
        LineOfInquiry loi = disk.getLOI(USERNAME, tloi.getParentLoiId());
        MethodAdapter methodAdapter = this.getMethodAdapters().getMethodAdapterByName(wf.getSource());
        disk.processWorkflowOutputs(tloi, loi, wf, run, methodAdapter, meta);
    }

    private boolean addMetaBindings (TriggeredLOI tloi) {
        List<String> dates = new ArrayList<String>();
        Map<String, List<String>> files = new HashMap<String, List<String>>();
            System.out.println("Adding data to metaworkflow");
        boolean allOk = true;
        //Get all 
        for (TriggeredLOI cur: disk.listTriggeredLOIs(USERNAME)) {
            if (cur.getParentHypothesisId().equals(tloi.getParentHypothesisId()) && 
                cur.getParentLoiId().equals(tloi.getParentLoiId())) {
                //TLOIs that match both, LOI & Hypothesis
                for (WorkflowBindings wf: cur.getWorkflows()) {
            //MethodAdapter adapter = this.methodAdapters.getMethodAdapterByName(wf.getSource());
            //Map<String, WorkflowVariable> outputVariables = adapter.getWorkflowOutputs(wf.getWorkflow());
            //List<WorkflowVariable> outputVariables = adapter.getWorkflowVariables(wf.getWorkflow());
            //System.out.println(outputVariables);
            //FIXME: continue here.
                    for (WorkflowRun run: wf.getRuns().values()) {
                        for (String outputName: run.getOutputs().keySet()) {
                            RunBinding out = run.getOutputs().get(outputName);
                            if (!files.containsKey(outputName)) {
                                files.put(outputName, new ArrayList<String>());
                            }
                            List<String> list = files.get(outputName);
                            list.add(out.id.replaceAll("^.*#", ""));
                        }
                        dates.add(String.valueOf(run.getExecutionInfo().endTime));
                    }
                }
            }
        }

        for (WorkflowBindings wf: tloi.getMetaWorkflows()) {
            MethodAdapter adapter = this.methodAdapters.getMethodAdapterByName(wf.getSource());
            List<WorkflowVariable> vars = adapter.getWorkflowVariables(wf.getWorkflow());
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
        }

        return allOk;
    }
}