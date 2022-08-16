package org.diskproject.server.util;

public class ConfigKeys {
    // File destination
    public static final String LOCAL_DIR = ".disk";
    public static final String SYSTEM_DIR = "/etc/disk";
    public static final String FILENAME = "server.properties";

    // Configuration sections
    public static final String VOCABULARIES = "vocabularies";
    public static final String DATA_ADAPTERS = "data-adapters";
    public static final String METHOD_ADAPTERS = "method-adapters";

    public static final String TITLE = "title";
    public static final String PREFIX = "prefix";
    public static final String PREFIX_RESOLUTION = "prefix-resolution";
    public static final String URL = "url";
    public static final String NAMESPACE = "namespace";
    public static final String DESCRIPTION = "description";

    public static final String ENDPOINT = "endpoint";
    public static final String TYPE = "type";
    public static final String VERSION = "version";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String INVENTORY = "inventory";

    public static final String DOMAIN = "domain";
    public static final String INTERNAL_SERVER = "internal_server";

    public static final String DATA_TYPE_SPARQL = "sparql";
    public static final String METHOD_TYPE_WINGS = "wings";
    public static final String METHOD_TYPE_AIRFLOW = "airflow";
    public static final String METHOD_TYPE_REANA = "reana";
}
