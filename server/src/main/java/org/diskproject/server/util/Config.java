package org.diskproject.server.util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

//import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.plist.PropertyListConfiguration;

public class Config {
  static Config singleton = null;
  PropertyListConfiguration props = null;
  //This is the configuration
  public class StorageConfig {
    public String local, tdb, db, external, username, password;
    public StorageConfig (String local, String tdb, String db, String external, String username, String password) {
      this.local = local;
      this.tdb = tdb;
      this.db = db;
      this.external = external;
      this.username = username;
      this.password = password;
    }
  }

  public class KeycloakConfig {
    public String url, realm;
    public KeycloakConfig (String url, String realm) {
      this.url = url;
      this.realm = realm;
    }
  }

  public class DataAdapterConfig {
    public String name, type, endpoint, repository, username, password, description, namespace, prefix, prefixResolution;
    public DataAdapterConfig (String name, String type, String endpoint, String repository, String username, String password,
        String description, String namespace, String prefix, String prefixResolution) {
      this.name = name;
      this.type = type;
      this.endpoint = endpoint;
      this.repository = repository;
      this.username = username;
      this.password = password;
      this.description = description;
      this.namespace = namespace;
      this.prefix = prefix;
      this.prefixResolution = prefixResolution;
    }
  }

  public class MethodAdapterConfig {
    public String name, type, endpoint, username, password, description, internalServer, domain;
    public Float version;
    public MethodAdapterConfig (String name, String type, String endpoint, String username, String password, String description, String internalServer, String domain, Float v) {
      this.name = name;
      this.type = type;
      this.endpoint = endpoint;
      this.username = username;
      this.password = password;
      this.description = description;
      this.internalServer = internalServer;
      this.domain = domain;
      this.version = v;
    }
  }

  public class VocabularyConfig {
    public String name, url, prefix, title, namespace, description;
    public VocabularyConfig (String name, String url, String prefix, String title, String namespace, String description) {
      this.name = name;
      this.url = url;
      this.prefix = prefix;
      this.title = title;
      this.namespace = namespace;
      this.description = description;
    }
  }

  public String server;
  public StorageConfig storage;
  public KeycloakConfig keycloak;
  public List<DataAdapterConfig> dataAdapters;
  public List<MethodAdapterConfig> methodAdapters;
  public List<VocabularyConfig> vocabularies;
  public Map<String, String> questions;

  public static void load() {
    singleton = new Config();
  }

  public static Config get() {
    return singleton;
  }

  public Config() {
    this.props = loadServerConfiguration();
    //Load configuration to memory;
    this.loadDISKConfiguration();
  }

  //public PropertyListConfiguration getProperties() {
  //  try {
  //    props.load();
  //  } catch (ConfigurationException e) {
  //    e.printStackTrace();
  //  }
  //  return this.props;
  //}

  private PropertyListConfiguration loadServerConfiguration() {
    String configFile = null;
    String home = System.getProperty("user.home");
    if (home != null && !home.equals(""))
      configFile = home + File.separator + ConfigKeys.LOCAL_DIR + File.separator + ConfigKeys.FILENAME;
    else
      configFile = ConfigKeys.SYSTEM_DIR + File.separator + ConfigKeys.FILENAME;
    // Create configFile if it doesn't exist (portal.properties)
    File cFile = new File(configFile);
    if (!cFile.exists()) {
      if (!cFile.getParentFile().exists() && !cFile.getParentFile().mkdirs()) {
        System.err.println("Cannot create config file directory : " + cFile.getParent());
        return null;
      }
      createDefaultServerConfig(configFile);
    }
    // Load properties from configFile
    PropertyListConfiguration props = new PropertyListConfiguration();
    try {
      props.load(configFile);
      props.setFileName(configFile);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return props;
  }

  private void createDefaultServerConfig(String configFile) {
    String storageDir = null;
    String home = System.getProperty("user.home");
    if (home != null && !home.equals(""))
      storageDir = home + File.separator + ConfigKeys.LOCAL_DIR + File.separator + "storage";
    else
      storageDir = System.getProperty("java.io.tmpdir") +
          File.separator + "disk" + File.separator + "storage";
    if (!new File(storageDir).mkdirs())
      System.err.println("Cannot create storage directory: " + storageDir);

    PropertyListConfiguration config = new PropertyListConfiguration();
    config.addProperty("server", "THIS_SERVER_URL");
    config.addProperty("storage.local", storageDir);
    config.addProperty("storage.tdb", storageDir + File.separator + "TDB");
    config.addProperty("storage.db", storageDir + File.separator + "DB");
    config.addProperty("keycloak.url", "YOUR_KEYCLOAK_URL");
    config.addProperty("keycloak.realm", "YOUR_KEYCLOAK_REALM");
    config.addProperty("data-adapters.EXAMPLE_ADAPTER.type", "ADD_HERE");
    config.addProperty("data-adapters.EXAMPLE_ADAPTER.endpoint", "URL_HERE");
    config.addProperty("data-adapters.EXAMPLE_ADAPTER.username", "ADD_HERE");
    config.addProperty("data-adapters.EXAMPLE_ADAPTER.password", "ADD_HERE");
    config.addProperty("method-adapters.EXAMPLE_ADAPTER.type", "ADD_HERE");
    config.addProperty("method-adapters.EXAMPLE_ADAPTER.endpoint", "URL_HERE");
    config.addProperty("method-adapters.EXAMPLE_ADAPTER.username", "ADD_HERE");
    config.addProperty("method-adapters.EXAMPLE_ADAPTER.password", "ADD_HERE");
    config.addProperty("question-templates.EXAMPLE_QUESTION_ONTOLOGY", "URL_HERE");
    config.addProperty("vocabularies.EXAMPLE_VOCABULARY.url", "URL_HERE");
    config.addProperty("vocabularies.EXAMPLE_VOCABULARY.prefix", "ADD_HERE");
    config.addProperty("vocabularies.EXAMPLE_VOCABULARY.namespace", "ADD_HERE");

    try {
      config.setFileName("file://" + configFile);
      config.save("file://" + configFile);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private Map<String, Map<String, String>> configIteratorToMap (Iterator<String> a) {
    Map<String, Map<String, String>> map = new HashMap<String, Map<String, String>>(); // vocabMap[name -> map[propName -> propValue]]
    while (a.hasNext()) {
      String key = a.next();
      String sp[] = key.split("\\.");
      if (sp != null && sp.length == 3) { // as the list is normalized length is how deep the property is, eg: vocabularies.NAME.property
        String name = sp[1];
        String propName = sp[2];
        String value = props.getString(key);
        Map<String, String> propMap;
        if (map.containsKey(name))
          propMap = map.get(name);
        else {
          propMap = new HashMap<String, String>();
          map.put(name, propMap);
        }
        propMap.put(propName, value);
      }
    }
    return map;
  }

  private void loadDISKConfiguration() {
    // Server configuration
    this.server = props.getString("server");
    String storageLocal = props.getString("storage.local");
    String storageTdb = props.getString("storage.tdb");
    String storageDb = props.getString("storage.db");

    String external = props.getString("storage.external");
    String ex_user = props.getString("storage.username");
    String ex_pass = props.getString("storage.password");

    this.storage = new StorageConfig(storageLocal, storageTdb, storageDb, external, ex_user, ex_pass);
    System.out.println(external);
    System.out.println(ex_user);
    System.out.println(ex_pass);

    String keycloakUrl = props.getString("keycloak.url");
    String keycloakRealm = props.getString("keycloak.realm");
    this.keycloak = new KeycloakConfig(keycloakUrl, keycloakRealm);
    // Question ontologies
    this.questions = new HashMap<String, String>();
    Iterator<String> a = props.getKeys(ConfigKeys.QUESTION_TEMPLATES);
    while (a.hasNext()) {
      String key = a.next();
      String[] sp = key.split("\\.");
      String name = sp[1];
      this.questions.put(name, props.getString(key));
    }
    // Data Adapters
    List<DataAdapterConfig> dataAdapterList = new ArrayList<DataAdapterConfig>();
    Map<String, Map<String, String>> dataMap = configIteratorToMap(props.getKeys(ConfigKeys.DATA_ADAPTERS));
    for (String name : dataMap.keySet()) {
      Map<String, String> propMap = dataMap.get(name);
      // Check minimal fields
      if (!(propMap.containsKey(ConfigKeys.ENDPOINT) && propMap.containsKey(ConfigKeys.TYPE))) {
        String errorMessage = "Error reading configuration file. Data adapters must have '" 
            + ConfigKeys.ENDPOINT + "' and '" + ConfigKeys.TYPE + "'";
        System.err.println( errorMessage );
        continue; // Non critical error
      }
      dataAdapterList.add(new DataAdapterConfig(
        name, 
        propMap.get(ConfigKeys.TYPE),
        propMap.get(ConfigKeys.ENDPOINT),
        propMap.get(ConfigKeys.REPOSITORY),
        propMap.get(ConfigKeys.USERNAME),
        propMap.get(ConfigKeys.PASSWORD),
        propMap.get(ConfigKeys.DESCRIPTION),
        propMap.get(ConfigKeys.NAMESPACE),
        propMap.get(ConfigKeys.PREFIX),
        propMap.get(ConfigKeys.PREFIX_RESOLUTION)
      )) ;
    }
    this.dataAdapters = dataAdapterList;
    // Method Adapters
    List<MethodAdapterConfig> methodAdapterList = new ArrayList<MethodAdapterConfig>();
    Map<String, Map<String, String>> methodMap = configIteratorToMap(props.getKeys(ConfigKeys.METHOD_ADAPTERS));
    for (String name : methodMap.keySet()) {
      Map<String, String> propMap = methodMap.get(name);
      // Check minimal fields
      if (!(propMap.containsKey(ConfigKeys.ENDPOINT) && propMap.containsKey(ConfigKeys.TYPE))) {
        String errorMessage = "Error reading configuration file. Method adapters must have '" 
            + ConfigKeys.ENDPOINT + "' and '" + ConfigKeys.TYPE + "'";
        System.err.println( errorMessage );
        continue; // Non critical error
      }
      methodAdapterList.add(new MethodAdapterConfig(
        name,
        propMap.get(ConfigKeys.TYPE),
        propMap.get(ConfigKeys.ENDPOINT),
        propMap.get(ConfigKeys.USERNAME),
        propMap.get(ConfigKeys.PASSWORD),
        propMap.get(ConfigKeys.DESCRIPTION),
        propMap.get(ConfigKeys.INTERNAL_SERVER),
        propMap.get(ConfigKeys.DOMAIN),
        propMap.get(ConfigKeys.VERSION) != null ? Float.parseFloat(propMap.get(ConfigKeys.VERSION)) : null
      )) ;
    }
    this.methodAdapters = methodAdapterList;
    // Vocabularies
    List<VocabularyConfig> vocabularyList = new ArrayList<VocabularyConfig>();
    Map<String, Map<String, String>> vocabMap = configIteratorToMap(props.getKeys(ConfigKeys.VOCABULARIES));
    for (String name : vocabMap.keySet()) {
      Map<String, String> propMap = vocabMap.get(name);
      // Check minimal fields
      if (!(propMap.containsKey(ConfigKeys.URL) && propMap.containsKey(ConfigKeys.PREFIX)
          && propMap.containsKey(ConfigKeys.NAMESPACE) && propMap.containsKey(ConfigKeys.TITLE))) {
        String errorMessage = "Error reading configuration file. Vocabularies must have '"
            + ConfigKeys.URL + "', '" + ConfigKeys.TITLE + "', '" + ConfigKeys.PREFIX + "' and '"
            + ConfigKeys.NAMESPACE + "'";
        System.out.println(errorMessage);
        throw new RuntimeException(errorMessage);
      }
      vocabularyList.add(new VocabularyConfig(
            name,
            propMap.get(ConfigKeys.URL), 
            propMap.get(ConfigKeys.PREFIX), 
            propMap.get(ConfigKeys.TITLE), 
            propMap.get(ConfigKeys.NAMESPACE), 
            propMap.get(ConfigKeys.DESCRIPTION)
      ));
    }
    this.vocabularies = vocabularyList;
  }
}