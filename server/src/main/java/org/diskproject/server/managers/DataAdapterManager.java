package org.diskproject.server.managers;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.diskproject.server.adapters.GraphDBAdapter;
import org.diskproject.server.adapters.SparqlAdapter;
import org.diskproject.server.util.Config;
import org.diskproject.server.util.Config.DataAdapterConfig;
import org.diskproject.server.util.ConfigKeys;
import org.diskproject.shared.classes.adapters.DataAdapter;

public class DataAdapterManager {
    protected Map<String, DataAdapter> byUrl, byName;
   
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
}