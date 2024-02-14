package org.diskproject.shared.classes.util;

import org.diskproject.shared.classes.adapters.DataAdapter;

public class DataAdapterResponse {
    public String url, name, prefix, namespace, description, prefixResolution, id;

    public DataAdapterResponse (DataAdapter dataAdapter) {
        this.url = dataAdapter.getEndpointUrl();
        this.name = dataAdapter.getName();
        if (dataAdapter.getNamespace() != null)
            this.namespace = dataAdapter.getNamespace();
        if (dataAdapter.getPrefix() != null)
            this.prefix = dataAdapter.getPrefix();
        if (dataAdapter.getPrefix() != null)
            this.prefix = dataAdapter.getPrefix();
        if (dataAdapter.getDescription() != null)
            this.description = dataAdapter.getDescription();
        if (dataAdapter.getPrefixResolution() != null)
            this.prefixResolution = dataAdapter.getPrefixResolution();
        if (dataAdapter.getId() != null)
            this.id = dataAdapter.getId();
    }
}