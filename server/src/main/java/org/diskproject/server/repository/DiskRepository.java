package org.diskproject.server.repository;

import java.io.ByteArrayOutputStream;
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

import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.diskproject.server.adapters.wings.WingsParser;
import org.diskproject.server.db.DiskDB;
import org.diskproject.server.db.QuestionDB;
import org.diskproject.server.managers.DataAdapterManager;
import org.diskproject.server.managers.MethodAdapterManager;
import org.diskproject.server.managers.StorageManager;
import org.diskproject.server.managers.VocabularyManager;
import org.diskproject.server.querying.Match;
import org.diskproject.server.threads.ThreadManager;
import org.diskproject.server.util.Config;
import org.diskproject.server.util.KBUtils;
import org.diskproject.shared.classes.adapters.DataAdapter;
import org.diskproject.shared.classes.adapters.DataResult;
import org.diskproject.shared.classes.adapters.MethodAdapter;
import org.diskproject.shared.classes.adapters.MethodAdapter.FileAndMeta;
import org.diskproject.shared.classes.common.Entity;
import org.diskproject.shared.classes.common.Status;
import org.diskproject.shared.classes.hypothesis.Goal;
import org.diskproject.shared.classes.hypothesis.GoalResult;
import org.diskproject.shared.classes.loi.LineOfInquiry;
import org.diskproject.shared.classes.loi.TriggeredLOI;
import org.diskproject.shared.classes.question.Question;
import org.diskproject.shared.classes.question.VariableOption;
import org.diskproject.shared.classes.util.DataAdapterResponse;
import org.diskproject.shared.classes.util.KBConstants;
import org.diskproject.shared.classes.util.QuestionOptionsRequest;
import org.diskproject.shared.classes.util.KBConstants.SPECIAL;
import org.diskproject.shared.classes.vocabulary.Vocabulary;
import org.diskproject.shared.classes.workflow.VariableBinding.BindingTypes;
import org.diskproject.shared.classes.workflow.Execution;
import org.diskproject.shared.classes.workflow.VariableBinding;
import org.diskproject.shared.classes.workflow.WorkflowInstantiation;
import org.diskproject.shared.classes.workflow.WorkflowSeed;
import org.diskproject.shared.ontologies.DISK;

import edu.isi.kcap.ontapi.KBAPI;
import edu.isi.kcap.ontapi.KBObject;
import edu.isi.kcap.ontapi.KBTriple;
import edu.isi.kcap.ontapi.OntSpec;
import javax.ws.rs.NotFoundException;

public class DiskRepository {
    static DiskRepository singleton;
    private static boolean creatingKB = false;

    private String tdbdir, server;
    private DiskRDF rdf;
    private DiskDB diskDB;
    private QuestionDB questionDB;

    private static SimpleDateFormat dateformatter = new SimpleDateFormat(KBConstants.DATE_FORMAT);
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
        this.diskDB = new DiskDB(server, this.rdf);
        this.questionDB = new QuestionDB(this.rdf, this.dataAdapters);
        this.vocabularyManager = new VocabularyManager(this.rdf);
        this.addInternalVocabularies();

        // Register adapters as Endpoints on the RDF store
        this.dataAdapters.registerAdapters(diskDB);
        this.methodAdapters.registerAdapters(diskDB);
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
     * Goal
     */
    public Goal addGoal(Goal goal) {
        return this.diskDB.AddOrUpdateGoal(goal, null);
    }

    public boolean removeGoal(String id) {
        return this.diskDB.deleteGoal(id);
    }

    public Goal getGoal(String id) {
        return this.diskDB.loadGoal(id);
    }

    public Goal updateGoal(String id, Goal goal) {
        return this.diskDB.AddOrUpdateGoal(goal, id);
    }

    public List<Goal> listGoals() {
        return this.diskDB.listGoals();
    }

    /*
     * Lines of Inquiry
     */

    public LineOfInquiry addLOI(LineOfInquiry loi) {
        return this.diskDB.AddOrUpdateLOI(loi, null);
    }

    public boolean removeLOI(String id) {
        return this.diskDB.deleteLOI(id);
    }

    public LineOfInquiry getLOI(String id) {
        return this.diskDB.loadLOI(id);
    }

    public LineOfInquiry updateLOI(String id, LineOfInquiry loi) {
        return this.diskDB.AddOrUpdateLOI(loi, id);
    }

    public List<LineOfInquiry> listLOIs() {
        return this.diskDB.listLOIPreviews();
    }

    /*
     * Triggered Lines of Inquiries (TLOIs)
     */

    public TriggeredLOI addTriggeredLOI(TriggeredLOI tloi) {
        TriggeredLOI newTloi = this.diskDB.addOrUpdateTLOI(tloi, null);
        if (newTloi != null) threadManager.executeTLOI(newTloi);
        return newTloi;
    }

    public boolean removeTriggeredLOI(String id) {
        return this.diskDB.deleteTLOI(id);
    }

    public TriggeredLOI getTriggeredLOI(String id) {
        return this.diskDB.loadTLOI(id);
    }

    public TriggeredLOI updateTriggeredLOI(String id, TriggeredLOI tloi) {
        return this.diskDB.addOrUpdateTLOI(tloi, id);
    }

    public List<TriggeredLOI> listTriggeredLOIs() {
        return this.diskDB.listTLOIs();
    }

    /*
     * Questions and option configuration
     */

    public List<Question> listHypothesesQuestions() {
        return this.questionDB.listQuestions();
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

    public void queryAllGoals () {
        for (Goal goal: this.listGoals()) {
            String localId = DiskDB.getLocalId(goal.getId());
            try {
                this.queryGoal(localId);
            } catch (Exception e) {
                System.err.println("Error querying goal " + localId);
            }
        }
    }

    public List<TriggeredLOI> queryGoal (String id) throws Exception, QueryParseException {
        System.out.println("Quering goal: " + id);
        Goal goal = this.getGoal(id);
        if (goal == null)
            throw new NotFoundException("Goal not found");
        Question goalQuestion = goal.getQuestion();
        if (goalQuestion == null || goalQuestion.getId() == null || goalQuestion.getId().equals(""))
            throw new NotFoundException("Goal is not linked to any question");
        String questionId = goalQuestion.getId();
        goalQuestion = this.questionDB.getQuestion(questionId);
        if (goalQuestion == null)
            throw new NotFoundException("Could not load question linked to this Goal");

        List<TriggeredLOI> tlois = new ArrayList<TriggeredLOI>();
        Map<String, List<DataResult>> queryCache = new HashMap<String, List<DataResult>>();
        Map<String, String> alreadyUploaded = new HashMap<String,String>();
        for (LineOfInquiry cur: this.listLOIs()) {
            if (cur.getQuestion() != null && cur.getQuestion().getId().equals(questionId)) {
                DataAdapter dataSource = this.dataAdapters.getMethodAdapterByLOI(cur);
                Match loiMatch = new Match(goal, cur, goalQuestion, dataSource); //datasource could be null here.
                String template = loiMatch.createQueryTemplate();
                if (dataSource != null && template != null) {
                    String query = this.getAllPrefixes() + "SELECT DISTINCT " +
                            String.join(" ", loiMatch.selectVariables)
                            + " WHERE { \n" + template + "\n}";
                    String cacheId = dataSource.getId() + "|" + query;
                    if (!queryCache.containsKey(cacheId)) {
                        // FIXME: add try catch here for network issues.
                        queryCache.put(cacheId, dataSource.query(query));
                    }
                    List<DataResult> solutions = queryCache.get(cacheId);
                    if (solutions.size() == 0) {
                        System.out.println("LOI " + DiskDB.getLocalId(cur.getId()) + " got no results. ");
                        System.out.println(query);
                        System.out.println("=========");
                        continue;
                    } else {
                        System.out.println("LOI " + DiskDB.getLocalId(cur.getId()) + " got " + solutions.size() + " results. ");
                        System.out.println(query);
                        System.out.println("=========");
                    }

                    if (loiMatch.fullCSV) {
                        String csvUri = alreadyUploaded.get(cacheId);
                        if (csvUri == null) {
                            byte[] csvFile = dataSource.queryCSV(query);
                            String csvHash = KBUtils.SHAsum(csvFile);
                            csvUri = this.externalStorage.upload(csvHash, "text/csv", csvFile);
                            alreadyUploaded.put(cacheId, csvUri);
                        }
                        // run the query again, this time get the bits to create csv file.
                        loiMatch.setCSVURL(csvUri);
                    }

                    loiMatch.setQueryResults(solutions, query);
                    tlois.add(uploadData(loiMatch.createTLOI()));
                } else {
                    System.out.println("Warning: No data source or template. " + cur.getId());
                }
            }
        }

        return checkExistingTLOIs(tlois);
    }

    private TriggeredLOI uploadData (TriggeredLOI tloi) {
        //Check and upload files.
        DataAdapter dataAdapter = dataAdapters.getMethodAdapterByEndpoint(tloi.getQueryResults().getEndpoint());
        List<WorkflowInstantiation> wf = new ArrayList<WorkflowInstantiation>(),
                mwf = new ArrayList<WorkflowInstantiation>();
        for (WorkflowInstantiation i: tloi.getWorkflows()) {
            wf.add(uploadData(i, dataAdapter));
        }
        for (WorkflowInstantiation i: tloi.getMetaWorkflows()) {
            mwf.add(uploadData(i, dataAdapter));
        }
        tloi.setWorkflows(wf);
        tloi.setMetaWorkflows(mwf);
        return tloi;
    }

    private WorkflowInstantiation uploadData (WorkflowInstantiation inst, DataAdapter dataAdapter) {
        Map<String,VariableBinding> dataBindings = new HashMap<String,VariableBinding>();
        for (VariableBinding b: inst.getDataBindings()) {
            dataBindings.put(b.getVariable(), b);
        }
        //Check and upload files.
        Map<String,Map<String,String>> uploaded = new HashMap<String,Map<String,String>>();
        for (VariableBinding b: inst.getInputs()) {
            VariableBinding val = dataBindings.get(b.getVariable());
            if (val != null) {
                //All of these variables should be links
                List<String> dsUrls = new ArrayList<String>();
                for (String url: val.getBinding()) {
                    if (url.startsWith("http")) { //Better way to check this later
                        dsUrls.add(url);
                    } else {
                        System.out.println("Error: Input binding is not an URL. " + b.toString());
                    }
                }

                try {
                    Map<String, String> urlToName = addData(dsUrls,
                            methodAdapters.getMethodAdapterByEndpoint(inst.getSource()), 
                            dataAdapter, b.getDatatype());
                    uploaded.put(b.getVariable(), urlToName);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        //Now, we have the IDs we need on WINGs. Replace the dataBindings.
        List<VariableBinding> newDataBindings = new ArrayList<VariableBinding>();
        for (VariableBinding b: inst.getDataBindings()) {
            Map<String, String> urlToName = uploaded.get(b.getVariable());
            if (urlToName != null) {
                VariableBinding newBinding = new VariableBinding(b);
                b.setVariable("_" + b.getVariable());
                List<String> replacedUrls = new ArrayList<String>();
                for (String val: newBinding.getBinding()) {
                    String newVal = urlToName.get(val);
                    if (newVal != null) {
                        replacedUrls.add(newVal);
                    } else {
                        //This should never happend
                        replacedUrls.add(val);
                    }
                }
                newBinding.setBinding(replacedUrls);
                newDataBindings.add(newBinding);
            }
            newDataBindings.add(b);
        }
        inst.setDataBindings(newDataBindings);
        return inst;
    }

    // This replaces all triggered lines of inquiry already executed.
    private List<TriggeredLOI> checkExistingTLOIs(List<TriggeredLOI> tlois) {
        List<TriggeredLOI> checked = new ArrayList<TriggeredLOI>();
        Map<String, List<TriggeredLOI>> cache = new HashMap<String, List<TriggeredLOI>>();
        for (TriggeredLOI tloi : tlois) {
            String parentLoiId = tloi.getParentLoi().getId();
            //System.out.println("Checking " + tloi.getId() + " (" + parentLoiId + ")");
            if (!cache.containsKey(parentLoiId)) {
                cache.put(parentLoiId,
                        getTLOIsForHypothesisAndLOI(tloi.getParentGoal().getId(), parentLoiId));
            }
            List<TriggeredLOI> candidates = cache.get(parentLoiId);
            TriggeredLOI real = tloi;
            for (TriggeredLOI cur : candidates) {
                if (cur.toString().equals(tloi.toString())) {
                    // TODO: We could download the input datasets to checks hashes here
                    System.out.println("Replaced " + tloi.getId() + " with " + cur.getId());
                    real = cur;
                    break;
                }
            }
            // Run all non triggered TLOIs
            if (real.getStatus() == Status.PENDING) {
                System.out.println("TLOI " + tloi.getId() + " will be trigger");
                real = addTriggeredLOI(tloi);
            }
            checked.add(real);
        }
        return checked;
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

    public Boolean runAllHypotheses() throws Exception {
        List<String> hList = new ArrayList<String>();
        String url = this.server + "/hypotheses";
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
            tList.addAll(queryGoal(hid));
        }

        // Only hypotheses with status == null are new
        for (TriggeredLOI tloi : tList) {
            if (tloi.getStatus() == null) {
                System.out.println("TLOI " + tloi.getId() + " will be trigger");
                addTriggeredLOI(tloi);
            }
        }

        return true;
    }

    /*
     * Running
     */

    public List<TriggeredLOI> getTLOIsForHypothesisAndLOI(String hypId, String loiId) {
        // Get all TLOIs and filter out
        List<TriggeredLOI> list = new ArrayList<TriggeredLOI>();
        for (TriggeredLOI tloi : this.diskDB.listTLOIs()) {
            String parentHypId = tloi.getParentGoal().getId();
            String parentLOIId = tloi.getParentLoi().getId();
            if (parentHypId != null && parentHypId.equals(hypId) &&
                    parentLOIId != null && parentLOIId.equals(loiId)) {
                list.add(tloi);
            }
        }
        return list;
    }

    public List<TriggeredLOI> runHypothesisAndLOI(String hypId, String loiId) throws Exception {
        List<TriggeredLOI> hypTlois = queryGoal(hypId);
        // TriggeredLOI match = null;
        for (TriggeredLOI tloi : hypTlois) {
            if (tloi.getStatus() == null && tloi.getParentLoi().getId().equals(loiId)) {
                // Set basic metadata
                tloi.setAuthor(new Entity("0", "System", ""));
                Date date = new Date();
                tloi.setDateCreated(dateformatter.format(date));
                addTriggeredLOI(tloi);
                // match = tloi;
                break;
            }
        }

        return getTLOIsForHypothesisAndLOI(hypId, loiId);
    }

    public Execution getWorkflowRunStatus(String source, String id) {
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

    private String downloadAsString (MethodAdapter methodAdapter, String dataid) {
        FileAndMeta fm = methodAdapter.fetchData(dataid);
        byte[] byteConf = fm.data;
        return byteConf != null ? new String(byteConf, StandardCharsets.UTF_8) : null;
    }

    public void processWorkflowOutputs (WorkflowSeed workflow, Execution run, boolean meta) {
        MethodAdapter methodAdapter = methodAdapters.getMethodAdapterByEndpoint(workflow.getSource());
        List<VariableBinding> generatedOutputs = run.getOutputs();
        List<VariableBinding> variableOutputs  = workflow.getOutputs();
        GoalResult newResult = new GoalResult();

        for (VariableBinding definitions: variableOutputs) {
            for (VariableBinding values: generatedOutputs) {
                if (definitions.getVariable().equals(values.getVariable())) {
                    String binding = definitions.getBinding().get(0);
                    // The following require only one input file to be processed:
                    if (!values.getIsArray() && values.getBindingType() == BindingTypes.DISK_DATA) {
                        if (binding.equals(SPECIAL.CONFIDENCE_V)) {
                            String wingsP = downloadAsString(methodAdapter, values.getSingleBinding());
                            Double pVal = null, n = null;
                            try {
                                String[] lines = wingsP != null ? wingsP.split("\n") : null;
                                String strPVal = lines != null && lines.length > 0 ? lines[0] : null;
                                String strN = lines != null && lines.length > 1 ? lines[1] : null;
                                if (strPVal != null)
                                    pVal = Double.valueOf(strPVal);
                                if (strN != null)
                                    n = Double.valueOf(strN);
                            } catch (Exception e) {
                                System.err.println("[M] Error: " + values.getSingleBinding() + " is a non valid p-value: " + wingsP);
                            }
                            if (pVal != null) {
                                System.out.println("[M] Detected p-value: " + pVal);
                                newResult.setConfidenceType("p-Value"); //FIXME, set a way to determine the kind of confidence we use.
                                newResult.setConfidenceValue(pVal);
                            }
                            if (pVal != null && n != null) {
                                VariableBinding bindingX = new VariableBinding("X", String.valueOf(n));
                                bindingX.setDatatype(KBConstants.XSD_NS + "double");
                                newResult.addValue(bindingX);
                                VariableBinding bindingY = new VariableBinding("Y", String.valueOf(pVal));
                                bindingY.setDatatype(KBConstants.XSD_NS + "double");
                                newResult.addValue(bindingY);
                            }
                        } else if (binding.equals(SPECIAL.BRAIN_VIZ) || binding.equals(SPECIAL.SHINY_LOG)) {
                            String v = downloadAsString(methodAdapter, values.getSingleBinding());
                            if (v != null) {
                                VariableBinding newbinding = new VariableBinding(values.getVariable(), v);
                                newbinding.setDatatype(KBConstants.XSD_NS + "string");
                                newResult.addValue(newbinding);
                            }
                        }
                    }  
                    // The following can be a single value or a list.
                    if (binding.equals(SPECIAL.DOWNLOAD) || binding.equals(SPECIAL.IMAGE) || binding.equals(SPECIAL.VISUALIZE)) {
                        //Should upload the file to minio.
                        List<String> uris = new ArrayList<String>();
                        for (String v: values.getBinding()) {
                            System.out.println("[M] Downloading: " + v);
                            FileAndMeta fm = methodAdapter.fetchData(v);
                            uris.add(this.externalStorage.upload(WingsParser.getLocalId(v) , fm.contentType, fm.data));
                        }
                        if (uris.size() > 0) {
                            // Here we add the new bindings to the results
                            VariableBinding newBinding = new VariableBinding(values);
                            newBinding.setBinding(uris);
                            newResult.addValue(newBinding);
                        }
                    }
                }
            }
        }
        run.setResult(newResult);
    }

    public Entity getOrCreateEntity(String username) {
        return this.diskDB.loadOrRegisterEntity(username);
    }

    /*public FileAndMeta getOntologyAll() {
        this.rdf.startRead();
        Dataset all = this.rdf.fac.getDataset();
		ByteArrayOutputStream rawBytes = new ByteArrayOutputStream(); 
        RDFDataMgr.write(rawBytes, all, Lang.NQUADS);
		byte[] bytes = rawBytes.toByteArray();
		FileAndMeta fileData = new FileAndMeta(bytes, "application/n-quads");
        this.rdf.end();
        return fileData;
    }*/
}
