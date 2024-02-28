package org.diskproject.server.managers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.diskproject.server.adapters.AirFlowAdapter;
import org.diskproject.server.adapters.wings.WingsAdapter;
import org.diskproject.server.db.DiskDB;
import org.diskproject.server.util.Config;
import org.diskproject.server.util.Config.MethodAdapterConfig;
import org.diskproject.server.util.ConfigKeys;
import org.diskproject.shared.classes.adapters.MethodAdapter;
import org.diskproject.shared.classes.common.Endpoint;
import org.diskproject.shared.classes.util.WorkflowTemplateResponse;
import org.diskproject.shared.classes.workflow.WorkflowTemplate;
import org.diskproject.shared.classes.workflow.WorkflowVariable;

public class MethodAdapterManager {
    protected Map<String, MethodAdapter> byUrl, byName;
    protected Map<String, MethodAdapter> byId;  // By endpoints ID
   
    /**
     * Manages and indexes a group of method adapters.
     */
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

    /**
     * Finds a method adapter by its name.
     * @param   name    Name of the method adapter.
     * @return  The method adater with that name.
     */
    public MethodAdapter getMethodAdapterByName (String name) {
        if (this.byName.containsKey(name))
            return this.byName.get(name);
        return null;
    }

    public List<WorkflowTemplateResponse> getWorkflowList () {
        List<WorkflowTemplateResponse> list = new ArrayList<WorkflowTemplateResponse>();
        for (MethodAdapter adapter : this.byUrl.values()) {
            for (WorkflowTemplate wf : adapter.getWorkflowList()) {
                list.add(new WorkflowTemplateResponse(wf, 
                    new Endpoint(adapter.getName(), adapter.getEndpointUrl(), adapter.getId())
                ));
            }
        }
        return list;
    }

    public Collection<MethodAdapter> values () {
        return this.byUrl.values();
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

    /**
     * Create records for each adapter into the RDF database.
     * @param db DiskDB where to register the adapters.
     */
    public void registerAdapters (DiskDB db) {
        this.byId = new HashMap<String, MethodAdapter>();
        for (MethodAdapter adp: this.values()) {
            Endpoint cur = db.registerEndpoint(new Endpoint(adp.getName(), adp.getEndpointUrl()));
            String id = cur.getId();
            adp.setId(id);
            byId.put(id, adp);
        }
    }

    /**
     * Finds a method adapter by their endpoint id.
     * Only works after registerAdapters, as the id is dependent of the database.
     * @param   id    Id of the endpoint that represents the method adapter.
     * @return  The method adater with that endpoint id.
     */
    public MethodAdapter getMethodAdapterById (String id) {
        if (this.byId != null && this.byId.containsKey(id))
            return this.byId.get(id);
        return null;
    }

    /**
     * Finds a method adapter by their endpoint.
     * Only works after registerAdapters, as endpoints are dependent of the database.
     * @param   endpoint    Endpoint that represents the method adapter.
     * @return  The method adater for that endpoint.
     */
    public MethodAdapter getMethodAdapterByEndpoint (Endpoint endpoint) {
        if (endpoint != null && endpoint.getId() != null)
            return this.getMethodAdapterById(endpoint.getId());
        return null;
    }
}