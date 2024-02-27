package org.diskproject.server.db;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.diskproject.server.repository.DiskRDF;
import org.diskproject.server.util.KBCache;
import org.diskproject.server.util.KBUtils;
import org.diskproject.shared.classes.DISKResource;
import org.diskproject.shared.classes.common.Endpoint;
import org.diskproject.shared.classes.common.Entity;
import org.diskproject.shared.classes.common.Graph;
import org.diskproject.shared.classes.common.Status;
import org.diskproject.shared.classes.common.Triple;
import org.diskproject.shared.classes.common.Value;
import org.diskproject.shared.classes.hypothesis.Goal;
import org.diskproject.shared.classes.hypothesis.GoalResult;
import org.diskproject.shared.classes.loi.DataQueryResult;
import org.diskproject.shared.classes.loi.DataQueryTemplate;
import org.diskproject.shared.classes.loi.LineOfInquiry;
import org.diskproject.shared.classes.loi.TriggeredLOI;
import org.diskproject.shared.classes.question.Question;
import org.diskproject.shared.classes.util.GUID;
import org.diskproject.shared.classes.util.KBConstants;
import org.diskproject.shared.classes.workflow.Execution;
import org.diskproject.shared.classes.workflow.ExecutionRecord;
import org.diskproject.shared.classes.workflow.VariableBinding;
import org.diskproject.shared.classes.workflow.WorkflowInstantiation;
import org.diskproject.shared.classes.workflow.WorkflowSeed;
import org.diskproject.shared.ontologies.DISK;

import edu.isi.kcap.ontapi.KBAPI;
import edu.isi.kcap.ontapi.KBObject;
import edu.isi.kcap.ontapi.KBTriple;
import edu.isi.kcap.ontapi.OntSpec;

public class DiskDB {
    private static SimpleDateFormat dateformatter = new SimpleDateFormat(KBConstants.DATE_FORMAT);
    private String domain;
    private DiskRDF rdf;
    private KBAPI diskKB, domainKB;
    private KBCache DISKOnt;

    public DiskDB (String domain, DiskRDF rdf) {
        this.domain = domain;
        this.rdf = rdf;
        this.loadKB();
        this.domainKB = getOrCreateKB(domain);
        if (this.domainKB == null) //TODO: check this.
            System.out.println("Something when wrong creating the domain KB");
    }

    private void loadKB () {
        //This is the main KB of the DISK system, loads the ontology from the UI (class and property definitions)
        try {
            this.diskKB = this.rdf.getFactory().getKB(KBConstants.DISK_URI, OntSpec.PELLET, false, true);
        } catch (Exception e) {
            System.out.println("Error reading KB: " + KBConstants.DISK_URI);
            e.printStackTrace();
            return;
        }
        //This maps the terms of the ontology to use for creating new DISK resources.
        if (this.diskKB != null) {
            this.rdf.startRead();
            this.DISKOnt = new KBCache(diskKB);
            this.rdf.end();
        }
    }

    public void reloadKB () {
        KBUtils.clearKB(this.diskKB, this.rdf);
        this.loadKB();
    }

    public KBAPI getKB () {
        return this.diskKB;
    }

    public KBCache getOnt () {
        return this.DISKOnt;
    }

    static public String getLocalId(String fullId) {
        return fullId.replaceAll("^.*\\/", "");
    }
    public String createGoalURI (String id) {
        return this.domain + "/goals/" + id;
    }

    public String createLoiURI (String id) {
        return this.domain + "/lois/" + id;
    }

    public String createTloiURI (String id) {
        return this.domain + "/tlois/" + id;
    }

    public String createEntityURI (int id) {
        return this.domain + "/entities/" + id;
    }

    public String createEndpointURI (int id) {
        return this.domain + "/endpoint/" + id;
    }

    private KBAPI getOrCreateKB(String url) {
        KBAPI kb = null;
        try {
            kb = this.rdf.getFactory().getKB(url, OntSpec.PLAIN, true);
        } catch (Exception e) {
            System.err.print("Could not open or create KB: " + url);
        }
        return kb;
    }

    private KBAPI getKB(String url) {
        KBAPI kb = null;
        try {
            kb = this.rdf.getFactory().getKB(url, OntSpec.PLAIN, false);
        } catch (Exception e) {
            System.err.print("Could not open KB: " + url);
        }
        return kb;
    }

    private KBObject createKBObjectFromValue(Value v, KBAPI kb) {
        String dataType = v.getDatatype();
        Object value = v.getValue();
        if (v.getType() == Value.Type.LITERAL) {
            if (dataType != null)
                return kb.createXSDLiteral(value.toString(), v.getDatatype());
            else
                return kb.createLiteral(value.toString());
        } else {
            if (dataType != null) {
                // TODO: The idea here is that check if this dataType is a URI for a class in the ontologies
                // Then create a resource of that class.
                return kb.getResource(value.toString());
            } else {
                return kb.getResource(value.toString());
            }
        }
    }

    private Value createValueFromKBObject(KBObject obj) {
        Value v = new Value();
        if (obj.isLiteral()) {
            Object valObj = obj.getValue();
            if (valObj instanceof Date) {
                valObj = dateformatter.format((Date) valObj);
            } else if (valObj instanceof String) {
                // Fix quotes and \n
                valObj = ((String) valObj).replace("\"", "\\\"").replace("\n", "\\n");
            }
            v.setType(Value.Type.LITERAL);
            v.setValue(valObj);
            v.setDatatype(obj.getDataType());
        } else {
            v.setType(Value.Type.URI);
            v.setValue(obj.getID());
        }
        return v;
    }

    private Triple kbTripleToTriple (KBTriple kbTriple) {
        return new Triple(
            kbTriple.getSubject().getID(),
            kbTriple.getPredicate().getID(),
            createValueFromKBObject(kbTriple.getObject())
        );
    }

    private KBTriple tripleToKBTriple (Triple triple, KBAPI kb) {
        KBObject sub = kb.getResource(triple.getSubject());
        KBObject pre = kb.getResource(triple.getPredicate());
        KBObject obj = createKBObjectFromValue(triple.getObject(), kb);
        if (sub != null && pre != null && obj != null)
            return this.rdf.getFactory().getTriple(sub, pre, obj);
        return null;
    }

    private Graph loadGraphFromKB(String url) {
        KBAPI kb = getKB(url);
        if (kb != null) {
            Graph graph = new Graph();
            for (KBTriple t : kb.getAllTriples()) {
                graph.addTriple(kbTripleToTriple(t));
            }
            return graph;
        }
        return null;

    }

    private boolean isValidURL (String url) {
        try {
            new URL(url).toURI();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String createDomainUri (String domain, String txt) {
        return domain + "#" + txt.replaceAll(" ", "_").replaceAll(":|?", "");
    }

    private List<Triple> completeGraphWithDomains (Graph graph, String domain, String emptyResource) {
        // A graph must have the format: <uri> <uri> ["literal"|<uri>] .
        // If some resource is not an uri we create one using the domain.
        List<Triple> triples = new ArrayList<Triple>();
        for (Triple curTriple: graph.getTriples()) {
            String sub = curTriple.getSubject(); 
            String pre = curTriple.getPredicate();
            Value obj = curTriple.getObject();
            if (sub == null) {
                sub = (emptyResource != null) ? domain + "#" + emptyResource : domain;
            } else if (!isValidURL(sub)) {
                sub = createDomainUri(domain, sub);
            }
            if (!isValidURL(pre)) pre = createDomainUri(domain, pre);
            triples.add(new Triple(sub, pre, obj));
        }
        return triples;
    }

    // -- Endpoints
    public Endpoint registerEndpoint (Endpoint item) {
        int id = (item.getName() + item.getUrl()).hashCode();
        item.setId(createEndpointURI(id));

        this.rdf.startRead();
        KBObject KBEndpoint = domainKB.getIndividual(item.getId());
        Endpoint endpoint = loadEndpoint(KBEndpoint);
        this.rdf.end();
        if (endpoint == null) {
            System.out.println("Configuration changed! Adding endpoint:");
            this.rdf.startWrite();
            this.writeEndpoint(item);
            this.rdf.save(domainKB);
            this.rdf.end();
            System.out.println(item);
            return item;
        }
        return endpoint;
    }

    // -- Common
    private KBObject writeEndpoint (Endpoint p) {
        KBObject KBEndpoint = domainKB.createObjectOfClass(p.getId(), DISKOnt.getClass(DISK.ENDPOINT));
        domainKB.setPropertyValue(KBEndpoint, DISKOnt.getProperty(DISK.HAS_SOURCE_NAME),
                domainKB.createLiteral(p.getName()));
        domainKB.setPropertyValue(KBEndpoint, DISKOnt.getProperty(DISK.HAS_SOURCE_URL),
                domainKB.createLiteral(p.getUrl()));
        return KBEndpoint;
    }

    private Endpoint loadEndpoint (KBObject endpoint) {
        KBObject name  = domainKB.getPropertyValue(endpoint, DISKOnt.getProperty(DISK.HAS_SOURCE_NAME));
        KBObject url  = domainKB.getPropertyValue(endpoint, DISKOnt.getProperty(DISK.HAS_SOURCE_URL));
        if (name == null || url == null)
            return null;
        return new Endpoint(name.getValueAsString(), url.getValueAsString(), endpoint.getID());
    }

    private KBObject writeEntity (Entity src) {
        // This assumes the entity does not exists
        KBObject KBEntity = domainKB.createObjectOfClass(src.getId(), DISKOnt.getClass(DISK.ENTITY));
        domainKB.setPropertyValue(KBEntity, DISKOnt.getProperty(DISK.HAS_NAME), domainKB.createLiteral(src.getName()));
        domainKB.setPropertyValue(KBEntity, DISKOnt.getProperty(DISK.HAS_EMAIL), domainKB.createLiteral(src.getEmail()));
        return KBEntity;
    }

    private Entity loadEntity (KBObject author) {
        KBObject name  = domainKB.getPropertyValue(author, DISKOnt.getProperty(DISK.HAS_NAME));
        KBObject email = domainKB.getPropertyValue(author, DISKOnt.getProperty(DISK.HAS_EMAIL));
        if (name == null || email == null)
            return null;
        return new Entity(author.getID(), name.getValueAsString(), email.getValueAsString());
    }

    public Entity loadOrRegisterEntity (String email) {
        int id = email.hashCode();
        this.rdf.startRead();
        KBObject KBEntity = domainKB.getIndividual(createEntityURI(id));
        Entity entity = loadEntity(KBEntity);
        this.rdf.end();
        if (entity == null) {
            String name = email.split("@")[0];
            entity = new Entity(String.valueOf(id), name, email);
            this.rdf.startWrite();
            this.writeEntity(entity);
            this.rdf.save(domainKB);
            this.rdf.end();
        }
        return entity;
    }

    private KBObject writeCommonResource (DISKResource obj, String uri, KBObject cls) {
        //This writes name, description, notes, date created and date modified. The object is of class `cls`
        KBObject kbObj = domainKB.createObjectOfClass(uri, cls);
        if (obj.getName() != null)
            domainKB.setLabel(kbObj, obj.getName());
        if (obj.getDescription() != null)
            domainKB.setComment(kbObj, obj.getDescription());
        if (obj.getDateCreated() != null)
            domainKB.setPropertyValue(kbObj, DISKOnt.getProperty(DISK.DATE_CREATED),
                    domainKB.createLiteral(obj.getDateCreated()));
        if (obj.getDateModified() != null)
            domainKB.setPropertyValue(kbObj, DISKOnt.getProperty(DISK.DATE_MODIFIED),
                    domainKB.createLiteral(obj.getDateModified()));
        if (obj.getNotes() != null)
            domainKB.setPropertyValue(kbObj, DISKOnt.getProperty(DISK.HAS_USAGE_NOTES),
                    domainKB.createLiteral(obj.getNotes()));
        if (obj.getAuthor() != null) {
            // At this point entity already exists
            domainKB.setPropertyValue(kbObj, DISKOnt.getProperty(DISK.HAS_AUTHOR),
                    domainKB.getIndividual(obj.getAuthor().getId()));
        }
        return kbObj;
    }

    private DISKResource loadCommonResource (KBObject item) {
        DISKResource current = new DISKResource(item.getID(), domainKB.getLabel(item), domainKB.getComment(item));
        KBObject created = domainKB.getPropertyValue(item, DISKOnt.getProperty(DISK.DATE_CREATED));
        KBObject updated = domainKB.getPropertyValue(item, DISKOnt.getProperty(DISK.DATE_MODIFIED));
        KBObject notes   = domainKB.getPropertyValue(item, DISKOnt.getProperty(DISK.HAS_USAGE_NOTES));
        KBObject author  = domainKB.getPropertyValue(item, DISKOnt.getProperty(DISK.HAS_AUTHOR));
        if (created != null) current.setDateCreated(created.getValueAsString());
        if (updated != null) current.setDateModified(updated.getValueAsString());
        if (notes != null)   current.setNotes(notes.getValueAsString());
        if (author != null)  current.setAuthor(loadEntity(author));
        return current;
    }

    private List<String> listObjectIdPerClass (KBObject cls) {
        List<String> ids = new ArrayList<String>();
        this.rdf.startRead();
        KBObject typeProp = domainKB.getProperty(KBConstants.RDF_NS + "type");
        for (KBTriple t : domainKB.genericTripleQuery(null, typeProp, cls)) {
            ids.add(t.getSubject().getID());
        }
        this.rdf.end();
        return ids;
    }

    // -- Goals
    public Goal AddOrUpdateGoal (Goal goal, String id) {
        // Check required inputs
        String name = goal.getName();
        String desc = goal.getDescription();
        String questionId = goal.getQuestion().getId();
        boolean isCreating = id == null || id.equals("");
        // Set or update date
        String now = dateformatter.format(new Date());
        if (isCreating) {
            goal.setDateCreated(now);
        } else {
            goal.setDateModified(now);
        }
        if (name == null || desc == null || questionId == null || "".equals(name) || "".equals(desc) || "".equals(questionId)) {
            // These are necessary fields, should send an appropriate exception here.
            return null;
        } else {
            if ((isCreating || (getLocalId(goal.getId()).equals(id) && deleteGoal(id))) && writeGoal(goal)) {
                return isCreating ? goal : loadGoal(id);
            }
        }
        return null;
    }

    public boolean writeGoal(Goal goal) {
        Boolean newGoal = goal.getId() == null || goal.getId().equals("");
        if (newGoal) goal.setId(createGoalURI(GUID.randomId("Goal")));
        String fullId = goal.getId();
        //if (domainKB == null) return false;
        this.rdf.startWrite();
        KBObject goalItem = this.writeCommonResource(goal, fullId, DISKOnt.getClass(DISK.GOAL));
        // Writting question template bindings.
        String questionId = goal.getQuestion().getId();
        if (questionId != null)
            domainKB.setPropertyValue(goalItem, DISKOnt.getProperty(DISK.HAS_QUESTION),
                    domainKB.createLiteral(questionId));
        List<VariableBinding> questionBindings = goal.getQuestionBindings();
        if (questionBindings != null && questionBindings.size() > 0) {
            for (VariableBinding vb : questionBindings) {
                domainKB.addPropertyValue(goalItem, DISKOnt.getProperty(DISK.HAS_QUESTION_BINDINGS), 
                    writeVariableBinding(vb, fullId));
            }
        }

        this.rdf.save(domainKB);
        this.rdf.end();

        // Store hypothesis as a graph
        KBAPI goalKB = getOrCreateKB(fullId);
        if (goalKB == null) {
            // Already exists a goal with this ID.
            System.err.println("A Goal graph with this ID already exists: " + fullId +  " -- Aborted");
            return false;
        }

        this.rdf.startWrite();
        // This can be created server side too. maybe is better that way
        for (Triple triple : completeGraphWithDomains(goal.getGraph(), fullId, "goal")) {
            KBTriple curTriple = tripleToKBTriple(triple, goalKB);
            if (curTriple != null) {
                goalKB.addTriple(curTriple);
            }
        }
        this.rdf.save(goalKB);
        this.rdf.end();

        return true;
    }

    public Goal loadGoal (String id) {
        String goalId = createGoalURI(id);
        //if (domainKB == null) return null;

        this.rdf.startRead();
        KBObject goalItem = domainKB.getIndividual(goalId);
        Graph graph = this.loadGraphFromKB(goalId);
        if (goalItem == null || graph == null) {
            this.rdf.end();
            return null;
        }

        Goal goal = new Goal(loadCommonResource(goalItem));
        goal.setGraph(graph);

        // Question template info
        KBObject questionobj = domainKB.getPropertyValue(goalItem, DISKOnt.getProperty(DISK.HAS_QUESTION));
        if (questionobj != null) {
            String questionId = questionobj.getValueAsString();
            goal.setQuestion(new Question(questionId));
        }

        //Load question template binding values.
        ArrayList<KBObject> questionBindings = domainKB.getPropertyValues(goalItem, DISKOnt.getProperty(DISK.HAS_QUESTION_BINDINGS));
        List<VariableBinding> variableBindings = new ArrayList<VariableBinding>();
        if (questionBindings != null) {
            for (KBObject binding : questionBindings) {
                variableBindings.add(loadVariableBinding(binding));
            }
        }
        goal.setQuestionBindings(variableBindings);
        this.rdf.end();
        return goal;
    }

    public boolean deleteGoal(String id) {
        if (id == null)
            return false;

        String goalId = createGoalURI(id);
        KBAPI hypKB = getKB(goalId);

        if (domainKB != null && hypKB != null) {
            this.rdf.startRead();
            KBObject hypitem = domainKB.getIndividual(goalId);
            if (hypitem != null) {
                ArrayList<KBObject> questionBindings = domainKB.getPropertyValues(hypitem,
                        DISKOnt.getProperty(DISK.HAS_QUESTION_BINDINGS));
                this.rdf.end();
                // Remove question template bindings
                this.rdf.startWrite();
                if (questionBindings != null)
                    for (KBObject binding : questionBindings) {
                        domainKB.deleteObject(binding, true, true);
                    }
                domainKB.deleteObject(hypitem, true, true);
                this.rdf.save(domainKB);
                this.rdf.end();
            } else {
                this.rdf.end();
            }

            return this.rdf.startWrite() && hypKB.delete() && this.rdf.save(hypKB) && this.rdf.end();
        }
        return false;
    }

    public List<Goal> listGoals () {
        List<Goal> list = new ArrayList<Goal>();
        List<String> goalIds = listObjectIdPerClass(DISKOnt.getClass(DISK.GOAL));
        for (String fullId: goalIds) {
            String id = getLocalId(fullId);
            list.add(this.loadGoal(id));
        }
        return list;
    }

    // -- Data query template
    private KBObject writeDataQueryTemplate (DataQueryTemplate dataQuery)  {
        KBObject dq = domainKB.createObjectOfClass(GUID.randomId("dqt"), DISKOnt.getClass(DISK.DATA_QUERY_TEMPLATE));
        return _writeDataQueryTemplate(dataQuery, dq);
    }

    private KBObject _writeDataQueryTemplate (DataQueryTemplate dataQuery, KBObject dq)  {
        if (dataQuery.getDescription() != null)
            domainKB.setComment(dq, dataQuery.getDescription());
        if (dataQuery.getEndpoint() != null)
            domainKB.setPropertyValue(dq, DISKOnt.getProperty(DISK.HAS_DATA_SOURCE), domainKB.getIndividual(dataQuery.getEndpoint().getId()));
        if (dataQuery.getTemplate() != null)
            domainKB.setPropertyValue(dq, DISKOnt.getProperty(DISK.HAS_QUERY_TEMPLATE), domainKB.createLiteral(dataQuery.getTemplate()));
        if (dataQuery.getVariablesToShow() != null)
            domainKB.setPropertyValue(dq, DISKOnt.getProperty(DISK.HAS_TABLE_VARIABLES), domainKB.createLiteral(dataQuery.getVariablesToShow()));
        if (dataQuery.getFootnote() != null)
            domainKB.setPropertyValue(dq, DISKOnt.getProperty(DISK.HAS_TABLE_DESCRIPTION), domainKB.createLiteral(dataQuery.getFootnote()));
        return dq;
    }

    private DataQueryTemplate loadDataQueryTemplate (KBObject objTemplate)  {
        KBObject endpointObj = domainKB.getPropertyValue(objTemplate, DISKOnt.getProperty(DISK.HAS_DATA_SOURCE));
        KBObject templateObj = domainKB.getPropertyValue(objTemplate, DISKOnt.getProperty(DISK.HAS_QUERY_TEMPLATE));
        DataQueryTemplate dataQuery = new DataQueryTemplate(templateObj.getValueAsString(), loadEndpoint(endpointObj));
        if (domainKB.getComment(objTemplate) != null)
            dataQuery.setDescription(domainKB.getComment(objTemplate));
        KBObject objVars = domainKB.getPropertyValue(objTemplate, DISKOnt.getProperty(DISK.HAS_TABLE_VARIABLES));
        if (objVars != null)
            dataQuery.setVariablesToShow(objVars.getValueAsString());
        KBObject objFootnotes = domainKB.getPropertyValue(objTemplate, DISKOnt.getProperty(DISK.HAS_TABLE_DESCRIPTION));
        if (objFootnotes != null)
            dataQuery.setFootnote(objFootnotes.getValueAsString());
        return dataQuery;
    }

    private KBObject writeDataQueryResults (DataQueryResult queryResults) {
        KBObject dq = domainKB.createObjectOfClass(GUID.randomId("dqr"), DISKOnt.getClass(DISK.DATA_QUERY_RESULTS));
        KBObject qr = _writeDataQueryTemplate(queryResults, dq);
        if (queryResults.getQuery() != null)
            domainKB.setPropertyValue(qr, DISKOnt.getProperty(DISK.HAS_QUERY), domainKB.createLiteral(queryResults.getQuery()));
        if (queryResults.getResults() != null)
            domainKB.setPropertyValue(qr, DISKOnt.getProperty(DISK.HAS_RESULT), domainKB.createLiteral(queryResults.getResults()));
        return qr;
    }

    private DataQueryResult loadDataQueryResult (KBObject objResult) {
        DataQueryResult result = new DataQueryResult(loadDataQueryTemplate(objResult));
        KBObject objQuery = domainKB.getPropertyValue(objResult, DISKOnt.getProperty(DISK.HAS_QUERY));
        if (objQuery != null)
            result.setQuery(objQuery.getValueAsString());
        KBObject rawResult = domainKB.getPropertyValue(objResult, DISKOnt.getProperty(DISK.HAS_RESULT));
        if (rawResult != null)
            result.setResults(rawResult.getValueAsString());
        return result;
    }

    // -- Line of inquiry
    public LineOfInquiry AddOrUpdateLOI (LineOfInquiry loi, String id) {
        // Check required inputs
        String name = loi.getName();
        String desc = loi.getDescription();
        String question = loi.getQuestion().getId();
        boolean isCreating = id == null || id.equals("");
        // Set or update date
        String now = dateformatter.format(new Date());
        if (isCreating) {
            loi.setDateCreated(now);
        } else {
            loi.setDateModified(now);
        }
        if (name == null || desc == null || question == null || "".equals(name) || "".equals(desc) || "".equals(question)) {
            // These are necessary fields, should send an appropriate exception here.
            return null;
        } else {
            if ((isCreating || (getLocalId(loi.getId()).equals(id) && deleteLOI(id))) && writeLOI(loi)) {
                return isCreating ? loi : loadLOI(id);
            }
        }
        return null;
    }

    public boolean writeLOI(LineOfInquiry loi) {
        Boolean newLOI = loi.getId() == null || loi.getId().equals("");
        if (newLOI) loi.setId(createLoiURI(GUID.randomId("LOI")));
        String loiId = loi.getId();
        //if (domainKB == null) return false;
        this.rdf.startWrite();

        KBObject loiItem = writeCommonResource(loi, loiId, DISKOnt.getClass(DISK.LINE_OF_INQUIRY));
        writeLOIExtras(loi, loiItem);
        this.rdf.save(domainKB);
        this.rdf.end();

        return true;
    }

    public LineOfInquiry loadLOI(String id) {
        String loiId = createLoiURI(id);
        //if (domainKB == null) return null;

        this.rdf.startRead();
        KBObject loiItem = domainKB.getIndividual(loiId);
        if (loiItem == null) {
            this.rdf.end();
            return null;
        }

        LineOfInquiry loi = new LineOfInquiry(loadCommonResource(loiItem));
        loadLOIExtras(loi, loiItem);
        this.rdf.end();
        return loi;
    }

    private void writeLOIExtras (LineOfInquiry loi, KBObject loiItem) {
        domainKB.setPropertyValue(loiItem, DISKOnt.getProperty(DISK.HAS_UPDATE_CONDITION),
                domainKB.createLiteral(loi.getUpdateCondition()));
        if (loi.getGoalQuery() != null) {
            domainKB.setPropertyValue(loiItem, DISKOnt.getProperty(DISK.HAS_GOAL_QUERY),
                    domainKB.createLiteral(loi.getGoalQuery()));
        }
        if (loi.getDataQueryTemplate() != null) {
            KBObject dqt = writeDataQueryTemplate(loi.getDataQueryTemplate());
            domainKB.setPropertyValue(loiItem, DISKOnt.getProperty(DISK.HAS_DATA_QUERY), dqt);
        }
        String questionId = loi.getQuestion().getId();
        if (questionId != null)
            domainKB.setPropertyValue(loiItem, DISKOnt.getProperty(DISK.HAS_QUESTION),
                    domainKB.createLiteral(questionId));
        List<WorkflowSeed> wf = loi.getWorkflowSeeds(), mwf = loi.getMetaWorkflowSeeds();
        if (wf != null && wf.size() > 0) {
            for (WorkflowSeed wfSeed: wf) {
                domainKB.addPropertyValue(loiItem, DISKOnt.getProperty(DISK.HAS_WORKFLOW_SEED), 
                    writeWorkflowSeed(wfSeed, loi.getId()));
            }
        }
        if (mwf != null && mwf.size() > 0) {
            for (WorkflowSeed wfSeed: mwf) {
                domainKB.addPropertyValue(loiItem, DISKOnt.getProperty(DISK.HAS_META_WORKFLOW_SEED), 
                    writeWorkflowSeed(wfSeed, loi.getId()));
            }
        }
    }

    private void loadLOIExtras (LineOfInquiry loi, KBObject loiItem) {
        KBObject goalQueryObj = domainKB.getPropertyValue(loiItem, DISKOnt.getProperty(DISK.HAS_GOAL_QUERY));
        if (goalQueryObj != null)
            loi.setGoalQuery(goalQueryObj.getValueAsString());
        KBObject dataQueryObj = domainKB.getPropertyValue(loiItem, DISKOnt.getProperty(DISK.HAS_DATA_QUERY));
        if (dataQueryObj != null)
            loi.setDataQueryTemplate(loadDataQueryTemplate(dataQueryObj));
        KBObject questionobj = domainKB.getPropertyValue(loiItem, DISKOnt.getProperty(DISK.HAS_QUESTION));
        if (questionobj != null)
            loi.setQuestion(new Question(questionobj.getValueAsString()));
        KBObject updateCondObj = domainKB.getPropertyValue(loiItem, DISKOnt.getProperty(DISK.HAS_UPDATE_CONDITION));
        if (updateCondObj != null)
            loi.setUpdateCondition( Integer.parseInt(updateCondObj.getValueAsString()) );

        List<KBObject> wfSeeds  = domainKB.getPropertyValues(loiItem, DISKOnt.getProperty(DISK.HAS_WORKFLOW_SEED));
        List<KBObject> mwfSeeds = domainKB.getPropertyValues(loiItem, DISKOnt.getProperty(DISK.HAS_META_WORKFLOW_SEED));
        List<WorkflowSeed> wList = new ArrayList<WorkflowSeed>(), mList = new ArrayList<WorkflowSeed>();

        if (wfSeeds != null && wfSeeds.size() > 0) {
            for (KBObject t: wfSeeds) {
                wList.add(loadWorkflowSeed(t));
            }
        }
        if (mwfSeeds != null && mwfSeeds.size() > 0) {
            for (KBObject t: mwfSeeds) {
                mList.add(loadWorkflowSeed(t));
            }
        }
        loi.setWorkflowSeeds(wList);
        loi.setMetaWorkflowSeeds(mList);
    }

    public boolean deleteLOI(String id) {
        if (id == null)
            return false;

        String loiId = createLoiURI(id);
        //if (domainKB == null) return false;

        this.rdf.startWrite();
        KBObject hypitem = domainKB.getIndividual(loiId);
        if (hypitem != null)
            domainKB.deleteObject(hypitem, true, true);
            //TODO: remove query object too.

        return this.rdf.save(domainKB) && this.rdf.end();
    }

    public List<LineOfInquiry> listLOIPreviews() {
        List<LineOfInquiry> list = new ArrayList<LineOfInquiry>();
        List<String> ids = listObjectIdPerClass(DISKOnt.getClass(DISK.LINE_OF_INQUIRY));

        for (String fullId: ids) {
            String id = getLocalId(fullId);
            list.add(this.loadLOI(id));
        }
        return list;
    }

    // -- Workflow Seeds and execution
    private KBObject writeWorkflowSeed (WorkflowSeed seed, String parentId) {
        String prefix = parentId != null ? parentId + "/seeds/" : null;
        KBObject seedObj = domainKB.createObjectOfClass(prefix != null ? prefix + GUID.randomId("") : null , DISKOnt.getClass(DISK.WORKFLOW_SEED));
        return _writeWorkflowSeed(seed, seedObj);
    }

    private KBObject _writeWorkflowSeed (WorkflowSeed seed, KBObject seedObj) {
        if (seed.getName() != null)
            domainKB.setLabel(seedObj, seed.getName());
        if (seed.getDescription() != null)
            domainKB.setComment(seedObj, seed.getDescription());
        if (seed.getSource() != null)
            domainKB.setPropertyValue(seedObj, DISKOnt.getProperty(DISK.HAS_WORKFLOW_SOURCE), domainKB.getResource(seed.getSource().getId()));
        if (seed.getLink() != null)
            domainKB.setPropertyValue(seedObj, DISKOnt.getProperty(DISK.HAS_WORKFLOW), domainKB.createLiteral(seed.getLink()) );
        
        List<VariableBinding> parameters = seed.getParameters(), inputs = seed.getInputs(), outputs = seed.getOutputs();
        if (parameters != null && parameters.size() > 0) {
            for (VariableBinding vBinding: parameters) {
                domainKB.addPropertyValue(seedObj, DISKOnt.getProperty(DISK.HAS_PARAMETER), 
                    writeVariableBinding(vBinding, seedObj.getID()));
            }
        }
        if (inputs != null && inputs.size() > 0) {
            for (VariableBinding vBinding: inputs) {
                domainKB.addPropertyValue(seedObj, DISKOnt.getProperty(DISK.HAS_INPUT), 
                    writeVariableBinding(vBinding, seedObj.getID()));
            }
        }
        if (outputs != null && outputs.size() > 0) {
            for (VariableBinding vBinding: outputs) {
                domainKB.addPropertyValue(seedObj, DISKOnt.getProperty(DISK.HAS_OUTPUT), 
                    writeVariableBinding(vBinding, seedObj.getID()));
            }
        }
        return seedObj;
    }

    private WorkflowSeed loadWorkflowSeed (KBObject seedObj) {
        WorkflowSeed seed = new WorkflowSeed();
        String name = domainKB.getLabel(seedObj);
        if (name != null) seed.setName(name);
        String comment = domainKB.getComment(seedObj);
        if (comment != null) seed.setDescription(comment);
        KBObject source = domainKB.getPropertyValue(seedObj, DISKOnt.getProperty(DISK.HAS_WORKFLOW_SOURCE));
        if (source != null) seed.setSource(loadEndpoint(source));
        KBObject link = domainKB.getPropertyValue(seedObj, DISKOnt.getProperty(DISK.HAS_WORKFLOW));
        if (link != null) seed.setLink(link.getValueAsString());

        List<KBObject> params = domainKB.getPropertyValues(seedObj, DISKOnt.getProperty(DISK.HAS_PARAMETER));
        List<KBObject> inputs = domainKB.getPropertyValues(seedObj, DISKOnt.getProperty(DISK.HAS_INPUT));
        List<KBObject> outputs= domainKB.getPropertyValues(seedObj, DISKOnt.getProperty(DISK.HAS_OUTPUT));
        List<VariableBinding> pList = new ArrayList<VariableBinding>(), iList = new ArrayList<VariableBinding>(),
                oList = new ArrayList<VariableBinding>();
        if (params != null && params.size() > 0) {
            for (KBObject t: params) {
                pList.add(loadVariableBinding(t));
            }
        }
        if (inputs != null && inputs.size() > 0) {
            for (KBObject t: inputs) {
                iList.add(loadVariableBinding(t));
            }
        }
        if (outputs != null && outputs.size() > 0) {
            for (KBObject t: outputs) {
                oList.add(loadVariableBinding(t));
            }
        }
        seed.setParameters(pList);
        seed.setInputs(iList);
        seed.setOutputs(oList);
        return seed;
    }

    private KBObject writeWorkflowInstantiation (WorkflowInstantiation inst, String parentId) {
        String prefix = parentId != null ? parentId + "/instantiations/" : null;
        String fullId = prefix != null ? prefix + GUID.randomId("") : null;
        KBObject seedObj = domainKB.createObjectOfClass(fullId, DISKOnt.getClass(DISK.WORKFLOW_INSTANTIATION));
        KBObject instObj = _writeWorkflowSeed(inst, seedObj);
        String instId = instObj.getID();

        if (inst.getStatus() != null)
            domainKB.setPropertyValue(instObj, DISKOnt.getProperty(DISK.HAS_STATUS), domainKB.createLiteral(inst.getStatus().name()) );

        List<VariableBinding> data = inst.getDataBindings();
        if (data != null && data.size() > 0) {
            for (VariableBinding vBinding: data) {
                domainKB.addPropertyValue(instObj, DISKOnt.getProperty(DISK.HAS_DATA_BINDINGS), 
                    writeVariableBinding(vBinding, instId));
            }
        }

        List<Execution> execs = inst.getExecutions();
        if (execs != null && execs.size() > 0) {
            for (Execution exec: execs) {
                domainKB.addPropertyValue(instObj, DISKOnt.getProperty(DISK.HAS_EXECUTION), 
                    writeExecution(exec, instId));
            }
        }
        return instObj;
    }

    private WorkflowInstantiation loadWorkflowInstantiation (KBObject instObj) {
        WorkflowInstantiation inst = new WorkflowInstantiation(loadWorkflowSeed(instObj));

        KBObject status = domainKB.getPropertyValue(instObj, DISKOnt.getProperty(DISK.HAS_STATUS));
        if (status != null) inst.setStatus(Status.fromString(status.getValueAsString()));

        List<KBObject> data = domainKB.getPropertyValues(instObj, DISKOnt.getProperty(DISK.HAS_DATA_BINDINGS));
        List<VariableBinding> dList = new ArrayList<VariableBinding>();
        List<Execution> eList = new ArrayList<Execution>();
        if (data != null && data.size() > 0) {
            for (KBObject t: data) {
                dList.add(loadVariableBinding(t));
            }
        }
        List<KBObject> executions = domainKB.getPropertyValues(instObj, DISKOnt.getProperty(DISK.HAS_EXECUTION));
        if (executions != null && executions.size() > 0) {
            for (KBObject t: executions) {
                eList.add(loadExecution(t));
            }
        }
        inst.setDataBindings(dList);
        inst.setExecutions(eList);
        return inst;
    }

    private KBObject writeExecutionRecord (ExecutionRecord exec, String parentId) {
        String prefix = parentId != null ? parentId + "/executions/" : null;
        return _writeExecutionRecord(exec, domainKB.createObjectOfClass(
            prefix != null ? prefix + GUID.randomId("") : null,
            DISKOnt.getClass(DISK.EXECUTION_RECORD)));
    }

    private KBObject _writeExecutionRecord (ExecutionRecord exec, KBObject execObj) {
        if ( exec.getLog() != null )
            domainKB.setPropertyValue(execObj, DISKOnt.getProperty(DISK.HAS_LOG), domainKB.createLiteral(exec.getLog()));
        if ( exec.getStartDate() != null )
            domainKB.setPropertyValue(execObj, DISKOnt.getProperty(DISK.HAS_RUN_START_DATE), domainKB.createLiteral(exec.getStartDate()));
        if ( exec.getEndDate() != null )
            domainKB.setPropertyValue(execObj, DISKOnt.getProperty(DISK.HAS_RUN_END_DATE), domainKB.createLiteral(exec.getEndDate()));
        if (exec.getStatus() != null)
            domainKB.setPropertyValue(execObj, DISKOnt.getProperty(DISK.HAS_STATUS), domainKB.createLiteral(exec.getStatus().name()));
        return execObj;
    }

    private ExecutionRecord loadExecutionRecord (KBObject execObj) {
        ExecutionRecord record = new ExecutionRecord();
        KBObject log = domainKB.getPropertyValue(execObj, DISKOnt.getProperty(DISK.HAS_LOG));
        if (log != null) record.setLog(log.getValueAsString());
        KBObject startDate = domainKB.getPropertyValue(execObj, DISKOnt.getProperty(DISK.HAS_RUN_START_DATE));
        if (startDate != null) record.setStartDate(startDate.getValueAsString());
        KBObject endDate = domainKB.getPropertyValue(execObj, DISKOnt.getProperty(DISK.HAS_RUN_END_DATE));
        if (endDate != null) record.setEndDate(endDate.getValueAsString());
        KBObject status = domainKB.getPropertyValue(execObj, DISKOnt.getProperty(DISK.HAS_STATUS));
        if (status != null) record.setStatus(Status.fromString(status.getValueAsString()));
        return record;
    }

    private KBObject writeExecution (Execution exec, String parentId) {
        String prefix = parentId != null ? parentId + "/executions/" : null;
        String localId = prefix != null ? prefix + GUID.randomId("") : null;
        KBObject execObj = _writeExecutionRecord(exec, domainKB.createObjectOfClass(
            localId, DISKOnt.getClass(DISK.EXECUTION_RECORD))
        );

        if (exec.getExternalId() != null)
            domainKB.setPropertyValue(execObj, DISKOnt.getProperty(DISK.HAS_ID), domainKB.createLiteral(exec.getExternalId()));
        if (exec.getLink() != null)
            domainKB.setPropertyValue(execObj, DISKOnt.getProperty(DISK.HAS_RUN_LINK), domainKB.createLiteral(exec.getLink()));
        if (exec.getResult() != null) {
            domainKB.setPropertyValue(execObj, DISKOnt.getProperty(DISK.HAS_RESULT), writeGoalResult(exec.getResult(), parentId));
        }

        List<VariableBinding> inputs = exec.getInputs(), outputs = exec.getOutputs();
        if (inputs != null && inputs.size() > 0) {
            for (VariableBinding vBinding: inputs) {
                domainKB.addPropertyValue(execObj, DISKOnt.getProperty(DISK.HAS_INPUT_FILE), 
                    writeVariableBinding(vBinding, parentId));
            }
        }
        if (outputs != null && outputs.size() > 0) {
            for (VariableBinding vBinding: outputs) {
                domainKB.addPropertyValue(execObj, DISKOnt.getProperty(DISK.HAS_OUTPUT_FILE), 
                    writeVariableBinding(vBinding, parentId));
            }
        }

        List<ExecutionRecord> steps = exec.getSteps();
        if (steps != null && steps.size() > 0) {
            for (ExecutionRecord step: steps) {
                domainKB.addPropertyValue(execObj, DISKOnt.getProperty(DISK.HAS_STEP), 
                    writeExecutionRecord(step, parentId));
            }
        }
        return execObj;
    }

    private Execution loadExecution (KBObject execObj) {
        Execution execution = new Execution(loadExecutionRecord(execObj));
        KBObject externalId = domainKB.getPropertyValue(execObj, DISKOnt.getProperty(DISK.HAS_ID));
        KBObject link = domainKB.getPropertyValue(execObj, DISKOnt.getProperty(DISK.HAS_RUN_LINK));
        KBObject result = domainKB.getPropertyValue(execObj, DISKOnt.getProperty(DISK.HAS_RESULT));
        List<KBObject> inputs = domainKB.getPropertyValues(execObj, DISKOnt.getProperty(DISK.HAS_INPUT_FILE));
        List<KBObject> outputs = domainKB.getPropertyValues(execObj, DISKOnt.getProperty(DISK.HAS_OUTPUT_FILE));
        List<KBObject> steps = domainKB.getPropertyValues(execObj, DISKOnt.getProperty(DISK.HAS_STEP));
        List<VariableBinding> inList = new ArrayList<VariableBinding>();
        List<VariableBinding> outList = new ArrayList<VariableBinding>();
        List<ExecutionRecord> stepList = new ArrayList<ExecutionRecord>();

        if (externalId != null) execution.setExternalId(externalId.getValueAsString());
        if (link != null) execution.setLink(link.getValueAsString());
        if (result != null) execution.setResult(loadGoalResult(result));
        if (inputs != null && inputs.size() > 0) for (KBObject t: inputs) {
            inList.add(loadVariableBinding(t));
        }
        if (outputs != null && outputs.size() > 0) for (KBObject t: outputs) {
            outList.add(loadVariableBinding(t));
        }
        if (steps != null && steps.size() > 0) for (KBObject t: steps) {
            stepList.add(loadExecutionRecord(t));
        }
        execution.setInputs(inList);
        execution.setOutputs(outList);
        execution.setSteps(stepList);
        return execution;
    }

    private KBObject writeGoalResult (GoalResult result, String parentId) {
        String fullId = parentId != null ? parentId + "/results/" + GUID.randomId("") : null;
        KBObject resultObj = domainKB.createObjectOfClass(fullId, DISKOnt.getClass(DISK.GOAL_RESULT));

        if (result.getConfidenceType() != null)
            domainKB.setPropertyValue(resultObj, DISKOnt.getProperty(DISK.HAS_CONFIDENCE_TYPE),
                    domainKB.createLiteral(result.getConfidenceType()));
        if (result.getConfidenceValue() != null) {
            domainKB.setPropertyValue(resultObj, DISKOnt.getProperty(DISK.HAS_CONFIDENCE_VALUE),
                    domainKB.createLiteral(result.getConfidenceValue()));
        }
        List<VariableBinding> bindings = result.getExtras();
        if (bindings != null && bindings.size() > 0) {
            for (VariableBinding vBinding: bindings) {
                domainKB.addPropertyValue(resultObj, DISKOnt.getProperty(DISK.HAS_DATA_BINDINGS), 
                    writeVariableBinding(vBinding, fullId));
            }
        }
        return resultObj;
    }

    private GoalResult loadGoalResult (KBObject resultObj) {
        GoalResult result = new GoalResult();
        KBObject typeObj = domainKB.getPropertyValue(resultObj, DISKOnt.getProperty(DISK.HAS_CONFIDENCE_TYPE));
        if (typeObj != null) result.setConfidenceType(typeObj.getValueAsString());
        KBObject valueObj = domainKB.getPropertyValue(resultObj, DISKOnt.getProperty(DISK.HAS_CONFIDENCE_VALUE));
        if (valueObj != null) result.setConfidenceValue(Double.parseDouble(valueObj.getValueAsString()));

        List<KBObject> bindings = domainKB.getPropertyValues(resultObj, DISKOnt.getProperty(DISK.HAS_DATA_BINDINGS));
        List<VariableBinding> list = new ArrayList<VariableBinding>();
        if (bindings != null && bindings.size() > 0) for (KBObject t: bindings) {
            list.add(loadVariableBinding(t));
        }
        result.setExtras(list);
        return result;
    }

    // -- Variable bindings
    private KBObject createLiteralFromBindingValue (VariableBinding binding) {
        if (binding.getIsArray()) {
            List<String> values = binding.getBindings();
            int size = values.size(), i = 0;
            String str = "";
            for (String value: values) {
                str += "'" + value.replaceAll("'","\'") + "'";
                if (++i != size) {
                    str += ",";
                }
            }
            return domainKB.createLiteral("[" + str + "]");
        }
        return domainKB.createLiteral(binding.getSingleBinding());
    }

    private void readLiteralAsBindingValue (String rawValue, VariableBinding vb) {
        if (rawValue.startsWith("[") && rawValue.endsWith("]")) {
            vb.setIsArray(true);
            String str = rawValue.substring(1, rawValue.length()-1);
            String[] values = str.split(",");
            List<String> list = new ArrayList<String>();
            for (String v: values) {
                if (v.startsWith("'") && v.endsWith("'")) {
                    list.add(v.substring(1, v.length()-1));
                } else {
                    //Whats happening is that we get the mean value array as in an array [[]]
                    System.out.println("Something when wrong decoding binding value:" + rawValue);
                    list.add(v);
                }
            }
            vb.setBindings(list);
        } else {
            vb.setIsArray(false);
            vb.setSingleBinding(rawValue);
        }
    }

    private KBObject writeVariableBinding (VariableBinding vBinding, String prefix) {
        String id = null;
        if (vBinding.getVariable() != null && prefix !=null && !prefix.equals("")) {
            String[] fragments = vBinding.getVariable().split("/"); //This generally is the variable name
            id = prefix + "/bindings/" + GUID.randomId(fragments[fragments.length-1]);
        }
        KBObject vBindingObj = domainKB.createObjectOfClass(id, DISKOnt.getClass(DISK.VARIABLE_BINDING));
        if (vBinding.getVariable() != null)
            domainKB.setPropertyValue(vBindingObj, DISKOnt.getProperty(DISK.HAS_BINDING_VARIABLE), domainKB.createLiteral(vBinding.getVariable()));
        if (vBinding.getDatatype() != null)
            domainKB.setPropertyValue(vBindingObj, DISKOnt.getProperty(DISK.HAS_DATATYPE), domainKB.createLiteral(vBinding.getDatatype()));
        if (vBinding.getBinding() != null)
            domainKB.setPropertyValue(vBindingObj, DISKOnt.getProperty(DISK.HAS_BINDING_VALUE), createLiteralFromBindingValue(vBinding));
        if (vBinding.getType() != null)
            domainKB.setPropertyValue(vBindingObj, DISKOnt.getProperty(DISK.HAS_TYPE), domainKB.createLiteral(vBinding.getType()));
        return vBindingObj;
    }

    private VariableBinding loadVariableBinding (KBObject bindingObj) {
        VariableBinding vb = new VariableBinding();
        KBObject variable = domainKB.getPropertyValue(bindingObj, DISKOnt.getProperty(DISK.HAS_BINDING_VARIABLE));
        if (variable != null) vb.setVariable(variable.getValueAsString());
        KBObject datatype = domainKB.getPropertyValue(bindingObj, DISKOnt.getProperty(DISK.HAS_DATATYPE));
        if (datatype != null) vb.setDatatype(datatype.getValueAsString());
        KBObject rawValue = domainKB.getPropertyValue(bindingObj, DISKOnt.getProperty(DISK.HAS_BINDING_VALUE));
        if (rawValue != null) readLiteralAsBindingValue(rawValue.getValueAsString(), vb);
        KBObject type = domainKB.getPropertyValue(bindingObj, DISKOnt.getProperty(DISK.HAS_TYPE));
        if (type != null) vb.setType(type.getValueAsString());
        return vb;
    }

    // -- Triggered Lines of Inquiry
    public TriggeredLOI addOrUpdateTLOI (TriggeredLOI tloi, String id) {
        boolean isCreating = id == null || id.equals("");
        // Set or update date
        String now = dateformatter.format(new Date());
        if (isCreating) {
            tloi.setStatus(Status.QUEUED);
            tloi.setDateCreated(now);
        } else {
            tloi.setDateModified(now);
        }

        if ((isCreating || deleteTLOI(id)) && writeTLOI(tloi)) {
            return tloi;
        }
        return null;
    }

    public boolean writeTLOI(TriggeredLOI tloi) {
        Boolean newTLOI = tloi.getId() == null || tloi.getId().equals("");
        if (newTLOI) tloi.setId(createTloiURI(GUID.randomId("TriggeredLOI")));
        String tloiId = tloi.getId();

        this.rdf.startWrite();
        KBObject tloiItem = writeCommonResource(tloi, tloiId, DISKOnt.getClass(DISK.TRIGGERED_LINE_OF_INQUIRY));
        writeLOIExtras(tloi, tloiItem);
        if (tloi.getParentLoi() != null) {
            KBObject loiObj = domainKB.getResource(tloi.getParentLoi().getId());
            domainKB.setPropertyValue(tloiItem, DISKOnt.getProperty(DISK.HAS_LINE_OF_INQUIRY), loiObj);
        }
        if (tloi.getParentGoal() != null) {
            KBObject hypObj = domainKB.getResource(tloi.getParentGoal().getId());
            domainKB.setPropertyValue(tloiItem, DISKOnt.getProperty(DISK.HAS_GOAL), hypObj);
        }
        if (tloi.getStatus() != null) {
            KBObject statusObj = domainKB.createLiteral(tloi.getStatus().name());
            domainKB.setPropertyValue(tloiItem, DISKOnt.getProperty(DISK.HAS_STATUS), statusObj);
        }
        if (tloi.getQueryResults() != null) {
            domainKB.setPropertyValue(tloiItem, DISKOnt.getProperty(DISK.HAS_QUERY_RESULTS),
                writeDataQueryResults(tloi.getQueryResults()));
        }

        List<WorkflowInstantiation> wf = tloi.getWorkflows(), mwf = tloi.getMetaWorkflows();
        if (wf != null && wf.size() > 0) {
            for (WorkflowInstantiation wfInst: wf) {
                domainKB.addPropertyValue(tloiItem, DISKOnt.getProperty(DISK.HAS_WORKFLOW_INST), 
                    writeWorkflowInstantiation(wfInst, tloi.getId()));
            }
        }
        if (mwf != null && mwf.size() > 0) {
            for (WorkflowInstantiation wfInst: mwf) {
                domainKB.addPropertyValue(tloiItem, DISKOnt.getProperty(DISK.HAS_META_WORKFLOW_INST), 
                    writeWorkflowInstantiation(wfInst, tloi.getId()));
            }
        }

        this.rdf.save(domainKB);
        this.rdf.end();
        return true;
    }

    public TriggeredLOI loadTLOI(String id) {
        String tloiId = createTloiURI(id);
        //if (domainKB == null) return null;

        this.rdf.startRead();
        KBObject obj = domainKB.getIndividual(tloiId);
        if (obj != null && obj.getName() != null) {
            TriggeredLOI tloi = new TriggeredLOI(loadCommonResource(obj));
            loadLOIExtras(tloi, obj);

            KBObject parentLOI = domainKB.getPropertyValue(obj, DISKOnt.getProperty(DISK.HAS_LINE_OF_INQUIRY));
            if (parentLOI != null) // We do not load the LOI
                tloi.setParentLoi(new LineOfInquiry(parentLOI.getValueAsString()));
            KBObject parentGoal = domainKB.getPropertyValue(obj, DISKOnt.getProperty(DISK.HAS_GOAL));
            if (parentGoal != null) // We do not load the LOI
                tloi.setParentGoal(new Goal(parentGoal.getValueAsString()));
            KBObject status = domainKB.getPropertyValue(obj, DISKOnt.getProperty(DISK.HAS_STATUS));
            if (status != null)
                tloi.setStatus(Status.fromString(status.getValueAsString()));
            KBObject queryResult = domainKB.getPropertyValue(obj, DISKOnt.getProperty(DISK.HAS_QUERY_RESULTS));
            if (queryResult != null)
                tloi.setQueryResults(loadDataQueryResult(queryResult));

            List<KBObject> wfInst = domainKB.getPropertyValues(obj, DISKOnt.getProperty(DISK.HAS_WORKFLOW_INST));
            List<KBObject> mwfInst = domainKB.getPropertyValues(obj, DISKOnt.getProperty(DISK.HAS_META_WORKFLOW_INST));
            List<WorkflowInstantiation> wfList = new ArrayList<WorkflowInstantiation>();
            List<WorkflowInstantiation> metaWfList = new ArrayList<WorkflowInstantiation>();

            if (wfInst != null && wfInst.size() > 0) {
                for (KBObject t : wfInst) {
                    wfList.add(loadWorkflowInstantiation(t));
                }
            }
            if (mwfInst != null && mwfInst.size() > 0) {
                for (KBObject t : mwfInst) {
                    metaWfList.add(loadWorkflowInstantiation(t));
                }
            }
            tloi.setWorkflows(wfList);
            tloi.setMetaWorkflows(metaWfList);

            this.rdf.end();
            return tloi;
        } else {
            this.rdf.end();
            return null;
        }
    }

    public boolean deleteTLOI(String id) {
        if (id == null)
            return false;

        String tloiId = createTloiURI(id);
        //if (domainKB == null) return false;

        // Remove this TLOI TODO missing some props
        this.rdf.startRead();
        KBObject item = domainKB.getIndividual(tloiId);
        this.rdf.end();
        if (item != null) {
            this.rdf.startWrite();
            domainKB.deleteObject(item, true, true);
            return this.rdf.save(domainKB) && this.rdf.end();
        } else {
            return false;
        }
    }

    public List<TriggeredLOI> listTLOIs() {
        List<TriggeredLOI> list = new ArrayList<TriggeredLOI>();
        List<String> ids = listObjectIdPerClass(DISKOnt.getClass(DISK.TRIGGERED_LINE_OF_INQUIRY));
        for (String fullId: ids) {
            String id = getLocalId(fullId);
            TriggeredLOI cur = this.loadTLOI(id);
            if (cur != null) {
                list.add(cur);
            }
        }
        return list;
    }
}