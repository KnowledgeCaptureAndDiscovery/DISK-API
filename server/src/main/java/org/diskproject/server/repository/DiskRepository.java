package org.diskproject.server.repository;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.query.QueryParseException;

import org.diskproject.server.db.DiskDB;
import org.diskproject.server.db.QuestionDB;
import org.diskproject.server.managers.DataAdapterManager;
import org.diskproject.server.managers.MethodAdapterManager;
import org.diskproject.server.managers.StorageManager;
import org.diskproject.server.managers.VocabularyManager;
import org.diskproject.server.threads.ThreadManager;
import org.diskproject.server.util.Config;
import org.diskproject.server.util.KBUtils;
import org.diskproject.shared.classes.adapters.DataAdapter;
import org.diskproject.shared.classes.adapters.DataResult;
import org.diskproject.shared.classes.adapters.MethodAdapter;
import org.diskproject.shared.classes.adapters.MethodAdapter.FileAndMeta;
import org.diskproject.shared.classes.hypothesis.Hypothesis;
import org.diskproject.shared.classes.loi.LineOfInquiry;
import org.diskproject.shared.classes.loi.WorkflowBindings;
import org.diskproject.shared.classes.loi.TriggeredLOI;
import org.diskproject.shared.classes.workflow.WorkflowRun.RunBinding;
import org.diskproject.shared.classes.question.Question;
import org.diskproject.shared.classes.question.VariableOption;
import org.diskproject.shared.classes.util.DataAdapterResponse;
import org.diskproject.shared.classes.util.KBConstants;
import org.diskproject.shared.classes.util.QuestionOptionsRequest;
import org.diskproject.shared.classes.vocabulary.Vocabulary;
import org.diskproject.shared.classes.workflow.WorkflowVariable;
import org.diskproject.shared.classes.workflow.VariableBinding;
import org.diskproject.shared.classes.workflow.WorkflowRun;
import org.diskproject.shared.ontologies.DISK;

import edu.isi.kcap.ontapi.KBAPI;
import edu.isi.kcap.ontapi.KBObject;
import edu.isi.kcap.ontapi.KBTriple;
import edu.isi.kcap.ontapi.OntSpec;
import edu.isi.kcap.ontapi.SparqlQuerySolution;
import javax.ws.rs.NotFoundException;

public class DiskRepository {
    static DiskRepository singleton;
    private static boolean creatingKB = false;

    private String tdbdir, server;
    private DiskRDF rdf;
    private DiskDB diskDB;
    private QuestionDB questionDB;

    private static SimpleDateFormat dateformatter = new SimpleDateFormat("HH:mm:ss yyyy-MM-dd");
    Pattern varPattern = Pattern.compile("\\?(.+?)\\b");
    Pattern varCollPattern = Pattern.compile("\\[\\s*\\?(.+?)\\s*\\]");

    StorageManager externalStorage;
    ThreadManager threadManager;
    VocabularyManager vocabularyManager;
    public DataAdapterManager dataAdapters;
    public MethodAdapterManager methodAdapters;

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

    private boolean loadAndSetConfiguration () {
        Config currentConfig =  Config.get();
        if (currentConfig != null) {
            this.tdbdir = currentConfig.storage.tdb;
            this.server = currentConfig.server;
            File tdbdirF = new File(tdbdir);
            if (!tdbdirF.exists() && !tdbdirF.mkdirs()) {
                System.err.println("Cannot create tdb directory : " + tdbdirF.getAbsolutePath());
            } else {
                return this.tdbdir != null && this.server != null;
            }
        }
        return false;

    }

    public DiskRepository() {
        this.loadAndSetConfiguration();
        // Create RDF transaction API
        this.rdf = new DiskRDF(tdbdir); // Transaction API & Factory.
        // Create managers reading from configuration
        this.dataAdapters = new DataAdapterManager();
        this.methodAdapters = new MethodAdapterManager();
        this.externalStorage = new StorageManager();
        // Threads
        this.threadManager = new ThreadManager(methodAdapters, this);  // FIXME: instead of this should be the db & adapters.
        // These read/write RDF
        this.diskDB = new DiskDB(server, this.rdf, this.methodAdapters);
        this.questionDB = new QuestionDB(this.rdf, this.dataAdapters);
        this.vocabularyManager = new VocabularyManager(this.rdf);
        this.addInternalVocabularies();
    }

    public void shutdownExecutors() {
        this.threadManager.shutdownExecutors();
    }

    /********************
     * Initialization
     */

     private void addInternalVocabularies () {
        this.vocabularyManager.addVocabularyFromKB(diskDB.getKB(), KBConstants.DISK_URI, KBConstants.DISK_NS,
            "The DISK Ontology", "disk", "DISK Main ontology. Defines all resources used on the DISK system.");
        this.vocabularyManager.addVocabularyFromKB(questionDB.getKB(), KBConstants.QUESTION_URI, KBConstants.QUESTION_NS,
            "Scientific Question Ontology", "sqo", "Ontology to define questions templates.");
     }

    public void reloadKBCaches() throws Exception {
        this.diskDB.reloadKB();
        this.questionDB.reloadKB();
        this.vocabularyManager.reloadKB();
        this.addInternalVocabularies();
    }

    // -- Data adapters
    private DataAdapter getDataAdapter(String url) {
        return this.dataAdapters.getDataAdapterByUrl(url);
    }

    public List<DataAdapterResponse> getDataAdapters() {
        List<DataAdapterResponse> adapters = new ArrayList<DataAdapterResponse>();
        for (DataAdapter da : this.dataAdapters.values()) {
            adapters.add(new DataAdapterResponse(da));
        }
        return adapters;
    }

    public Vocabulary getVocabulary(String uri) {
        return this.vocabularyManager.getVocabulary(uri);
    }

    public Map<String, Vocabulary> getVocabularies() {
        return this.vocabularyManager.getVocabularies();
    }

    /*
     * Hypotheses
     */
    public Hypothesis addHypothesis(String username, Hypothesis hypothesis) {
        return this.diskDB.AddOrUpdateHypothesis(username, hypothesis, null);
    }

    public boolean removeHypothesis(String username, String id) {
        return this.diskDB.deleteHypothesis(username, id);
    }

    public Hypothesis getHypothesis(String username, String id) {
        return this.diskDB.loadHypothesis(username, id);
    }

    public Hypothesis updateHypothesis(String username, String id, Hypothesis hypothesis) {
        return this.diskDB.AddOrUpdateHypothesis(username, hypothesis, id);
    }

    public List<Hypothesis> listHypotheses(String username) {
        return this.diskDB.listHypothesesPreviews(username);
    }

    /*
     * Lines of Inquiry
     */

    public LineOfInquiry addLOI(String username, LineOfInquiry loi) {
        return this.diskDB.AddOrUpdateLOI(username, loi, null);
    }

    public boolean removeLOI(String username, String id) {
        return this.diskDB.deleteLOI(username, id);
    }

    public LineOfInquiry getLOI(String username, String id) {
        return this.diskDB.loadLOI(username, id);
    }

    public LineOfInquiry updateLOI(String username, String id, LineOfInquiry loi) {
        return this.diskDB.AddOrUpdateLOI(username, loi, id);
    }

    public List<LineOfInquiry> listLOIs(String username) {
        return this.diskDB.listLOIPreviews(username);
    }

    /*
     * Triggered Lines of Inquiries (TLOIs)
     */

    public TriggeredLOI addTriggeredLOI(String username, TriggeredLOI tloi) {
        TriggeredLOI newTloi = this.diskDB.addOrUpdateTLOI(username, tloi, null);
        if (newTloi != null) threadManager.executeTLOI(newTloi);
        return newTloi;
    }

    public boolean removeTriggeredLOI(String username, String id) {
        return this.diskDB.deleteTLOI(username, id);
    }

    public TriggeredLOI getTriggeredLOI(String username, String id) {
        return this.diskDB.loadTLOI(username, id);
    }

    public TriggeredLOI updateTriggeredLOI(String username, String id, TriggeredLOI tloi) {
        return this.diskDB.addOrUpdateTLOI(username, tloi, id);
    }

    public List<TriggeredLOI> listTriggeredLOIs(String username) {
        return this.diskDB.listTLOIs(username);
    }

    /*
     * Questions and option configuration
     */

    public List<Question> listHypothesesQuestions() {
        return this.questionDB.listHypothesesQuestions();
    }

    public Map<String,List<VariableOption>> listDynamicOptions(QuestionOptionsRequest cfg) throws Exception {
        return this.questionDB.listDynamicOptions(cfg);
    }

    /*
     * Querying
     */

    private String getAllPrefixes() {
        return KBConstants.getAllPrefixes() + this.vocabularyManager.getPrefixes();
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
        // Mandatory fields for Lnes of Inquiry
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
        }
        return true;
    }

    public Map<LineOfInquiry, List<Map<String, String>>> getLOIByHypothesisId(String username, String id) {
        String hypuri = this.server + "/" + username + "/hypotheses";
        // LoiId -> [{ variable -> value }]
        Map<String, List<Map<String, String>>> allMatches = new HashMap<String, List<Map<String, String>>>();
        List<LineOfInquiry> lois = this.diskDB.listLOIPreviews(username);

        // Starts checking all LOIs that match the hypothesis directly from the KB.
        try {
            this.rdf.startRead();
            KBAPI hypKB = this.rdf.getFactory().getKB(hypuri, OntSpec.PLAIN, true);
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
            this.rdf.end();
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
                } else {
                    System.out.println("Invalid:" + hBindings.toString());
                }
            }
        }

        // LOI -> [{ variable -> value }, {...}, ...]
        return results;
    }

    //public List<TriggeredLOI> newQueryHypothesis (String username, String id) throws Exception, QueryParseException {
    //    // New approach, use the question bindings.
    //    List<TriggeredLOI> tlois = new ArrayList<TriggeredLOI>();
    //    Hypothesis hypothesis = this.getHypothesis(username, id);
    //    List<LineOfInquiry> lois = this.diskDB.listLOIPreviews(username);
    //    List<LineOfInquiry> matchingLois = new ArrayList<LineOfInquiry>();
    //    for (LineOfInquiry loi: lois) {
    //        if (loi.getQuestionId().equals(hypothesis.getQuestionId())) {
    //            matchingLois.add(loi);
    //        }
    //    }
    //    List<VariableBinding> questionBindings = hypothesis.getQuestionBindings();
    //    for (LineOfInquiry loi: matchingLois) {
    //    }
    //    return tlois;
    //}

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
            DataAdapter dataAdapter = this.getDataAdapter(loi.getDataSource());
            if (dataAdapter == null) {
                System.out.println("Warning: " + loi.getId() + " uses an unknown data adapter: " + loi.getDataSource());
                continue;
            } else {
                boolean allOk = true;
                for (WorkflowBindings wb: loi.getWorkflows()) {
                    String source =  wb.getSource();
                    if (source == null || methodAdapters.getMethodAdapterByName(source) == null) {
                        allOk = false;
                        System.out.println("Warning: " + loi.getId() + " uses an unknown method adapter: " + source);
                        break;
                    }
                }
                if (allOk)
                    for (WorkflowBindings wb: loi.getMetaWorkflows()) {
                        String source =  wb.getSource();
                        if (source == null || methodAdapters.getMethodAdapterByName(source) == null) {
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
                //TODO: work here to allow csv file and other stuff

                String dq = getQueryBindings(loi.getDataQuery(), varPattern, values);
                String query = this.getAllPrefixes() + "SELECT DISTINCT ";

                Set<String> setSelect = new HashSet<String>();
                boolean attachCSV = false;
                for (String qVar : loi.getAllWorkflowVariables()) {
                    if (qVar.startsWith("?"))
                        setSelect.add(qVar);
                    else if (qVar.equals("_CSV_")) {
                        attachCSV = true;
                        for (String inVar: getDataQueryVariables(dq)) {
                            setSelect.add(inVar);
                        }
                    }
                }
                String selectedVars = "";
                for (String inVar: setSelect) {
                    selectedVars += inVar + " ";
                }

                query += selectedVars + " WHERE {\n" + dq + "}";

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

                    // Attach CSV
                    if (this.externalStorage == null) {
                        System.out.println("Warning: External storage not found. Can not upload file.");
                    } else if (attachCSV) {
                        //run the query again, this time get the bits to create csv file.
                        byte[] csvFile = dataAdapter.queryCSV(query);
                        String csvHash = KBUtils.SHAsum(csvFile);
                        String csvUri = this.externalStorage.upload(csvHash, "text/csv", csvFile);

                        dataVarBindings.put("_CSV_", new ArrayList<String>());
                        dataVarBindings.get("_CSV_").add(csvUri);
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

    private Set<String> getDataQueryVariables(String wherePart) {
        Pattern varPattern = Pattern.compile("\\?(.+?)\\b");
        Set<String> l = new HashSet<String>();
        Matcher a = varPattern.matcher(wherePart);
        while (a.find()) {
            String var = a.group();
            if (var.charAt(1) != '_')
                l.add(var);
        }
        return l;
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

    private List<WorkflowBindings> getTLOIBindings(String username, List<WorkflowBindings> workflowList,
            Map<String, List<String>> dataVarBindings, DataAdapter dataAdapter) throws Exception {
        List<WorkflowBindings> tloiWorkflowList = new ArrayList<WorkflowBindings>();

        for (WorkflowBindings workflowDef : workflowList) { // FOR EACH WORKFLOW
            // For each Workflow, create an empty copy to set the values
            WorkflowBindings tloiWorkflowDef = new WorkflowBindings(
                    workflowDef.getWorkflow(),
                    workflowDef.getWorkflowLink());
            tloiWorkflowDef.setSource(workflowDef.getSource());
            tloiWorkflowDef.setMeta(workflowDef.getMeta());
            tloiWorkflowList.add(tloiWorkflowDef);
            MethodAdapter methodAdapter = methodAdapters.getMethodAdapterByName(workflowDef.getSource());
            List<WorkflowVariable> allVars = methodAdapter.getWorkflowVariables(workflowDef.getWorkflow());
            Map<VariableBinding, Integer> binSize = new HashMap<VariableBinding, Integer>();

            // We need to order bindings by the number of datasets.
            for (VariableBinding vBinding : workflowDef.getBindings()) { // Normal variable bindings.
                String binding = vBinding.getBinding();
                Matcher collmat = varCollPattern.matcher(binding);
                Matcher mat = varPattern.matcher(binding);
                // Get the sparql variable
                String sparqlVar = null;
                if (collmat.find() && dataVarBindings.containsKey(collmat.group(1))) {
                    sparqlVar = collmat.group(1);
                } else if (mat.find() && dataVarBindings.containsKey(mat.group(1))) {
                    sparqlVar = mat.group(1);
                } else if (binding.equals("_CSV_")) {
                    sparqlVar = "_CSV_";
                }

                if (sparqlVar == null) {
                    binSize.put(vBinding, 0);
                } else {
                    //List<String> dsUrls = dataVarBindings.get(sparqlVar);
                    binSize.put(vBinding, dataVarBindings.containsKey(sparqlVar) ? dataVarBindings.get(sparqlVar).size() : 0);
                }
            }
            List<VariableBinding> LIST = workflowDef.getBindings();
            LIST.sort((VariableBinding b1, VariableBinding b2) -> binSize.get(b1) - binSize.get(b2));

            for (VariableBinding vBinding : LIST) { // Normal variable bindings.
            //for (VariableBinding vBinding : LIST) {
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
                } else if (binding.equals("_CSV_")) {
                    sparqlVar = "_CSV_";
                }

                if (sparqlVar == null) {
                    tloiWorkflowDef.addBinding(vBinding);
                    continue;
                }

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
                    for (WorkflowVariable v: allVars) {
                        //This does not have in consideration output variables.
                        if (varName.equals(v.getName())) {
                            List<String> classes = v.getType();
                            if (classes != null && classes.size() > 0) {
                                dType = classes.contains(vBinding.getDatatype()) ? vBinding.getDatatype() : classes.get(0);
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
                    VariableBinding cur = new VariableBinding(vBinding.getVariable(), dsNames.toString());
                    cur.setDatatype(vBinding.getDatatype());
                    tloiWorkflowDef.addBinding(cur);
                } else {
                    if (dsNames.size() == 1) {
                        VariableBinding cur = new VariableBinding(vBinding.getVariable(), dsNames.get(0));
                        cur.setDatatype(vBinding.getDatatype());
                        tloiWorkflowDef.addBinding(cur);
                    } else {
                        System.out.println("IS MORE THAN ONE VALUE BUT NOT COLLECTION! Creating new workflow runs");
                        System.out.println("Variable: " + vBinding.getVariable());
                        System.out.println("Binding: " + vBinding.getBinding());
                        System.out.println("datasets: " + dsNames);
                        // This variable expects a single file. Add new tloi bindings for each dataset
                        // FIXME: if the variable with multiple values gets here first. Other variable bindings are not added!
                        List<WorkflowBindings> newTloiBindings = new ArrayList<WorkflowBindings>();
                        for (WorkflowBindings tmpWorkflow : tloiWorkflowList) { // For all already processed workflows
                            for (String dsName : dsNames) {
                                List<VariableBinding> newBindings = new ArrayList<VariableBinding>();
                                for (VariableBinding cur: tmpWorkflow.getBindings()) {
                                    VariableBinding newV = new VariableBinding(cur.getVariable(), cur.getBinding());
                                    newV.setDatatype(cur.getDatatype());
                                    newBindings.add(newV);
                                }

                                WorkflowBindings newWorkflow = new WorkflowBindings(
                                        workflowDef.getWorkflow(),
                                        workflowDef.getWorkflowLink(),
                                        newBindings);

                                VariableBinding cur = new VariableBinding(vBinding.getVariable(), dsName);
                                cur.setDatatype(vBinding.getDatatype());
                                newWorkflow.addBinding(cur);

                                newWorkflow.setMeta(workflowDef.getMeta());
                                newWorkflow.setSource(workflowDef.getSource());
                                newTloiBindings.add(newWorkflow);
                            }
                        }
                        tloiWorkflowList = newTloiBindings;
                    }
                }
            }
        }

        return tloiWorkflowList;
    }

    //This adds dsUrls to the data-repository, returns filename -> URL
    private Map<String, String> addData(List<String> dsUrls, MethodAdapter methodAdapter, DataAdapter dataAdapter, String dType)
            throws Exception {
        // To add files to wings and not replace anything, we need to get the hash from the wiki.
        // TODO: We should upload all files to minio
        Map<String, String> nameToUrl = new HashMap<String, String>();
        Map<String, String> urlToName = new HashMap<String, String>();
        Map<String, String> filesETag = dataAdapter.getFileHashesByETag(dsUrls);  // File -> ETag

        for (String fileUrl: dsUrls) {
            if (filesETag.containsKey(fileUrl)) {
                String eTag = filesETag.get(fileUrl);
                // This name should be different now, this is not the SHA
                String uniqueName = "SHA" + eTag.substring(0, 6) + "_" + fileUrl.replaceAll("^.*\\/", "");
                nameToUrl.put(uniqueName, fileUrl);
                urlToName.put(fileUrl, uniqueName);
            } else {
                System.err.println("ETag not found: " + fileUrl);
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
            System.out.println("Uploading to " + methodAdapter.getName() + ": " + newFile + " as " + newFilename + " (" + dType + ")");
            methodAdapter.addData(newFile, newFilename, dType);
        }

        return urlToName;
    }

    public Boolean runAllHypotheses(String username) throws Exception {
        List<String> hList = new ArrayList<String>();
        String url = this.server + "/" + username + "/hypotheses";
        try {
            KBAPI kb = this.rdf.getFactory().getKB(url, OntSpec.PLAIN, true);
            KBObject hypCls = this.diskDB.getOnt().getClass(DISK.HYPOTHESIS);

            this.rdf.startRead();
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
            this.rdf.end();
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
     * Running
     */

    public List<TriggeredLOI> getTLOIsForHypothesisAndLOI(String username, String hypId, String loiId) {
        // Get all TLOIs and filter out
        List<TriggeredLOI> list = new ArrayList<TriggeredLOI>();
        for (TriggeredLOI tloi : this.diskDB.listTLOIs(username)) {
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

    public WorkflowRun getWorkflowRunStatus(String source, String id) {
        MethodAdapter methodAdapter = this.methodAdapters.getMethodAdapterByName(source);
        return methodAdapter != null ? methodAdapter.getRunStatus(id) : null;
    }

    public FileAndMeta getOutputData(String source, String id) {
        MethodAdapter methodAdapter = this.methodAdapters.getMethodAdapterByName(source);
        return (methodAdapter != null) ? methodAdapter.fetchData(methodAdapter.getDataUri(id)) : null;
    }

    /*
     * Threads
     */

    public void processWorkflowOutputs (TriggeredLOI tloi, LineOfInquiry loi, WorkflowBindings workflow, WorkflowRun run, MethodAdapter methodAdapter, boolean meta) {
        Map<String, RunBinding> outputs = run.getOutputs();
        if (outputs == null) return;

        Map<String,String> outputAssignations = new HashMap<String,String>();
        for (WorkflowVariable wb: methodAdapter.getWorkflowVariables(workflow.getWorkflow())) {
            if (!wb.isInput() || wb.isOutput()) { // This could be more strict
                outputAssignations.put(wb.getName(), "DO_NO_STORE");
            }
        }
        // We need to get the loi var assignations
        String id = workflow.getWorkflow();
        for (WorkflowBindings wb: (meta? loi.getMetaWorkflows() : loi.getWorkflows())) {
            if (id.contains(wb.getWorkflow())) {
                for (VariableBinding b: wb.getBindings()) {
                    String varName = b.getVariable();
                    if (outputAssignations.containsKey(varName)) {
                        outputAssignations.put(varName, b.getBinding());
                    }
                }
            }
        }

        // Now process generated outputs.
        for (String outname : outputs.keySet()) {
            String varBinding = outputAssignations.get(outname);
            //for (String varName: outputAssignations.keySet()) {
                //String varBinding = outputAssignations.get(varName);
                if (varBinding == null) {
                    System.out.println("[M] Variable binding not found for " + outname);
                } else if (varBinding.contains("_DO_NO_STORE_") ||
                    varBinding.contains("_DOWNLOAD_ONLY_") ||
                    varBinding.contains("_IMAGE_") ||
                    varBinding.contains("_VISUALIZE_")) {
                    // DO NOTHING, some of these should be upload to MINIO
                } else if (varBinding.contains("_CONFIDENCE_VALUE_")) {
                    System.out.println("OUT: " + outname);
                    //System.out.println("var: " + varName);
                    System.out.println("bin: " + varBinding);

                    String dataid = outputs.get(outname).id;
                    FileAndMeta fm = methodAdapter.fetchData(dataid);
                    byte[] byteConf = fm.data;
                    String wingsP = byteConf != null ? new String(byteConf, StandardCharsets.UTF_8) : null;
                    Double pVal = null;
                    try {
                        String strPVal = wingsP != null ? wingsP.split("\n", 2)[0] : "";
                        pVal = Double.valueOf(strPVal);
                    } catch (Exception e) {
                        System.err.println("[M] Error: " + dataid + " is a non valid p-value: " + wingsP);
                    }
                    if (pVal != null) {
                        System.out.println("[M] Detected p-value: " + pVal);
                        tloi.setConfidenceValue(pVal);
                        tloi.setConfidenceType("P-VALUE");
                    }
                } else {
                    System.out.println("Output information not found");
                }
            //}
        }
    }
}
