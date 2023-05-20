package org.diskproject.server.repository;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
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
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.SerializationUtils;
import org.apache.jena.query.QueryParseException;
import org.diskproject.server.adapters.AirFlowAdapter;
import org.diskproject.server.adapters.GraphDBAdapter;
import org.diskproject.server.adapters.SparqlAdapter;
import org.diskproject.server.util.Config;
import org.diskproject.server.util.ConfigKeys;
import org.diskproject.server.util.KBCache;
import org.diskproject.server.util.KBUtils;
import org.diskproject.server.util.VocabularyConfiguration;
import org.diskproject.server.util.Config.DataAdapterConfig;
import org.diskproject.server.util.Config.MethodAdapterConfig;
import org.diskproject.server.util.Config.VocabularyConfig;
import org.diskproject.shared.classes.adapters.DataAdapter;
import org.diskproject.shared.classes.adapters.DataResult;
import org.diskproject.shared.classes.adapters.MethodAdapter;
import org.diskproject.shared.classes.common.TreeItem;
import org.diskproject.shared.classes.common.Triple;
import org.diskproject.shared.classes.common.Value;
import org.diskproject.shared.classes.hypothesis.Hypothesis;
import org.diskproject.shared.classes.loi.LineOfInquiry;
import org.diskproject.shared.classes.loi.WorkflowBindings;
import org.diskproject.shared.classes.loi.TriggeredLOI;
import org.diskproject.shared.classes.loi.TriggeredLOI.Status;
import org.diskproject.shared.classes.question.BoundingBoxQuestionVariable;
import org.diskproject.shared.classes.question.DynamicOptionsQuestionVariable;
import org.diskproject.shared.classes.question.Question;
import org.diskproject.shared.classes.question.QuestionCategory;
import org.diskproject.shared.classes.question.QuestionVariable;
import org.diskproject.shared.classes.question.StaticOptionsQuestionVariable;
import org.diskproject.shared.classes.question.TimeIntervalQuestionVariable;
import org.diskproject.shared.classes.question.UserInputQuestionVariable;
import org.diskproject.shared.classes.question.VariableOption;
import org.diskproject.shared.classes.question.QuestionVariable.QuestionSubtype;
import org.diskproject.shared.classes.util.DataAdapterResponse;
import org.diskproject.shared.classes.util.KBConstants;
import org.diskproject.shared.classes.util.QuestionOptionsRequest;
import org.diskproject.shared.classes.vocabulary.Vocabulary;
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

    private Map<String, List<VariableOption>> optionsCache;
    private Map<String, VocabularyConfiguration> externalVocabularies;

    private Map<String, Question> allQuestions;
    private Map<String, QuestionVariable> allVariables;

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
        optionsCache = new WeakHashMap<String, List<VariableOption>>();
        this.start_read();
        SQOnt = new KBCache(this.questionKB);
        this.end();
        loadQuestionTemplates();
        this.loadVocabulariesFromConfig();
        this.initializeVocabularies();
    }

    private void loadVocabulariesFromConfig() throws Exception {
        this.externalVocabularies = new HashMap<String, VocabularyConfiguration>();
        for (VocabularyConfig v: Config.get().vocabularies) {
            KBAPI curKB = null;
            try {
                curKB = fac.getKB(v.url, OntSpec.PLAIN, false, true);
            } catch (Exception e) {
                System.out.println("Could not load " + v.url);
            }
            if (curKB != null) {
                VocabularyConfiguration vc = new VocabularyConfiguration(v.prefix, v.url, v.namespace, v.title);
                vc.setKB(curKB);
                if (v.description != null)
                    vc.setDescription(v.description);
                this.externalVocabularies.put(v.prefix, vc);
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

    // -- Data adapters
    private void initializeDataAdapters() {
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
                this.dataAdapters.put(da.endpoint, curAdapter);
            }
        }

        // Check data adapters:
        if (this.dataAdapters.size() == 0) {
            System.err.println("WARNING: No data adapters found on configuration file.");
        } else {
            for (DataAdapter curAdp : this.dataAdapters.values()) {
                if (!curAdp.ping()) {
                    System.err.println("ERROR: Could not connect with " + curAdp.getEndpointUrl());
                }
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
        for (MethodAdapterConfig ma: Config.get().methodAdapters) {
            MethodAdapter curAdapter = null;
            switch (ma.type) {
                case ConfigKeys.METHOD_TYPE_WINGS:
                    curAdapter = new WingsAdapter(ma.name, ma.endpoint, ma.username, ma.password, ma.domain, ma.internalServer);
                    break;
                case ConfigKeys.METHOD_TYPE_AIRFLOW:
                    curAdapter = new AirFlowAdapter(ma.name, ma.endpoint, ma.username, ma.password);
                    break;
                default:
                    System.out.println("Error: Method adapter type not found: '" + ma.type + "'");
                    break;
            }
            if (curAdapter != null) {
                if (ma.version != null)
                    curAdapter.setVersion(ma.version);
                this.methodAdapters.put(ma.endpoint, curAdapter);
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
                            "DISK Hypothesis Ontology: Defines general terms to express hypotheses."));

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
        KBUtils.fetchTypesAndIndividualsFromKB(kb, vocabulary);
        KBUtils.fetchPropertiesFromKB(kb, vocabulary);
        return vocabulary;
    }

    public Map<String, Vocabulary> getVocabularies() {
        return this.vocabularies;
    }

    /*
     * Hypotheses
     */
    public Hypothesis addHypothesis(String username, Hypothesis hypothesis) {
        // Check required inputs and set ID if necessary
        String name = hypothesis.getName();
        String desc = hypothesis.getDescription();
        String question = hypothesis.getQuestionId();
        String dateCreated = hypothesis.getDateCreated();
        // Set or update date
        if (dateCreated == null || dateCreated.equals("")) {
            hypothesis.setDateCreated(dateformatter.format(new Date()));
        } else {
            hypothesis.setDateModified(dateformatter.format(new Date()));
        }
        if (name != null && desc != null && question != null && !name.equals("") && !desc.equals("")
                && !question.equals("") && writeHypothesis(username, hypothesis)) {
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
        String question = hypothesis.getQuestionId();
        if (name != null && desc != null && question != null && !name.equals("") && !desc.equals("")
                && !question.equals("") &&
                hypothesis.getId().equals(id) && this.deleteHypothesis(username, id))
            return this.addHypothesis(username, hypothesis);
        return null;
    }

    public List<Hypothesis> listHypotheses(String username) {
        return listHypothesesPreviews(username);
    }

    /*
     * Lines of Inquiry
     */

    public LineOfInquiry addLOI(String username, LineOfInquiry loi) {
        String name = loi.getName();
        String desc = loi.getDescription();
        String question = loi.getQuestionId();
        String dateCreated = loi.getDateCreated();
        // Set or update date
        if (dateCreated == null || dateCreated.equals("")) {
            loi.setDateCreated(dateformatter.format(new Date()));
        } else {
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
            if (l.getQuestionId() != null)
                item.setQuestion(l.getQuestionId());
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

    private void loadQuestionTemplates() {
        this.allQuestions = new HashMap<String, Question>();
        this.allVariables = new HashMap<String, QuestionVariable>();
        for (String url : Config.get().questions.values()) {
            // Clear cache first
            try {
                start_write();
                KBAPI kb = fac.getKB(url, OntSpec.PLAIN, true, true);
                kb.removeAllTriples();
                kb.delete();
                this.save(kb);
                this.end();
            } catch (Exception e) {
                System.err.println("Error while loading " + url);
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
            // Load question variables:
            for (KBTriple t : kb.genericTripleQuery(null, typeprop, SQOnt.getClass(SQO.QUESTION_VARIABLE))) {
                KBObject qv = t.getSubject();
                allVariables.put(qv.getID(), LoadQuestionVariableFromKB(qv, kb));
            }

            // Load questions and subproperties
            for (KBTriple t : kb.genericTripleQuery(null, typeprop, SQOnt.getClass(SQO.QUESTION))) {
                // System.out.println(t.toString());
                KBObject question = t.getSubject();
                KBObject name = kb.getPropertyValue(question, labelprop);
                KBObject template = kb.getPropertyValue(question, SQOnt.getProperty(SQO.HAS_TEMPLATE));
                KBObject constraint = kb.getPropertyValue(question, SQOnt.getProperty(SQO.HAS_QUESTION_CONSTRAINT_QUERY));
                KBObject category = kb.getPropertyValue(question, SQOnt.getProperty(SQO.HAS_QUESTION_CATEGORY));
                ArrayList<KBObject> variables = kb.getPropertyValues(question, SQOnt.getProperty(SQO.HAS_VARIABLE));
                KBObject pattern = kb.getPropertyValue(question, SQOnt.getProperty(SQO.HAS_PATTERN));
                if (name != null && template != null && pattern != null) {
                    List<QuestionVariable> vars = null;
                    if (variables != null && variables.size() > 0) {
                        vars = new ArrayList<QuestionVariable>();
                        for (KBObject var : variables) {
                            QuestionVariable qv = allVariables.get(var.getID());
                            if (qv != null)
                                vars.add(qv);
                            else
                                System.err.println("Could not find " + var.getID());
                        }
                    }

                    Question q = new Question(question.getID(), name.getValueAsString(), template.getValueAsString(),
                            LoadStatements(pattern, kb), vars);
                    if (constraint != null) q.setConstraint(constraint.getValueAsString());
                    if (category != null) {
                        KBObject catName = kb.getPropertyValue(category, labelprop);
                        q.setCategory( new QuestionCategory(category.getID(), catName.getValueAsString()) );
                    }
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

    public List<Triple> LoadStatements(KBObject kbList, KBAPI kb) {
        List<KBObject> patterns = kb.getListItems(kbList);
        List<Triple> triples = new ArrayList<Triple>();
        for (KBObject p : patterns) {
            KBObject sub = kb.getPropertyValue(p, kb.getProperty(KBConstants.RDF_NS + "subject"));
            KBObject pre = kb.getPropertyValue(p, kb.getProperty(KBConstants.RDF_NS + "predicate"));
            KBObject obj = kb.getPropertyValue(p, kb.getProperty(KBConstants.RDF_NS + "object"));
            if (pre != null && obj != null) {
                triples.add(new Triple(
                    sub == null? null : sub.getValueAsString(),
                    pre.getValueAsString(),
                    obj.isLiteral() ? new Value(obj.getValueAsString(), obj.getDataType()) : new Value(obj.getValueAsString())
                ));
            }
        }
        return triples;
    }

    public QuestionVariable LoadQuestionVariableFromKB (KBObject var, KBAPI kb) {
        KBObject typeprop = kb.getProperty(KBConstants.RDF_NS + "type");
        KBObject variableName = kb.getPropertyValue(var, SQOnt.getProperty(SQO.HAS_VARIABLE_NAME));
        // Get possible subtype for this Question Variable
        List<KBObject> types = kb.getPropertyValues(var, typeprop);
        String additionalType = null;
        if (types != null) {
            for (KBObject typ : types) {
                String urlvalue = typ.getValueAsString();
                if (urlvalue.startsWith(KBConstants.QUESTION_NS)
                        && !urlvalue.equals(KBConstants.QUESTION_NS + SQO.QUESTION_VARIABLE)) {
                    additionalType = urlvalue;
                }
            }
        }

        // Create appropriate Question Variable type
        if (variableName != null) {
            QuestionVariable q = null;
            if (additionalType == null) {
                q = new QuestionVariable(var.getID(), variableName.getValueAsString());
            } else if (additionalType.equals(KBConstants.QUESTION_NS + SQO.USER_INPUT_QUESTION_VARIABLE)) {
                q = new UserInputQuestionVariable(var.getID(), variableName.getValueAsString());
                KBObject inputDatatype = kb.getPropertyValue(var, SQOnt.getProperty(SQO.HAS_INPUT_DATATYPE));
                if (inputDatatype != null)
                    ((UserInputQuestionVariable) q).setInputDatatype(inputDatatype.getValueAsString());
            } else if (additionalType.equals(KBConstants.QUESTION_NS + SQO.DYNAMIC_OPTIONS_QUESTION_VARIABLE)) {
                q = new DynamicOptionsQuestionVariable(var.getID(), variableName.getValueAsString());
                KBObject optionsQuery = kb.getPropertyValue(var, SQOnt.getProperty(SQO.HAS_OPTIONS_QUERY));
                if (optionsQuery != null)
                    ((DynamicOptionsQuestionVariable) q).setOptionsQuery(optionsQuery.getValueAsString());
            } else if (additionalType.equals(KBConstants.QUESTION_NS + SQO.STATIC_OPTIONS_QUESTION_VARIABLE)) {
                q = new StaticOptionsQuestionVariable(var.getID(), variableName.getValueAsString());
                List<KBObject> kbOptions = kb.getPropertyValues(var, SQOnt.getProperty(SQO.HAS_OPTION));
                List<VariableOption> options = new ArrayList<VariableOption>();
                for (KBObject curOption: kbOptions) {
                    KBObject label = kb.getPropertyValue(curOption, SQOnt.getProperty(SQO.HAS_LABEL));
                    KBObject value = kb.getPropertyValue(curOption, SQOnt.getProperty(SQO.HAS_VALUE));
                    KBObject comment = kb.getPropertyValue(curOption, SQOnt.getProperty(SQO.HAS_COMMENT));
                    if (label != null && value != null) {
                        VariableOption cur = new VariableOption(value.getValueAsString(), label.getValueAsString());
                        if (comment != null)
                            cur.setComment(comment.getValueAsString());
                        options.add(cur);
                    }
                }
                if (options != null)
                    ((StaticOptionsQuestionVariable) q).setOptions(options);
            } else if (additionalType.equals(KBConstants.QUESTION_NS + SQO.BOUNDING_BOX_QUESTION_VARIABLE)) {
                q = new BoundingBoxQuestionVariable(var.getID(), variableName.getValueAsString());
                KBObject minLatVar = kb.getPropertyValue(var, SQOnt.getProperty(SQO.HAS_MIN_LAT));
                KBObject minLngVar = kb.getPropertyValue(var, SQOnt.getProperty(SQO.HAS_MIN_LNG));
                KBObject maxLatVar = kb.getPropertyValue(var, SQOnt.getProperty(SQO.HAS_MAX_LAT));
                KBObject maxLngVar = kb.getPropertyValue(var, SQOnt.getProperty(SQO.HAS_MAX_LNG));
                if (minLatVar != null && minLngVar != null && maxLatVar != null && maxLngVar != null) {
                    ((BoundingBoxQuestionVariable) q).setBoundingBoxVariables(
                        ((UserInputQuestionVariable) LoadQuestionVariableFromKB(minLatVar, kb)),
                        ((UserInputQuestionVariable) LoadQuestionVariableFromKB(maxLatVar, kb)),
                        ((UserInputQuestionVariable) LoadQuestionVariableFromKB(minLngVar, kb)),
                        ((UserInputQuestionVariable) LoadQuestionVariableFromKB(maxLngVar, kb))
                    );
                }
            } else if (additionalType.equals(KBConstants.QUESTION_NS + SQO.TIME_INTERVAL_QUESTION_VARIABLE)) {
                q = new TimeIntervalQuestionVariable(var.getID(), variableName.getValueAsString());
                KBObject startTimeVar = kb.getPropertyValue(var, SQOnt.getProperty(SQO.HAS_START_TIME));
                KBObject endTimeVar = kb.getPropertyValue(var, SQOnt.getProperty(SQO.HAS_END_TIME));
                KBObject timeTypeVar = kb.getPropertyValue(var, SQOnt.getProperty(SQO.HAS_TIME_TYPE));
                if (startTimeVar != null && endTimeVar != null && timeTypeVar != null) {
                    ((TimeIntervalQuestionVariable) q).setStartTime((UserInputQuestionVariable) LoadQuestionVariableFromKB(startTimeVar, kb));
                    ((TimeIntervalQuestionVariable) q).setEndTime((UserInputQuestionVariable) LoadQuestionVariableFromKB(endTimeVar, kb));
                    ((TimeIntervalQuestionVariable) q).setTimeType((StaticOptionsQuestionVariable) LoadQuestionVariableFromKB(timeTypeVar, kb));
                }
            } else {
                System.err.println("WARN: Question subtype not implemented: " + additionalType);
                q = new QuestionVariable(var.getID(), variableName.getValueAsString());
            }

            // Set basic Question variables properties:
            KBObject minCardinality = kb.getPropertyValue(var, SQOnt.getProperty(SQO.HAS_MIN_CARDINALITY));
            KBObject maxCardinality = kb.getPropertyValue(var, SQOnt.getProperty(SQO.HAS_MAX_CARDINALITY));
            KBObject representation = kb.getPropertyValue(var, SQOnt.getProperty(SQO.HAS_REPRESENTATION));
            KBObject explanation = kb.getPropertyValue(var, SQOnt.getProperty(SQO.HAS_EXPLANATION));
            KBObject patternFragment = kb.getPropertyValue(var, SQOnt.getProperty(SQO.HAS_PATTERN_FRAGMENT));

            q.setMinCardinality(minCardinality != null ? Double.valueOf(minCardinality.getValueAsString()) : 1);
            q.setMaxCardinality(maxCardinality != null ? Double.valueOf(maxCardinality.getValueAsString()) : 1);
            if (representation != null) q.setRepresentation(representation.getValueAsString());
            if (explanation != null) q.setExplanation(explanation.getValueAsString());
            if (patternFragment != null) {
                List<Triple> pFragList = LoadStatements(patternFragment, kb);
                if (pFragList.size() > 0) q.setPatternFragment(pFragList);
            }
            return q;
        }
        return null;
    }

    public List<Question> listHypothesesQuestions() {
        return new ArrayList<Question>(this.allQuestions.values());
    }

    public List<VariableOption> listVariableOptions(String sid) throws Exception {
        if (!optionsCache.containsKey(sid)) {
            optionsCache.put(sid, this.loadVariableOptions(sid));
        }
        return optionsCache.get(sid);
    }

    private List<VariableOption> loadVariableOptions(String sid) throws Exception {
        QuestionVariable variable = null;
        // FIXME: Find a better way to handle url prefix or change the request to
        // include the full URI
        if (allVariables.containsKey("http://disk-project.org/resources/enigma/variable/" + sid)) {
            variable = allVariables.get("http://disk-project.org/resources/enigma/variable/" + sid);
        } else if (allVariables.containsKey("http://disk-project.org/resources/question/" + sid)) {
            variable = allVariables.get("http://disk-project.org/resources/question/" + sid);
        } else if (allVariables.containsKey("https://w3id.org/sqo/resource/" + sid)) {
            variable = allVariables.get("https://w3id.org/sqo/resource/" + sid);
        }
        // -----
        //QuestionVariable variable = allVariables.containsKey(sid) ? allVariables.get(sid) : null;
        if (variable != null) {
            String varname = variable.getVariableName();
            if (variable.getSubType() == QuestionVariable.QuestionSubtype.DYNAMIC_OPTIONS) {
                String optionsQuery = ((DynamicOptionsQuestionVariable) variable).getOptionsQuery();
                if (optionsQuery != null)
                    return queryForOptions(varname, optionsQuery);
            }
            if (variable.getSubType() == QuestionVariable.QuestionSubtype.STATIC_OPTIONS) {
                List<VariableOption> fixedOptions = ((StaticOptionsQuestionVariable) variable).getOptions();
                if (fixedOptions != null)
                    return fixedOptions;
            }
        }
        return null;
    }

    private List<VariableOption> queryForOptions (String varName, String query) throws Exception {
        List<VariableOption> options = new ArrayList<VariableOption>();
        // If there is a constraint query, send it to all data providers;
        Map<String, List<DataResult>> solutions = new HashMap<String, List<DataResult>>();
        for (DataAdapter adapter : this.dataAdapters.values()) {
            solutions.put(adapter.getName(), adapter.queryOptions(varName, query));
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
                List<String> opt = sameLabelOptions.get(0);
                options.add(new VariableOption(opt.get(0), opt.get(1)));
            } else { // There's more than one option with the same label
                boolean allTheSame = true;
                String lastValue = sameLabelOptions.get(0).get(0); // Comparing IDs
                for (List<String> curOption : sameLabelOptions) {
                    if (!lastValue.equals(curOption.get(0))) {
                        allTheSame = false;
                        break;
                    }
                }
                if (allTheSame) { // All ids are the same, add one option.
                    List<String> opt = sameLabelOptions.get(0);
                    options.add(new VariableOption(opt.get(0), opt.get(1)));
                } else {
                    Map<String, Integer> dsCount = new HashMap<String, Integer>();

                    for (List<String> curOption : sameLabelOptions) {
                        String curValue = curOption.get(0);

                        String dataSource = curOption.get(2);
                        Integer count = dsCount.containsKey(dataSource) ? dsCount.get(dataSource) : 0;
                        String label = curOption.get(1) + " (" + dataSource
                                + (count > 0 ? "_" + count.toString() : "") + ")";
                        dsCount.put(dataSource, (count + 1));

                        options.add(new VariableOption(curValue, label));
                    }
                }
            }
        }
        return options;
    }

    public String createQuestionOptionsQuery (Question q) {
        if (q != null) {
            String queryConstraint = q.getConstraint();
            String query = queryConstraint != null ? queryConstraint : "";
            for (QuestionVariable qv: q.getVariables()) {
                QuestionSubtype t = qv.getSubType();
                if (t == QuestionSubtype.DYNAMIC_OPTIONS || t == QuestionSubtype.BOUNDING_BOX || t == QuestionSubtype.TIME_INTERVAL) {
                    String queryFragment = ((DynamicOptionsQuestionVariable) qv).getOptionsQuery();
                    if (queryFragment != null)
                        query += queryFragment;
                }
            }
            return query;
        }
        return null;
    }

    public Map<String,List<VariableOption>> listDynamicOptions (QuestionOptionsRequest cfg) throws Exception {
        Map<String, String> bindings = cfg.getBindings();
        Question q = allQuestions.get(cfg.getId());
        String query = createQuestionOptionsQuery(q);
        if (q == null) return null;

        // Create map variableName -> filter
        Map<String, String> filters = new HashMap<String, String>();
        if (bindings != null && query != null) {
            for (String varUrl: bindings.keySet()) {
                QuestionVariable curVar = allVariables.get(varUrl);
                if (curVar != null) {
                    String value = bindings.get(varUrl);
                    String name = curVar.getVariableName();
                    String sparqlValue = value.startsWith("http") ? "<" + value + ">" : "\"" + value + "\"";
                    String line = "VALUES " + name + " { " + sparqlValue + " }";
                    filters.put(name, line);
                } else {
                    System.err.println("Cannot find variable ID: " + varUrl);
                }
            }
        }

        Map<String, List<VariableOption>> varNameToOptions = new HashMap<String, List<VariableOption>>();
        for (QuestionVariable qv: q.getVariables()) {
            QuestionSubtype t = qv.getSubType();
            String varName = qv.getVariableName();
            if (t == QuestionSubtype.STATIC_OPTIONS) {
                varNameToOptions.put(varName, ((StaticOptionsQuestionVariable) qv).getOptions());
            } else if (t == QuestionSubtype.DYNAMIC_OPTIONS) {
                // We add all the filter except the value for the queried variable
                if (query == null) {
                    System.err.println("WARN: Could not find suitable query for " + qv.getId());
                } else {
                    String curQuery = query;
                    if (filters != null && filters.size() > 0) {
                        for (String filterVarName: filters.keySet()) {
                            if (!filterVarName.equals(qv.getVariableName())) {
                                curQuery += "\n" + filters.get(filterVarName);
                            }
                        }
                    }
                    varNameToOptions.put(varName, queryForOptions(varName, curQuery));
                    if (varNameToOptions.get(varName).size() == 0) {
                        System.out.println(qv.getId() + " got 0 results:");
                        System.out.println(curQuery);
                    } else {
                        System.out.println(qv.getId() + " got " + varNameToOptions.get(varName).size() + " results.");
                    }
                }
            }
        }
        return varNameToOptions;
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
        // LoiId -> [{ variable -> value }]
        Map<String, List<Map<String, String>>> allMatches = new HashMap<String, List<Map<String, String>>>();
        List<LineOfInquiry> lois = this.listLOIPreviews(username);

        // Starts checking all LOIs that match the hypothesis directly from the KB.
        try {
            this.start_read();
            KBAPI hypKB = this.fac.getKB(hypuri, OntSpec.PLAIN, true);
            System.out.println("GRAPH: " + hypKB.getAllTriples().toString().replace("),", ")\n"));
            for (LineOfInquiry loi : lois) {
                String hq = loi.getHypothesisQuery();
                if (hq != null) {
                    String query = this.getAllPrefixes() + "SELECT DISTINCT * WHERE { \n"
                            + loi.getHypothesisQuery() + " }";
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
                            //String errorMesString = "No solutions found for the query: \n" + query;
                            //System.out.println(errorMesString);
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
                                // If there is at least one variable, add to match list.
                                if (cur.size() > 0) {
                                    String loiId = loi.getId().replaceAll("^.*\\/", "");
                                    if (!allMatches.containsKey(loiId))
                                        allMatches.put(loiId, new ArrayList<Map<String, String>>());
                                    List<Map<String, String>> curList = allMatches.get(loiId);
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
                for (String qVar : loi.getAllWorkflowVariables())
                    query += qVar + " ";
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
                } else {
                    System.out.println("LOI " + loi.getId() + " got no results. " + values);
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
            for (TriggeredLOI cur : candidates) {
                if (cur.toString().equals(tloi.toString())) {
                    // TODO: compare the hash of the input files
                    System.out.println("Replaced " + tloi.getId() + " with " + cur.getId());
                    real = cur;
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

            for (VariableBinding vBinding : bindings.getBindings()) { // Normal variable bindings.
                // For each Variable binding, check :
                // - If this variable expects a collection or single values
                // - Check the binding values on the data store
                String binding = vBinding.getBinding();
                Matcher collmat = varCollPattern.matcher(binding);
                Matcher mat = varPattern.matcher(binding);

                // Get the sparql variable
                boolean isCollection = false;
                String sparqlVar = null;
                if (collmat.find() && dataVarBindings.containsKey(collmat.group(1))) {
                    sparqlVar = collmat.group(1);
                    isCollection = true;
                } else if (mat.find() && dataVarBindings.containsKey(mat.group(1))) {
                    sparqlVar = mat.group(1);
                }

                if (sparqlVar == null)
                    continue;

                // Get the data bindings for the sparql variable
                List<String> dsUrls = dataVarBindings.get(sparqlVar);

                // Checks if the bindings are input files.
                boolean bindingsAreFiles = true;
                for (String curUrl : dsUrls) {
                    if (!curUrl.startsWith("http")) {
                        bindingsAreFiles = false;
                        break;
                    }
                }

                // Datasets names
                List<String> dsNames = new ArrayList<String>();

                if (bindingsAreFiles) {
                    String varName = vBinding.getVariable();
                    String dType = null;
                    for (Variable v: allVars) {
                        if (varName.equals(v.getName())) {
                            List<String> classes = v.getType();
                            if (classes != null && classes.size() > 0) {
                                dType = classes.contains(vBinding.getType()) ? vBinding.getType() : classes.get(0);
                            }
                        }
                    }
                    // TODO: this should be async
                    // Check hashes, create local name and upload data:
                    Map<String, String> urlToName = addData(dsUrls, methodAdapter, dataAdapter, dType);
                    for (String dsUrl : dsUrls) {
                        String dsName = urlToName.containsKey(dsUrl) ? urlToName.get(dsUrl)
                                : dsUrl.replaceAll("^.*\\/", "");
                        dsNames.add(dsName);
                    }
                } else {
                    // If the binding is not a file, send the value with no quotes
                    for (String value : dsUrls) {
                        // Remove quotes from parameters
                        if (value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
                            value = value.substring(1, value.length() - 1);
                        }
                        dsNames.add(value);
                    }
                }

                // If Collection, all datasets go to same workflow
                if (isCollection) {
                    // This variable expects a collection. Modify the existing tloiBinding values,
                    // collections of non-files are send as comma separated values:
                    tloiBinding.addBinding(new VariableBinding(vBinding.getVariable(), dsNames.toString()));
                } else {
                    if (dsNames.size() == 1) {
                        tloiBinding.addBinding(new VariableBinding(vBinding.getVariable(), dsNames.get(0)));
                    } else {
                        System.out.println("IS MORE THAN ONE VALUE BUT NOT COLLECTION!");
                        // This variable expects a single file. Add new tloi bindings for each dataset
                        List<WorkflowBindings> newTloiBindings = new ArrayList<WorkflowBindings>();
                        for (WorkflowBindings tmpBinding : tloiBindings) { // For all already processed workflow
                                                                           // bindings
                            for (String dsName : dsNames) {
                                ArrayList<VariableBinding> newBindings = (ArrayList<VariableBinding>) SerializationUtils
                                        .clone((Serializable) tmpBinding.getBindings());

                                WorkflowBindings newWorkflowBindings = new WorkflowBindings(
                                        bindings.getWorkflow(),
                                        bindings.getWorkflowLink(),
                                        newBindings);
                                newWorkflowBindings.addBinding(new VariableBinding(vBinding.getVariable(), dsName));
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
    private Map<String, String> addData(List<String> dsUrls, MethodAdapter methodAdapter, DataAdapter dataAdapter, String dType)
            throws Exception {
        // To add files to wings and not replace anything, we need to get the hash from the wiki.
        // TODO: here connect with minio.
        Map<String, String> nameToUrl = new HashMap<String, String>();
        Map<String, String> urlToName = new HashMap<String, String>();
        Map<String, String> filesETag = dataAdapter.getFileHashesByETag(dsUrls);  // File -> ETag
        boolean allOk = true; // All is OK if we have all file ETags.

        for (String fileUrl: dsUrls) {
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
            Map<String, String> hashes = dataAdapter.getFileHashes(dsUrls); // File -> SHA1
            for (String fileUrl : dsUrls) {
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
        for (String file : dsUrls) {
            if (!urlToName.containsKey(file)) {
                //TODO: handle exception
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
            System.out.println("Uploading to " + methodAdapter.getName() + ": " + newFile + " as " + newFilename + "(" + dType + ")");
            methodAdapter.addData(newFile, newFilename, dType);
        }

        return urlToName;
    }

    public Boolean runAllHypotheses(String username) throws Exception {
        List<String> hList = new ArrayList<String>();
        String url = this.HYPURI(username);
        try {
            KBAPI kb = this.fac.getKB(url, OntSpec.PLAIN, true);
            KBObject hypCls = DISKOnt.getClass(DISK.HYPOTHESIS);

            this.start_read();
            KBObject typeprop = kb.getProperty(KBConstants.RDF_NS + "type");
            for (KBTriple t : kb.genericTripleQuery(null, typeprop, hypCls)) {
                KBObject hypobj = t.getSubject();
                String uri = hypobj.getID();
                String[] sp = uri.split("/");
                hList.add(sp[sp.length - 1]);
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

        List<TriggeredLOI> tList = new ArrayList<TriggeredLOI>();

        for (String hid : hList) {
            tList.addAll(queryHypothesis(username, hid));
        }

        // Only hypotheses with status == null are new
        for (TriggeredLOI tloi : tList) {
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

    public Map<String, String> getNarratives(String username, String tloiId) {
        // DEPRECATED!
        Map<String, String> narratives = new HashMap<String, String>();
        TriggeredLOI tloi = this.getTriggeredLOI(username, tloiId);
        if (tloi != null) {
            String hypId = tloi.getParentHypothesisId();
            String loiId = tloi.getParentLoiId();
            Hypothesis hyp = this.getHypothesis(username, hypId);
            LineOfInquiry loi = this.getLOI(username, loiId);

            if (loi == null || hyp == null) {
                System.out.println("ERROR: could not get hypotheses or LOI");
                return narratives;
            }

            // Assuming each tloi only has a workflow or metaworkflow:
            WorkflowBindings wf = null;
            List<WorkflowBindings> wfs = tloi.getWorkflows();
            List<WorkflowBindings> metaWfs = tloi.getMetaWorkflows();

            if (metaWfs != null && metaWfs.size() > 0) {
                wf = metaWfs.get(0);
            } else if (wfs != null && wfs.size() > 0) {
                wf = wfs.get(0);
            } else {
                System.out.println("TLO ID: " + tloiId + " has does not have any workflow.");
                return null;
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
                        String bindingData = bindings[i];
                        String dataname = bindingData.replaceAll("^.*#", "").replaceAll("SHA\\w{6}_", "");
                        String url = fileprefix + bindingData;
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
                        for (String bindingData : ds.getBindingAsArray()) {
                            String dataname = bindingData.replaceAll("^.*#", "").replaceAll("SHA\\w{6}_", "");
                            String url = fileprefix + bindingData;
                            String anchor = "<a target=\"_blank\" href=\"" + url + "\">" + dataname + "</a>";
                            dataset += "<li>" + anchor + "</li>";
                        }
                    }
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
            // + "</ol></div>The resulting p-value is $(tloi.pVal).";
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

    public List<TriggeredLOI> runHypothesisAndLOI(String username, String hypId, String loiId) throws Exception {
        List<TriggeredLOI> hypTlois = queryHypothesis(username, hypId);
        // TriggeredLOI match = null;
        for (TriggeredLOI tloi : hypTlois) {
            if (tloi.getStatus() == null && tloi.getParentLoiId().equals(loiId)) {
                // Set basic metadata
                tloi.setAuthor("System");
                Date date = new Date();
                tloi.setDateCreated(dateformatter.format(date));
                addTriggeredLOI(username, tloi);
                // match = tloi;
                break;
            }
        }

        return getTLOIsForHypothesisAndLOI(username, hypId, loiId);
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

    public byte[] getOutputData(String source, String id) {
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

                boolean allOk = true;
                // Start workflows from tloi
                for (WorkflowBindings bindings : wflowBindings) {
                    MethodAdapter methodAdapter = getMethodAdapterByName(bindings.getSource());
                    if (methodAdapter == null) {
                        allOk = false;
                        break; // This could be `continue`, so to execute the other workflows...
                    }
                    // Get workflow input details
                    Map<String, Variable> inputs = methodAdapter.getWorkflowInputs(bindings.getWorkflow());
                    List<VariableBinding> vBindings = bindings.getBindings();
                    List<VariableBinding> sendbindings = new ArrayList<VariableBinding>(vBindings);

                    // Special processing for Meta Workflows
                    if (this.metamode) {
                        // TODO: Here we should map workflow outputs to metaworkflow inputs.
                    }

                    // Execute workflow
                    System.out.println("[R] Executing " + bindings.getWorkflow() + " with:");
                    for (VariableBinding v : vBindings) {
                        String[] l = v.isCollection() ? v.getBindingAsArray() : null;
                        System.out.println("[R] - " + v.getVariable() + ": "
                                + (l == null ? v.getBinding() : l[0] + " (" + l.length + ")"));
                    }

                    List<String> runIds = methodAdapter.runWorkflow(bindings.getWorkflow(), sendbindings, inputs);

                    if (runIds != null) {
                        System.out.println("[R] Workflow send: ");
                        for (String rid: runIds) {
                            WorkflowRun run = new WorkflowRun();
                            run.setId(rid);
                            System.out.println("[R]   ID: " + rid);
                            bindings.setRun(run);
                        }
                    } else {
                        allOk = false;
                        System.out.println("[R] Error: Could not run workflow");
                    }
                }

                tloi.setStatus(allOk ? Status.RUNNING : Status.FAILED);
                updateTriggeredLOI(username, tloi.getId(), tloi);

                // Start monitoring
                if (allOk) {
                    TLOIMonitoringThread monitorThread = new TLOIMonitoringThread(username, tloi, metamode);
                    monitor.schedule(monitorThread, 15, TimeUnit.SECONDS);
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
        Map<String, Map<String, Status>> runs;
        boolean error;

        public TLOIMonitoringThread(String username, TriggeredLOI tloi, boolean metamode) {
            this.username = username;
            this.tloi = tloi;
            this.error = false;
            this.metamode = metamode;
            this.runs = new HashMap<String, Map<String, Status>>();
            for (WorkflowBindings bindings : (metamode ? this.tloi.getMetaWorkflows() : this.tloi.getWorkflows())) {
                Map<String, Status> curMap = new HashMap<String,Status>();
                for (String runId: bindings.getRuns().keySet()){
                    curMap.put(runId, Status.QUEUED);
                }
                this.runs.put(bindings.getWorkflow(), curMap);
            }
        }

        private String getRandomRunId (String wfName) {
            Map<String, Status> s = this.runs.get(wfName);
            List<String> queued = new ArrayList<String>();
            for (String key: s.keySet()) {
                if (s.get(key) == Status.QUEUED)
                    queued.add(key);
            }
            int size = queued.size();
            if (size == 0) return null;

            int rnd = new Random().nextInt(size);
            Iterator<String> iter = queued.iterator();
            for (int i = 0; i < rnd; i++) {
                iter.next();
            }
            return iter.next();
        }

        private void removeFromQueue (String wfName, String id, Status s) {
            this.runs.get(wfName).put(id, s);
        }

        private boolean isFinished () {
            for (WorkflowBindings wf  : (metamode ? tloi.getMetaWorkflows() : tloi.getWorkflows())) {
                if (getRandomRunId(wf.getWorkflow()) != null)
                    return false;
            }
            return true;
        }

        @Override
        public void run() {
            try {
                System.out.println("[M] Running monitoring thread");
                // Check workflow runs from tloi
                List<WorkflowBindings> wflowBindings = this.metamode ? tloi.getMetaWorkflows() : tloi.getWorkflows();

                Status overallStatus = tloi.getStatus();
                for (WorkflowBindings bindings : wflowBindings) {
                    String wfName = bindings.getWorkflow();
                    String runId = getRandomRunId(wfName);
                    System.out.println("[M] Pending runs: " + bindings.getRuns().size());
                    MethodAdapter methodAdapter = getMethodAdapterByName(bindings.getSource());
                    if (runId == null) { // lets assume thats is bc we have finished.
                        continue;
                    }
                    String rName = runId.replaceAll("^.*#", "");
                    WorkflowRun wStatus = methodAdapter.getRunStatus(rName);
                    bindings.setRun(wStatus);

                    if (wStatus.getStatus() == null || wStatus.getStatus().equals("FAILURE")) {
                        if (wStatus.getStatus() == null)
                            System.out.println("[E] Cannot get status for " + tloi.getId() + " - RUN " + rName);
                        overallStatus = Status.FAILED;
                        this.error = true;
                        removeFromQueue(wfName, runId, Status.FAILED);
                        continue;
                    }
                    if (wStatus.getStatus().equals("RUNNING")) {
                        if (overallStatus != Status.FAILED)
                            overallStatus = Status.RUNNING;
                        continue;
                    }
                    if (wStatus.getStatus().equals("SUCCESS")) {
                        removeFromQueue(wfName, runId, Status.SUCCESSFUL);

                        // Search for p-value on the outputs
                        // TODO: change this to allow any output.
                        Map<String, String> outputs = wStatus.getOutputs();
                        if (outputs != null) {
                            for (String outname : outputs.keySet()) {
                                if (outname.equals("p_value") || outname.equals("pval") || outname.equals("p_val")) {
                                    String dataid = outputs.get(outname);
                                    byte[] byteConf = methodAdapter.fetchData(dataid);
                                    String wingsP = byteConf != null ? new String(byteConf, StandardCharsets.UTF_8) : null;
                                    Double pVal = null;
                                    try {
                                        String strPVal = wingsP != null ? wingsP.split("\n",2)[0] : "";
                                        pVal = Double.valueOf(strPVal);
                                    } catch (Exception e) {
                                        System.err.println("[M] Error: " + dataid + " is a non valid p-value: " + wingsP);
                                    }
                                    if (pVal != null) {
                                        System.out.println("[M] Detected p-value: " + pVal);
                                        tloi.setConfidenceValue(pVal);
                                        tloi.setConfidenceType("P-VALUE");
                                    }
                                }
                            }
                        }
                    }
                }
                // Check runs
                if (isFinished()) {
                    if (metamode) {
                        if (error) {
                            overallStatus = Status.FAILED;
                            System.out.println("[M] " + this.tloi.getId() + " was executed with errors.");
                        } else {
                            overallStatus = Status.SUCCESSFUL;
                            System.out.println("[M] " + this.tloi.getId() + " was successfully executed.");
                        }
                    } else {
                        if (error) {
                            overallStatus = Status.FAILED;
                            System.out.println("[M] " + this.tloi.getId() + " will not initialice metamode as some runs failed.");
                        } else {
                            overallStatus = Status.RUNNING;
                            System.out.println("[M] Starting metamode after n workflows.");
                            // Start meta workflows TODO: should pass workflow results.
                            TLOIExecutionThread wflowThread = new TLOIExecutionThread(username, tloi, true);
                            executor.execute(wflowThread);
                        }
                    }
                } else {
                    monitor.schedule(this, 20, TimeUnit.SECONDS);
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
