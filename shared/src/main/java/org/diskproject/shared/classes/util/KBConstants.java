package org.diskproject.shared.classes.util;

public class KBConstants {
    // Internal URI
    public static final String DISK_URI     = "https://knowledgecaptureanddiscovery.github.io/DISK-Ontologies/disk/release/1.2.5/ontology.ttl";
    public static final String HYP_URI      = "https://knowledgecaptureanddiscovery.github.io/DISK-Ontologies/hypothesis/release/0.0.3/ontology.xml";
    public static final String QUESTION_URI = "https://knowledgecaptureanddiscovery.github.io/QuestionOntology/release/v1.3.1/ontology.ttl";

    public static final String DISK_NS      = "http://disk-project.org/ontology/disk#";
    public static final String HYP_NS       = "http://disk-project.org/ontology/hypothesis#";
    public static final String QUESTION_NS  = "https://w3id.org/sqo#";

    // Common namespaces 
    public static final String OWL_NS     = "http://www.w3.org/2002/07/owl#";
    public static final String RDF_NS     = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    public static final String RDFS_NS    = "http://www.w3.org/2000/01/rdf-schema#";
    public static final String XSD_NS     = "http://www.w3.org/2001/XMLSchema#";
    public static final String DCTERMS_NS = "http://purl.org/dc/terms/";
    public static final String DC_NS      = "http://purl.org/dc/elements/1.1/";

    public static String getAllPrefixes () {
        return "PREFIX xsd:  <" + KBConstants.XSD_NS + ">\n"
             + "PREFIX rdfs: <" + KBConstants.RDFS_NS + ">\n"
             + "PREFIX rdf:  <" + KBConstants.RDF_NS + ">\n"
             + "PREFIX disk: <" + KBConstants.DISK_NS + ">\n"
             + "PREFIX sqo: <" + KBConstants.QUESTION_NS + ">\n"
             + "PREFIX hyp: <" + KBConstants.HYP_NS + ">\n";
    }
}