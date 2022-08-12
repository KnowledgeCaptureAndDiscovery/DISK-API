package org.diskproject.shared.classes.util;

public class KBConstants {
  private static String diskuri = "https://knowledgecaptureanddiscovery.github.io/DISK-Ontologies/disk/release/1.2.4/ontology.ttl";
  private static String hypuri  = "https://knowledgecaptureanddiscovery.github.io/DISK-Ontologies/hypothesis/release/0.0.3/ontology.xml";
  private static String questionsuri = "https://knowledgecaptureanddiscovery.github.io/QuestionOntology/release/v1.0.0/ontology.xml";

  private static String diskns = "http://disk-project.org/ontology/disk#";
  private static String hypns = "http://disk-project.org/ontology/hypothesis#";
  private static String questionsns = "https://w3id.org/sqo#";

  private static String owlns = "http://www.w3.org/2002/07/owl#";
  private static String rdfns = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
  private static String rdfsns = "http://www.w3.org/2000/01/rdf-schema#";
  private static String xsdns = "http://www.w3.org/2001/XMLSchema#";

  private static String dctermsns = "http://purl.org/dc/terms/";
  private static String dcns = "http://purl.org/dc/elements/1.1/";

  public static String QUESTIONSURI() {
    return questionsuri;
  }
  
  public static String QUESTIONSNS () {
	  return questionsns;
  }

  public static String DISKURI() {
    return diskuri;
  }
  
  public static String DISKNS() {
    return diskns;
  }
  
  public static String HYPURI() {
    return hypuri;
  }

  public static String HYPNS() {
    return hypns;
  }
 
  public static String DCTERMSNS() {
    return dctermsns;
  }

  public static String DCNS() {
    return dcns;
  }

  public static String OWLNS() {
    return owlns;
  }

  public static String RDFNS() {
    return rdfns;
  }

  public static String RDFSNS() {
    return rdfsns;
  }
  
  public static String XSDNS() {
    return xsdns;
  }
}
