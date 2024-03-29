package org.diskproject.shared.classes.vocabulary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Vocabulary {
  String namespace, description, prefix, title;
  
  Map<String, Type> types;
  Map<String, Property> properties;
  Map<String, Individual> individuals;
  
  public Vocabulary() {
    this.types = new HashMap<String, Type>();
    this.properties = new HashMap<String, Property>();
    this.individuals = new HashMap<String, Individual>();
  }
  
  public Vocabulary(String ns) {
    this.namespace = ns;
    this.types = new HashMap<String, Type>();
    this.properties = new HashMap<String, Property>();
    this.individuals = new HashMap<String, Individual>();
  }

  public void setPrefix (String prefix) {
    this.prefix = prefix;
  }

  public String getPrefix () {
    return this.prefix;
  }

  public void setTitle (String title) {
    this.title = title;
  }

  public String getTitle () {
    return this.title;
  }
  
  public void refreshChildren() {
    for(Type type : this.types.values()) {
      type.getChildren().clear();
    }
    for(Type type : this.types.values()) {
      Type pType = this.getType(type.getParent());
      if(pType != null) {
        pType.getChildren().add(type.getId());
      }
    }
  }
  
  public void mergeWith(Vocabulary vocab) {
    this.types.putAll(vocab.getTypes());
    this.properties.putAll(vocab.getProperties());
    this.individuals.putAll(vocab.getIndividuals());
  }

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String ns) {
    this.namespace = ns;
  }

  public String getDescription () {
    return this.description;
  }

  public void setDescription (String desc) {
    this.description = desc;
  }

  public void addType(Type category) {
    this.types.put(category.getId(), category);
  }
  
  public void addProperty(Property property) {
    this.properties.put(property.getId(), property);
  }
  
  public void addIndividual(Individual individual) {
    this.individuals.put(individual.getId(), individual);
  }
  
  public Map<String, Type> getTypes() {
    return this.types;
  }
  
  public Map<String, Property> getProperties() {
    return this.properties;
  }
  
  public Map<String, Individual> getIndividuals() {
    return this.individuals;
  }
  
  public Type getType(String typeid) {
    return this.types.get(typeid);
  }
  
  public Property getProperty(String propertyid) {
    return this.properties.get(propertyid);
  }
  
  public Individual getIndividual(String individualId) {
    return this.individuals.get(individualId);
  }
  
  public List<Property> getPropertiesForType(Type type) {
    HashMap<String, Boolean> domains = new HashMap<String, Boolean>();
    ArrayList<Type> queue = new ArrayList<Type>();
    queue.add(type);
    while(!queue.isEmpty()) {
      Type qType = queue.remove(0);
      if(qType != null) {
        domains.put(qType.getId(), true);
        queue.add(this.getType(qType.getParent()));
      }
    }
    ArrayList<Property> list = new ArrayList<Property>();
    for(String propId : this.properties.keySet()) {
      Property prop = this.properties.get(propId);
      if(domains.containsKey(prop.getDomain()))
        list.add(prop);
    }
    return list;
  }
  
  public List<Individual> getIndividualsOfType(Type type) {
    ArrayList<Individual> list = new ArrayList<Individual>();
    for(Individual ind : this.individuals.values()) {
      if(ind.getType().equals(type.getId()))
        list.add(ind);
    }
    return list;
  }
  
  public List<Type> getSubTypes(Type type) {
    ArrayList<Type> types = new ArrayList<Type>();
    ArrayList<Type> queue = new ArrayList<Type>();
    queue.add(type);
    while(!queue.isEmpty()) {
      Type qType = queue.remove(0);
      if(qType != null) {
        types.add(qType);
        for(String subtypeId : qType.getChildren())
          queue.add(this.getType(subtypeId));
      }
    }
    return types;
  }
  
  public boolean isA(Type cls1, Type cls2) {
    ArrayList<Type> queue = new ArrayList<Type>();
    queue.add(cls1);
    while(!queue.isEmpty()) {
      Type qType = queue.remove(0);
      if(qType.getId().equals(cls2.getId()))
        return true;
      if(qType.getParent() != null)
        queue.add(this.getType(qType.getParent()));
    }
    return false;
  }
}
