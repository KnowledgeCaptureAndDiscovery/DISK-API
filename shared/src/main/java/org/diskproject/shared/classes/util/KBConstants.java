package org.diskproject.shared.classes.util;

public class KBConstants {
    // Internal URI
    //public static final String DISK_URI     = "https://knowledgecaptureanddiscovery.github.io/DISK-Ontologies/disk/release/1.2.5/ontology.ttl";
    public static final String DISK_URI     = "https://raw.githubusercontent.com/KnowledgeCaptureAndDiscovery/DISK-Ontologies/1.3/disk/release/1.3.0/ontology.xml";
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
             + "PREFIX rdf:  <" + KBConstants.RDF_NS + ">\n";
            // + "PREFIX disk: <" + KBConstants.DISK_NS + ">\n"
            // + "PREFIX sqo: <" + KBConstants.QUESTION_NS + ">\n"
            // + "PREFIX hyp: <" + KBConstants.HYP_NS + ">\n";
    }

    // Date config, how is written on the db
    public static final String DATE_FORMAT      = "yyyy-MM-dd'T'HH:mm:ssX";

    public static final class SPECIAL {
        public static final String CSV          = "_CSV_";
        public static final String NO_STORE     = "_DO_NO_STORE_";
        public static final String DOWNLOAD     = "_DOWNLOAD_ONLY_";
        public static final String IMAGE        = "_IMAGE_";
        public static final String VISUALIZE    = "_VISUALIZE_";
        public static final String BRAIN_VIZ    = "_BRAIN_VISUALIZATION_";
        public static final String CONFIDENCE_V = "_CONFIDENCE_VALUE_";
        public static final String RUN_DATES    = "_RUN_DATE_";
        public static final String SHINY_LOG    = "_SHINY_LOG_";
    }
}