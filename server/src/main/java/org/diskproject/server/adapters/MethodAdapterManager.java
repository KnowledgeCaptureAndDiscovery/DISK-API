package org.diskproject.server.adapters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.diskproject.server.repository.WingsAdapter;
import org.diskproject.server.util.Config;
import org.diskproject.server.util.Config.MethodAdapterConfig;
import org.diskproject.server.util.ConfigKeys;
import org.diskproject.shared.classes.adapters.MethodAdapter;
import org.diskproject.shared.classes.workflow.Workflow;
import org.diskproject.shared.classes.workflow.WorkflowVariable;

public class MethodAdapterManager {
    protected Map<String, MethodAdapter> byUrl, byName;
   
    public MethodAdapterManager () {
        this.byUrl = new HashMap<String, MethodAdapter>();
        this.byName = new HashMap<String, MethodAdapter>();
        // Read Config 
        for (MethodAdapterConfig ma: Config.get().methodAdapters) {
            MethodAdapter curAdapter = null;
            switch (ma.type) {
                case ConfigKeys.METHOD_TYPE_WINGS:
                    curAdapter = new WingsAdapter(ma.name, ma.endpoint, ma.username, ma.password, ma.domain, ma.internalServer);
                    break;
                case ConfigKeys.METHOD_TYPE_AIRFLOW:
                    curAdapter = new AirFlowAdapter(ma.name, ma.endpoint, ma.username, ma.password);
                    break;
                default:
                    System.out.println("Error: Method adapter type not found: '" + ma.type + "'");
                    break;
            }
            if (curAdapter != null) {
                if (ma.version != null)
                    curAdapter.setVersion(ma.version);
                this.byUrl.put(ma.endpoint, curAdapter);
            }
        }

        // Check method adapters:
        if (this.byUrl.size() == 0) {
            System.err.println("WARNING: No method adapters found on configuration file.");
        } else
            for (MethodAdapter curAdp : this.byUrl.values()) {
                this.byName.put(curAdp.getName(), curAdp);
                if (!curAdp.ping()) {
                    System.err.println("ERROR: Could not connect with " + curAdp.getEndpointUrl());
                }
            }
    }

    public MethodAdapter getMethodAdapterByUrl (String url) {
        if (this.byUrl.containsKey(url))
            return this.byUrl.get(url);
        return null;
    }

    public MethodAdapter getMethodAdapterByName (String name) {
        if (this.byName.containsKey(name))
            return this.byName.get(name);
        return null;
    }

    public List<Workflow> getWorkflowList () {
        List<Workflow> list = new ArrayList<Workflow>();
        for (MethodAdapter adapter : this.byUrl.values()) {
            for (Workflow wf : adapter.getWorkflowList()) {
                list.add(wf);
            }
        }
        return list;
    }

    public List<WorkflowVariable> getWorkflowVariablesByName (String sourceName, String id) {
        MethodAdapter cur = this.getMethodAdapterByName(sourceName);
        if (cur != null)
            return cur.getWorkflowVariables(id);
        return null;
    }

    public List<WorkflowVariable> getWorkflowVariablesByUrl (String sourceUrl, String id) {
        MethodAdapter cur = this.getMethodAdapterByUrl(sourceUrl);
        if (cur != null)
            return cur.getWorkflowVariables(id);
        return null;
    }

    public String toString () {
        String txt = "";
        for (String name: byName.keySet()) {
            txt += name + " -> " + byName.get(name).getEndpointUrl() + "\n";
        }
        return txt;
    }
}