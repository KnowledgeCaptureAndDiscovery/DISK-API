package org.diskproject.server.repository;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import org.diskproject.server.adapters.AirFlowAdapter;
import org.diskproject.server.adapters.DataAdapter;
import org.diskproject.server.adapters.DataResult;
import org.diskproject.server.adapters.MethodAdapter;
import org.diskproject.server.adapters.SparqlAdapter;
//import org.diskproject.server.repository.GmailService.MailMonitor;
import org.diskproject.server.util.Config;
import org.diskproject.server.util.DataQuery;
import org.diskproject.shared.classes.common.Graph;
import org.diskproject.shared.classes.common.TreeItem;
import org.diskproject.shared.classes.common.Triple;
import org.diskproject.shared.classes.common.TripleDetails;
import org.diskproject.shared.classes.common.TripleUtil;
import org.diskproject.shared.classes.common.Value;
import org.diskproject.shared.classes.hypothesis.Hypothesis;
import org.diskproject.shared.classes.loi.LineOfInquiry;
import org.diskproject.shared.classes.loi.WorkflowBindings;
import org.diskproject.shared.classes.loi.TriggeredLOI;
import org.diskproject.shared.classes.loi.TriggeredLOI.Status;
import org.diskproject.shared.classes.question.Question;
import org.diskproject.shared.classes.question.QuestionVariable;
import org.diskproject.shared.classes.util.KBConstants;
import org.diskproject.shared.classes.vocabulary.Individual;
import org.diskproject.shared.classes.vocabulary.Property;
import org.diskproject.shared.classes.vocabulary.Vocabulary;
import org.diskproject.shared.classes.vocabulary.Type;
import org.diskproject.shared.classes.workflow.Variable;
import org.diskproject.shared.classes.workflow.VariableBinding;
import org.diskproject.shared.classes.workflow.WorkflowRun;

import edu.isi.kcap.ontapi.KBAPI;
import edu.isi.kcap.ontapi.KBObject;
import edu.isi.kcap.ontapi.KBTriple;
import edu.isi.kcap.ontapi.OntSpec;
import edu.isi.kcap.ontapi.SparqlQuerySolution;

public class DiskRepository extends WriteKBRepository {
    static DiskRepository singleton;
    private static boolean creatingKB = false;

    private static SimpleDateFormat dateformatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
    Pattern varPattern = Pattern.compile("\\?(.+?)\\b");
    Pattern varCollPattern = Pattern.compile("\\[\\s*\\?(.+?)\\s*\\]");

    protected KBAPI hypontkb;
    protected KBAPI questionkb;

    Map<String, Vocabulary> vocabularies;
    ScheduledExecutorService monitor;
    ScheduledExecutorService monitorData;
    ExecutorService executor;
    static DataMonitor dataThread;

    private Map<String, List<List<String>>> optionsCache;
    private Map<String, DataAdapter> dataAdapters;
    private Map<String, MethodAdapter> methodAdapters;

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
        setConfiguration();

        dataAdapters = new HashMap<String, DataAdapter>();
        methodAdapters = new HashMap<String, MethodAdapter>();
        optionsCache = new WeakHashMap<String, List<List<String>>>();
        //Set domain for writing the KB
        this.setDomain(this.server);
        // Initialize
        this.initializeDataAdapters();
        this.initializeMethodAdapters();
        initializeKB();
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

    public void initializeKB() {
        super.initializeKB(); // This initializes this.ontkb 
        if (fac == null)
            return;
        
        try {
            this.hypontkb   = fac.getKB(KBConstants.HYPURI(), OntSpec.PLAIN, false, true);
            this.questionkb = fac.getKB(KBConstants.QUESTIONSURI(), OntSpec.PLAIN, false, true);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error reading KB");
            return;
        }
           
        this.initializeVocabularies();
        loadQuestionTemplates();
    }

    public void reloadKBCaches () {
        KBAPI[] kbs = {this.ontkb, this.hypontkb, this.questionkb};

        try {
            this.start_write();
            for (KBAPI kb: kbs) if (kb != null) {
                System.out.println("Reloading " + kb.getURI());
                kb.removeAllTriples();
                kb.delete();
                this.save(kb);
                this.end(); this.start_write();
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
    private void initializeDataAdapters () {
        //Reads data adapters from config file.
        PropertyListConfiguration cfg = this.getConfig();
        Map<String, Map<String,String>> endpoints = new HashMap<String, Map<String,String>>();
        Iterator<String> a = cfg.getKeys("data-adapters");
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
        
        for (String name: endpoints.keySet()) {
            Map<String,String> cur = endpoints.get(name);
            String curURI = cur.get("endpoint"),
                   curType = cur.get("type");
            String curUser = null, curPass = null, curNamespace = null, curPrefix = null;
            if (cur.containsKey("username")) curUser = cur.get("username");
            if (cur.containsKey("password")) curPass = cur.get("password");
            if (cur.containsKey("namespace")) curNamespace = cur.get("namespace");
            if (cur.containsKey("prefix")) curPrefix = cur.get("prefix");

            switch (curType) {
                case "sparql":
                    DataAdapter curAdapter = new SparqlAdapter(curURI, name, curUser, curPass);
                    if (curNamespace != null && curPrefix != null) {
                        curAdapter.setPrefix(curPrefix, curNamespace);
                        //this.vocabularies.put(curURI, this.initializeVocabularyFromDataAdapter();
                    }
                    this.dataAdapters.put(curURI, curAdapter);
                    break;

                default:
                    break;
            }
        }
    }

    private DataAdapter getDataAdapter (String url) {
        if (this.dataAdapters.containsKey(url))
            return this.dataAdapters.get(url);
        return null;
    }

    // -- Method adapters
    private void initializeMethodAdapters () {
        //Reads method adapters from config file.
        PropertyListConfiguration cfg = this.getConfig();
        Map<String, Map<String,String>> adapters = new HashMap<String, Map<String,String>>();
        Iterator<String> a = cfg.getKeys("method-adapters");
        while (a.hasNext()) {
            String key = a.next();
            String sp[] = key.split("\\.");
            
            if (sp != null && sp.length == 3) { // as the list is normalized length is how deep the property is
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
        
        for (String name: adapters.keySet()) {
            Map<String,String> cur = adapters.get(name);
            String curURI = cur.get("endpoint"), curType = cur.get("type");
            String curUser = null, curPass = null, curDomain = null, curInternalServer = null;
            if (cur.containsKey("username")) curUser = cur.get("username");
            if (cur.containsKey("password")) curPass = cur.get("password");
            if (cur.containsKey("domain")) curDomain = cur.get("domain");
            if (cur.containsKey("internal_server")) curInternalServer = cur.get("internal_server");

            MethodAdapter curAdapter = null;
            switch (curType) {
                case "wings":
                    curAdapter = new WingsAdapter(name, curURI, curUser, curPass, curDomain, curInternalServer);
                    break;
                case "airflow":
                    curAdapter = new AirFlowAdapter(name, curURI, curUser, curPass);
                    break;
                default:
                    break;
            }
            if (curAdapter != null)
                this.methodAdapters.put(curURI, curAdapter);
        }
    }

    private MethodAdapter getMethodAdapter (String url) {
        if (this.methodAdapters.containsKey(url))
            return this.methodAdapters.get(url);
        return null;
    }

    // -- Vocabulary Initialization
    private void initializeVocabularies () {
        this.vocabularies = new HashMap<String, Vocabulary>();
        try {
            this.start_read();
            this.vocabularies.put(KBConstants.HYPURI(),
                    this.initializeVocabularyFromKB(this.hypontkb, KBConstants.HYPNS()));
            this.vocabularies.put(KBConstants.DISKURI(),
                    this.initializeVocabularyFromKB(this.ontkb, KBConstants.DISKNS()));
            this.vocabularies.put(KBConstants.QUESTIONSURI(),
                    this.initializeVocabularyFromKB(this.questionkb, KBConstants.QUESTIONSNS()));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            this.end();
        }
    }

    public Vocabulary getVocabulary(String uri) {
        return this.vocabularies.get(uri);
    }

    public Vocabulary initializeVocabularyFromKB(KBAPI kb, String ns) {
        Vocabulary vocabulary = new Vocabulary(ns);
        this.fetchTypesAndIndividualsFromKB(kb, vocabulary);
        this.fetchPropertiesFromKB(kb, vocabulary);
        return vocabulary;
    }

    private void fetchPropertiesFromKB(KBAPI kb, Vocabulary vocabulary) {
        for (KBObject prop : kb.getAllProperties()) {
            if (!prop.getID().startsWith(vocabulary.getNamespace()))
                continue;

            KBObject domcls = kb.getPropertyDomain(prop);
            KBObject rangecls = kb.getPropertyRange(prop);

            Property mprop = new Property();
            mprop.setId(prop.getID());
            mprop.setName(prop.getName());

            String label = this.createPropertyLabel(prop.getName());
            mprop.setLabel(label);
            if (domcls != null)   mprop.setDomain(domcls.getID());
            if (rangecls != null) mprop.setRange(rangecls.getID());

            vocabulary.addProperty(mprop);
        }
    }

    private void fetchTypesAndIndividualsFromKB(KBAPI kb, Vocabulary vocabulary) {
        KBObject typeprop = kb.getProperty(KBConstants.RDFNS() + "type");
        for (KBTriple t : kb.genericTripleQuery(null, typeprop, null)) {
            KBObject inst = t.getSubject();
            KBObject typeobj = t.getObject();
            String instid = inst.getID();

            if (instid == null || instid.startsWith(vocabulary.getNamespace()) 
                || typeobj.getNamespace().equals(KBConstants.OWLNS())) {
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
            String clsid = typeobj.getID();
            Type type = new Type();
            type.setId(clsid);
            type.setName(typeobj.getName());
            type.setLabel(kb.getLabel(typeobj));
            vocabulary.addType(type);
        }

        // Add types not asserted
        KBObject clsobj = kb.getProperty(KBConstants.OWLNS() + "Class");
        for (KBTriple t : kb.genericTripleQuery(null, typeprop, clsobj)) {
            KBObject cls = t.getSubject();
            String clsid = cls.getID();
            if (clsid == null || !clsid.startsWith(vocabulary.getNamespace()) || cls.getNamespace().equals(KBConstants.OWLNS()) ) {
                continue;
            }

            Type type = vocabulary.getType(clsid);
            if (type == null) {
                type = new Type();
                type.setId(clsid);
                type.setName(cls.getName());
                type.setLabel(kb.getLabel(cls));
                vocabulary.addType(type);
            }
        }

        // Add type hierarchy
        KBObject subclsprop = kb.getProperty(KBConstants.RDFSNS() + "subClassOf");
        for (KBTriple t : kb.genericTripleQuery(null, subclsprop, null)) {
            KBObject subcls = t.getSubject();
            KBObject cls = t.getObject();
            String clsid = cls.getID();

            Type subtype = vocabulary.getType(subcls.getID());
            if (subtype == null)
                continue;

            if (!clsid.startsWith(KBConstants.OWLNS()))
                subtype.setParent(clsid);

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

    public Map<String, String> getEndpoints () {
      Map<String, String> endpoints = new HashMap<String, String>();
      for (String key: dataAdapters.keySet()) {
          DataAdapter d = dataAdapters.get(key);
          endpoints.put(d.getName(), d.getEndpointUrl());
      }
      return endpoints;
    }

    public Map<String, Vocabulary> getVocabularies() {
        return this.vocabularies;
    }

    /*
     * Hypotheses
     */

    public boolean addHypothesis (String username, Hypothesis hypothesis) {
        return writeHypothesis(username, hypothesis);
    }

    public boolean removeHypothesis (String username, String id) {
        return this.deleteHypothesis(username, id);
    }

    public Hypothesis getHypothesis(String username, String id) {
        return loadHypothesis(username, id);
    }

    public Hypothesis updateHypothesis(String username, String id, Hypothesis hypothesis) {
        if (hypothesis.getId() != null && this.deleteHypothesis(username, id) && this.addHypothesis(username, hypothesis))
            return hypothesis;
        return null;
    }

    public List<TreeItem> listHypotheses(String username) {
        List<TreeItem> list = new ArrayList<TreeItem>();
        List<Hypothesis> hypothesisList = listHypothesesPreviews(username);

        for (Hypothesis h : hypothesisList) {
            TreeItem item = new TreeItem(h.getId(), h.getName(), h.getDescription(), h.getParentId(), h.getDateCreated(), h.getAuthor());
            if (h.getDateModified() != null)
                item.setDateModified(h.getDateModified());
            list.add(item);
        }
        return list;
    }

    /*
     * Lines of Inquiry
     */

    public boolean addLOI(String username, LineOfInquiry loi) {
        return writeLOI(username, loi);
    }

    public boolean removeLOI(String username, String id) {
        return deleteLOI(username, id);
    }

    public LineOfInquiry getLOI(String username, String id) {
        return this.loadLOI(username, id);
    }

    public LineOfInquiry updateLOI(String username, String id, LineOfInquiry loi) {
        if (loi.getId() != null && this.deleteLOI(username, id) && this.addLOI(username, loi))
            return loi;
        return null;
    }

    public List<TreeItem> listLOIs(String username) {
        List<TreeItem> list = new ArrayList<TreeItem>();
        List<LineOfInquiry> lois = this.listLOIPreviews(username);

        for (LineOfInquiry l: lois) {
            TreeItem item = new TreeItem(l.getId(), l.getName(), l.getDescription(), null, l.getDateCreated(), l.getAuthor());
            if (l.getDateModified() != null) item.setDateModified(l.getDateModified());
            list.add(item);
        }
        return list;
    }

    /*
     * Triggered Lines of Inquiries (TLOIs)
     */

    public void addTriggeredLOI(String username, TriggeredLOI tloi) {
        tloi.setStatus(Status.QUEUED);
        writeTLOI(username, tloi);

        TLOIExecutionThread wflowThread = new TLOIExecutionThread(username, tloi, false);
        executor.execute(wflowThread);
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

    public List<TriggeredLOI> listTriggeredLOIs(String username) {
        return listTLOIs(username);
    }

    /*
     * Questions and option configuration
     */

    private Map<String, Question> allQuestions;
    private Map<String, QuestionVariable> allVariables;
    
    private void loadQuestionTemplates () {
        List<String> urls = new ArrayList<String>();
        
        Iterator<String> a = this.getConfig().getKeys("question-templates");
        while (a.hasNext()) {
            String key = a.next();
            urls.add(this.getConfig().getString(key));
        }

        this.allQuestions = new HashMap<String, Question>();
        this.allVariables = new HashMap<String, QuestionVariable>();
        for (String url: urls) { 
            //Clear cache first
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
            //Load questions and add them to cache
            for (Question q: loadQuestionsFromKB(url)) {
                this.allQuestions.put(q.getId(), q);
            }
        }
    }
    
    public List<Question> loadQuestionsFromKB (String url) {
        System.out.println("Loading Question Templates: " + url);
        List<Question> questions = new ArrayList<Question>();
        try {
            KBAPI kb = fac.getKB(url, OntSpec.PLAIN, true, true);

            this.start_read();
            KBObject questionClass = this.questionkb.getIndividual(KBConstants.QUESTIONSNS() + "Question");
            KBObject hasTemplate = this.questionkb.getProperty(KBConstants.QUESTIONSNS() + "hasQuestionTemplate");
            KBObject hasPattern  = this.questionkb.getProperty(KBConstants.QUESTIONSNS() + "hasQuestionPattern");
            KBObject hasVariable = this.questionkb.getProperty(KBConstants.QUESTIONSNS() + "hasQuestionVariable");

            KBObject hasName = this.questionkb.getProperty(KBConstants.QUESTIONSNS() + "hasVariableName");
            KBObject hasConstraints = this.questionkb.getProperty(KBConstants.QUESTIONSNS() + "hasConstraints");
            KBObject hasFixedOptions = this.questionkb.getProperty(KBConstants.QUESTIONSNS() + "hasFixedOptions");
            KBObject typeprop  = kb.getProperty(KBConstants.RDFNS() + "type");
            KBObject labelprop = kb.getProperty(KBConstants.RDFSNS() + "label");

            // Load question classes and properties
            for (KBTriple t : kb.genericTripleQuery(null, typeprop, questionClass)) {
                //System.out.println(t.toString());
                KBObject question = t.getSubject();
                KBObject name = kb.getPropertyValue(question, labelprop);
                KBObject template = kb.getPropertyValue(question, hasTemplate);
                KBObject pattern = kb.getPropertyValue(question, hasPattern);
                ArrayList<KBObject> variables = kb.getPropertyValues(question, hasVariable);

                if (name != null && template != null && pattern != null) {
                    List<QuestionVariable> vars = null;
                    
                    if (variables != null && variables.size() > 0) {
                        vars = new ArrayList<QuestionVariable>();
                        for (KBObject var : variables) {
                            KBObject vname = kb.getPropertyValue(var, hasName);
                            KBObject vconstraints = kb.getPropertyValue(var, hasConstraints);
                            KBObject vfixedOptions = kb.getPropertyValue(var, hasFixedOptions);
                            if (vname != null) {
                                QuestionVariable q = new QuestionVariable(var.getID(), vname.getValueAsString(), 
                                        vconstraints == null ? null : vconstraints.getValueAsString());
                                if (vfixedOptions != null) {
                                    q.setFixedOptions(vfixedOptions.getValueAsString().split(","));
                                }
                                vars.add(q);
                            }
                        }
                        // Store question variables
                        for (QuestionVariable v: vars) 
                            allVariables.put(v.getId(), v);
                    }

                    Question q = new Question(question.getID(), name.getValueAsString(), template.getValueAsString(), pattern.getValueAsString(), vars);
                    questions.add(q);
                }
            }
            this.end();
            return questions;
        } catch (Exception e) {
            e.printStackTrace();
            if (this.is_in_transaction()) this.end();
            // TODO: handle exception
        }
        return null;
    }
    
    public List<Question> listHypothesesQuestions () {
        return new ArrayList<Question>(this.allQuestions.values());
    }

    public List<List<String>> listVariableOptions (String sid) {
        if (!optionsCache.containsKey(sid)) {
            optionsCache.put(sid, this.loadVariableOptions(sid));
        }
        return optionsCache.get(sid);
    }

    private List<List<String>> loadVariableOptions (String sid) {
        List<List<String>> options = new ArrayList<List<String>>();
        System.out.println("Loading options for " + sid);
        QuestionVariable variable = null;
        // FIXME: Find a better way to handle url prefix or change the request to include the full URI
        if (allVariables.containsKey("http://disk-project.org/examples/" + sid)) {
            variable = allVariables.get("http://disk-project.org/examples/" + sid);
        } else if (allVariables.containsKey("http://disk-project.org/resources/question/" + sid)) {
            variable = allVariables.get("http://disk-project.org/resources/question/" + sid);
        }
        if (variable != null) {
            String varname = variable.getVarName();
            String constraintQuery = variable.getConstraints();
            String[] fixedoptions = variable.getFixedOptions();
            // If there are fixed options, return these
            if (fixedoptions != null) {
                for (String val: fixedoptions) {
                    List<String> opt = new ArrayList<String>();
                    opt.add(val);
                    opt.add(val);
                    options.add(opt);
                }
            } else if (constraintQuery != null) {
                //If there is a constraint query, send it to all data providers;
                Map<String, List<DataResult>> solutions = new HashMap<String, List<DataResult>>();
                for (DataAdapter adapter: this.dataAdapters.values()) {
                    //TODO: add some way to check if this adapter support this type of query. All SPARQL for the moment.
                    solutions.put(adapter.getName(), adapter.queryOptions(varname, constraintQuery));
                }

                // To check that all labels are only once
                Map<String, List<List<String>>> labelToOption = new HashMap<String, List<List<String>>>();
                
                for (String dataSourceName: solutions.keySet()) {
                    for (DataResult solution: solutions.get(dataSourceName)) {
                        String uri = solution.getValue(DataAdapter.VARURI);
                        String label = solution.getValue(DataAdapter.VARLABEL);

                        if (uri != null && label != null) {
                            List<List<String>> sameLabelOptions = labelToOption.containsKey(label) ? labelToOption.get(label) : new ArrayList<List<String>>();
                            List<String> thisOption = new ArrayList<String>();
                            thisOption.add(uri);
                            thisOption.add(label);
                            thisOption.add(dataSourceName);
                            sameLabelOptions.add(thisOption);
                            labelToOption.put(label, sameLabelOptions);
                        }
                    }
                }
                        
                //Add all options
                for (List<List<String>> sameLabelOptions: labelToOption.values()) {
                    if (sameLabelOptions.size() == 1) {
                        options.add(sameLabelOptions.get(0).subList(0, 2));
                    } else { //There's more than one option with the same label
                        boolean allTheSame = true;
                        String lastValue = sameLabelOptions.get(0).get(0); //Comparing IDs
                        for (List<String> candOption: sameLabelOptions) {
                            if (!lastValue.equals(candOption.get(0))) {
                                allTheSame = false;
                                break;
                            }
                        }
                        
                        if (allTheSame) {
                            options.add(sameLabelOptions.get(0).subList(0, 2));
                        } else {
                            for (List<String> candOption: sameLabelOptions) {
                                List<String> newOption = new ArrayList<String>();
                                newOption.add(candOption.get(0));
                                newOption.add(candOption.get(1) + " (" + candOption.get(2) + ")");
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
    
    private String getAllPrefixes () {
        return "PREFIX hyp: <" + KBConstants.HYPNS()   + ">\nPREFIX xsd: <" + KBConstants.XSDNS() + ">\n"
             + "PREFIX rdfs: <" + KBConstants.RDFSNS() + ">\nPREFIX rdf: <" + KBConstants.RDFNS() + ">\n"                
             + "PREFIX disk: <" + KBConstants.DISKNS() + ">\n";
    }
    
    public Map<String, List<String>> queryExternalStore(String endpoint, String sparqlQuery, String variables) {
        //FIXME: change this to DataResults
        // Variable name -> [row0, row1, ...]
        Map<String, List<String>> dataVarBindings = new HashMap<String, List<String>>();
        
        //Check that the variables string contains valid sparql
        String queryVars;
        if (variables == null || variables.contentEquals("") || variables.contains("*")) {
            queryVars = "*";
        } else {
            String[] vars = variables.replaceAll("\\s+", " ").split(" ");
            for (String v: vars) {
                if (v.charAt(0) != '?')
                    return dataVarBindings;
            }
            queryVars = variables;
        }
        
        DataAdapter dataAdapter = this.getDataAdapter(endpoint);
        if (dataAdapter == null)
            return dataVarBindings;
        
        //FIXME: These prefixes should be configured
        String dataQuery = "PREFIX bio: <http://disk-project.org/ontology/omics#>\n" + 
                "PREFIX neuro: <https://w3id.org/disk/ontology/enigma_hypothesis#>\n" + 
                "PREFIX hyp: <http://disk-project.org/ontology/hypothesis#>\n" + 
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" + 
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" + 
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "SELECT DISTINCT " + queryVars + " WHERE {\n" +
                sparqlQuery + "\n} LIMIT 200";
        //There's a limit here to prevent {?a ?b ?c} and so on
        
        List<DataResult> solutions = dataAdapter.query(dataQuery);
        int size = solutions.size();
        
        if (size > 0) {
            Set<String> varnames = solutions.get(0).getVariableNames();
            for (String varname: varnames)
                dataVarBindings.put(varname, new ArrayList<String>());
            for (DataResult solution: solutions) {
                for (String varname: varnames) {
                    dataVarBindings.get(varname).add(solution.getValue(varname));
                }
            }
        }

        System.out.println("External query to " + endpoint + " returned " + solutions.size() + " elements.");
        return dataVarBindings;
    }

    private String getQueryBindings(String queryPattern, Pattern variablePattern, Map<String, String> variableBindings) {
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
            pattern += line;
            //if (!line.matches(".+\\.\\s*$")) pattern += " .";
            pattern += "\n";
        }
        return pattern;
    }

    /*
     * Executing hypothesis
     */

    public Map<LineOfInquiry, List<Map<String, String>>> getLOIByHypothesisId (String username, String id) {
        String hypuri = this.HYPURI(username) + "/" + id;
        // LOIID -> [{ variable -> value }]
        Map<String, List<Map<String, String>>> allMatches = new HashMap<String, List<Map<String,String>>>();
        List<LineOfInquiry> lois = this.listLOIPreviews(username);

        //Starts checking all LOIs that match the hypothesis directly from the KB.
        try {
            this.start_read();
            KBAPI hypkb = this.fac.getKB(hypuri, OntSpec.PLAIN, true);
            for (LineOfInquiry loi : lois) {
                String hq = loi.getHypothesisQuery();
                if (hq != null) {
                    String query = this.getAllPrefixes() + "SELECT DISTINCT * WHERE { \n" + hq.replaceAll("\n", ".\n") + " }";
                    ArrayList<ArrayList<SparqlQuerySolution>> allSolutions = hypkb.sparqlQuery(query);
                    for (List<SparqlQuerySolution> row: allSolutions) {
                        // One match per cell, store variables on cur.
                        Map<String, String> cur = new HashMap<String, String>();
                        for (SparqlQuerySolution cell: row) {
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
                                allMatches.put(loiid, new ArrayList<Map<String,String>>());
                            List<Map<String, String>> curList = allMatches.get(loiid);
                            curList.add(cur);
                            /*for (String k : cur.keySet()) {
                                System.out.println("> " + k + ": " + cur.get(k) );
                            }*/
                        }
                    }
                } else {
                    System.out.println("no hyp query");
                }
            }
            this.end();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Map<LineOfInquiry, List<Map<String, String>>> results = new HashMap<LineOfInquiry, List<Map<String,String>>>();
        // allMatches contains LOIs that matches hypothesis, now check other conditions
        for (String loiid: allMatches.keySet()) {
            LineOfInquiry loi = this.getLOI(username, loiid);
            DataAdapter dataAdapter = getDataAdapter(loi.getDataSource());
            MethodAdapter methodAdapter = WingsAdapter.get();
            //System.out.println(loi.getId());
            if (dataAdapter != null) {
                if (!results.containsKey(loi))
                    results.put(loi, new ArrayList<Map<String,String>>());
                List<Map<String, String>> curList = results.get(loi);
                for (Map<String, String> values: allMatches.get(loiid)) {
                    if (dataAdapter.validateLOI(loi, values) && methodAdapter.validateLOI(loi, values)) {
                        curList.add(values);
                    }
                }
            }
        }
        
        // LOI -> [{ variable -> value }, {...}, ...]
        return results;
    }
    
    public List<TriggeredLOI> queryHypothesis(String username, String id) {
        List<TriggeredLOI> tlois = new ArrayList<TriggeredLOI>();
        Set<String> queries = new HashSet<String>();

        System.out.println("Quering hypothesis: " + id);
        Map<LineOfInquiry, List<Map<String, String>>> matchingBindings = this.getLOIByHypothesisId(username, id);
        for (LineOfInquiry loi: matchingBindings.keySet()) {
            //One hypothesis can match the same LOI in more than one way, the following for-loop handles that
            for (Map<String, String> values: matchingBindings.get(loi)) {
                String dq = getQueryBindings(loi.getDataQuery(), varPattern, values);
                String query = this.getAllPrefixes() + "SELECT DISTINCT ";
                for (String qvar: loi.getAllWorkflowVariables()) query += qvar + " ";
                query += "{\n" + dq + "}";
                
                //Prevents executing the same query several times.
                if (queries.contains(query))
                    continue;
                queries.add(query);

                List<DataResult> solutions = this.dataAdapters.get(loi.getDataSource()).query(query);
                if (solutions.size() > 0) {
                    System.out.println("  LOI " + loi.getId() + " got " + solutions.size() + " results");
                    
                    // Store solutions in dataVarBindings
                    Map<String, List<String>> dataVarBindings = new HashMap<String, List<String>>();
                    Set<String> varNames =  solutions.get(0).getVariableNames();
                    for (String varname: varNames)
                        dataVarBindings.put(varname, new ArrayList<String>());

                    for (DataResult solution: solutions) {
                        for (String varname: varNames) {
                            String cur = solution.getValue(varname);
                            dataVarBindings.get(varname).add(cur);
                        }
                    }

                    // Add the parameters
                    for (String param: loi.getAllWorkflowParameters()) {
                        if (param.charAt(0) == '?') param = param.substring(1);
                        String bind = values.get(param);
                        if (bind != null) {
                            List<String> abind = new ArrayList<String>();
                            abind.add(bind);
                            dataVarBindings.put(param, abind);
                        }
                    }
                
                    // check collections
                    Set<String> varNonCollection = loi.getAllWorkflowNonCollectionVariables();

                    for (String key: dataVarBindings.keySet()) {
                      String var = (key.charAt(0) != '?') ? '?' + key : key;
                      if (varNonCollection.contains(var)) {
                        //System.out.println("  Is not a collection");
                        Set<String> fixed = new HashSet<String>(dataVarBindings.get(key));
                        dataVarBindings.put(key, new ArrayList<String>(fixed));
                      }
                    }
                    
                    String endpoint = loi.getDataSource();
                    TriggeredLOI tloi = new TriggeredLOI(loi, id);
                    tloi.setWorkflows(
                        this.getTLOIBindings(username, loi.getWorkflows(), dataVarBindings, endpoint)
                        );
                    tloi.setMetaWorkflows(
                        this.getTLOIBindings(username, loi.getMetaWorkflows(), dataVarBindings, endpoint));
                    tloi.setDataQuery(dq);
                    tloi.setRelevantVariables(loi.getRelevantVariables());
                    tloi.setExplanation(loi.getExplanation());
                    tlois.add(tloi);
                    
                }
            }
        }
        return checkExistingTLOIs(username, tlois);
  }
    

    /*private String addQueryAssertions (String queryPattern, String assertionUri) {
      Pattern ASSERTION_PATTERN = Pattern.compile("(user:[^\\s]+)");
        String extra = "";
      try {
          this.start_read();
          Graph graph = this.getKBGraph(assertionUri);
          Matcher m = ASSERTION_PATTERN.matcher(queryPattern);
        String aURL = assertionUri + "#";
          while (m.find()) {
              String s = m.group(1);
              String id = s.replace("user:", aURL);
              for (Triple t: graph.getTriplesForSubject(id)) {
                String o = t.getObject().toString();
                String p = t.getPredicate().toString();
                if (o != null && p != null && !p.contains("rdf-syntax-ns#type")) { //TODO: do not add types.
                  extra += s + " <" + p + "> " + o + " .\n";
                  System.out.println("+ " +s + " " + p + " " + o);
                }
              }
          }
      }
      finally {
        this.end();
      }
      return queryPattern + extra;
    }*/

    // This replaces all triggered lines of inquiry already executed. tlois should be from the same hypothesis.
    private List<TriggeredLOI> checkExistingTLOIs(String username, List<TriggeredLOI> tlois) {
        List<TriggeredLOI> checked = new ArrayList<TriggeredLOI>();
        Map<String, List<TriggeredLOI>> cache = new HashMap<String, List<TriggeredLOI>>();
        for (TriggeredLOI tloi: tlois) {
            //System.out.println("Checking " + tloi.getId() + " (" + tloi.getLoiId() + ")");
            if (!cache.containsKey(tloi.getLoiId())) {
                cache.put(tloi.getLoiId(), getTLOIsForHypothesisAndLOI(username, tloi.getParentHypothesisId(), tloi.getLoiId()));
            }
            List<TriggeredLOI> candidates = cache.get(tloi.getLoiId());
            TriggeredLOI real = tloi;
            for (TriggeredLOI cand: candidates) {
                if (cand.toString().equals(tloi.toString()) ) {
                    //TODO: compare the hash of the input files
                    System.out.println("Replaced " + tloi.getId() + " with " + cand.getId());
                    real = cand;
                    break;
                }
            }
            checked.add(real);
        }
        return checked;
    }

    @SuppressWarnings("unchecked")
    private List<WorkflowBindings> getTLOIBindings(String username, 
            List<WorkflowBindings> wflowBindings, Map<String, List<String>> dataVarBindings, String endpoint) {
      
        List<WorkflowBindings> tloiBindings = new ArrayList<WorkflowBindings>();

        for (WorkflowBindings bindings : wflowBindings) { //FOR EACH WORKFLOW
            // For each Workflow, create an empty copy to set the values
            WorkflowBindings tloiBinding = new WorkflowBindings(
                    bindings.getWorkflow(),
                    bindings.getWorkflowLink());
            tloiBinding.setMeta(bindings.getMeta());
            tloiBindings.add(tloiBinding);
            
            // Add parameters values
            for (VariableBinding param: bindings.getParameters()) {
                String binding = param.getBinding();
                if (binding.charAt(0)=='?') binding = binding.substring(1);
                if (dataVarBindings.containsKey(binding)) {
                    List<String> possibleBindings = dataVarBindings.get(binding);
                    if (possibleBindings.size() == 1) {
                        //param.setBinding(possibleBindings.get(0));
                        tloiBinding.addParameter(new VariableBinding(param.getVariable(), possibleBindings.get(0)));
                    } else {
                        System.out.println("more than one value for " + binding);
                    }
                } else {
                    System.out.println("dict does not contain " + binding);
                }
            }

            // Add optional parameters
            for (VariableBinding param: bindings.getOptionalParameters()) {
                String binding = param.getBinding();
                if (binding.charAt(0)=='?') binding = binding.substring(1);
                //System.out.println("param binding: " + binding);
                if (dataVarBindings.containsKey(binding)) {
                    List<String> possibleBindings = dataVarBindings.get(binding);
                    if (possibleBindings.size() == 1) {
                        //param.setBinding(possibleBindings.get(0));
                        tloiBinding.addOptionalParameter(new VariableBinding(param.getVariable(), possibleBindings.get(0)));
                    } else {
                        System.out.println("more than one value for optional " + binding);
                    }
                } else {
                    //System.out.println("dict does not contain optional " + binding);
                }
            }
            // CHECK ^
      
            for (VariableBinding vbinding : bindings.getBindings()) { //Normal variable bindings.
                // For each Variable binding, check :
                // - If this variable expects a collection or single values
                // - Check the binding values on the data store
                String binding = vbinding.getBinding();
                Matcher collmat = varCollPattern.matcher(binding);
                Matcher mat = varPattern.matcher(binding);
        
                // Get the sparql variable
                boolean isCollection = false;
                String sparqlvar = null;
                if(collmat.find() && dataVarBindings.containsKey(collmat.group(1))) {
                    sparqlvar = collmat.group(1);
                    isCollection = true;
                } else if(mat.find() && dataVarBindings.containsKey(mat.group(1))) {
                    sparqlvar = mat.group(1);
                }
        
                if (sparqlvar != null) {
                    // Get the data bindings for the sparql variable
                    List<String> dsurls = dataVarBindings.get(sparqlvar);
                  
                    // Checks if the bindings are input files.
                    boolean bindingsAreFiles = true;
                    for (String candurl: dsurls) {
                        if (!candurl.startsWith("http")) {
                            bindingsAreFiles = false;
                            break;
                        }
                    }

                    // Datasets Wings names
                    List<String> dsnames = new ArrayList<String>();

                    // Check hashes, create local name and upload data:
                    if (bindingsAreFiles) {
                        Map<String, String> urlToName = addDataToWings(username, dsurls, endpoint);
                        for(String dsurl : dsurls) {
                            String dsname = urlToName.containsKey(dsurl) ? urlToName.get(dsurl) : dsurl.replaceAll("^.*\\/", "");
                            dsnames.add(dsname);
                        }
                    } else {
                        for(String dsurl : dsurls) {
                            String dsname = dsurl.replaceAll("^.*\\/", "");
                            dsnames.add(dsname);
                        }
                    }
                  
                    // If Collection, all datasets go to same workflow
                    if (isCollection) {
                        // This variable expects a collection. Modify the existing tloiBinding values,
                        // collections of non-files are send as comma separated values:
                        tloiBinding.addBinding(new VariableBinding( vbinding.getVariable(), dsnames.toString()));
                        
                    } else {
                        // This variable expects a single file. Add new tloi bindings for each dataset
                        List<WorkflowBindings> newTloiBindings = new ArrayList<WorkflowBindings>();
                        for (WorkflowBindings tmpBinding : tloiBindings) { //For all already processed workflow bindings 
                            for (String dsname: dsnames) {
                                ArrayList<VariableBinding> newBindings = (ArrayList<VariableBinding>) SerializationUtils
                                        .clone((Serializable) tmpBinding.getBindings());
                                ArrayList<VariableBinding> newParameters = (ArrayList<VariableBinding>) SerializationUtils
                                        .clone((Serializable) tmpBinding.getParameters());
                                ArrayList<VariableBinding> newOptionalParameters = (ArrayList<VariableBinding>) SerializationUtils
                                        .clone((Serializable) tmpBinding.getOptionalParameters());

                                WorkflowBindings newWorkflowBindings = new WorkflowBindings(
                                        bindings.getWorkflow(), 
                                        bindings.getWorkflowLink(), 
                                        newBindings,
                                        newParameters,
                                        newOptionalParameters
                                );
                                newWorkflowBindings.addBinding(new VariableBinding(vbinding.getVariable(), dsname));
                                newWorkflowBindings.setMeta(bindings.getMeta());
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
    
    private Map<String, String> addDataToWings(String username, List<String> dsurls, String endpoint) {
        //To add files to wings and do not replace anything, we need to get the hash from the wiki.
        Map<String, String> nameToUrl = new HashMap<String, String>();
        Map<String, String> urlToName = new HashMap<String, String>();
        
        Map<String, String> hashes = getDataAdapter(endpoint).getFileHashes(dsurls);
        for (String file: hashes.keySet()) {
            String hash = hashes.get(file);
            String newName = "SHA" + hash.substring(0,6) + "_" + file.replaceAll("^.*\\/", "");
            nameToUrl.put(newName, file);
            urlToName.put(file, newName);
        }
        
        //Show files with no hash
        for (String file: dsurls) {
            if (!urlToName.containsKey(file))
                System.out.println("Warning: file " + file + " does not contain hash on " + endpoint);
        }
        
        Set<String> names = nameToUrl.keySet();
        List<String> onWings = WingsAdapter.get().isFileListOnWings(names);
        
        names.removeAll(onWings);

        for (String newFilename: names) {
            String newFile = nameToUrl.get(newFilename);
            System.out.println("Uploading to WINGS: " + newFile + " as " + newFilename);
            WingsAdapter.get().addRemoteDataToWings(newFile, newFilename);
        }

        return urlToName;
    }

    public Boolean runAllHypotheses (String username) {
        List<String> hlist = new ArrayList<String>();
        String url = this.HYPURI(username);
        try {
            KBAPI kb = this.fac.getKB(url, OntSpec.PLAIN, true);
            KBObject hypcls = this.cmap.get("Hypothesis");
            
            this.start_read();
            KBObject typeprop = kb.getProperty(KBConstants.RDFNS() + "type");
            for (KBTriple t : kb.genericTripleQuery(null, typeprop, hypcls)) {
                KBObject hypobj = t.getSubject();
                String uri = hypobj.getID();
                String[] sp = uri.split("/");
                hlist.add(sp[sp.length-1]);
                System.out.println("Hyp ID: " + sp[sp.length-1]);
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
        
        for (String hid: hlist) {
            tlist.addAll(queryHypothesis(username, hid));
        }

        //Only hypotheses with status == null are new
        for (TriggeredLOI tloi: tlist) {
            if (tloi.getStatus() == null) {
                System.out.println("TLOI " + tloi.getId() + " will be trigger");
                addTriggeredLOI(username, tloi);
            }
        }

        return true;
    }

    /**
     * Assertions
     */

    private KBObject getKBValue(Value v, KBAPI kb) {
        if (v.getType() == Value.Type.LITERAL) {
            if (v.getDatatype() != null)
                return kb.createXSDLiteral(v.getValue().toString(), v.getDatatype());
            else
                return kb.createLiteral(v.getValue());
        } else {
            return kb.getResource(v.getValue().toString());
        }
    }

    private KBTriple getKBTriple(Triple triple, KBAPI kb) {
        KBObject subj = kb.getResource(triple.getSubject());
        KBObject pred = kb.getResource(triple.getPredicate());
        KBObject obj = getKBValue(triple.getObject(), kb);

        if (subj != null && pred != null && obj != null)
            return this.fac.getTriple(subj, pred, obj);
        return null;
    }

    private Value getObjectValue(KBObject obj) {
        Value v = new Value();
        if (obj.isLiteral()) {
            Object valobj = obj.getValue();
            if (valobj instanceof Date) {
                valobj = dateformatter.format((Date) valobj);
            }
            if (valobj instanceof String) {
                //Fix quotes and \n
                valobj = ((String) valobj).replace("\"", "\\\"").replace("\n", "\\n");
            }
            v.setType(Value.Type.LITERAL);
            v.setValue(valobj);
            v.setDatatype(obj.getDataType());
        } else {
            v.setType(Value.Type.URI);
            v.setValue(obj.getID());
        }
        return v;
    }

    private Graph getKBGraph(String url) {
        try {
            Graph graph = new Graph();
            KBAPI kb = this.fac.getKB(url, OntSpec.PLAIN, false);
            if (kb == null)
                return null;
            for (KBTriple t : kb.genericTripleQuery(null, null, null)) {
                Value value = this.getObjectValue(t.getObject());
                Triple triple = new Triple();
                triple.setSubject(t.getSubject().getID());
                triple.setPredicate(t.getPredicate().getID());
                triple.setObject(value);
                graph.addTriple(triple);
            }
            return graph;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void addQueries(String username, List<String> ToBeQueried) {
        try {

            String[] temp;
            for (int i = 0; i < ToBeQueried.size(); i++) {

                temp = DataQuery.queryFor(ToBeQueried.get(i).substring(ToBeQueried.get(i).indexOf(" ") + 1))[1]
                        .split("\n\",\"\n");
                for (int j = 0; j < temp.length - 1; j += 2) {
                    //FIXME
                    WingsAdapter.get().addOrUpdateData(temp[j].substring(4),
                            "/export/users/" + username + "/data/ontology.owl#File", temp[j + 1], true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String[] addQuery(String username, String query, String type, boolean upload) {
        try {

            String[] temp = DataQuery.queryFor(query.substring(query.indexOf(" ") + 1))[1].split("\n\",\"\n");
            if (upload)
                for (int j = 0; j < temp.length - 1; j += 2) {
                    //FIXME
                    WingsAdapter.get().addOrUpdateData(temp[j].substring(4), type, temp[j + 1], false);
                }
            return temp;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void addAssertion(String username, Graph assertion) {
        String url = this.ASSERTIONSURI(username);
        try {
            KBAPI kb = this.fac.getKB(url, OntSpec.PLAIN, true);
            this.start_write();
            for (Triple triple : assertion.getTriples()) {
                KBTriple t = this.getKBTriple(triple, kb);
                if (t != null)
                    kb.addTriple(t);
            }
            this.save(kb); 
            this.end();

            // Re-run hypotheses if needed TODO
            //this.requeryHypotheses(username, domain);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Graph listAssertions(String username, String domain) {
      try {
          String url = this.ASSERTIONSURI(username);
          this.start_read();
          return this.getKBGraph(url);
      }
      finally {
        this.end();
      }
    }

    /*public List<String> getQueriesToBeRun(Graph assertions) {
        List<Triple> UITriples = assertions.getTriples();
        HashSet<String> toQuery = new HashSet<String>();
        String temp;
        //String[] temp2;
        String file;
        String query;
        //Triple tri;
        for (int i = 0; i < UITriples.size(); i++) {
            temp = UITriples.get(i).getPredicate(); // check if asking for query
            if (temp.equals(KBConstants.NEURONS() + "hasEnigmaQueryLiteral")) {
                temp = UITriples.get(i).getSubject().toString();
                file = temp.substring(temp.indexOf("#") + 1);
                temp = UITriples.get(i).getObject().getValue().toString();
                query = temp.replace("|", "/");
                toQuery.add(file + " " + query);

            }
        }
        List<String> ToBeQueried = new ArrayList<String>();
        for (String strTemp : toQuery)
            ToBeQueried.add(strTemp);
        return ToBeQueried;
    }*/

    public void updateAssertions(String username, Graph assertions) {
        String url = this.ASSERTIONSURI(username);
        try {
            KBAPI kb = this.fac.getKB(url, OntSpec.PLAIN, true);
            this.start_write();
            if(kb.delete() && this.save(kb) && this.end()) {
              this.addAssertion(username, assertions);
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.end();
        }
    }

    private void requeryHypotheses(String username) {
        // Cache already run/queries hypotheses (check tlois)
        HashMap<String, Boolean> tloikeys = new HashMap<String, Boolean>();
        List<String> bindkeys = new ArrayList<String>();
        HashMap<String, TriggeredLOI> tloimap = new HashMap<String, TriggeredLOI>();
        for (TriggeredLOI tloi : this.listTriggeredLOIs(username)) {
            if (!tloi.getStatus().equals(TriggeredLOI.Status.FAILED)) {
                TriggeredLOI fulltloi = this.getTriggeredLOI(username, tloi.getId());
                String key = fulltloi.toString();
                tloikeys.put(key, true);

                if (tloi.getStatus().equals(TriggeredLOI.Status.SUCCESSFUL)
                        && fulltloi.getResultingHypothesisIds().size() > 0) {
                    String partkey = "";
                    for (WorkflowBindings wb : fulltloi.getWorkflows())
                        partkey += wb.toString();
                    bindkeys.add(partkey);
                    tloimap.put(partkey, fulltloi);
                }
            }
        }
        Collections.sort(bindkeys);
        // Get all Hypotheses
        for (TreeItem hypitem : this.listHypotheses(username)) {
            // Run/Query only top-level hypotheses
            if (hypitem.getParentId() == null) {
                List<TriggeredLOI> tlois = this.queryHypothesis(username, hypitem.getId());
                Collections.sort(tlois);
                for (TriggeredLOI tloi : tlois) {
                    List<WorkflowBindings> wfbindings = tloi.getWorkflows(); //this.addEnimgaFiles(username, domain, tloi, false, false);
                    tloi.setWorkflows(wfbindings);
                    String key = tloi.toString();
                    if (tloikeys.containsKey(key))
                        continue;
                    for (String partkey : bindkeys) {
                        if (key.contains(partkey)) {
                            TriggeredLOI curtloi = tloimap.get(partkey);
                            if (curtloi.getLoiId().equals(tloi.getLoiId()))
                                tloi.setParentHypothesisId(tloimap.get(partkey).getResultingHypothesisIds().get(0));
                        }
                    }
                    this.addTriggeredLOI(username, tloi);
                }
            }
        }
    }

    public void deleteAssertion(String username, Graph assertion) {
        String url = this.ASSERTIONSURI(username);
        try {
            KBAPI kb = this.fac.getKB(url, OntSpec.PLAIN, true);
            for (Triple triple : assertion.getTriples()) {
                KBTriple t = this.getKBTriple(triple, kb);
                if (t != null)
                    kb.removeTriple(t);
            }
            kb.save();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Narratives 
     */
    
    public Map<String, String> getNarratives (String username, String tloid) {
        Map<String,String> narratives = new HashMap<String, String>();
        //if (true) return narratives;
        TriggeredLOI tloi = this.getTriggeredLOI(username, tloid);
        if (tloi != null) {
            String hypId = tloi.getParentHypothesisId();
            String loiId = tloi.getLoiId();
            Hypothesis hyp = this.getHypothesis(username, hypId);
            LineOfInquiry loi = this.getLOI(username, loiId);
            
            if (loi == null || hyp == null) {
                System.out.print("ERROR: could not get hypotheses or LOI");
                return narratives;
            }
            
            // Assuming each tloi only has a workflow or metaworkdflow:
            WorkflowBindings wf = null;
            List<WorkflowBindings> wfs = tloi.getWorkflows();
            List<WorkflowBindings> mwfs = tloi.getMetaWorkflows();

            if (mwfs != null && mwfs.size() > 0) {
                wf = mwfs.get(0);
            } else if (wfs != null && wfs.size() > 0){
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
            for (VariableBinding ds: wf.getBindings()) {
                if (!ds.isCollection()) {
                    allCollections = false;
                    break;
                }
                len = ds.getBindingAsArray().length > len ? ds.getBindingAsArray().length : len;
            }
            if (allCollections) {
                //dataset += "<table>";
                dataset += "<table><thead><tr><td><b>#</b></td>";
                for (VariableBinding ds: wf.getBindings()) {
                    dataset += "<td><b>" + ds.getVariable() + "</b></td>";
                }
                dataset += "</tr></thead><tbody>";
                for (int i = 0; i < len; i++) {
                    dataset += "<tr>";
                    dataset += "<td>" + (i+1) + "</td>";
                    for (VariableBinding ds: wf.getBindings()) {
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
                for (VariableBinding ds: wf.getBindings()) {
                    String binding = ds.getBinding();
                    if (binding.startsWith("[")) {
                        for (String datas: ds.getBindingAsArray()) {
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
            DecimalFormat df = new DecimalFormat(confidence != 0 && confidence < 0.001 ?
                    "0.#E0"
                    : "0.###");
            
            String pval = df.format(confidence);           
            //Execution narratives
            String execution = "The Hypothesis with title: <b>" + hyp.getName()
                             + "</b> was runned <span class=\"" + tloi.getStatus() + "\">" 
                             + tloi.getStatus() + "</span>"
                             + " with the Line of Inquiry: <b>" + loi.getName()
                             + "</b>. The LOI triggered the <a target=\"_blank\" href=\"" + wf.getWorkflowLink() 
                             + "\">workflow on WINGS</a>"
                             + " where it was tested with the following datasets:<div class=\"data-list\"><ol>" + dataset
                             + "</ol></div>The resulting p-value is " + pval + ".";
            narratives.put("execution", execution);

            //System.out.println("EXECUTION NARRATIVE: " + execution);
            
            String dataQuery = "<b>Data Query Narrative:</b><br/>" + this.dataQueryNarrative(loi.getDataQuery());

            //System.out.println("DATA Query NARRATIVE: " + dataQuery);
            
            narratives.put("dataquery", dataQuery);
        }
        return narratives;
    }

    private String dataQueryNarrative(String dataQuery) {
      String dataQuery1 = dataQuery.replaceAll("^(//)n${1}",""); //this is necessary to replace the new line characters in query
      String[] querylist = dataQuery1.split("\\.");
      String rdfs_label = "rdfs:label";
      
      try {
      Map<String, String> properties = new HashMap<String, String>();
      for(int i = 0; i < querylist.length; i++) {
          if(querylist[i].contains(rdfs_label)){
              String[] line = querylist[i].replace("\\","").split(" ");
              properties.put(line[2],line[4].replace('"',' '));
          }
      }

      //We map all the objects to the properties they were identified with, by using the objects dictionary
      Map<String, List<List<String>>> inputs = new HashMap<>();
      Map<List<String>, String> outputs = new HashMap<>();
      for(int i = 0; i < querylist.length; i++) {
          if(!querylist[i].contains(rdfs_label)){
              String[] line = querylist[i].split(" ");
              String schema = "Schema";
              if(!inputs.containsKey(line[2])&!line[2].contains(schema)){
                  List<List<String>> list = new ArrayList<List<String>>();
                  List<String> item = new ArrayList<String>();
                  item.add(line[2]);
                  item.add(properties.get(line[3]));
                  list.add(item);
                  inputs.put(line[2],list);
                  outputs.put(item,line[4]);
              } else if(inputs.containsKey(line[2])&!line[2].contains(schema)){
                  List<List<String>> list2 = inputs.get(line[2]);
                  List<String> item = new ArrayList<String>();
                  item.add(line[2]);
                  item.add(properties.get(line[3]));
                  list2.add(item);
                  inputs.put(line[2],list2);
                  List<String> list = new ArrayList<String>();
                  list.add(line[2]);
                  list.add(properties.get(line[3]));
                  outputs.put(item,line[4]);
              }
          }
      }
      //Now we traverse the path
      String path = "";
      for (String key : inputs.keySet()) {
          List<List<String>> value = inputs.get(key);
          for(int j=0;j<value.size();j++){
              //p = v
              List<String> p = value.get(j);
              try {
                  path = path+key.replace("?","")+"->"+ p.get(1).toString().trim().replace("?","") +"->"+outputs.get(p).toString().replace("?","")+"<br/>";
              } catch (NullPointerException e){

              }
              }
          }
//      System.out.println("Narrative"+path);
      return path;
      } catch (Exception e) {
        return "Error generating narrative";
    }

  }

    /*
     * Running
     */

    public List<TriggeredLOI> getTLOIsForHypothesisAndLOI (String username, String hypid, String loiid) {
        // Get all TLOIs and filter out 
        List<TriggeredLOI> list = new ArrayList<TriggeredLOI>();
        String hypURI = this.HYPURI(username) + "/" + hypid;
        String loiURI = this.LOIURI(username) + "/" + loiid;

        for (TriggeredLOI tloi: listTLOIs(username)) {
            String parentHypId = tloi.getParentHypothesisId();
            String parentLOIId = tloi.getLoiId();
            if (parentHypId != null && parentHypId.equals(hypURI) && 
                parentLOIId != null && parentLOIId.equals(loiURI)) {
                    list.add(tloi);
            }
        }
        return list;
    }
    
    public List<TriggeredLOI> runHypothesisAndLOI (String username, String hypid, String loiid) {
        List<TriggeredLOI> hyptlois = queryHypothesis(username, hypid);
        //TriggeredLOI match = null;
        for (TriggeredLOI tloi: hyptlois) {
            if (tloi.getStatus() == null && tloi.getLoiId().equals(loiid)) {
                //Set basic metadata
                tloi.setAuthor("System");
                Date date = new Date(); 
                SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss yyyy-MM-dd");
                tloi.setDateCreated(formatter.format(date));
                addTriggeredLOI(username, tloi);
                //match = tloi;
                break;
            }
        }
        
        return getTLOIsForHypothesisAndLOI(username, hypid, loiid);
        //if (match != null) old.add
    }

    /*
     * Threads helpers
     */
    
    private String getWorkflowExecutionRunIds(TriggeredLOI tloi, String workflow) {
        String runids = null;
        for (WorkflowBindings bindings : tloi.getWorkflows()) {
            if (bindings.getWorkflow().equals(workflow)) {
                runids = (runids == null) ? "" : runids + ", ";
                runids += bindings.getRun().getId();
            }
        }
        return runids;
    }
    
    public String getDataFromWings (String username, String domain, String id) {
        /* Wings IDs are URIs */
        WingsAdapter wings = WingsAdapter.get();
        String dataid = wings.DOMURI() + "/data/library.owl#" + id;
        return wings.fetchDataFromWings(dataid);
    }

    /* Retrieves the revised_hypothesis from wings and stores it as a disk-hypothesis (quad) */
    private String fetchOutputHypothesis(String username, WorkflowBindings bindings, TriggeredLOI tloi) {
        String varname = bindings.getMeta().getRevisedHypothesis();
        Map<String, String> varmap = WingsAdapter.get().getRunVariableBindings(bindings.getRun().getId());
        if (varmap.containsKey(varname)) {
            String dataid = varmap.get(varname);
            String dataname = dataid.replaceAll(".*#", "");
            String content = WingsAdapter.get().fetchDataFromWings(dataid);

            HashMap<String, Integer> workflows = new HashMap<String, Integer>();
            for (WorkflowBindings wb : tloi.getWorkflows()) {
                String wid = wb.getWorkflow();
                if (workflows.containsKey(wid))
                    workflows.put(wid, workflows.get(wid) + 1);
                else
                    workflows.put(wid, 1);
            }
            String wflows = "";
            for (String wid : workflows.keySet()) {
                if (!wflows.equals(""))
                    wflows += ", ";
                int num = workflows.get(wid);
                wflows += (num > 1 ? num : "a") + " " + wid + " workflow" + (num > 1 ? "s" : "");
            }
            String meta = bindings.getWorkflow();

            Hypothesis parentHypothesis = this.getHypothesis(username, tloi.getParentHypothesisId());
            Hypothesis newHypothesis = new Hypothesis();
            newHypothesis.setId(dataname);
            newHypothesis.setName("[Revision] " + parentHypothesis.getName());
            String description = "Followed the line of inquiry: \"" + tloi.getName() + "\" to run " + wflows
                    + ", then run " + meta + " meta-workflow, to create a revised hypothesis.";
            newHypothesis.setDescription(description);
            newHypothesis.setParentId(parentHypothesis.getId());

            List<Triple> triples = new ArrayList<Triple>();
            TripleUtil util = new TripleUtil();
            for (String line : content.split("\\n")) {
                String[] parts = line.split("\\s+", 4);
                TripleDetails details = new TripleDetails();
                if(parts.length > 3)
                  details.setConfidenceValue(Double.parseDouble(parts[3]));
                details.setTriggeredLOI(tloi.getId());
                Triple t = util.fromString(parts[0] + " " + parts[1] + " " + parts[2]);
                t.setDetails(details);
                triples.add(t);
            }
            Graph newgraph = new Graph();
            newgraph.setTriples(triples);
            newHypothesis.setGraph(newgraph);

            this.addHypothesis(username, newHypothesis);
            return newHypothesis.getId();
        }
        return null;
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
                    System.out.println("Running execution thread on META mode");
                else 
                    System.out.println("Running execution thread");

                WingsAdapter wings = WingsAdapter.get(); //TODO
                List<WorkflowBindings> wflowBindings = this.metamode ? tloi.getMetaWorkflows() : tloi.getWorkflows();
                
                boolean allok = true;
        
                // Start workflows from tloi
                for (WorkflowBindings bindings : wflowBindings) {
                    // Get workflow input details
                    Map<String, Variable> inputs = wings.getWorkflowInputs(bindings.getWorkflow());
                    List<VariableBinding> vbindings = bindings.getBindings();
                    List<VariableBinding> params = bindings.getParameters();
                    List<VariableBinding> optionalparams = bindings.getOptionalParameters();
                    List<VariableBinding> sendbindings = new ArrayList<VariableBinding>(vbindings);
                    
                    //Remove " on parameters
                    for (VariableBinding p: params) {
                        String bind = p.getBinding();
                        if (bind.charAt(0) == '"') {
                            bind = bind.substring(1, bind.length() - 1);
                        }
                        sendbindings.add(new VariableBinding(p.getVariable(), bind));
                    }
                    //Same for optional parameters
                    for (VariableBinding p: optionalparams) {
                        String bind = p.getBinding();
                        if (!(bind == null || bind.equals("") || bind.equals("\"\"") || bind.charAt(0) == '?')) {
                            if (bind.charAt(0) == '"') {
                                bind = bind.substring(1, bind.length() - 1);
                            }
                            sendbindings.add(new VariableBinding(p.getVariable(), bind));
                        }
                    }
                    
                    // Special processing for Meta Workflows
                    if (this.metamode) {
                        // Replace workflow ids with workflow run ids in
                        // Variable Bindings
                        for (VariableBinding vbinding : vbindings) {
                            String runids = getWorkflowExecutionRunIds(tloi, vbinding.getBinding());
                            if( runids != null && runids.length() > 0 )
                                vbinding.setBinding(runids);
                        }
                        // Upload hypothesis to Wings as a file, and add to
                        // Variable Bindings
                        String hypVarId = bindings.getMeta().getHypothesis();
                        System.out.println("Hypothesis Variable ID: " + hypVarId);
                        if (hypVarId != null && inputs.containsKey(hypVarId)) {
                            Variable hypVar = inputs.get(hypVarId);
                            String hypId = tloi.getParentHypothesisId();
                            Hypothesis hypothesis = getHypothesis(username, tloi.getParentHypothesisId());
                            String contents = "";
                            for (Triple t : hypothesis.getGraph().getTriples())
                                contents += t.toString() + "\n";

                            if (hypVar.getType() == null) {
                                System.err.println("Couldn't retrieve hypothesis type information");
                                continue;
                            }
                            String dataid = wings.addDataToWings(hypId, hypVar.getType(), contents);
                            if (dataid == null) {
                                System.err.println("Couldn't add hypothesis to wings");
                                continue;
                            }
                            VariableBinding hypBinding = new VariableBinding();
                            hypBinding.setVariable(hypVarId);
                            hypBinding.setBinding(dataid.replaceAll("^.*#", ""));
                            sendbindings.add(hypBinding);
                        } else {
                            System.err.println("Workflow doesn't have hypothesis information");
                            continue;
                        }
                    }

                    // Execute workflow
                    System.out.println("Executing " + bindings.getWorkflow() + " with:\n" + vbindings);
                    String runid = wings.runWorkflow(bindings.getWorkflow(), sendbindings, inputs);

                    if (runid != null) {
                        bindings.getRun().setId(runid);// .replaceAll("^.*#", ""));
                    } else {
                        allok = false;
                        System.out.println("Error executing workflow");
                    }
                }
                
                tloi.setStatus(allok ? Status.RUNNING : Status.FAILED);
                updateTriggeredLOI(username, tloi.getId(), tloi);

                // Start monitoring
                if (allok) {
                    TLOIMonitoringThread monitorThread = new TLOIMonitoringThread(username, tloi, metamode);
                    monitor.schedule(monitorThread, 10, TimeUnit.SECONDS);
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
                System.out.println("Running monitoring thread");
                // Check workflow runs from tloi
                List<WorkflowBindings> wflowBindings = tloi.getWorkflows();
                if (this.metamode)
                    wflowBindings = tloi.getMetaWorkflows();

                Status overallStatus = tloi.getStatus();
                int numSuccessful = 0;
                int numFinished = 0;
                for (WorkflowBindings bindings : wflowBindings) {
                    String runid = bindings.getRun().getId();
                    if (runid == null) {
                        overallStatus = Status.FAILED;
                        numFinished++;
                        continue;
                    }
                    String rname = runid.replaceAll("^.*#", "");
                    WorkflowRun wstatus = WingsAdapter.get().getWorkflowRunStatus(rname);
                    bindings.setRun(wstatus);
                    
                    //Add input files:
                    Map<String, String> inputs = wstatus.getFiles();
                    Collection<String> urls = inputs.values();
                    tloi.setInputFiles(new ArrayList<String>(urls));

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
                        
                        //Add outputs
                        Map<String, String> outputs = wstatus.getOutputs();
                        if (outputs != null) {
                            List<String> outputlist = new ArrayList<String>(outputs.values());
                            tloi.setOutputFiles(outputlist);

                            for (String outname: outputs.keySet()) {
                                if (outname.equals("p_value") || outname.equals("pval") || outname.equals("p_val")) {
                                    String dataid = outputs.get(outname);
                                    String wingsP = WingsAdapter.get().fetchDataFromWings(dataid);
                                    Double pval = 0.0;
                                    try {
                                        //pval = Double.parseDouble(wingsP);
                                        pval = Double.valueOf(wingsP);
                                    } catch (Exception e) {
                                        System.err.println(dataid + " is a non valid p-value");
                                    }
                                    if (pval > 0) {
                                        System.out.println("Detected p-value: " + pval);
                                        tloi.setConfidenceValue(pval);
                                    }
                                }
                            }
                        }

                        if (metamode) {
                            // Fetch the output hypothesis file, and create a
                            // new hypothesis
                            String hypId = fetchOutputHypothesis(username, bindings, tloi);
                            // String hypId = createDummyHypothesis(username,
                            // domain, bindings, tloi);
                            if (hypId != null)
                                tloi.addResultingHypothesisId(hypId);
                        }
                    }
                }
                // If all the workflows are successfully finished
                if (numSuccessful == wflowBindings.size()) {
                    if (metamode) {
                        overallStatus = Status.SUCCESSFUL;
                    } else {
                        overallStatus = Status.RUNNING;
                        System.out.println("Starting metamode after " + numSuccessful + " workflows.");

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
        String defaultUsername;
        String defaultDomain;

        public DataMonitor() {
            stop = false;
            scheduledFuture = monitor.scheduleWithFixedDelay(this, 0, 1, TimeUnit.DAYS);
        }

        public void run() {
            System.out.println("Running data monitor thread");
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
                    defaultUsername = Config.get().getProperties().getString("username");
                    defaultDomain = Config.get().getProperties().getString("domain");
                    String url = ASSERTIONSURI(defaultUsername);
                    KBAPI kb = fac.getKB(url, OntSpec.PLAIN, false);
                    KBObject typeprop = kb.getProperty(/*KBConstants.NEURONS() +*/ "hasEnigmaQueryLiteral");
                    List<KBTriple> equeries = kb.genericTripleQuery(null, typeprop, null);
                    for (KBTriple kbt : equeries)
                        if (DataQuery.wasUpdatedInLastDay(kbt.getObject().getValueAsString())) {
                            requeryHypotheses(defaultUsername);
                            break;
                        }

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
