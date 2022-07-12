package org.diskproject.server.util;

import java.io.File;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.plist.PropertyListConfiguration;

public class Config {
  static Config singleton = null;
  PropertyListConfiguration props = null;
  
  public static void load() {
    singleton = new Config();
  }
  
  public static Config get() {
    return singleton;
  }
  
  public Config () {
    this.props = loadServerConfiguration();
  }
  
  public PropertyListConfiguration getProperties() {
    try {
		  props.load();
    } catch (ConfigurationException e) {
      e.printStackTrace();
    }
    return this.props;
  }
  
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
}