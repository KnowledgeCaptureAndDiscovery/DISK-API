package org.diskproject.server.managers;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.diskproject.server.adapters.GraphDBAdapter;
import org.diskproject.server.adapters.SparqlAdapter;
import org.diskproject.server.db.DiskDB;
import org.diskproject.server.util.Config;
import org.diskproject.server.util.Config.DataAdapterConfig;
import org.diskproject.server.util.ConfigKeys;
import org.diskproject.shared.classes.adapters.DataAdapter;
import org.diskproject.shared.classes.common.Endpoint;
import org.diskproject.shared.classes.loi.DataQueryTemplate;
import org.diskproject.shared.classes.loi.LineOfInquiry;

public class DataAdapterManager {
    protected Map<String, DataAdapter> byUrl, byName;
    protected Map<String, DataAdapter> byId;  // By endpoints ID
   
    public DataAdapterManager () {
        this.byUrl = new HashMap<String, DataAdapter>();
        this.byName = new HashMap<String, DataAdapter>();
        // Read Config 
        for (DataAdapterConfig da: Config.get().dataAdapters) {
            DataAdapter curAdapter = null;
            switch (da.type) {
                case ConfigKeys.DATA_TYPE_SPARQL:
                    curAdapter = new SparqlAdapter(da.endpoint, da.name, da.username, da.password);
                    break;
                case ConfigKeys.DATA_TYPE_GRAPH_DB:
                    GraphDBAdapter ga = new GraphDBAdapter(da.endpoint, da.name, da.username, da.password);
                    if (da.repository != null)
                        ga.setRepository(da.repository);
                    curAdapter = ga;
                    break;
                default:
                    System.out.println("Error: Data adapter type not found: '" + da.type + "'");
                    break;
            }
            if (da.type != null && curAdapter != null) {
                if (da.namespace != null && da.prefix != null) {
                    curAdapter.setPrefix(da.prefix, da.namespace);
                }
                if (da.prefixResolution != null) {
                    curAdapter.setPrefixResolution(da.prefixResolution);
                }
                if (da.description != null) {
                    curAdapter.setDescription(da.description);
                }
                this.byUrl.put(da.endpoint, curAdapter);
            }
        }

        // Check data adapters:
        if (this.byUrl.size() == 0) {
            System.err.println("WARNING: No data adapters found on configuration file.");
        } else {
            for (DataAdapter curAdp : this.byUrl.values()) {
                this.byName.put(curAdp.getName(), curAdp);
                if (!curAdp.ping()) {
                    System.err.println("ERROR: Could not connect with " + curAdp.getEndpointUrl());
                }
            }
        }
    }

    public DataAdapter getDataAdapterByUrl (String url) {
        if (this.byUrl.containsKey(url))
            return this.byUrl.get(url);
        return null;
    }

    public DataAdapter getDataAdapterByName (String name) {
        if (this.byName.containsKey(name))
            return this.byName.get(name);
        return null;
    }

    public String toString () {
        String txt = "";
        for (String name: byName.keySet()) {
            txt += name + " -> " + byName.get(name).getEndpointUrl() + "\n";
        }
        return txt;
    }

    public Collection<DataAdapter> values () {
        return this.byUrl.values();
    }

    /**
     * Create records for each adapter into the RDF database.
     * @param db Diskdb where to register the adapters.
     */
    public void registerAdapters (DiskDB db) {
        this.byId = new HashMap<String, DataAdapter>();
        for (DataAdapter adp: this.values()) {
            Endpoint cur = db.registerEndpoint(new Endpoint(adp.getName(), adp.getEndpointUrl()));
            String id = cur.getId();
            adp.setId(id);
            this.byId.put(id, adp);
        }
    }

    /**
     * Finds a data adapter by their endpoint id.
     * Only works after registerAdapters, as the id is dependent of the database.
     * @param   id    Id of the endpoint that represents the data adapter.
     * @return  The data adater with that endpoint id.
     */
    public DataAdapter getMethodAdapterById (String id) {
        if (this.byId != null && this.byId.containsKey(id))
            return this.byId.get(id);
        return null;
    }

    /**
     * Finds a data adapter by their endpoint.
     * Only works after registerAdapters, as endpoints are dependent of the database.
     * @param   endpoint    Endpoint that represents the data adapter.
     * @return  The data adater for that endpoint.
     */
    public DataAdapter getMethodAdapterByEndpoint (Endpoint endpoint) {
        if (endpoint != null && endpoint.getId() != null)
            return this.getMethodAdapterById(endpoint.getId());
        return null;
    }

    /**
     * Finds a data adapter by their endpoint.
     * Only works after registerAdapters, as endpoints are dependent of the database.
     * @param   endpoint    Endpoint that represents the data adapter.
     * @return  The data adater for that endpoint.
     */
    public DataAdapter getMethodAdapterByLOI (LineOfInquiry loi) {
        DataQueryTemplate dqt = loi.getDataQueryTemplate();
        Endpoint e = dqt == null ? null : dqt.getEndpoint();
        return e == null ? null : getMethodAdapterByEndpoint(e);
    }
}