package org.diskproject.server.repository;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.configuration.plist.PropertyListConfiguration;
import org.apache.commons.lang.SerializationUtils;
import org.apache.jena.query.QueryParseException;
import org.diskproject.server.adapters.AirFlowAdapter;
import org.diskproject.server.adapters.GraphDBAdapter;
import org.diskproject.server.adapters.SparqlAdapter;
import org.diskproject.server.util.Config;
import org.diskproject.server.util.ConfigKeys;
import org.diskproject.server.util.KBCache;
import org.diskproject.server.util.VocabularyConfiguration;
import org.diskproject.shared.classes.adapters.DataAdapter;
import org.diskproject.shared.classes.adapters.DataResult;
import org.diskproject.shared.classes.adapters.MethodAdapter;
import org.diskproject.shared.classes.common.Graph;
import org.diskproject.shared.classes.common.TreeItem;
import org.diskproject.shared.classes.common.Triple;
import org.diskproject.shared.classes.hypothesis.Hypothesis;
import org.diskproject.shared.classes.loi.LineOfInquiry;
import org.diskproject.shared.classes.loi.WorkflowBindings;
import org.diskproject.shared.classes.loi.TriggeredLOI;
import org.diskproject.shared.classes.loi.TriggeredLOI.Status;
import org.diskproject.shared.classes.question.Question;
import org.diskproject.shared.classes.question.QuestionVariable;
import org.diskproject.shared.classes.util.DataAdapterResponse;
import org.diskproject.shared.classes.util.GUID;
import org.diskproject.shared.classes.util.KBConstants;
import org.diskproject.shared.classes.vocabulary.Individual;
import org.diskproject.shared.classes.vocabulary.Property;
import org.diskproject.shared.classes.vocabulary.Vocabulary;
import org.diskproject.shared.classes.vocabulary.Type;
import org.diskproject.shared.classes.workflow.Variable;
import org.diskproject.shared.classes.workflow.VariableBinding;
import org.diskproject.shared.classes.workflow.Workflow;
import org.diskproject.shared.classes.workflow.WorkflowRun;
import org.diskproject.shared.ontologies.DISK;
import org.diskproject.shared.ontologies.SQO;

import edu.isi.kcap.ontapi.KBAPI;
import edu.isi.kcap.ontapi.KBObject;
import edu.isi.kcap.ontapi.KBTriple;
import edu.isi.kcap.ontapi.OntSpec;
import edu.isi.kcap.ontapi.SparqlQuerySolution;
import javax.ws.rs.NotFoundException;

public class DiskRepository extends WriteKBRepository {
    static DiskRepository singleton;
    private static boolean creatingKB = false;

    private static SimpleDateFormat dateformatter = new SimpleDateFormat("HH:mm:ss yyyy-MM-dd");
    Pattern varPattern = Pattern.compile("\\?(.+?)\\b");
    Pattern varCollPattern = Pattern.compile("\\[\\s*\\?(.+?)\\s*\\]");

    protected KBAPI questionKB, hypothesisVocabulary;
    protected KBCache SQOnt;

    Map<String, Vocabulary> vocabularies;
    ScheduledExecutorService monitor, monitorData;
    ExecutorService executor;
    static DataMonitor dataThread;

    private Map<String, List<List<String>>> optionsCache;
    private Map<String, VocabularyConfiguration> externalVocabularies;

    public static void main(String[] args) {
        get();
        get().shutdownExecutors();
    }

    public static DiskRepository get() {
        if (!creatingKB && singleton == null) {
            creatingKB = true;
            singleton = new DiskRepository();
            creatingKB = false;
        }
        return singleton;
    }

    public DiskRepository() {
        // Set domain for writing the KB
        setConfiguration();
        this.setDomain(this.server);

        // Initialize
        this.dataAdapters = new HashMap<String, DataAdapter>();
        this.methodAdapters = new HashMap<String, MethodAdapter>();
        this.initializeDataAdapters();
        this.initializeMethodAdapters();
        try {
            initializeKB();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        // Threads
        monitor = Executors.newScheduledThreadPool(0);
        executor = Executors.newFixedThreadPool(2);
        dataThread = new DataMonitor();
    }

    public void shutdownExecutors() {
        if (monitor != null)
            monitor.shutdownNow();
        if (executor != null)
            executor.shutdownNow();
        if (dataThread != null)
            dataThread.stop();
        if (monitorData != null)
            monitorData.shutdownNow();
    }

    /********************
     * Initialization
     */

    public void initializeKB() throws Exception {
        super.initializeKB(); // This initializes the DISK ontology
        if (fac == null)
            throw new Exception("Could not load DISK ontology");

        this.questionKB = fac.getKB(KBConstants.QUESTION_URI, OntSpec.PLAIN, false, true);
        this.hypothesisVocabulary = fac.getKB(KBConstants.HYP_URI, OntSpec.PLAIN, false, true);
        // Load questions
        optionsCache = new WeakHashMap<String, List<List<String>>>();
        this.start_read();
        SQOnt = new KBCache(this.questionKB);
        this.end();
        loadQuestionTemplates();
        this.loadKBFromConfig();
        this.initializeVocabularies();
    }

    private void loadKBFromConfig() throws Exception {
        this.externalVocabularies = new HashMap<String, VocabularyConfiguration>();

        PropertyListConfiguration cfg = this.getConfig();
        Map<String, Map<String, String>> ontologies = new HashMap<String, Map<String, String>>();
        Iterator<String> a = cfg.getKeys(ConfigKeys.VOCABULARIES);
        while (a.hasNext()) {
            String key = a.next();
            String sp[] = key.split("\\.");

            if (sp != null && sp.length == 3) { // as the list is normalized length is how deep the property is
                Map<String, String> map;
                if (ontologies.containsKey(sp[1]))
                    map = ontologies.get(sp[1]);
                else {
                    map = new HashMap<String, String>();
                    ontologies.put(sp[1], map);
                }
                map.put(sp[2], cfg.getProperty(key).toString());
            }
        }

        for (String name : ontologies.keySet()) {
            Map<String, String> cur = ontologies.get(name);
            // Check minimal fields
            if (!(cur.containsKey(ConfigKeys.URL) && cur.containsKey(ConfigKeys.PREFIX)
                    && cur.containsKey(ConfigKeys.NAMESPACE) && cur.containsKey(ConfigKeys.TITLE))) {
                String errorMessage = "Error reading configuration file. Vocabularies must have '"
                        + ConfigKeys.URL + "', '" + ConfigKeys.TITLE + "', '" + ConfigKeys.PREFIX + "' and '"
                        + ConfigKeys.NAMESPACE + "'";
                System.out.println(errorMessage);
                throw new RuntimeException(errorMessage);
            }
            String curUrl = cur.get(ConfigKeys.URL),
                    curPrefix = cur.get(ConfigKeys.PREFIX),
                    curNamespace = cur.get(ConfigKeys.NAMESPACE),
                    curTitle = cur.get(ConfigKeys.TITLE);

            KBAPI curKB = null;
            try {
                curKB = fac.getKB(curUrl, OntSpec.PLAIN, false, true);
            } catch (Exception e) {
                System.out.println("Could not load " + curUrl);
            }
            if (curKB != null) {
                VocabularyConfiguration vc = new VocabularyConfiguration(curPrefix, curUrl, curNamespace, curTitle);
                vc.setKB(curKB);
                if (cur.containsKey(ConfigKeys.DESCRIPTION))
                    vc.setDescription(cur.get(ConfigKeys.DESCRIPTION));

                this.externalVocabularies.put(curPrefix, vc);
            }
        }

        if (this.externalVocabularies.size() == 0) {
            System.err.println("WARNING: No external vocabularies found on the configuration file.");
        }
    }

    public void reloadKBCaches() throws Exception {
        KBAPI[] kbs = { this.ontKB, this.questionKB, this.hypothesisVocabulary };

        try {
            this.start_write();
            for (KBAPI kb : kbs)
                if (kb != null) {
                    System.out.println("Reloading " + kb.getURI());
                    kb.removeAllTriples();
                    kb.delete();
                    this.save(kb);
                    this.end();
                    this.start_write();
                }
            for (VocabularyConfiguration vc : this.externalVocabularies.values()) {
                KBAPI kb = vc.getKB();
                System.out.println("Reloading " + kb.getURI());
                kb.removeAllTriples();
                kb.delete();
                this.save(kb);
                this.end();

                vc.setKB(null);
                this.start_write();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            this.end();
        }

        this.initializeKB();
    }

    public PropertyListConfiguration getConfig() {
        return Config.get().getProperties();
    }

    // -- Data adapters
    private void initializeDataAdapters() {
        // Reads data adapters from config file.
        PropertyListConfiguration cfg = this.getConfig();
        Map<String, Map<String, String>> endpoints = new HashMap<String, Map<String, String>>();
        Iterator<String> a = cfg.getKeys(ConfigKeys.DATA_ADAPTERS);
        while (a.hasNext()) {
            String key = a.next();
            String sp[] = key.split("\\.");

            if (sp != null && sp.length == 3) { // as the list is normalized length is how deep the property is
                Map<String, String> map;
                if (endpoints.containsKey(sp[1]))
                    map = endpoints.get(sp[1]);
                else {
                    map = new HashMap<String, String>();
                    endpoints.put(sp[1], map);
                }
                map.put(sp[2], cfg.getProperty(key).toString());
            }
        }

        for (String name : endpoints.keySet()) {
            Map<String, String> cur = endpoints.get(name);
            if (!(cur.containsKey(ConfigKeys.ENDPOINT) && cur.containsKey(ConfigKeys.TYPE))) {
                System.err.println("Error reading configuration file. Data adapters must have '" + ConfigKeys.ENDPOINT
                        + "' and '" + ConfigKeys.TYPE + "'");
                continue;
            }

            String curURI = cur.get(ConfigKeys.ENDPOINT),
                    curType = cur.get(ConfigKeys.TYPE);
            String curUser = null, curPass = null, curNamespace = null, curPrefix = null, curDesc = null,
                    curPrefixRes = null;
            if (cur.containsKey(ConfigKeys.USERNAME))
                curUser = cur.get(ConfigKeys.USERNAME);
            if (cur.containsKey(ConfigKeys.PASSWORD))
                curPass = cur.get(ConfigKeys.PASSWORD);
            if (cur.containsKey(ConfigKeys.NAMESPACE))
                curNamespace = cur.get(ConfigKeys.NAMESPACE);
            if (cur.containsKey(ConfigKeys.PREFIX))
                curPrefix = cur.get(ConfigKeys.PREFIX);
            if (cur.containsKey(ConfigKeys.DESCRIPTION))
                curDesc = cur.get(ConfigKeys.DESCRIPTION);
            if (cur.containsKey(ConfigKeys.PREFIX_RESOLUTION))
                curPrefixRes = cur.get(ConfigKeys.PREFIX_RESOLUTION);

            DataAdapter curAdapter = null;
            switch (curType) {
                case ConfigKeys.DATA_TYPE_SPARQL:
                    curAdapter = new SparqlAdapter(curURI, name, curUser, curPass);
                    break;
                case ConfigKeys.DATA_TYPE_GRAPH_DB:
                    //curAdapter = new GraphDBAdapter(curURI, name, curUser, curPass);
                    GraphDBAdapter ga = new GraphDBAdapter(curURI, name, curUser, curPass);
                    if (cur.containsKey(ConfigKeys.REPOSITORY))
                        ga.setRepository(cur.get(ConfigKeys.REPOSITORY));
                    curAdapter = ga;
                    break;
                default:
                    break;
            }
            if (curType != null) {
                if (curNamespace != null && curPrefix != null) {
                    curAdapter.setPrefix(curPrefix, curNamespace);
                }
                if (curPrefixRes != null) {
                    curAdapter.setPrefixResolution(curPrefixRes);
                }
                if (curDesc != null) {
                    curAdapter.setDescription(curDesc);
                }
                this.dataAdapters.put(curURI, curAdapter);
            }
        }

        // Check data adapters:
        if (this.dataAdapters.size() == 0) {
            System.err.println("WARNING: No data adapters found on configuration file.");
        } else
            for (DataAdapter curAdp : this.dataAdapters.values()) {
                if (!curAdp.ping()) {
                    System.err.println("ERROR: Could not connect with " + curAdp.getEndpointUrl());
                }
            }
    }

    private DataAdapter getDataAdapter(String url) {
        if (this.dataAdapters.containsKey(url))
            return this.dataAdapters.get(url);
        return null;
    }

    public List<DataAdapterResponse> getDataAdapters() {
        List<DataAdapterResponse> adapters = new ArrayList<DataAdapterResponse>();
        for (DataAdapter da : this.dataAdapters.values()) {
            adapters.add(new DataAdapterResponse(da));
        }
        return adapters;
    }

    // -- Method adapters
    private void initializeMethodAdapters() {
        // Reads method adapters from config file.
        PropertyListConfiguration cfg = this.getConfig();
        Map<String, Map<String, String>> adapters = new HashMap<String, Map<String, String>>();
        Iterator<String> a = cfg.getKeys(ConfigKeys.METHOD_ADAPTERS);
        while (a.hasNext()) {
            String key = a.next();
            String sp[] = key.split("\\.");

            if (sp != null && sp.length == 3) { // as the list is normalized, length is how deep the property is
                Map<String, String> map;
                if (adapters.containsKey(sp[1]))
                    map = adapters.get(sp[1]);
                else {
                    map = new HashMap<String, String>();
                    adapters.put(sp[1], map);
                }
                map.put(sp[2], cfg.getProperty(key).toString());
            }
        }

        for (String name : adapters.keySet()) {
            Map<String, String> cur = adapters.get(name);
            if (!(cur.containsKey(ConfigKeys.ENDPOINT) && cur.containsKey(ConfigKeys.TYPE))) {
                System.err.println("Error reading configuration file. Method adapters must have '" + ConfigKeys.ENDPOINT
                        + "' and '" + ConfigKeys.TYPE + "'");
                continue;
            }
            String curURI = cur.get(ConfigKeys.ENDPOINT), curType = cur.get(ConfigKeys.TYPE);
            String curUser = null, curPass = null, curDomain = null, curInternalServer = null;
            Float curVersion = null;
            if (cur.containsKey(ConfigKeys.USERNAME))
                curUser = cur.get(ConfigKeys.USERNAME);
            if (cur.containsKey(ConfigKeys.PASSWORD))
                curPass = cur.get(ConfigKeys.PASSWORD);
            if (cur.containsKey(ConfigKeys.DOMAIN))
                curDomain = cur.get(ConfigKeys.DOMAIN);
            if (cur.containsKey(ConfigKeys.INTERNAL_SERVER))
                curInternalServer = cur.get(ConfigKeys.INTERNAL_SERVER);
            if (cur.containsKey(ConfigKeys.VERSION))
                curVersion = Float.parseFloat(cur.get(ConfigKeys.VERSION));

            MethodAdapter curAdapter = null;
            switch (curType) {
                case ConfigKeys.METHOD_TYPE_WINGS:
                    curAdapter = new WingsAdapter(name, curURI, curUser, curPass, curDomain, curInternalServer);
                    break;
                case ConfigKeys.METHOD_TYPE_AIRFLOW:
                    curAdapter = new AirFlowAdapter(name, curURI, curUser, curPass);
                    break;
                default:
                    break;
            }
            if (curAdapter != null) {
                if (curVersion != null)
                    curAdapter.setVersion(curVersion);
                this.methodAdapters.put(curURI, curAdapter);
            }
        }

        // Check method adapters:
        if (this.methodAdapters.size() == 0) {
            System.err.println("WARNING: No method adapters found on configuration file.");
        } else
            for (MethodAdapter curAdp : this.methodAdapters.values()) {
                if (!curAdp.ping()) {
                    System.err.println("ERROR: Could not connect with " + curAdp.getEndpointUrl());
                }
            }
    }

    public MethodAdapter getMethodAdapter(String url) {
        if (this.methodAdapters.containsKey(url))
            return this.methodAdapters.get(url);
        return null;
    }

    public List<Workflow> getWorkflowList() {
        List<Workflow> list = new ArrayList<Workflow>();
        for (MethodAdapter adapter : this.methodAdapters.values()) {
            for (Workflow wf : adapter.getWorkflowList()) {
                list.add(wf);
            }
        }
        return list;
    }

    public List<Variable> getWorkflowVariables(String source, String id) {
        for (MethodAdapter adapter : this.methodAdapters.values()) {
            if (adapter.getName().equals(source)) {
                return adapter.getWorkflowVariables(id);
            }
        }
        return null;
    }

    // -- Vocabulary Initialization
    private void initializeVocabularies() {
        this.vocabularies = new HashMap<String, Vocabulary>();
        try {
            this.start_read();
            this.vocabularies.put(KBConstants.DISK_URI,
                    this.initializeVocabularyFromKB(this.ontKB, KBConstants.DISK_NS,
                            "disk", "The DISK Ontology",
                            "DISK Main ontology. Defines all resources used on the DISK system."));
            this.vocabularies.put(KBConstants.QUESTION_URI,
                    this.initializeVocabularyFromKB(this.questionKB, KBConstants.QUESTION_NS,
                            "sqo", "Scientific Question Ontology", "Ontology to define questions templates."));
            this.vocabularies.put(KBConstants.HYP_URI,
                    this.initializeVocabularyFromKB(this.hypothesisVocabulary, KBConstants.HYP_NS,
                            "hyp", "DISK Hypothesis Ontology",
                            "The DISK Hypothesis Ontology. Defines properties to be used on Hypothesis creation."));

            // Load vocabularies from config file
            for (VocabularyConfiguration vc : this.externalVocabularies.values()) {
                KBAPI cur = vc.getKB();
                this.vocabularies.put(vc.getPrefix(), this.initializeVocabularyFromKB(cur, vc.getNamespace(),
                        vc.getPrefix(), vc.getTitle(), vc.getDescription()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            this.end();
        }
    }

    public Vocabulary getVocabulary(String uri) {
        return this.vocabularies.get(uri);
    }

    public Vocabulary initializeVocabularyFromKB(KBAPI kb, String ns, String prefix, String title, String description) {
        Vocabulary vocabulary = new Vocabulary(ns);
        vocabulary.setPrefix(prefix);
        vocabulary.setTitle(title);
        if (description != null)
            vocabulary.setDescription(description);
        this.fetchTypesAndIndividualsFromKB(kb, vocabulary);
        this.fetchPropertiesFromKB(kb, vocabulary);
        return vocabulary;
    }

    private void fetchPropertiesFromKB(KBAPI kb, Vocabulary vocabulary) {
        for (KBObject prop : kb.getAllProperties()) {
            if (!prop.getID().startsWith(vocabulary.getNamespace()))
                continue;

            KBObject domCls = kb.getPropertyDomain(prop);
            KBObject rangeCls = kb.getPropertyRange(prop);
            String desc = kb.getComment(prop);

            Property mProp = new Property();
            mProp.setId(prop.getID());
            mProp.setName(prop.getName());
            if (desc != null)
                mProp.setDescription(desc);

            String label = this.createPropertyLabel(prop.getName());
            mProp.setLabel(label);
            if (domCls != null)
                mProp.setDomain(domCls.getID());
            if (rangeCls != null)
                mProp.setRange(rangeCls.getID());

            vocabulary.addProperty(mProp);
        }
    }

    private void fetchTypesAndIndividualsFromKB(KBAPI kb, Vocabulary vocabulary) {
        KBObject typeprop = kb.getProperty(KBConstants.RDF_NS + "type");
        for (KBTriple t : kb.genericTripleQuery(null, typeprop, null)) {
            KBObject inst = t.getSubject();
            KBObject typeobj = t.getObject();
            String instId = inst.getID();

            if (instId == null || instId.startsWith(vocabulary.getNamespace())
                    || typeobj.getNamespace().equals(KBConstants.OWL_NS)) {
                continue;
            }

            // Add individual, this does not ADD individuals without type.
            Individual ind = new Individual();
            ind.setId(inst.getID());
            ind.setName(inst.getName());
            ind.setType(typeobj.getID());
            String label = kb.getLabel(inst);
            if (label == null)
                label = inst.getName();
            ind.setLabel(label);
            vocabulary.addIndividual(ind);

            // Add asserted types
            if (!typeobj.getID().startsWith(vocabulary.getNamespace()))
                continue;
            String clsId = typeobj.getID();
            Type type = new Type();
            type.setId(clsId);
            type.setName(typeobj.getName());
            type.setLabel(kb.getLabel(typeobj));
            vocabulary.addType(type);
        }

        // Add types not asserted
        KBObject clsObj = kb.getProperty(KBConstants.OWL_NS + "Class");
        for (KBTriple t : kb.genericTripleQuery(null, typeprop, clsObj)) {
            KBObject cls = t.getSubject();
            String clsId = cls.getID();
            if (clsId == null || !clsId.startsWith(vocabulary.getNamespace())
                    || cls.getNamespace().equals(KBConstants.OWL_NS)) {
                continue;
            }

            String desc = kb.getComment(cls);
            Type type = vocabulary.getType(clsId);
            if (type == null) {
                type = new Type();
                type.setId(clsId);
                type.setName(cls.getName());
                type.setLabel(kb.getLabel(cls));
                if (desc != null)
                    type.setDescription(desc);
                vocabulary.addType(type);
            }
        }

        // Add type hierarchy
        KBObject subClsProp = kb.getProperty(KBConstants.RDFS_NS + "subClassOf");
        for (KBTriple t : kb.genericTripleQuery(null, subClsProp, null)) {
            KBObject subCls = t.getSubject();
            KBObject cls = t.getObject();
            String clsId = cls.getID();

            Type subtype = vocabulary.getType(subCls.getID());
            if (subtype == null)
                continue;

            if (!clsId.startsWith(KBConstants.OWL_NS))
                subtype.setParent(clsId);

            Type type = vocabulary.getType(cls.getID());
            if (type != null && subtype.getId().startsWith(vocabulary.getNamespace())) {
                type.addChild(subtype.getId());
            }
        }
    }

    private String createPropertyLabel(String pname) {
        // Remove starting "has"
        pname = pname.replaceAll("^has", "");
        // Convert camel case to spaced human readable string
        pname = pname.replaceAll(String.format("%s|%s|%s", "(?<=[A-Z])(?=[A-Z][a-z])", "(?<=[^A-Z])(?=[A-Z])",
                "(?<=[A-Za-z])(?=[^A-Za-z])"), " ");
        // Make first letter upper case
        return pname.substring(0, 1).toUpperCase() + pname.substring(1);
    }

    /********************
     * API methods
     */

    public Map<String, Vocabulary> getVocabularies() {
        return this.vocabularies;
    }

    private Graph resolvePrefixesForGraph(Graph graph, String localDomain) {
        List<Triple> triples = new ArrayList<Triple>();
        for (Triple triple : graph.getTriples()) {
            String subj = resolvePrefixes(triple.getSubject(), localDomain);
            String pred = resolvePrefixes(triple.getPredicate(), localDomain);
            triples.add(new Triple(subj, pred, triple.getObject(), triple.getDetails()));
        }
        Graph newGraph = new Graph();
        newGraph.setTriples(triples);
        return newGraph;
    }

    private String resolvePrefixes(String value, String localDomain) {
        if (localDomain != null && !localDomain.equals("") && value.charAt(0) == ':') { // replace ":" for local domain
            value = localDomain + value.substring(1);
        } else {
            // Resolve SQO and HYP first
            if (value.startsWith("sqo:")) {
                value = KBConstants.QUESTION_NS + value.substring(4);
            } else if (value.startsWith("hyp:")) {
                value = KBConstants.HYP_NS + value.substring(4);
            } else
                for (String prefix : this.externalVocabularies.keySet()) {
                    if (value.startsWith(prefix + ":")) {
                        String namespace = this.externalVocabularies.get(prefix).getNamespace();
                        value = namespace + value.substring(prefix.length() + 1);
                    }
                }
        }
        return value;
    }

    /*
     * Hypotheses
     */
    public Hypothesis addHypothesis(String username, Hypothesis hypothesis) {
        // Check required inputs and set ID if necessary
        String name = hypothesis.getName();
        String desc = hypothesis.getDescription();
        String question = hypothesis.getQuestion();
        String dateCreated = hypothesis.getDateCreated();
        if (dateCreated == null || dateCreated.equals("")) {
            // SET DATE
            hypothesis.setDateCreated(dateformatter.format(new Date()));
        } else {
            // Update date
            hypothesis.setDateModified(dateformatter.format(new Date()));
        }
        if (name != null && desc != null && question != null && !name.equals("") && !desc.equals("")
                && !question.equals("")) {
            String id = hypothesis.getId();
            if (id == null || id.equals("")) // Create new Hypothesis ID
                hypothesis.setId(GUID.randomId("Hypothesis"));

            String hypothesisDomain = this.HYPURI(username) + "/" + hypothesis.getId() + "#";
            hypothesis.setGraph(resolvePrefixesForGraph(hypothesis.getGraph(), hypothesisDomain));
            if (writeHypothesis(username, hypothesis))
                return hypothesis;
        }
        return null;
    }

    public boolean removeHypothesis(String username, String id) {
        return this.deleteHypothesis(username, id);
    }

    public Hypothesis getHypothesis(String username, String id) {
        return loadHypothesis(username, id);
    }

    public Hypothesis updateHypothesis(String username, String id, Hypothesis hypothesis) {
        String name = hypothesis.getName();
        String desc = hypothesis.getDescription();
        String question = hypothesis.getQuestion();
        if (name != null && desc != null && question != null && !name.equals("") && !desc.equals("")
                && !question.equals("") &&
                hypothesis.getId().equals(id) && this.deleteHypothesis(username, id))
            return this.addHypothesis(username, hypothesis);
        return null;
    }

    public List<TreeItem> listHypotheses(String username) {
        List<TreeItem> list = new ArrayList<TreeItem>();
        List<Hypothesis> hypothesisList = listHypothesesPreviews(username);

        for (Hypothesis h : hypothesisList) {
            TreeItem item = new TreeItem(h.getId(), h.getName(), h.getDescription(), h.getParentId(),
                    h.getDateCreated(), h.getAuthor());
            if (h.getDateModified() != null)
                item.setDateModified(h.getDateModified());
            if (h.getQuestion() != null)
                item.setQuestion(h.getQuestion());
            list.add(item);
        }
        return list;
    }

    /*
     * Lines of Inquiry
     */

    public LineOfInquiry addLOI(String username, LineOfInquiry loi) {
        String name = loi.getName();
        String desc = loi.getDescription();
        String question = loi.getQuestion();
        String dateCreated = loi.getDateCreated();
        if (dateCreated == null || dateCreated.equals("")) {
            // SET DATE
            loi.setDateCreated(dateformatter.format(new Date()));
        } else {
            // Update date
            loi.setDateModified(dateformatter.format(new Date()));
        }
        if (name != null && desc != null && question != null &&
                !name.equals("") && !desc.equals("") && !question.equals("") &&
                writeLOI(username, loi))
            return loi;
        else
            return null;
    }

    public boolean removeLOI(String username, String id) {
        return deleteLOI(username, id);
    }

    public LineOfInquiry getLOI(String username, String id) {
        return this.loadLOI(username, id);
    }

    public LineOfInquiry updateLOI(String username, String id, LineOfInquiry loi) {
        if (loi.getId() != null && this.deleteLOI(username, id))
            return this.addLOI(username, loi);
        return null;
    }

    public List<TreeItem> listLOIs(String username) {
        List<TreeItem> list = new ArrayList<TreeItem>();
        List<LineOfInquiry> lois = this.listLOIPreviews(username);

        for (LineOfInquiry l : lois) {
            TreeItem item = new TreeItem(l.getId(), l.getName(), l.getDescription(),
                    null, l.getDateCreated(), l.getAuthor());
            if (l.getDateModified() != null)
                item.setDateModified(l.getDateModified());
            if (l.getQuestion() != null)
                item.setQuestion(l.getQuestion());
            list.add(item);
        }
        return list;
    }

    /*
     * Triggered Lines of Inquiries (TLOIs)
     */

    public TriggeredLOI addTriggeredLOI(String username, TriggeredLOI tloi) {
        tloi.setStatus(Status.QUEUED);
        String dateCreated = tloi.getDateCreated();
        if (dateCreated == null || dateCreated.equals("")) {
            tloi.setDateCreated(dateformatter.format(new Date()));
        } else {
            tloi.setDateModified(dateformatter.format(new Date()));
        }
        writeTLOI(username, tloi);

        TLOIExecutionThread wflowThread = new TLOIExecutionThread(username, tloi, false);
        executor.execute(wflowThread);
        return tloi;
    }

    public boolean removeTriggeredLOI(String username, String id) {
        return deleteTLOI(username, id);
    }

    public TriggeredLOI getTriggeredLOI(String username, String id) {
        return loadTLOI(username, id);
    }

    private TriggeredLOI updateTriggeredLOI(String username, String id, TriggeredLOI tloi) {
        if (tloi.getId() != null && this.deleteTLOI(username, id) && this.writeTLOI(username, tloi))
            return tloi;
        return null;
    }

    public TriggeredLOI updateTLOINotes(String username, String id, TriggeredLOI tloi) {
        TriggeredLOI updatedTLOI = getTriggeredLOI(username, id);
        if (updatedTLOI != null && tloi != null) {
            updatedTLOI.setNotes(tloi.getNotes());
            if (this.deleteTLOI(username, id) && this.writeTLOI(username, updatedTLOI))
                return tloi;
        }
        //TODO: We return the request as default, check what when wrong and send apropiate error.
        return tloi;
    }

    public List<TriggeredLOI> listTriggeredLOIs(String username) {
        return listTLOIs(username);
    }

    /*
     * Questions and option configuration
     */

    private Map<String, Question> allQuestions;
    private Map<String, QuestionVariable> allVariables;

    private void loadQuestionTemplates() {
        List<String> urls = new ArrayList<String>();

        Iterator<String> a = this.getConfig().getKeys("question-templates");
        while (a.hasNext()) {
            String key = a.next();
            urls.add(this.getConfig().getString(key));
        }

        this.allQuestions = new HashMap<String, Question>();
        this.allVariables = new HashMap<String, QuestionVariable>();
        for (String url : urls) {
            // Clear cache first
            try {
                start_write();
                KBAPI kb = fac.getKB(url, OntSpec.PLAIN, true, true);
                kb.removeAllTriples();
                kb.delete();
                this.save(kb);
                this.end();
            } catch (Exception e) {
                e.printStackTrace();
                if (is_in_transaction()) {
                    this.end();
                }
            }
            // Load questions and add them to cache
            for (Question q : loadQuestionsFromKB(url)) {
                this.allQuestions.put(q.getId(), q);
            }
        }
    }

    public List<Question> loadQuestionsFromKB(String url) {
        System.out.println("Loading Question Templates: " + url);
        List<Question> questions = new ArrayList<Question>();
        try {
            KBAPI kb = fac.getKB(url, OntSpec.PLAIN, true, true);
            this.start_read();
            KBObject typeprop = kb.getProperty(KBConstants.RDF_NS + "type");
            KBObject labelprop = kb.getProperty(KBConstants.RDFS_NS + "label");

            // Load question classes and properties
            for (KBTriple t : kb.genericTripleQuery(null, typeprop, SQOnt.getClass(SQO.QUESTION))) {
                // System.out.println(t.toString());
                KBObject question = t.getSubject();
                KBObject name = kb.getPropertyValue(question, labelprop);
                KBObject template = kb.getPropertyValue(question, SQOnt.getProperty(SQO.HAS_TEMPLATE));
                KBObject pattern = kb.getPropertyValue(question, SQOnt.getProperty(SQO.HAS_PATTERN));
                ArrayList<KBObject> variables = kb.getPropertyValues(question, SQOnt.getProperty(SQO.HAS_VARIABLE));

                if (name != null && template != null && pattern != null) {
                    List<QuestionVariable> vars = null;

                    if (variables != null && variables.size() > 0) {
                        vars = new ArrayList<QuestionVariable>();
                        for (KBObject var : variables) {
                            KBObject vname = kb.getPropertyValue(var, SQOnt.getProperty(SQO.HAS_VARIABLE_NAME));
                            KBObject vconstraints = kb.getPropertyValue(var,
                                    SQOnt.getProperty(SQO.HAS_CONSTRAINT_QUERY));
                            KBObject vfixedOptions = kb.getPropertyValue(var, SQOnt.getProperty(SQO.HAS_FIXED_OPTIONS));
                            if (vname != null) {
                                QuestionVariable q = new QuestionVariable(var.getID(), vname.getValueAsString(),
                                        vconstraints == null ? null : vconstraints.getValueAsString());
                                if (vfixedOptions != null) {
                                    q.setFixedOptions(vfixedOptions.getValueAsString().split("\\s*,\\s*"));
                                }
                                vars.add(q);
                            }
                        }
                        // Store question variables
                        for (QuestionVariable v : vars)
                            allVariables.put(v.getId(), v);
                    }

                    Question q = new Question(question.getID(), name.getValueAsString(), template.getValueAsString(),
                            pattern.getValueAsString(), vars);
                    questions.add(q);
                }
            }
            this.end();
            return questions;
        } catch (Exception e) {
            e.printStackTrace();
            if (this.is_in_transaction())
                this.end();
            // TODO: handle exception
        }
        return null;
    }

    public List<Question> listHypothesesQuestions() {
        return new ArrayList<Question>(this.allQuestions.values());
    }

    public List<List<String>> listVariableOptions(String sid) throws Exception {
        if (!optionsCache.containsKey(sid)) {
            optionsCache.put(sid, this.loadVariableOptions(sid));
        }
        return optionsCache.get(sid);
    }

    private List<List<String>> loadVariableOptions(String sid) throws Exception {
        List<List<String>> options = new ArrayList<List<String>>();
        QuestionVariable variable = null;
        // FIXME: Find a better way to handle url prefix or change the request to
        // include the full URI
        if (allVariables.containsKey("http://disk-project.org/examples/" + sid)) {
            variable = allVariables.get("http://disk-project.org/examples/" + sid);
        } else if (allVariables.containsKey("http://disk-project.org/resources/question/" + sid)) {
            variable = allVariables.get("http://disk-project.org/resources/question/" + sid);
        } else if (allVariables.containsKey("https://w3id.org/sqo/resource/" + sid)) {
            variable = allVariables.get("https://w3id.org/sqo/resource/" + sid);
        }
        if (variable != null) {
            String varname = variable.getVarName();
            String constraintQuery = variable.getConstraints();
            String[] fixedoptions = variable.getFixedOptions();

            // If there are fixed options, return these
            if (fixedoptions != null) {
                for (String val : fixedoptions) {
                    List<String> opt = new ArrayList<String>();
                    opt.add(val);
                    opt.add(val);
                    options.add(opt);
                }
            } else if (constraintQuery != null) {
                // If there is a constraint query, send it to all data providers;
                Map<String, List<DataResult>> solutions = new HashMap<String, List<DataResult>>();
                for (DataAdapter adapter : this.dataAdapters.values()) {
                    // TODO: add some way to check if this adapter support this type of query. All
                    // SPARQL for the moment.
                    solutions.put(adapter.getName(), adapter.queryOptions(varname, constraintQuery));
                    // System.out.println("> " + adapter.getEndpointUrl() + " -- " +
                    // solutions.get(adapter.getName()).size() );
                }

                // To check that all labels are only once
                Map<String, List<List<String>>> labelToOption = new HashMap<String, List<List<String>>>();

                for (String dataSourceName : solutions.keySet()) {
                    for (DataResult solution : solutions.get(dataSourceName)) {
                        String uri = solution.getValue(DataAdapter.VARURI);
                        String label = solution.getValue(DataAdapter.VARLABEL);

                        if (uri != null && label != null) {
                            List<List<String>> sameLabelOptions = labelToOption.containsKey(label)
                                    ? labelToOption.get(label)
                                    : new ArrayList<List<String>>();
                            List<String> thisOption = new ArrayList<String>();
                            thisOption.add(uri);
                            thisOption.add(label);
                            thisOption.add(dataSourceName);
                            sameLabelOptions.add(thisOption);
                            labelToOption.put(label, sameLabelOptions);
                        }
                    }
                }

                // Add all options with unique labels
                for (List<List<String>> sameLabelOptions : labelToOption.values()) {
                    if (sameLabelOptions.size() == 1) {
                        options.add(sameLabelOptions.get(0).subList(0, 2));
                    } else { // There's more than one option with the same label
                        boolean allTheSame = true;
                        String lastValue = sameLabelOptions.get(0).get(0); // Comparing IDs
                        for (List<String> candOption : sameLabelOptions) {
                            if (!lastValue.equals(candOption.get(0))) {
                                allTheSame = false;
                                break;
                            }
                        }

                        if (allTheSame) {
                            options.add(sameLabelOptions.get(0).subList(0, 2));
                        } else {
                            Map<String, Integer> dsCount = new HashMap<String, Integer>();

                            for (List<String> candOption : sameLabelOptions) {
                                List<String> newOption = new ArrayList<String>();
                                newOption.add(candOption.get(0));
                                String dataSource = candOption.get(2);
                                Integer count = dsCount.containsKey(dataSource) ? dsCount.get(dataSource) : 0;
                                String label = candOption.get(1) + " (" + dataSource
                                        + (count > 0 ? "_" + count.toString() : "") + ")";
                                dsCount.put(dataSource, (count + 1));

                                newOption.add(label);
                                options.add(newOption);
                            }
                        }
                    }
                }
            }
        }
        return options;
    }

    /*
     * Querying
     */

    private String getAllPrefixes() {
        String prefixes = KBConstants.getAllPrefixes();
        for (VocabularyConfiguration vc : this.externalVocabularies.values()) {
            prefixes += "PREFIX " + vc.getPrefix() + ": <" + vc.getNamespace() + ">\n";
        }
        return prefixes;
    }

    public Map<String, List<String>> queryExternalStore(String endpoint, String sparqlQuery, String variables)
            throws Exception, QueryParseException {
        // FIXME: change this to DataResults
        // Variable name -> [row0, row1, ...]
        Map<String, List<String>> dataVarBindings = new HashMap<String, List<String>>();

        // Check that the variables string contains valid sparql
        String queryVars;
        if (variables == null || variables.contentEquals("") || variables.contains("*")) {
            queryVars = "*";
        } else {
            String[] vars = variables.replaceAll("\\s+", " ").split(" ");
            for (String v : vars) {
                if (v.charAt(0) != '?')
                    return dataVarBindings;
            }
            queryVars = variables;
        }

        DataAdapter dataAdapter = this.getDataAdapter(endpoint);
        if (dataAdapter == null)
            return dataVarBindings;

        // FIXME: These prefixes should be configured
        String dataQuery = "PREFIX bio: <http://disk-project.org/ontology/omics#>\n" +
                "PREFIX neuro: <https://w3id.org/disk/ontology/enigma_hypothesis#>\n" +
                "PREFIX hyp: <http://disk-project.org/ontology/hypothesis#>\n" +
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "SELECT DISTINCT " + queryVars + " WHERE {\n" +
                sparqlQuery + "\n} LIMIT 200";
        // There's a limit here to prevent {?a ?b ?c} and so on
        List<DataResult> solutions = dataAdapter.query(dataQuery);
        int size = solutions.size();

        if (size > 0) {
            Set<String> varnames = solutions.get(0).getVariableNames();
            for (String varname : varnames)
                dataVarBindings.put(varname, new ArrayList<String>());
            for (DataResult solution : solutions) {
                for (String varname : varnames) {
                    dataVarBindings.get(varname).add(solution.getValue(varname));
                }
            }
        }

        System.out.println("External query to " + endpoint + " returned " + solutions.size() + " elements.");
        return dataVarBindings;
    }

    private String getQueryBindings(String queryPattern, Pattern variablePattern,
            Map<String, String> variableBindings) {
        String pattern = "";
        for (String line : queryPattern.split("\\n")) {
            line = line.trim();
            if (line.equals(""))
                continue;
            if (variableBindings != null) {
                Matcher mat = variablePattern.matcher(line);
                int diff = 0;
                while (mat.find()) {
                    if (variableBindings.containsKey(mat.group(1))) {
                        String varbinding = variableBindings.get(mat.group(1));
                        int st = mat.start(1);
                        int end = mat.end(1);
                        line = line.substring(0, st - 1 + diff) + varbinding + line.substring(end + diff);
                        diff += varbinding.length() - (end - st) - 1;
                    }
                }
            }
            pattern += line + "\n";
        }
        return pattern;
    }

    public Set<String> interceptVariables(final String queryA, final String queryB) {
        Set<String> A = new HashSet<String>();
        Matcher a = varPattern.matcher(queryA);
        while (a.find())
            A.add(a.group());

        Set<String> B = new HashSet<String>();
        Matcher b = varPattern.matcher(queryB);
        while (b.find()) {
            String v = b.group();
            for (String v2 : A) {
                if (v.equals(v2)) {
                    B.add(v);
                }
            }
        }
        return B;
    }

    /*
     * Executing hypothesis
     */

    private Boolean isValid(LineOfInquiry loi) {
        // Mandatory fields for Lines of Inquiry
        String hq = loi.getHypothesisQuery();
        if (hq == null || hq.equals(""))
            return false;

        String dq = loi.getDataQuery();
        if (dq == null || dq.equals(""))
            return false;

        String source = loi.getDataSource();
        if (source == null || source.equals(""))
            return false;

        // All workflows must have a source and at least one binding
        for (WorkflowBindings wb : loi.getWorkflows()) {
            String ws = wb.getSource();
            List<VariableBinding> vars = wb.getBindings();
            if (ws == null || ws.equals("") || vars == null || vars.size() == 0) {
                return false;
            }
        }
        // TODO: Validate meta-workflows.
        return true;
    }

    private Boolean isValid(LineOfInquiry loi, Map<String, String> hypothesisBindings) {
        // Check if the hypothesis bindings (values from the user) are valid in this LOI
        if (!isValid(loi))
            return false;

        String dataQuery = loi.getDataQuery();

        // All variables used on both, the hypothesis query and data query, must have
        // valid values in this point.
        Set<String> inVars = interceptVariables(loi.getHypothesisQuery(), dataQuery);
        if (inVars.size() == 0)
            return false;
        for (String variable : inVars) {
            if (!hypothesisBindings.containsKey(variable.substring(1)))
                return false;
        }

        // All variables used on the workflows must come from the hypothesis bindings or
        // be present on the data-query.
        for (WorkflowBindings wb : loi.getWorkflows()) {
            List<String> varList = wb.getAllVariables();
            if (varList.size() == 0)
                return false;
            for (String varName : varList) {
                if (!(hypothesisBindings.containsKey(varName.substring(1)) || dataQuery.contains(varName)))
                    return false;
            }
        }
        return true;
    }

    public Map<LineOfInquiry, List<Map<String, String>>> getLOIByHypothesisId(String username, String id) {
        String hypuri = this.HYPURI(username) + "/" + id;
        // LOIID -> [{ variable -> value }]
        Map<String, List<Map<String, String>>> allMatches = new HashMap<String, List<Map<String, String>>>();
        List<LineOfInquiry> lois = this.listLOIPreviews(username);

        // Starts checking all LOIs that match the hypothesis directly from the KB.
        try {
            this.start_read();
            KBAPI hypKB = this.fac.getKB(hypuri, OntSpec.PLAIN, true);
            System.out.println("GRAPH: " + hypKB.getAllTriples());
            for (LineOfInquiry loi : lois) {
                String hq = loi.getHypothesisQuery();
                if (hq != null) {
                    String query = this.getAllPrefixes() + "SELECT DISTINCT * WHERE { \n"
                            + loi.getHypothesisQuery().replaceAll("\n", ".\n") + " }";
                    //System.out.println("Query: " + query + "\n---------------------------");
                    ArrayList<ArrayList<SparqlQuerySolution>> allSolutions = null;
                    try {
                        allSolutions = hypKB.sparqlQuery(query);
                    } catch (Exception e) {
                        System.out.println("Error querying:\n" + query);
                        System.out.println(e);
                        continue;
                    }
                    if (allSolutions != null) {
                        if (allSolutions.size() == 0) {
                            System.out.println("No solutions for " + loi.getId());
                            // String errorMesString = "No solutions found for the query: \n" + query;
                            // System.out.println(errorMesString);
                            // throw new NotFoundException(errorMesString);
                        } else
                            for (List<SparqlQuerySolution> row : allSolutions) {
                                // One match per cell, store variables on cur.
                                Map<String, String> cur = new HashMap<String, String>();
                                for (SparqlQuerySolution cell : row) {
                                    String var = cell.getVariable(), val = null;
                                    KBObject obj = cell.getObject();
                                    if (obj != null) {
                                        if (obj.isLiteral()) {
                                            val = '"' + obj.getValueAsString() + '"';
                                        } else {
                                            val = "<" + obj.getID() + ">";
                                        }
                                        cur.put(var, val);
                                    }
                                }
                                // If there is at least one variable binded, add to match list.
                                if (cur.size() > 0) {
                                    String loiid = loi.getId().replaceAll("^.*\\/", "");
                                    if (!allMatches.containsKey(loiid))
                                        allMatches.put(loiid, new ArrayList<Map<String, String>>());
                                    List<Map<String, String>> curList = allMatches.get(loiid);
                                    curList.add(cur);
                                }
                            }
                    }
                } else {
                    System.out.println("Error: No hypothesis query");
                }
            }
            // this.end();
        } catch (Exception e) {
            // throw e;
            e.printStackTrace();
        } finally {
            this.end();
        }

        Map<LineOfInquiry, List<Map<String, String>>> results = new HashMap<LineOfInquiry, List<Map<String, String>>>();
        // allMatches contains LOIs that matches hypothesis, now check other conditions
        for (String loiId : allMatches.keySet()) {
            LineOfInquiry loi = this.getLOI(username, loiId);
            for (Map<String, String> hBindings : allMatches.get(loiId)) {
                if (isValid(loi, hBindings)) {
                    if (!results.containsKey(loi))
                        results.put(loi, new ArrayList<Map<String, String>>());
                    List<Map<String, String>> curList = results.get(loi);
                    curList.add(hBindings);
                }
            }
        }

        // LOI -> [{ variable -> value }, {...}, ...]
        return results;
    }

    public List<TriggeredLOI> queryHypothesis(String username, String id) throws Exception, QueryParseException {
        // Create TLOIs that match with a hypothesis and username
        List<TriggeredLOI> tlois = new ArrayList<TriggeredLOI>();
        Map<String, List<DataResult>> queryCache = new HashMap<String, List<DataResult>>();

        System.out.println("Quering hypothesis: " + id);
        Map<LineOfInquiry, List<Map<String, String>>> matchingBindings = this.getLOIByHypothesisId(username, id);
        if (matchingBindings.isEmpty()) {
            throw new NotFoundException("No LOI match this Hypothesis.");
        }

        for (LineOfInquiry loi : matchingBindings.keySet()) {
            // Check that the adapters are configured.
            DataAdapter dataAdapter = this.dataAdapters.get(loi.getDataSource());
            if (dataAdapter == null) {
                System.out.println("Warning: " + loi.getId() + " uses an unknown data adapter: " + loi.getDataSource());
                continue;
            } else {
                boolean allOk = true;
                for (WorkflowBindings wb: loi.getWorkflows()) {
                    String source =  wb.getSource();
                    if (source == null || getMethodAdapterByName(source) == null) {
                        allOk = false;
                        System.out.println("Warning: " + loi.getId() + " uses an unknown method adapter: " + source);
                        break;
                    }
                }
                if (allOk)
                    for (WorkflowBindings wb: loi.getMetaWorkflows()) {
                        String source =  wb.getSource();
                        if (source == null || getMethodAdapterByName(source) == null) {
                            allOk = false;
                            System.out.println("Warning: " + loi.getId() + " uses an unknown method adapter: " + source);
                            break;
                        }
                    }
                if (!allOk) {
                    continue;
                }
            }

            // One hypothesis can match the same LOI in more than one way, the following
            // for-loop handles that
            for (Map<String, String> values : matchingBindings.get(loi)) {

                // Creating query
                String dq = getQueryBindings(loi.getDataQuery(), varPattern, values);
                String query = this.getAllPrefixes() + "SELECT DISTINCT ";
                for (String qvar : loi.getAllWorkflowVariables())
                    query += qvar + " ";
                query += "{\n" + dq + "}";

                // Prevents executing the same query several times.
                Boolean cached = queryCache.containsKey(query);
                if (!cached) {
                    List<DataResult> results = dataAdapter.query(query);
                    List<DataResult> solutions = results;
                    queryCache.put(query, solutions);
                }
                List<DataResult> solutions = queryCache.get(query);

                if (solutions.size() > 0) {
                    System.out.println("LOI " + loi.getId() + " got " + solutions.size() + " results");

                    // Store solutions in dataVarBindings
                    // Varname -> [value, value, value]
                    Map<String, List<String>> dataVarBindings = new HashMap<String, List<String>>();
                    Set<String> varNames = solutions.get(0).getVariableNames();
                    for (String varName : varNames)
                        dataVarBindings.put(varName, new ArrayList<String>());

                    for (DataResult solution : solutions) {
                        for (String varname : varNames) {
                            String cur = solution.getValue(varname);
                            if (cur != null && cur.contains(" "))
                                cur = "\"" + cur + "\"";
                            dataVarBindings.get(varname).add(cur);
                        }
                    }

                    // Remove duplicated values for non-colletion variables
                    Set<String> sparqlNonCollVar = loi.getAllWorkflowNonCollectionVariables();
                    for (String varName : varNames) {
                        String sparqlVar = "?" + varName;
                        if (sparqlNonCollVar.contains(sparqlVar)) {
                            Set<String> fixed = new HashSet<String>(dataVarBindings.get(varName));
                            dataVarBindings.put(varName, new ArrayList<String>(fixed));
                        }
                    }

                    // Add the parameters directly from hypothesis
                    for (String varName : varNames) {
                        List<String> cur = dataVarBindings.get(varName);
                        if (cur.size() == 1 && cur.get(0) == null) {
                            // This variable was not set on the data-query, extract from hypothesis
                            // bindings.
                            String newBinding = values.get(varName);
                            if (newBinding != null) {
                                List<String> tmp = new ArrayList<String>();
                                tmp.add(newBinding);
                                dataVarBindings.put(varName, tmp);
                            }
                        }
                    }

                    TriggeredLOI tloi = new TriggeredLOI(loi, id);
                    tloi.setWorkflows(
                            this.getTLOIBindings(username, loi.getWorkflows(), dataVarBindings, dataAdapter));
                    tloi.setMetaWorkflows(
                            this.getTLOIBindings(username, loi.getMetaWorkflows(), dataVarBindings,
                                    dataAdapter));
                    tloi.setDataQuery(dq); // Updated data query
                    tloi.setDateCreated(dateformatter.format(new Date()));
                    tlois.add(tloi);
                }
                else {
                    System.out.println("LOI " + loi.getId() + " got no results");
                }
            }
        }

        return checkExistingTLOIs(username, tlois);
    }

    // This replaces all triggered lines of inquiry already executed.
    private List<TriggeredLOI> checkExistingTLOIs(String username, List<TriggeredLOI> tlois) {
        List<TriggeredLOI> checked = new ArrayList<TriggeredLOI>();
        Map<String, List<TriggeredLOI>> cache = new HashMap<String, List<TriggeredLOI>>();
        for (TriggeredLOI tloi : tlois) {
            String parentLoiId = tloi.getParentLoiId();
            System.out.println("Checking " + tloi.getId() + " (" + parentLoiId + ")");
            if (!cache.containsKey(parentLoiId)) {
                cache.put(parentLoiId,
                        getTLOIsForHypothesisAndLOI(username, tloi.getParentHypothesisId(), parentLoiId));
            }
            List<TriggeredLOI> candidates = cache.get(parentLoiId);
            TriggeredLOI real = tloi;
            for (TriggeredLOI cand : candidates) {
                if (cand.toString().equals(tloi.toString())) {
                    // TODO: compare the hash of the input files
                    System.out.println("Replaced " + tloi.getId() + " with " + cand.getId());
                    real = cand;
                    break;
                }
            }
            // Run all non triggered TLOIs
            if (real.getStatus() == null) {
                System.out.println("TLOI " + tloi.getId() + " will be trigger");
                real = addTriggeredLOI(username, tloi);
            }
            checked.add(real);
        }
        return checked;
    }

    @SuppressWarnings("unchecked")
    private List<WorkflowBindings> getTLOIBindings(String username, List<WorkflowBindings> wflowBindings,
            Map<String, List<String>> dataVarBindings, DataAdapter dataAdapter) throws Exception {
        List<WorkflowBindings> tloiBindings = new ArrayList<WorkflowBindings>();
        for (WorkflowBindings bindings : wflowBindings) { // FOR EACH WORKFLOW
            // For each Workflow, create an empty copy to set the values
            WorkflowBindings tloiBinding = new WorkflowBindings(
                    bindings.getWorkflow(),
                    bindings.getWorkflowLink());
            tloiBinding.setSource(bindings.getSource());
            tloiBinding.setMeta(bindings.getMeta());
            tloiBindings.add(tloiBinding);
            MethodAdapter methodAdapter = getMethodAdapterByName(bindings.getSource());

            List<Variable> allVars = methodAdapter.getWorkflowVariables(bindings.getWorkflow());

            for (VariableBinding vbinding : bindings.getBindings()) { // Normal variable bindings.
                // For each Variable binding, check :
                // - If this variable expects a collection or single values
                // - Check the binding values on the data store
                String binding = vbinding.getBinding();
                Matcher collmat = varCollPattern.matcher(binding);
                Matcher mat = varPattern.matcher(binding);

                // Get the sparql variable
                boolean isCollection = false;
                String sparqlvar = null;
                if (collmat.find() && dataVarBindings.containsKey(collmat.group(1))) {
                    sparqlvar = collmat.group(1);
                    isCollection = true;
                } else if (mat.find() && dataVarBindings.containsKey(mat.group(1))) {
                    sparqlvar = mat.group(1);
                }

                if (sparqlvar == null)
                    continue;

                // Get the data bindings for the sparql variable
                List<String> dsurls = dataVarBindings.get(sparqlvar);

                // Checks if the bindings are input files.
                boolean bindingsAreFiles = true;
                for (String candurl : dsurls) {
                    if (!candurl.startsWith("http")) {
                        bindingsAreFiles = false;
                        break;
                    }
                }

                // Datasets names
                List<String> dsnames = new ArrayList<String>();

                if (bindingsAreFiles) {
                    String varName = vbinding.getVariable();
                    String dType = null;
                    for (Variable v: allVars) {
                        if (varName.equals(v.getName()))
                            dType = v.getType();
                    }
                    // TODO: this should be async
                    // Check hashes, create local name and upload data:
                    Map<String, String> urlToName = addData(dsurls, methodAdapter, dataAdapter, dType);
                    for (String dsurl : dsurls) {
                        String dsname = urlToName.containsKey(dsurl) ? urlToName.get(dsurl)
                                : dsurl.replaceAll("^.*\\/", "");
                        dsnames.add(dsname);
                    }
                } else {
                    // If the binding is not a file, send the value with no quotes
                    for (String value : dsurls) {
                        // Remove quotes from parameters
                        if (value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
                            value = value.substring(1, value.length() - 1);
                        }
                        dsnames.add(value);
                    }
                }

                // If Collection, all datasets go to same workflow
                if (isCollection) {
                    // This variable expects a collection. Modify the existing tloiBinding values,
                    // collections of non-files are send as comma separated values:
                    tloiBinding.addBinding(new VariableBinding(vbinding.getVariable(), dsnames.toString()));
                } else {
                    if (dsnames.size() == 1) {
                        tloiBinding.addBinding(new VariableBinding(vbinding.getVariable(), dsnames.get(0)));
                    } else {
                        System.out.println("IS MORE THAN ONE VALUE BUT NOT COLLECTION!");
                        // This variable expects a single file. Add new tloi bindings for each dataset
                        List<WorkflowBindings> newTloiBindings = new ArrayList<WorkflowBindings>();
                        for (WorkflowBindings tmpBinding : tloiBindings) { // For all already processed workflow
                                                                           // bindings
                            for (String dsname : dsnames) {
                                ArrayList<VariableBinding> newBindings = (ArrayList<VariableBinding>) SerializationUtils
                                        .clone((Serializable) tmpBinding.getBindings());

                                WorkflowBindings newWorkflowBindings = new WorkflowBindings(
                                        bindings.getWorkflow(),
                                        bindings.getWorkflowLink(),
                                        newBindings);
                                newWorkflowBindings.addBinding(new VariableBinding(vbinding.getVariable(), dsname));
                                newWorkflowBindings.setMeta(bindings.getMeta());
                                newWorkflowBindings.setSource(bindings.getSource());
                                newTloiBindings.add(newWorkflowBindings);
                            }
                        }
                        tloiBindings = newTloiBindings;
                    }
                }
            }
        }

        return tloiBindings;
    }

    //This adds dsUrls to the data-repository, returns filename -> URL
    private Map<String, String> addData(List<String> dsurls, MethodAdapter methodAdapter, DataAdapter dataAdapter, String dType)
            throws Exception {
        // To add files to wings and not replace anything, we need to get the hash from the wiki.
        // TODO: here connect with minio.
        Map<String, String> nameToUrl = new HashMap<String, String>();
        Map<String, String> urlToName = new HashMap<String, String>();
        Map<String, String> filesETag = dataAdapter.getFileHashesByETag(dsurls);  // File -> ETag
        boolean allOk = true; // All is OK if we have all file ETags.

        for (String fileUrl: dsurls) {
            if (filesETag.containsKey(fileUrl)) {
                String eTag = filesETag.get(fileUrl);
                // This name should be different now, this is not the SHA
                String uniqueName = "SHA" + eTag.substring(0, 6) + "_" + fileUrl.replaceAll("^.*\\/", "");
                nameToUrl.put(uniqueName, fileUrl);
                urlToName.put(fileUrl, uniqueName);
            } else {
                System.err.println("ETag not found: " + fileUrl);
                allOk = false;
            }
        }

        if (!allOk) { // Get hashes from the data-adapter (SPARQL)
            Map<String, String> hashes = dataAdapter.getFileHashes(dsurls); // File -> SHA1
            for (String fileUrl : dsurls) {
                if (hashes.containsKey(fileUrl)) {
                    if (!urlToName.containsKey(fileUrl)) {
                        String hash = hashes.get(fileUrl);
                        String uniqueName = "SHA" + hash.substring(0, 6) + "_" + fileUrl.replaceAll("^.*\\/", "");
                        nameToUrl.put(uniqueName, fileUrl);
                        urlToName.put(fileUrl, uniqueName);
                    }
                } else {
                    System.err.println("HASH not found: " + fileUrl);
                }
            }
        }

        // Show files with no hash and throw a exception.
        for (String file : dsurls) {
            if (!urlToName.containsKey(file)) {
                //TODO: hadnle exception
                System.err.println("Warning: file " + file + " does not contain any hash on " + dataAdapter.getName());
            }
        }

        // avoid to duplicate files
        Set<String> names = nameToUrl.keySet();
        List<String> availableFiles = methodAdapter.areFilesAvailable(names, dType);
        names.removeAll(availableFiles);

        // upload the files
        for (String newFilename : names) {
            String newFile = nameToUrl.get(newFilename);
            System.out.println("Uploading to " + methodAdapter.getName() + ": " + newFile + " as " + newFilename);
            methodAdapter.addData(newFile, newFilename, dType);
        }

        return urlToName;
    }

    public Boolean runAllHypotheses(String username) throws Exception {
        List<String> hlist = new ArrayList<String>();
        String url = this.HYPURI(username);
        try {
            KBAPI kb = this.fac.getKB(url, OntSpec.PLAIN, true);
            KBObject hypcls = DISKOnt.getClass(DISK.HYPOTHESIS);

            this.start_read();
            KBObject typeprop = kb.getProperty(KBConstants.RDF_NS + "type");
            for (KBTriple t : kb.genericTripleQuery(null, typeprop, hypcls)) {
                KBObject hypobj = t.getSubject();
                String uri = hypobj.getID();
                String[] sp = uri.split("/");
                hlist.add(sp[sp.length - 1]);
                System.out.println("Hyp ID: " + sp[sp.length - 1]);
            }
        } catch (ConcurrentModificationException e) {
            System.out.println("Error trying to run all hypotheses. Could not read KB.");
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            this.end();
        }

        List<TriggeredLOI> tlist = new ArrayList<TriggeredLOI>();

        for (String hid : hlist) {
            tlist.addAll(queryHypothesis(username, hid));
        }

        // Only hypotheses with status == null are new
        for (TriggeredLOI tloi : tlist) {
            if (tloi.getStatus() == null) {
                System.out.println("TLOI " + tloi.getId() + " will be trigger");
                addTriggeredLOI(username, tloi);
            }
        }

        return true;
    }

    /*
     * Narratives
     */

    public Map<String, String> getNarratives(String username, String tloid) {
        Map<String, String> narratives = new HashMap<String, String>();
        TriggeredLOI tloi = this.getTriggeredLOI(username, tloid);
        if (tloi != null) {
            String hypId = tloi.getParentHypothesisId();
            String loiId = tloi.getParentLoiId();
            Hypothesis hyp = this.getHypothesis(username, hypId);
            LineOfInquiry loi = this.getLOI(username, loiId);

            if (loi == null || hyp == null) {
                System.out.println("ERROR: could not get hypotheses or LOI");
                return narratives;
            }

            // Assuming each tloi only has a workflow or metaworkdflow:
            WorkflowBindings wf = null;
            List<WorkflowBindings> wfs = tloi.getWorkflows();
            List<WorkflowBindings> mwfs = tloi.getMetaWorkflows();

            if (mwfs != null && mwfs.size() > 0) {
                wf = mwfs.get(0);
            } else if (wfs != null && wfs.size() > 0) {
                wf = wfs.get(0);
            } else {
                System.out.println("TLOID: " + tloid + " has does not have any workflow.");
            }

            // List of data used.
            String dataset = "";
            String fileprefix = "https://enigma-disk.wings.isi.edu/wings-portal/users/admin/test/data/fetch?data_id=http%3A//skc.isi.edu%3A8080/wings-portal/export/users/"
                    + username + "/data/library.owl%23";
            boolean allCollections = true;
            int len = 0;
            for (VariableBinding ds : wf.getBindings()) {
                if (!ds.isCollection()) {
                    allCollections = false;
                    break;
                }
                len = ds.getBindingAsArray().length > len ? ds.getBindingAsArray().length : len;
            }
            if (allCollections) {
                dataset += "<table><thead><tr><td><b>#</b></td>";
                for (VariableBinding ds : wf.getBindings()) {
                    dataset += "<td><b>" + ds.getVariable() + "</b></td>";
                }
                dataset += "</tr></thead><tbody>";
                for (int i = 0; i < len; i++) {
                    dataset += "<tr>";
                    dataset += "<td>" + (i + 1) + "</td>";
                    for (VariableBinding ds : wf.getBindings()) {
                        String[] bindings = ds.getBindingAsArray();
                        String datas = bindings[i];
                        String dataname = datas.replaceAll("^.*#", "").replaceAll("SHA\\w{6}_", "");
                        String url = fileprefix + datas;
                        String anchor = "<a target=\"_blank\" href=\"" + url + "\">" + dataname + "</a>";
                        dataset += "<td>" + anchor + "</td>";
                    }
                    dataset += "</tr>";
                }
                dataset += "</tbody></table>";

            } else {
                for (VariableBinding ds : wf.getBindings()) {
                    String binding = ds.getBinding();
                    if (binding.startsWith("[")) {
                        for (String datas : ds.getBindingAsArray()) {
                            String dataname = datas.replaceAll("^.*#", "").replaceAll("SHA\\w{6}_", "");
                            String url = fileprefix + datas;
                            String anchor = "<a target=\"_blank\" href=\"" + url + "\">" + dataname + "</a>";
                            dataset += "<li>" + anchor + "</li>";
                        }
                    }
                    System.out.println("binding: " + binding);
                }
            }
            Double confidence = tloi.getConfidenceValue();
            DecimalFormat df = new DecimalFormat(confidence != 0 && confidence < 0.001 ? "0.#E0"
                    : "0.###");

            String confidenceValueString = df.format(confidence);
            String confidenceType = tloi.getConfidenceType();
            // Execution narratives
            // String executionTemplate = "The Hypothesis with title: <b>${HYP.NAME}</b> was
            // runned <span class=\"${TLOI.STATUS}\">${TLOI.STATUS}</span>"
            // + "with the Line of Inquiry: <b>${LOI.NAME}"</b>."
            // + "The LOI triggered the <a target=\"_blank\"
            // href="{WF.getWorkflowLink()}">workflow on WINGS</a>"
            // + " where it was tested with the following datasets:<div
            // class=\"data-list\"><ol> ${[WF.getInputFiles]}"
            // + "</ol></div>The resulting p-value is $(tloi.pval).";
            String execution = "The Hypothesis with title: <b>" + hyp.getName()
                    + "</b> was runned <span class=\"" + tloi.getStatus() + "\">"
                    + tloi.getStatus() + "</span>"
                    + " with the Line of Inquiry: <b>" + loi.getName()
                    + "</b>. The LOI triggered the <a target=\"_blank\" href=\"" + wf.getWorkflowLink()
                    + "\">workflow on WINGS</a>"
                    + " where it was tested with the following datasets:<div class=\"data-list\"><ol>" + dataset
                    + "</ol></div>The resulting " + confidenceType + " is " + confidenceValueString + ".";
            narratives.put("execution", execution);

            String dataQuery = "<b>Data Query Narrative:</b><br/>" + this.dataQueryNarrative(loi.getDataQuery());

            narratives.put("dataquery", dataQuery);
        }
        return narratives;
    }

    private String dataQueryNarrative(String dataQuery) {
        // this is necessary to replace the new line characters in query
        String dataQuery1 = dataQuery.replaceAll("^(//)n${1}", "");
        String[] querylist = dataQuery1.split("\\.");
        String rdfs_label = "rdfs:label";

        try {
            Map<String, String> properties = new HashMap<String, String>();
            for (int i = 0; i < querylist.length; i++) {
                if (querylist[i].contains(rdfs_label)) {
                    String[] line = querylist[i].replace("\\", "").split(" ");
                    properties.put(line[2], line[4].replace('"', ' '));
                }
            }

            // We map all the objects to the properties they were identified with, by using
            // the objects dictionary
            Map<String, List<List<String>>> inputs = new HashMap<>();
            Map<List<String>, String> outputs = new HashMap<>();
            for (int i = 0; i < querylist.length; i++) {
                if (!querylist[i].contains(rdfs_label)) {
                    String[] line = querylist[i].split(" ");
                    String schema = "Schema";
                    if (!inputs.containsKey(line[2]) & !line[2].contains(schema)) {
                        List<List<String>> list = new ArrayList<List<String>>();
                        List<String> item = new ArrayList<String>();
                        item.add(line[2]);
                        item.add(properties.get(line[3]));
                        list.add(item);
                        inputs.put(line[2], list);
                        outputs.put(item, line[4]);
                    } else if (inputs.containsKey(line[2]) & !line[2].contains(schema)) {
                        List<List<String>> list2 = inputs.get(line[2]);
                        List<String> item = new ArrayList<String>();
                        item.add(line[2]);
                        item.add(properties.get(line[3]));
                        list2.add(item);
                        inputs.put(line[2], list2);
                        List<String> list = new ArrayList<String>();
                        list.add(line[2]);
                        list.add(properties.get(line[3]));
                        outputs.put(item, line[4]);
                    }
                }
            }
            // Now we traverse the path
            String path = "";
            for (String key : inputs.keySet()) {
                List<List<String>> value = inputs.get(key);
                for (int j = 0; j < value.size(); j++) {
                    // p = v
                    List<String> p = value.get(j);
                    try {
                        path = path + key.replace("?", "") + "->" + p.get(1).toString().trim().replace("?", "") + "->"
                                + outputs.get(p).toString().replace("?", "") + "<br/>";
                    } catch (NullPointerException e) {

                    }
                }
            }
            // System.out.println("Narrative"+path);
            return path;
        } catch (Exception e) {
            return "Error generating narrative";
        }

    }

    /*
     * Running
     */

    public List<TriggeredLOI> getTLOIsForHypothesisAndLOI(String username, String hypId, String loiId) {
        // Get all TLOIs and filter out
        List<TriggeredLOI> list = new ArrayList<TriggeredLOI>();
        for (TriggeredLOI tloi : listTLOIs(username)) {
            String parentHypId = tloi.getParentHypothesisId();
            String parentLOIId = tloi.getParentLoiId();
            if (parentHypId != null && parentHypId.equals(hypId) &&
                    parentLOIId != null && parentLOIId.equals(loiId)) {
                list.add(tloi);
            }
        }
        return list;
    }

    public List<TriggeredLOI> runHypothesisAndLOI(String username, String hypid, String loiid) throws Exception {
        List<TriggeredLOI> hyptlois = queryHypothesis(username, hypid);
        // TriggeredLOI match = null;
        for (TriggeredLOI tloi : hyptlois) {
            if (tloi.getStatus() == null && tloi.getParentLoiId().equals(loiid)) {
                // Set basic metadata
                tloi.setAuthor("System");
                Date date = new Date();
                tloi.setDateCreated(dateformatter.format(date));
                addTriggeredLOI(username, tloi);
                // match = tloi;
                break;
            }
        }

        return getTLOIsForHypothesisAndLOI(username, hypid, loiid);
    }

    /*
     * Threads helpers
     */

    public WorkflowRun getWorkflowRunStatus(String source, String id) {
        MethodAdapter methodAdapter = getMethodAdapterByName(source);
        if (methodAdapter == null)
            return null;
        return methodAdapter.getRunStatus(id);
    }

    public String getOutputData(String source, String id) {
        MethodAdapter methodAdapter = getMethodAdapterByName(source);
        if (methodAdapter == null)
            return null;
        return methodAdapter.fetchData(methodAdapter.getDataUri(id));
    }

    /*
     * Threads
     */

    class TLOIExecutionThread implements Runnable {
        String username;
        boolean metamode;
        TriggeredLOI tloi;

        public TLOIExecutionThread(String username, TriggeredLOI tloi, boolean metamode) {
            this.username = username;
            this.tloi = tloi;
            this.metamode = metamode;
        }

        @Override
        public void run() {
            try {
                if (this.metamode)
                    System.out.println("[R] Running execution thread on META mode");
                else
                    System.out.println("[R] Running execution thread");

                List<WorkflowBindings> wflowBindings = this.metamode ? tloi.getMetaWorkflows() : tloi.getWorkflows();

                boolean allok = true;
                // Start workflows from tloi
                for (WorkflowBindings bindings : wflowBindings) {
                    MethodAdapter methodAdapter = getMethodAdapterByName(bindings.getSource());
                    if (methodAdapter == null) {
                        allok = false;
                        break; // This could be `continue`, so to execute the other workflows...
                    }
                    // Get workflow input details
                    Map<String, Variable> inputs = methodAdapter.getWorkflowInputs(bindings.getWorkflow());
                    List<VariableBinding> vbindings = bindings.getBindings();
                    List<VariableBinding> sendbindings = new ArrayList<VariableBinding>(vbindings);

                    // Special processing for Meta Workflows
                    if (this.metamode) {
                        // TODO: Here we should map workflow outputs to metaworkflow inputs.
                    }

                    // Execute workflow
                    System.out.println("[R] Executing " + bindings.getWorkflow() + " with:");
                    for (VariableBinding v : vbindings) {
                        String[] l = v.isCollection() ? v.getBindingAsArray() : null;
                        System.out.println("[R] - " + v.getVariable() + ": "
                                + (l == null ? v.getBinding() : l[0] + " (" + l.length + ")"));
                    }

                    String runid = methodAdapter.runWorkflow(bindings.getWorkflow(), sendbindings, inputs);

                    if (runid != null) {
                        System.out.println("[R] Run ID: " + runid);
                        bindings.getRun().setId(runid);// .replaceAll("^.*#", ""));
                    } else {
                        allok = false;
                        System.out.println("[R] Error: Could not get run id");
                    }
                }

                tloi.setStatus(allok ? Status.RUNNING : Status.FAILED);
                updateTriggeredLOI(username, tloi.getId(), tloi);

                // Start monitoring
                if (allok) {
                    TLOIMonitoringThread monitorThread = new TLOIMonitoringThread(username, tloi, metamode);
                    monitor.schedule(monitorThread, 10, TimeUnit.SECONDS);
                } else {
                    System.out.println("[E] Finished: Something when wrong.");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    class TLOIMonitoringThread implements Runnable {
        String username;
        boolean metamode;
        TriggeredLOI tloi;

        public TLOIMonitoringThread(String username, TriggeredLOI tloi, boolean metamode) {
            this.username = username;
            this.tloi = tloi;
            this.metamode = metamode;
        }

        @Override
        public void run() {
            try {
                System.out.println("[M] Running monitoring thread");
                // Check workflow runs from tloi
                List<WorkflowBindings> wflowBindings = this.metamode ? tloi.getMetaWorkflows() : tloi.getWorkflows();

                Status overallStatus = tloi.getStatus();
                int numSuccessful = 0;
                int numFinished = 0;
                for (WorkflowBindings bindings : wflowBindings) {
                    String runid = bindings.getRun().getId();
                    MethodAdapter methodAdapter = getMethodAdapterByName(bindings.getSource());
                    if (runid == null) {
                        overallStatus = Status.FAILED;
                        numFinished++;
                        continue;
                    }
                    String rname = runid.replaceAll("^.*#", "");
                    WorkflowRun wstatus = methodAdapter.getRunStatus(rname);
                    bindings.setRun(wstatus);

                    if (wstatus.getStatus().equals("FAILURE")) {
                        overallStatus = Status.FAILED;
                        numFinished++;
                        continue;
                    }
                    if (wstatus.getStatus().equals("RUNNING")) {
                        if (overallStatus != Status.FAILED)
                            overallStatus = Status.RUNNING;
                        continue;
                    }
                    if (wstatus.getStatus().equals("SUCCESS")) {
                        numFinished++;
                        numSuccessful++;

                        // Search for p-value on the outputs
                        Map<String, String> outputs = wstatus.getOutputs();
                        if (outputs != null) {
                            for (String outname : outputs.keySet()) {
                                if (outname.equals("p_value") || outname.equals("pval") || outname.equals("p_val")) {
                                    String dataid = outputs.get(outname);
                                    String wingsP = methodAdapter.fetchData(dataid);
                                    Double pval = 0.0;
                                    try {
                                        pval = Double.valueOf(wingsP);
                                    } catch (Exception e) {
                                        System.err.println("[M] Error: " + dataid + " is a non valid p-value");
                                    }
                                    if (pval > 0) {
                                        System.out.println("[M] Detected p-value: " + pval);
                                        tloi.setConfidenceValue(pval);
                                        tloi.setConfidenceType("P-VALUE");
                                    }
                                }
                            }
                        }
                    }
                }
                // If all the workflows are successfully finished
                if (numSuccessful == wflowBindings.size()) {
                    if (metamode) {
                        overallStatus = Status.SUCCESSFUL;
                        System.out.println("[M] " + this.tloi.getId() + " was successfully executed.");
                    } else {
                        overallStatus = Status.RUNNING;
                        System.out.println("[M] Starting metamode after " + numSuccessful + " workflows.");

                        // Start meta workflows
                        TLOIExecutionThread wflowThread = new TLOIExecutionThread(username, tloi, true);
                        executor.execute(wflowThread);
                    }
                } else if (numFinished < wflowBindings.size()) {
                    monitor.schedule(this, 2, TimeUnit.MINUTES);
                }
                tloi.setStatus(overallStatus);
                updateTriggeredLOI(username, tloi.getId(), tloi);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public class DataMonitor implements Runnable {
        boolean stop;
        ScheduledFuture<?> scheduledFuture;

        public DataMonitor() {
            stop = false;
            scheduledFuture = monitor.scheduleWithFixedDelay(this, 0, 1, TimeUnit.DAYS);
        }

        public void run() {
            System.out.println("[D] Running data monitor thread");
            try {
                Thread.sleep(5000);
                if (stop) {
                    scheduledFuture.cancel(false);
                    while (!Thread.currentThread().isInterrupted()) {
                        Thread.currentThread().interrupt();
                    }
                } else if (!this.equals(dataThread)) {
                    stop();
                    return;
                } else {
                    // Re-run all hypothesis FIXME:
                    // runAllHypotheses("admin");
                }
            } catch (Exception e) {
                scheduledFuture.cancel(false);
                while (!Thread.interrupted()) {
                    stop = true;
                    Thread.currentThread().interrupt();
                }
            }

        }

        public void stop() {
            while (!Thread.interrupted()) {
                stop = true;
                scheduledFuture.cancel(false);
                Thread.currentThread().interrupt();
            }
        }
    }
}
