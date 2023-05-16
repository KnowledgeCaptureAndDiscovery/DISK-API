package org.diskproject.server.repository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.diskproject.shared.classes.adapters.MethodAdapter;
import org.diskproject.shared.classes.common.Graph;
import org.diskproject.shared.classes.common.Triple;
import org.diskproject.shared.classes.common.TripleDetails;
import org.diskproject.shared.classes.common.Value;
import org.diskproject.shared.classes.hypothesis.Hypothesis;
import org.diskproject.shared.classes.loi.LineOfInquiry;
import org.diskproject.shared.classes.loi.TriggeredLOI;
import org.diskproject.shared.classes.loi.TriggeredLOI.Status;
import org.diskproject.shared.classes.loi.WorkflowBindings;
import org.diskproject.shared.classes.util.GUID;
import org.diskproject.shared.classes.util.KBConstants;
import org.diskproject.shared.classes.workflow.VariableBinding;
import org.diskproject.shared.classes.workflow.WorkflowRun;
import org.diskproject.shared.ontologies.DISK;

import edu.isi.kcap.ontapi.KBAPI;
import edu.isi.kcap.ontapi.KBObject;
import edu.isi.kcap.ontapi.KBTriple;
import edu.isi.kcap.ontapi.OntSpec;

public class WriteKBRepository extends KBRepository {
    protected String _domain;

    private static SimpleDateFormat dateformatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");

    public void setDomain(String url) {
        this._domain = url;
    }

    public String DOMURI(String username) {
        return this._domain + "/" + username;
    }

    public String HYPURI(String username) {
        return this.DOMURI(username) + "/hypotheses";
    }

    public String LOIURI(String username) {
        return this.DOMURI(username) + "/lois";
    }

    public String TLOIURI(String username) {
        return this.DOMURI(username) + "/tlois";
    }

    private KBAPI getOrCreateKB(String url) {
        KBAPI kb = null;
        try {
            kb = this.fac.getKB(url, OntSpec.PLAIN, true);
        } catch (Exception e) {
            System.err.print("Could not open or create KB: " + url);
        }
        return kb;
    }

    private KBAPI getKB(String url) {
        KBAPI kb = null;
        try {
            kb = this.fac.getKB(url, OntSpec.PLAIN, false);
        } catch (Exception e) {
            System.err.print("Could not open KB: " + url);
        }
        return kb;
    }

    private KBObject getKBValue(Value v, KBAPI kb) {
        String dataType = v.getDatatype();
        Object value = v.getValue();
        if (v.getType() == Value.Type.LITERAL) {
            if (dataType != null)
                return kb.createXSDLiteral(value.toString(), v.getDatatype());
            else
                return kb.createLiteral(value.toString());
        } else {
            if (dataType != null) {
                //FIXME: if we assing a new datatype to V we must ensure is an URI
                //KBObject cls = kb.createClass(dataType);
                //return kb.createObjectOfClass(value.toString(), cls);
                return kb.getResource(value.toString());
            } else {
                return kb.getResource(value.toString());
            }
        }
    }

    private void setKBStatement(Triple triple, KBAPI kb, KBObject st) {
        KBObject subj = kb.getResource(triple.getSubject());
        KBObject pred = kb.getResource(triple.getPredicate());
        KBObject obj = getKBValue(triple.getObject(), kb);
        KBObject subprop = kb.getProperty(KBConstants.RDF_NS + "subject");
        KBObject predprop = kb.getProperty(KBConstants.RDF_NS + "predicate");
        KBObject objprop = kb.getProperty(KBConstants.RDF_NS + "object");
        kb.addTriple(st, subprop, subj);
        kb.addTriple(st, predprop, pred);
        kb.addTriple(st, objprop, obj);
    }

    // Deprecate this!
    private void storeTripleDetails(Triple triple, String provId, KBAPI provKB) {
        //TripleDetails details = triple.getDetails();
        //if (details != null) {
        //    KBObject stobj = provKB.getResource(provId + "#" + GUID.randomId("Statement"));
        //    this.setKBStatement(triple, provKB, stobj);

        //    if (details.getConfidenceValue() > 0)
        //        provKB.setPropertyValue(stobj, DISKOnt.getProperty(DISK.HAS_CONFIDENCE_VALUE),
        //                provKB.createLiteral(triple.getDetails().getConfidenceValue()));
        //    if (details.getConfidenceType() != null)
        //        provKB.setPropertyValue(stobj, DISKOnt.getProperty(DISK.HAS_CONFIDENCE_TYPE),
        //                provKB.createLiteral(triple.getDetails().getConfidenceType()));
        //    if (details.getTriggeredLOI() != null)
        //        provKB.setPropertyValue(stobj, DISKOnt.getProperty(DISK.HAS_TLOI),
        //                provKB.getResource(triple.getDetails().getTriggeredLOI()));
        //}
    }

    private Graph updateTripleDetails(Graph graph, KBAPI provKB) {
        HashMap<String, Triple> tripleMap = new HashMap<String, Triple>();
        for (Triple t : graph.getTriples())
            tripleMap.put(t.toString(), t);

        KBObject subprop = provKB.getProperty(KBConstants.RDF_NS + "subject");
        KBObject predprop = provKB.getProperty(KBConstants.RDF_NS + "predicate");
        KBObject objprop = provKB.getProperty(KBConstants.RDF_NS + "object");

        for (KBTriple kbt : provKB.genericTripleQuery(null, subprop, null)) {
            KBObject stobj = kbt.getSubject();
            KBObject subjobj = kbt.getObject();
            KBObject predobj = provKB.getPropertyValue(stobj, predprop);
            KBObject objobj = provKB.getPropertyValue(stobj, objprop);

            Value value = this.getObjectValue(objobj);
            Triple triple = new Triple();
            triple.setSubject(subjobj.getID());
            triple.setPredicate(predobj.getID());
            triple.setObject(value);

            String triplestr = triple.toString();
            if (tripleMap.containsKey(triplestr)) {
                Triple t = tripleMap.get(triplestr);

                KBObject conf = provKB.getPropertyValue(stobj, DISKOnt.getProperty(DISK.HAS_CONFIDENCE_VALUE));
                KBObject confidenceType = provKB.getPropertyValue(stobj, DISKOnt.getProperty(DISK.HAS_CONFIDENCE_TYPE));
                KBObject tloi = provKB.getPropertyValue(stobj, DISKOnt.getProperty(DISK.HAS_TLOI));

                TripleDetails details = new TripleDetails();
                if (conf != null && conf.getValue() != null)
                    details.setConfidenceValue((Double) conf.getValue());
                if (confidenceType != null && confidenceType.getValue() != null){
                    details.setConfidenceType((String) confidenceType.getValue());
                }
                if (tloi != null)
                    details.setTriggeredLOI(tloi.getID());

                // TODO:
                //t.setDetails(details);
            }
        }
        return graph;
    }

    private Value getObjectValue(KBObject obj) {
        Value v = new Value();
        if (obj.isLiteral()) {
            Object valobj = obj.getValue();
            if (valobj instanceof Date) {
                valobj = dateformatter.format((Date) valobj);
            }
            if (valobj instanceof String) {
                // Fix quotes and \n
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
        Graph graph = new Graph();
        KBAPI kb = getKB(url);
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
    }

    // --- Hypothesis
    protected boolean writeHypothesis(String username, Hypothesis hypothesis) {
        String userDomain = this.HYPURI(username);
        String hypothesisId = userDomain + "/" + hypothesis.getId();

        KBAPI userKB = getOrCreateKB(userDomain);

        if (userKB == null)
            return false;
        this.start_write();

        // Insert hypothesis info on user's graph
        KBObject hypitem = userKB.createObjectOfClass(hypothesisId, DISKOnt.getClass(DISK.HYPOTHESIS));

        if (hypothesis.getName() != null)
            userKB.setLabel(hypitem, hypothesis.getName());
        if (hypothesis.getDescription() != null)
            userKB.setComment(hypitem, hypothesis.getDescription());
        if (hypothesis.getDateCreated() != null)
            userKB.setPropertyValue(hypitem, DISKOnt.getProperty(DISK.DATE_CREATED),
                    userKB.createLiteral(hypothesis.getDateCreated()));
        if (hypothesis.getDateModified() != null)
            userKB.setPropertyValue(hypitem, DISKOnt.getProperty(DISK.DATE_MODIFIED),
                    userKB.createLiteral(hypothesis.getDateModified()));
        if (hypothesis.getAuthor() != null)
            userKB.setPropertyValue(hypitem, DISKOnt.getProperty(DISK.HAS_AUTHOR),
                    userKB.createLiteral(hypothesis.getAuthor()));
        if (hypothesis.getNotes() != null)
            userKB.setPropertyValue(hypitem, DISKOnt.getProperty(DISK.HAS_USAGE_NOTES),
                    userKB.createLiteral(hypothesis.getNotes()));

        // Adding parent hypothesis ID
        if (hypothesis.getParentId() != null) {
            String fullParentId = userDomain + "/" + hypothesis.getParentId();
            userKB.setPropertyValue(hypitem, DISKOnt.getProperty(DISK.HAS_PARENT_HYPOTHESIS),
                    userKB.getResource(fullParentId));
        }

        // Adding question template details
        if (hypothesis.getQuestionId() != null)
            userKB.setPropertyValue(hypitem, DISKOnt.getProperty(DISK.HAS_QUESTION),
                    userKB.createLiteral(hypothesis.getQuestionId()));
        List<VariableBinding> questionBindings = hypothesis.getQuestionBindings();
        if (questionBindings != null) {
            for (VariableBinding vb : questionBindings) {
                String ID = hypothesisId + "/bindings/";
                String[] sp = vb.getVariable().split("/");
                ID += sp[sp.length - 1];
                KBObject binding = userKB.createObjectOfClass(ID, DISKOnt.getClass(DISK.VARIABLE_BINDING));
                userKB.setPropertyValue(binding, DISKOnt.getProperty(DISK.HAS_VARIABLE),
                        userKB.createLiteral(vb.getVariable()));
                userKB.setPropertyValue(binding, DISKOnt.getProperty(DISK.HAS_BINDING_VALUE),
                        userKB.createLiteral(vb.getBinding()));
                userKB.addPropertyValue(hypitem, DISKOnt.getProperty(DISK.HAS_VARIABLE_BINDING), binding);
            }
        }

        this.save(userKB);
        this.end();

        // Store hypothesis graph
        KBAPI hypKb = getOrCreateKB(hypothesisId);
        if (hypKb == null)
            return false;

        this.start_write();
        for (Triple triple : hypothesis.getGraph().getTriples()) {
            KBObject subj = hypKb.getResource(triple.getSubject());
            KBObject pred = hypKb.getResource(triple.getPredicate());
            KBObject obj = getKBValue(triple.getObject(), hypKb);

            if (subj != null && pred != null && obj != null) {
                hypKb.addTriple(this.fac.getTriple(subj, pred, obj));
            }
        }
        this.save(hypKb);
        this.end();

        // FIXME: remove this way of getting the p-value
        String hypothesisProv = hypothesisId + "/provenance";
        KBAPI provKB = getOrCreateKB(hypothesisProv);
        if (provKB == null)
            return false;

        this.start_write();
        for (Triple triple : hypothesis.getGraph().getTriples()) {
            // Add triple details (confidence value, provenance, etc)
            this.storeTripleDetails(triple, hypothesisProv, provKB);
        }
        this.save(provKB);
        this.end();

        return true;
    }

    protected Hypothesis loadHypothesis(String username, String id) {
        String userDomain = this.HYPURI(username);
        String hypothesisId = userDomain + "/" + id;

        KBAPI userKB = getKB(userDomain);
        if (userKB == null)
            return null;

        this.start_read();

        KBObject hypitem = userKB.getIndividual(hypothesisId);
        Graph graph = this.getKBGraph(hypothesisId);
        if (hypitem == null || graph == null) {
            this.end();
            return null;
        }

        Hypothesis hypothesis = new Hypothesis();
        hypothesis.setId(id);
        hypothesis.setName(userKB.getLabel(hypitem));
        hypothesis.setDescription(userKB.getComment(hypitem));
        hypothesis.setGraph(graph);

        KBObject dateobj = userKB.getPropertyValue(hypitem, DISKOnt.getProperty(DISK.DATE_CREATED));
        if (dateobj != null)
            hypothesis.setDateCreated(dateobj.getValueAsString());

        KBObject dateModifiedObj = userKB.getPropertyValue(hypitem, DISKOnt.getProperty(DISK.DATE_MODIFIED));
        if (dateModifiedObj != null)
            hypothesis.setDateModified(dateModifiedObj.getValueAsString());

        KBObject authorobj = userKB.getPropertyValue(hypitem, DISKOnt.getProperty(DISK.HAS_AUTHOR));
        if (authorobj != null)
            hypothesis.setAuthor(authorobj.getValueAsString());

        KBObject notesobj = userKB.getPropertyValue(hypitem, DISKOnt.getProperty(DISK.HAS_USAGE_NOTES));
        if (notesobj != null)
            hypothesis.setNotes(notesobj.getValueAsString());

        // Parent hypothesis ID
        KBObject parentobj = userKB.getPropertyValue(hypitem, DISKOnt.getProperty(DISK.HAS_PARENT_HYPOTHESIS));
        if (parentobj != null)
            hypothesis.setParentId(parentobj.getName());

        // Question template info
        KBObject questionobj = userKB.getPropertyValue(hypitem, DISKOnt.getProperty(DISK.HAS_QUESTION));
        if (questionobj != null)
            hypothesis.setQuestionId(questionobj.getValueAsString());

        ArrayList<KBObject> questionBindings = userKB.getPropertyValues(hypitem,
                DISKOnt.getProperty(DISK.HAS_VARIABLE_BINDING));
        List<VariableBinding> variableBindings = new ArrayList<VariableBinding>();
        if (questionBindings != null) {
            for (KBObject binding : questionBindings) {
                KBObject kbVar = userKB.getPropertyValue(binding, DISKOnt.getProperty(DISK.HAS_VARIABLE));
                KBObject kbVal = userKB.getPropertyValue(binding, DISKOnt.getProperty(DISK.HAS_BINDING_VALUE));
                if (kbVar != null && kbVal != null) {
                    String var = kbVar.getValueAsString();
                    String val = kbVal.getValueAsString();
                    variableBindings.add(new VariableBinding(var, val));
                }
            }
        }
        hypothesis.setQuestionBindings(variableBindings);

        // FIXME: There are several problems on how I store prov.
        String provId = hypothesisId + "/provenance";
        KBAPI provKB = getKB(provId);
        if (provKB != null)
            this.updateTripleDetails(graph, provKB);

        this.end();
        return hypothesis;
    }

    protected boolean deleteHypothesis(String username, String id) {
        if (id == null)
            return false;

        String userDomain = this.HYPURI(username);
        String hypothesisId = userDomain + "/" + id;
        String provId = hypothesisId + "/provenance";

        KBAPI userKB = getKB(userDomain);
        KBAPI hypKB = getKB(hypothesisId);
        KBAPI provKB = getKB(provId);

        if (userKB != null && hypKB != null && provKB != null) {
            this.start_read();
            KBObject hypitem = userKB.getIndividual(hypothesisId);
            if (hypitem != null) {
                ArrayList<KBTriple> childHypotheses = userKB.genericTripleQuery(null,
                        DISKOnt.getProperty(DISK.HAS_PARENT_HYPOTHESIS), hypitem);
                ArrayList<KBObject> questionBindings = userKB.getPropertyValues(hypitem,
                        DISKOnt.getProperty(DISK.HAS_VARIABLE_BINDING));
                this.end();

                // Remove question template bindings
                this.start_write();
                if (questionBindings != null)
                    for (KBObject binding : questionBindings) {
                        userKB.deleteObject(binding, true, true);
                    }
                userKB.deleteObject(hypitem, true, true);
                this.save(userKB);
                this.end();

                // Remove all child hypotheses
                for (KBTriple t : childHypotheses) {
                    this.deleteHypothesis(username, t.getSubject().getName());
                }
            } else {
                this.end();
            }

            return this.start_write() && hypKB.delete() && this.save(hypKB) && this.end() &&
                    this.start_write() && provKB.delete() && this.save(provKB) && this.end();
        }
        return false;
    }

    protected List<Hypothesis> listHypothesesPreviews(String username) {
        List<Hypothesis> list = new ArrayList<Hypothesis>();
        String userDomain = this.HYPURI(username);

        KBAPI userKB = getKB(userDomain);
        if (userKB != null) {
            this.start_read();
            KBObject hypCls = DISKOnt.getClass(DISK.HYPOTHESIS);
            KBObject typeProp = userKB.getProperty(KBConstants.RDF_NS + "type");
            for (KBTriple t : userKB.genericTripleQuery(null, typeProp, hypCls)) {
                KBObject hypobj = t.getSubject();
                String name = userKB.getLabel(hypobj);
                String description = userKB.getComment(hypobj);

                String parentId = null;
                KBObject parentobj = userKB.getPropertyValue(hypobj, DISKOnt.getProperty(DISK.HAS_PARENT_HYPOTHESIS));
                if (parentobj != null)
                    parentId = parentobj.getName();

                String dateCreated = null;
                KBObject dateobj = userKB.getPropertyValue(hypobj, DISKOnt.getProperty(DISK.DATE_CREATED));
                if (dateobj != null)
                    dateCreated = dateobj.getValueAsString();

                String dateModified = null;
                KBObject dateModifiedObj = userKB.getPropertyValue(hypobj, DISKOnt.getProperty(DISK.DATE_MODIFIED));
                if (dateModifiedObj != null)
                    dateModified = dateModifiedObj.getValueAsString();

                String author = null;
                KBObject authorobj = userKB.getPropertyValue(hypobj, DISKOnt.getProperty(DISK.HAS_AUTHOR));
                if (authorobj != null)
                    author = authorobj.getValueAsString();

                String question = null;
                KBObject questionobj = userKB.getPropertyValue(hypobj, DISKOnt.getProperty(DISK.HAS_QUESTION));
                if (questionobj != null)
                    question = questionobj.getValueAsString();

                ArrayList<KBObject> questionBindings = userKB.getPropertyValues(hypobj,
                        DISKOnt.getProperty(DISK.HAS_VARIABLE_BINDING));
                List<VariableBinding> variableBindings = new ArrayList<VariableBinding>();
                if (questionBindings != null) {
                    for (KBObject binding : questionBindings) {
                        KBObject kbVar = userKB.getPropertyValue(binding, DISKOnt.getProperty(DISK.HAS_VARIABLE));
                        KBObject kbVal = userKB.getPropertyValue(binding, DISKOnt.getProperty(DISK.HAS_BINDING_VALUE));
                        if (kbVar != null && kbVal != null) {
                            String var = kbVar.getValueAsString();
                            String val = kbVal.getValueAsString();
                            variableBindings.add(new VariableBinding(var, val));
                        }
                    }
                }

                Hypothesis item = new Hypothesis(hypobj.getName(), name, description, parentId, null);
                if (dateCreated != null)
                    item.setDateCreated(dateCreated);
                if (dateModified != null)
                    item.setDateModified(dateModified);
                if (author != null)
                    item.setAuthor(author);
                if (question != null)
                    item.setQuestionId(question);

                if (variableBindings.size() > 0)
                    item.setQuestionBindings(variableBindings);

                list.add(item);
            }
            this.end();
        }
        return list;
    }

    // -- Line of inquiry
    protected boolean writeLOI(String username, LineOfInquiry loi) {
        Boolean newLOI = loi.getId() == null || loi.getId().equals("");
        if (newLOI) {
            loi.setId(GUID.randomId("LOI"));
        }

        String userDomain = this.LOIURI(username);
        String loiId = userDomain + "/" + loi.getId();
        KBAPI userKB = getOrCreateKB(userDomain);

        if (userKB == null)
            return false;

        this.start_write();

        KBObject loiItem = userKB.createObjectOfClass(loiId, DISKOnt.getClass(DISK.LOI));
        if (loi.getName() != null)
            userKB.setLabel(loiItem, loi.getName());
        if (loi.getDescription() != null)
            userKB.setComment(loiItem, loi.getDescription());
        if (loi.getDateCreated() != null)
            userKB.setPropertyValue(loiItem, DISKOnt.getProperty(DISK.DATE_CREATED),
                    userKB.createLiteral(loi.getDateCreated()));
        if (loi.getDateModified() != null)
            userKB.setPropertyValue(loiItem, DISKOnt.getProperty(DISK.DATE_MODIFIED),
                    userKB.createLiteral(loi.getDateModified()));
        if (loi.getAuthor() != null)
            userKB.setPropertyValue(loiItem, DISKOnt.getProperty(DISK.HAS_AUTHOR),
                    userKB.createLiteral(loi.getAuthor()));

        if (loi.getHypothesisQuery() != null)
            userKB.setPropertyValue(loiItem, DISKOnt.getProperty(DISK.HAS_HYPOTHESIS_QUERY),
                    userKB.createLiteral(loi.getHypothesisQuery()));
        if (loi.getDataQuery() != null)
            userKB.setPropertyValue(loiItem, DISKOnt.getProperty(DISK.HAS_DATA_QUERY),
                    userKB.createLiteral(loi.getDataQuery()));
        if (loi.getDataSource() != null)
            userKB.setPropertyValue(loiItem, DISKOnt.getProperty(DISK.HAS_DATA_SOURCE),
                    userKB.createLiteral(loi.getDataSource()));
        if (loi.getNotes() != null)
            userKB.setPropertyValue(loiItem, DISKOnt.getProperty(DISK.HAS_USAGE_NOTES),
                    userKB.createLiteral(loi.getNotes()));
        if (loi.getTableVariables() != null)
            userKB.setPropertyValue(loiItem, DISKOnt.getProperty(DISK.HAS_TABLE_VARIABLES),
                    userKB.createLiteral(loi.getTableVariables()));
        if (loi.getTableDescription() != null)
            userKB.setPropertyValue(loiItem, DISKOnt.getProperty(DISK.HAS_TABLE_DESCRIPTION),
                    userKB.createLiteral(loi.getTableDescription()));
        if (loi.getQuestionId() != null)
            userKB.setPropertyValue(loiItem, DISKOnt.getProperty(DISK.HAS_QUESTION),
                    userKB.createLiteral(loi.getQuestionId()));
        if (loi.getExplanation() != null)
            userKB.setPropertyValue(loiItem, DISKOnt.getProperty(DISK.HAS_DATA_QUERY_DESCRIPTION),
                    userKB.createLiteral(loi.getExplanation()));

        this.save(userKB);
        this.end();

        writeWorkflowsBindings(username, loi);
        writeMetaWorkflowsBindings(username, loi);

        return true;
    }

    protected LineOfInquiry loadLOI(String username, String id) {
        String userDomain = this.LOIURI(username);
        String loiId = userDomain + "/" + id;

        KBAPI userKB = getKB(userDomain);
        if (userKB == null)
            return null;

        this.start_read();
        /*
        KBObject typeprop = userKB.getProperty(KBConstants.RDF_NS + "type");
        for (KBTriple t : userKB.genericTripleQuery(null, typeprop, null)) {
            KBObject s = t.getSubject();
            KBObject o = t.getObject();
            if (o != null && !o.getValueAsString().startsWith("http://disk-project.org/ontology/disk#")) {
                System.out.println("> " + s + " = " + o);
                if (s!= null) {
                    this.end();
                    this.start_write();
                    userKB.removeTriple(t);
                    this.save(userKB);
                    this.end();
                    this.start_read();
                }
            }
        } */

        KBObject loiItem = userKB.getIndividual(loiId);
        if (loiItem == null) {
            this.end();
            return null;
        }

        LineOfInquiry loi = new LineOfInquiry();
        loi.setId(id);
        loi.setName(userKB.getLabel(loiItem));
        loi.setDescription(userKB.getComment(loiItem));

        KBObject dateobj = userKB.getPropertyValue(loiItem, DISKOnt.getProperty(DISK.DATE_CREATED));
        if (dateobj != null)
            loi.setDateCreated(dateobj.getValueAsString());

        KBObject datasourceobj = userKB.getPropertyValue(loiItem, DISKOnt.getProperty(DISK.HAS_DATA_SOURCE));
        if (datasourceobj != null)
            loi.setDataSource(datasourceobj.getValueAsString());

        KBObject dateModifiedObj = userKB.getPropertyValue(loiItem, DISKOnt.getProperty(DISK.DATE_MODIFIED));
        if (dateModifiedObj != null)
            loi.setDateModified(dateModifiedObj.getValueAsString());

        KBObject authorobj = userKB.getPropertyValue(loiItem, DISKOnt.getProperty(DISK.HAS_AUTHOR));
        if (authorobj != null)
            loi.setAuthor(authorobj.getValueAsString());

        KBObject notesobj = userKB.getPropertyValue(loiItem, DISKOnt.getProperty(DISK.HAS_USAGE_NOTES));
        if (notesobj != null)
            loi.setNotes(notesobj.getValueAsString());

        KBObject tableVarObj = userKB.getPropertyValue(loiItem, DISKOnt.getProperty(DISK.HAS_TABLE_VARIABLES));
        if (tableVarObj != null)
            loi.setTableVariables(tableVarObj.getValueAsString());

        KBObject tableDescObj = userKB.getPropertyValue(loiItem, DISKOnt.getProperty(DISK.HAS_TABLE_DESCRIPTION));
        if (tableDescObj != null)
            loi.setTableDescription(tableDescObj.getValueAsString());

        KBObject hypQueryObj = userKB.getPropertyValue(loiItem, DISKOnt.getProperty(DISK.HAS_HYPOTHESIS_QUERY));
        if (hypQueryObj != null)
            loi.setHypothesisQuery(hypQueryObj.getValueAsString());

        KBObject dataQueryObj = userKB.getPropertyValue(loiItem, DISKOnt.getProperty(DISK.HAS_DATA_QUERY));
        if (dataQueryObj != null)
            loi.setDataQuery(dataQueryObj.getValueAsString());

        KBObject questionobj = userKB.getPropertyValue(loiItem, DISKOnt.getProperty(DISK.HAS_QUESTION));
        if (questionobj != null)
            loi.setQuestionId(questionobj.getValueAsString());

        KBObject explanationObj = userKB.getPropertyValue(loiItem, DISKOnt.getProperty(DISK.HAS_DATA_QUERY_DESCRIPTION));
        if (explanationObj != null) {
            loi.setExplanation(explanationObj.getValueAsString());
        }

        this.end();

        loi.setWorkflows(loadWorkflowsBindings(userDomain, loi.getId()));
        loi.setMetaWorkflows(loadMetaWorkflowsBindings(userDomain, loi.getId()));

        return loi;
    }

    protected boolean deleteLOI(String username, String id) {
        if (id == null)
            return false;

        String userDomain = this.LOIURI(username);
        String loiId = userDomain + "/" + id;

        KBAPI userKB = getKB(userDomain);
        if (userKB == null)
            return false;

        this.start_write();
        KBObject hypitem = userKB.getIndividual(loiId);
        if (hypitem != null)
            userKB.deleteObject(hypitem, true, true);

        return this.save(userKB) && this.end();
    }

    protected List<LineOfInquiry> listLOIPreviews(String username) {
        List<LineOfInquiry> list = new ArrayList<LineOfInquiry>();
        String userDomain = this.LOIURI(username);
        KBAPI userKB = getKB(userDomain);

        if (userKB != null) {
            this.start_read();
            KBObject loiCls = DISKOnt.getClass(DISK.LOI);
            KBObject typeprop = userKB.getProperty(KBConstants.RDF_NS + "type");
            for (KBTriple t : userKB.genericTripleQuery(null, typeprop, loiCls)) {
                KBObject loiObj = t.getSubject();
                String name = userKB.getLabel(loiObj);
                String description = userKB.getComment(loiObj);

                KBObject dateobj = userKB.getPropertyValue(loiObj, DISKOnt.getProperty(DISK.DATE_CREATED));
                String dateCreated = (dateobj != null) ? dateobj.getValueAsString() : null;

                KBObject dateModifiedObj = userKB.getPropertyValue(loiObj, DISKOnt.getProperty(DISK.DATE_MODIFIED));
                String dateModified = (dateModifiedObj != null) ? dateModifiedObj.getValueAsString() : null;

                KBObject authorobj = userKB.getPropertyValue(loiObj, DISKOnt.getProperty(DISK.HAS_AUTHOR));
                String author = (authorobj != null) ? authorobj.getValueAsString() : null;

                KBObject questionobj = userKB.getPropertyValue(loiObj, DISKOnt.getProperty(DISK.HAS_QUESTION));
                String questionId =  (questionobj != null) ? questionobj.getValueAsString() : null;

                KBObject hypQueryObj = userKB.getPropertyValue(loiObj, DISKOnt.getProperty(DISK.HAS_HYPOTHESIS_QUERY));
                String hypothesisQuery = (hypQueryObj != null) ? hypQueryObj.getValueAsString() : null;

                LineOfInquiry item = new LineOfInquiry(loiObj.getName(), name, description);
                if (dateCreated != null)
                    item.setDateCreated(dateCreated);
                if (dateModified != null)
                    item.setDateModified(dateModified);
                if (author != null)
                    item.setAuthor(author);
                if (hypothesisQuery != null)
                    item.setHypothesisQuery(hypothesisQuery);
                if (questionId != null)
                    item.setQuestionId(questionId);

                list.add(item);
            }
            this.end();
        }
        return list;
    }

    // -- Triggered Lines of Inquiry
    protected boolean writeTLOI(String username, TriggeredLOI tloi) {

        Boolean newTLOI = tloi.getId() == null || tloi.getId().equals("");
        if (newTLOI)
            tloi.setId(GUID.randomId("TriggeredLOI"));

        String userDomain = this.TLOIURI(username);
        String tloiId = userDomain + "/" + tloi.getId();
        String hypNs = this.HYPURI(username) + "/";
        String loins = this.LOIURI(username) + "/";

        KBAPI userKB = getOrCreateKB(userDomain);
        if (userKB == null)
            return false;

        this.start_write();
        KBObject tloiItem = userKB.createObjectOfClass(tloiId, DISKOnt.getClass(DISK.TLOI));

        if (tloi.getName() != null)
            userKB.setLabel(tloiItem, tloi.getName());
        if (tloi.getDescription() != null)
            userKB.setComment(tloiItem, tloi.getDescription());
        if (tloi.getNotes() != null)
            userKB.setPropertyValue(tloiItem, DISKOnt.getProperty(DISK.HAS_USAGE_NOTES),
                    userKB.createLiteral(tloi.getNotes()));
        if (tloi.getDataSource() != null)
            userKB.setPropertyValue(tloiItem, DISKOnt.getProperty(DISK.HAS_DATA_SOURCE),
                    userKB.createLiteral(tloi.getDataSource()));
        if (tloi.getDateCreated() != null)
            userKB.setPropertyValue(tloiItem, DISKOnt.getProperty(DISK.DATE_CREATED),
                    userKB.createLiteral(tloi.getDateCreated()));
        if (tloi.getDateModified() != null)
            userKB.setPropertyValue(tloiItem, DISKOnt.getProperty(DISK.DATE_MODIFIED),
                    userKB.createLiteral(tloi.getDateModified()));
        if (tloi.getAuthor() != null)
            userKB.setPropertyValue(tloiItem, DISKOnt.getProperty(DISK.HAS_AUTHOR),
                    userKB.createLiteral(tloi.getAuthor()));
        if (tloi.getDataQuery() != null)
            userKB.setPropertyValue(tloiItem, DISKOnt.getProperty(DISK.HAS_DATA_QUERY),
                    userKB.createLiteral(tloi.getDataQuery()));
        if (tloi.getTableVariables() != null)
            userKB.setPropertyValue(tloiItem, DISKOnt.getProperty(DISK.HAS_TABLE_VARIABLES),
                    userKB.createLiteral(tloi.getTableVariables()));
        if (tloi.getTableDescription() != null)
            userKB.setPropertyValue(tloiItem, DISKOnt.getProperty(DISK.HAS_TABLE_DESCRIPTION),
                    userKB.createLiteral(tloi.getTableDescription()));
        if (tloi.getDataQueryExplanation() != null)
            userKB.setPropertyValue(tloiItem, DISKOnt.getProperty(DISK.HAS_DATA_QUERY_DESCRIPTION),
                    userKB.createLiteral(tloi.getDataQueryExplanation()));
        if (tloi.getConfidenceValue() > 0)
            userKB.setPropertyValue(tloiItem, DISKOnt.getProperty(DISK.HAS_CONFIDENCE_VALUE),
                    userKB.createLiteral(Double.toString(tloi.getConfidenceValue())));
        if (tloi.getConfidenceType() != null)
            userKB.setPropertyValue(tloiItem, DISKOnt.getProperty(DISK.HAS_CONFIDENCE_TYPE),
                    userKB.createLiteral(tloi.getConfidenceType()));
        if (tloi.getStatus() != null)
            userKB.setPropertyValue(tloiItem, DISKOnt.getProperty(DISK.HAS_TLOI_STATUS),
                    userKB.createLiteral(tloi.getStatus().toString()));
        if (tloi.getParentLoiId() != null) {
            KBObject loiObj = userKB.getResource(loins + tloi.getParentLoiId());
            userKB.setPropertyValue(tloiItem, DISKOnt.getProperty(DISK.HAS_LOI), loiObj);
        }
        if (tloi.getParentHypothesisId() != null) {
            KBObject hypObj = userKB.getResource(hypNs + tloi.getParentHypothesisId());
            userKB.setPropertyValue(tloiItem, DISKOnt.getProperty(DISK.HAS_PARENT_HYPOTHESIS), hypObj);
        }

        this.save(userKB);
        this.end();

        writeWorkflowsBindings(username, tloi);
        writeMetaWorkflowsBindings(username, tloi);

        return true;
    }

    protected TriggeredLOI loadTLOI(String username, String id) {
        String userDomain = this.TLOIURI(username);
        String tloiId = userDomain + "/" + id;

        KBAPI userKB = getKB(userDomain);
        if (userKB == null)
            return null;

        this.start_read();
        KBObject obj = userKB.getIndividual(tloiId);
        if (obj != null && obj.getName() != null) {
            TriggeredLOI tloi = new TriggeredLOI();
            tloi.setId(obj.getName());
            tloi.setName(userKB.getLabel(obj));
            tloi.setDescription(userKB.getComment(obj));
            KBObject hasLOI = userKB.getPropertyValue(obj, DISKOnt.getProperty(DISK.HAS_LOI));
            if (hasLOI != null)
                tloi.setParentLoiId(hasLOI.getName());

            KBObject parentHypObj = userKB.getPropertyValue(obj, DISKOnt.getProperty(DISK.HAS_PARENT_HYPOTHESIS));
            if (parentHypObj != null)
                tloi.setParentHypothesisId(parentHypObj.getName());

            KBObject stobj = userKB.getPropertyValue(obj, DISKOnt.getProperty(DISK.HAS_TLOI_STATUS));
            if (stobj != null)
                tloi.setStatus(Status.valueOf(stobj.getValue().toString()));

            KBObject notesObj = userKB.getPropertyValue(obj, DISKOnt.getProperty(DISK.HAS_USAGE_NOTES));
            if (notesObj != null)
                tloi.setNotes(notesObj.getValueAsString());

            KBObject dateobj = userKB.getPropertyValue(obj, DISKOnt.getProperty(DISK.DATE_CREATED));
            if (dateobj != null)
                tloi.setDateCreated(dateobj.getValueAsString());

            KBObject dateModifiedObj = userKB.getPropertyValue(obj, DISKOnt.getProperty(DISK.DATE_MODIFIED));
            if (dateModifiedObj != null)
                tloi.setDateModified(dateModifiedObj.getValueAsString());

            KBObject authorobj = userKB.getPropertyValue(obj, DISKOnt.getProperty(DISK.HAS_AUTHOR));
            if (authorobj != null)
                tloi.setAuthor(authorobj.getValueAsString());

            KBObject dataQueryObj = userKB.getPropertyValue(obj, DISKOnt.getProperty(DISK.HAS_DATA_QUERY));
            if (dataQueryObj != null)
                tloi.setDataQuery(dataQueryObj.getValueAsString());

            KBObject dataSourceObj = userKB.getPropertyValue(obj, DISKOnt.getProperty(DISK.HAS_DATA_SOURCE));
            if (dataSourceObj != null)
                tloi.setDataSource(dataSourceObj.getValueAsString());

            KBObject tableVarObj = userKB.getPropertyValue(obj, DISKOnt.getProperty(DISK.HAS_TABLE_VARIABLES));
            if (tableVarObj != null)
                tloi.setTableVariables(tableVarObj.getValueAsString());

            KBObject tableDescrObj = userKB.getPropertyValue(obj, DISKOnt.getProperty(DISK.HAS_TABLE_DESCRIPTION));
            if (tableDescrObj != null)
                tloi.setTableDescription(tableDescrObj.getValueAsString());

            KBObject explanationObj = userKB.getPropertyValue(obj, DISKOnt.getProperty(DISK.HAS_DATA_QUERY_DESCRIPTION));
            if (explanationObj != null)
                tloi.setDataQueryExplanation(explanationObj.getValueAsString());

            KBObject confidenceType = userKB.getPropertyValue(obj, DISKOnt.getProperty(DISK.HAS_CONFIDENCE_TYPE));
            if (confidenceType != null)
                tloi.setConfidenceType(confidenceType.getValueAsString());

            KBObject confidenceObj = userKB.getPropertyValue(obj, DISKOnt.getProperty(DISK.HAS_CONFIDENCE_VALUE));
            if (confidenceObj != null)
                tloi.setConfidenceValue(Double.valueOf(confidenceObj.getValueAsString()));

            this.end();

            tloi.setWorkflows(loadWorkflowsBindings(userDomain, tloi.getId()));
            tloi.setMetaWorkflows(loadMetaWorkflowsBindings(userDomain, tloi.getId()));

            return tloi;
        } else {
            this.end();
            return null;
        }
    }

    protected boolean deleteTLOI(String username, String id) {
        if (id == null)
            return false;

        String userDomain = this.TLOIURI(username);
        String tloiId = userDomain + "/" + id;

        KBAPI userKB = getKB(userDomain);
        if (userKB == null)
            return false;

        // Remove this TLOI
        this.start_read();
        KBObject item = userKB.getIndividual(tloiId);
        this.end();
        if (item != null) {
            this.start_write();
            userKB.deleteObject(item, true, true);
            return this.save(userKB) && this.end();
        } else {
            return false;
        }
    }

    protected List<TriggeredLOI> listTLOIs(String username) {
        List<TriggeredLOI> list = new ArrayList<TriggeredLOI>();
        String userDomain = this.TLOIURI(username);
        KBAPI userKB = getKB(userDomain);

        if (userKB != null) {
            List<String> tloiIds = new ArrayList<String>();

            this.start_read();
            KBObject cls = DISKOnt.getClass(DISK.TLOI);
            KBObject typeprop = userKB.getProperty(KBConstants.RDF_NS + "type");

            for (KBTriple t : userKB.genericTripleQuery(null, typeprop, cls)) {
                tloiIds.add(t.getSubject().getID());
            }
            this.end();

            for (String tloiId : tloiIds) {
                TriggeredLOI tloi = loadTLOI(username, tloiId.replaceAll("^.*\\/", ""));
                if (tloi != null)
                    list.add(tloi);
            }
        }
        return list;
    }

    // -- Workflows... or methods.
    private void writeWorkflowsBindings(String username, LineOfInquiry loi) {
        writeBindings(LOIURI(username), loi.getId(), DISKOnt.getProperty(DISK.HAS_WORKFLOW_BINDING),
                loi.getWorkflows());
    }

    private void writeWorkflowsBindings(String username, TriggeredLOI tloi) {
        writeBindings(TLOIURI(username), tloi.getId(), DISKOnt.getProperty(DISK.HAS_WORKFLOW_BINDING),
                tloi.getWorkflows());
    }

    private void writeMetaWorkflowsBindings(String username, LineOfInquiry loi) {
        writeBindings(LOIURI(username), loi.getId(), DISKOnt.getProperty(DISK.HAS_METAWORKFLOW_BINDING),
                loi.getMetaWorkflows());
    }

    private void writeMetaWorkflowsBindings(String username, TriggeredLOI tloi) {
        writeBindings(TLOIURI(username), tloi.getId(), DISKOnt.getProperty(DISK.HAS_METAWORKFLOW_BINDING),
                tloi.getMetaWorkflows());
    }

    private void writeBindings(String userDomain, String id, KBObject bindingprop,
            List<WorkflowBindings> bindingsList) {
        if (bindingsList == null || bindingsList.size() == 0)
            return;

        String fullId = userDomain + "/" + id;
        KBAPI userKB = getOrCreateKB(userDomain);

        if (userKB != null) {
            this.start_write();
            KBObject item = userKB.getIndividual(fullId); // This is a LOI or TLOI

            for (WorkflowBindings bindings : bindingsList) {
                String source = bindings.getSource();
                String description = bindings.getDescription();
                MethodAdapter methodAdapter = this.getMethodAdapterByName(source);
                if (methodAdapter == null) {
                    System.out.println("Method adapter not found " + source);
                    continue;
                }
                String workflowId = methodAdapter.getWorkflowId(bindings.getWorkflow());
                String workflowuri = methodAdapter.getWorkflowUri(bindings.getWorkflow());
                KBObject bindingobj = userKB.createObjectOfClass(null, DISKOnt.getClass(DISK.WORKFLOW_BINDING));
                userKB.addPropertyValue(item, bindingprop, bindingobj);

                userKB.setPropertyValue(bindingobj, DISKOnt.getProperty(DISK.HAS_WORKFLOW),
                        userKB.getResource(workflowId));
                userKB.setPropertyValue(bindingobj, DISKOnt.getProperty(DISK.HAS_SOURCE), userKB.createLiteral(source));
                if (description != null)
                    userKB.setComment(bindingobj, description);

                // Get Run details
                WorkflowRun run = bindings.getRun();
                if (run != null) {
                    if (run.getId() != null)
                        userKB.setPropertyValue(bindingobj, DISKOnt.getProperty(DISK.HAS_ID),
                                userKB.createLiteral(run.getId()));
                    if (run.getStatus() != null)
                        userKB.setPropertyValue(bindingobj, DISKOnt.getProperty(DISK.HAS_STATUS),
                                userKB.createLiteral(run.getStatus()));
                    if (run.getLink() != null)
                        userKB.setPropertyValue(bindingobj, DISKOnt.getProperty(DISK.HAS_RUN_LINK),
                                userKB.createLiteral(run.getLink()));
                    if (run.getStartDate() != null)
                        userKB.setPropertyValue(bindingobj, DISKOnt.getProperty(DISK.HAS_RUN_START_DATE),
                                userKB.createLiteral(run.getStartDate()));
                    if (run.getEndDate() != null)
                        userKB.setPropertyValue(bindingobj, DISKOnt.getProperty(DISK.HAS_RUN_END_DATE),
                                userKB.createLiteral(run.getEndDate()));

                    // Input Files
                    Map<String, String> inputs = run.getFiles();
                    if (inputs != null)
                        for (String name : inputs.keySet()) {
                            String url = inputs.get(name);
                            KBObject fileBinding = userKB.createObjectOfClass(null,
                                    DISKOnt.getClass(DISK.VARIABLE_BINDING));
                            userKB.setPropertyValue(fileBinding, DISKOnt.getProperty(DISK.HAS_VARIABLE),
                                    userKB.getResource(workflowuri + "#" + name.replaceAll(" ", "_")));
                            userKB.setPropertyValue(fileBinding, DISKOnt.getProperty(DISK.HAS_BINDING_VALUE),
                                    userKB.createLiteral(url));
                            userKB.addPropertyValue(bindingobj, DISKOnt.getProperty(DISK.HAS_INPUT_FILE), fileBinding);
                        }

                    // Output Files
                    Map<String, String> outputs = run.getOutputs();
                    if (outputs != null)
                        for (String name : outputs.keySet()) {
                            String url = outputs.get(name);
                            KBObject fileBinding = userKB.createObjectOfClass(null,
                                    DISKOnt.getClass(DISK.VARIABLE_BINDING));
                            userKB.setPropertyValue(fileBinding, DISKOnt.getProperty(DISK.HAS_VARIABLE),
                                    userKB.getResource(workflowuri + "#" + name.replaceAll(" ", "_")));
                            userKB.setPropertyValue(fileBinding, DISKOnt.getProperty(DISK.HAS_BINDING_VALUE),
                                    userKB.createLiteral(url));
                            userKB.addPropertyValue(bindingobj, DISKOnt.getProperty(DISK.HAS_OUTPUT_FILE), fileBinding);
                        }
                }

                // Creating workflow data bindings
                for (VariableBinding vBinding : bindings.getBindings()) {
                    String varId = vBinding.getVariable();
                    String binding = vBinding.getBinding();
                    String cType = vBinding.getType();

                    String type = cType != null ? cType :  KBConstants.XSD_NS + "string"; // default type
                    Value bindingValue = new Value(binding, type);
                    if (cType != null && !cType.startsWith(KBConstants.XSD_NS)) {
                        bindingValue.setType(Value.Type.URI);
                    }

                    KBObject varbindingobj = userKB.createObjectOfClass(null, DISKOnt.getClass(DISK.VARIABLE_BINDING));
                    if (bindingValue.getType() == Value.Type.URI) {
                        // We store the type of the binding on the binding object. We should store it on the binding itself
                        // but most of the time the binding is a rdf-literal value.
                        KBObject typeProp = userKB.getProperty(KBConstants.RDF_NS + "type");
                        userKB.addTriple(varbindingobj, typeProp, userKB.getResource(type));
                        //userKB.addClassForInstance(varbindingobj, userKB.getResource(type));
                    }
                    userKB.setPropertyValue(varbindingobj, DISKOnt.getProperty(DISK.HAS_VARIABLE),
                            userKB.getResource(workflowuri + "#" + varId));
                    userKB.setPropertyValue(varbindingobj, DISKOnt.getProperty(DISK.HAS_BINDING_VALUE),
                            this.getKBValue(bindingValue, userKB));
                    userKB.addPropertyValue(bindingobj, DISKOnt.getProperty(DISK.HAS_VARIABLE_BINDING), varbindingobj);
                }

                String hypId = bindings.getMeta().getHypothesis();
                String revhypId = bindings.getMeta().getRevisedHypothesis();
                if (hypId != null)
                    userKB.setPropertyValue(bindingobj, DISKOnt.getProperty(DISK.HAS_HYPOTHESIS_VARIABLE),
                            userKB.getResource(workflowuri + "#" + hypId));
                if (revhypId != null)
                    userKB.setPropertyValue(bindingobj, DISKOnt.getProperty(DISK.HAS_REVISED_HYPOTHESIS_VARIABLE),
                            userKB.getResource(workflowuri + "#" + revhypId));
            }

            this.save(userKB);
            this.end();
        }
    }

    private List<WorkflowBindings> loadWorkflowsBindings(String userDomain, String id) {
        return loadBindings(userDomain, id, DISKOnt.getProperty(DISK.HAS_WORKFLOW_BINDING));
    }

    private List<WorkflowBindings> loadMetaWorkflowsBindings(String userDomain, String id) {
        return loadBindings(userDomain, id, DISKOnt.getProperty(DISK.HAS_METAWORKFLOW_BINDING));
    }

    private List<WorkflowBindings> loadBindings(String userDomain, String id, KBObject bindingprop) {
        List<WorkflowBindings> list = new ArrayList<WorkflowBindings>();
        String loiId = userDomain + "/" + id;
        KBAPI kb = getOrCreateKB(userDomain);

        if (kb != null) {
            this.start_write();
            KBObject loiItem = kb.getIndividual(loiId);

            for (KBTriple t : kb.genericTripleQuery(loiItem, bindingprop, null)) {
                KBObject wbObj = t.getObject();
                WorkflowBindings bindings = new WorkflowBindings();
                KBObject sourceObj = kb.getPropertyValue(wbObj, DISKOnt.getProperty(DISK.HAS_SOURCE));
                MethodAdapter methodAdapter = null;
                if (sourceObj != null) {
                    String source = sourceObj.getValueAsString();
                    bindings.setSource(source);
                    methodAdapter = this.getMethodAdapterByName(source);
                }

                String description = kb.getComment(wbObj);
                if (description != null)
                    bindings.setDescription(description);

                // Workflow Run details
                WorkflowRun run = new WorkflowRun();
                KBObject runIdObj = kb.getPropertyValue(wbObj, DISKOnt.getProperty(DISK.HAS_ID));
                if (runIdObj != null)
                    run.setId(runIdObj.getValue().toString());
                KBObject statusObj = kb.getPropertyValue(wbObj, DISKOnt.getProperty(DISK.HAS_STATUS));
                if (statusObj != null)
                    run.setStatus(statusObj.getValue().toString());
                KBObject linkObj = kb.getPropertyValue(wbObj, DISKOnt.getProperty(DISK.HAS_RUN_LINK));
                if (linkObj != null)
                    run.setLink(linkObj.getValue().toString());
                KBObject runStartObj = kb.getPropertyValue(wbObj, DISKOnt.getProperty(DISK.HAS_RUN_START_DATE));
                if (runStartObj != null)
                    run.setStartDate(runStartObj.getValue().toString());
                KBObject runEndObj = kb.getPropertyValue(wbObj, DISKOnt.getProperty(DISK.HAS_RUN_END_DATE));
                if (runEndObj != null)
                    run.setEndDate(runEndObj.getValue().toString());

                // Inputs
                for (KBObject inputObj : kb.getPropertyValues(wbObj, DISKOnt.getProperty(DISK.HAS_INPUT_FILE))) {
                    KBObject name = kb.getPropertyValue(inputObj, DISKOnt.getProperty(DISK.HAS_VARIABLE));
                    KBObject url = kb.getPropertyValue(inputObj, DISKOnt.getProperty(DISK.HAS_BINDING_VALUE));
                    run.addFile(name.getName(), url.getValueAsString());
                }

                // Outputs
                for (KBObject outputObj : kb.getPropertyValues(wbObj, DISKOnt.getProperty(DISK.HAS_OUTPUT_FILE))) {
                    KBObject name = kb.getPropertyValue(outputObj, DISKOnt.getProperty(DISK.HAS_VARIABLE));
                    KBObject url = kb.getPropertyValue(outputObj, DISKOnt.getProperty(DISK.HAS_BINDING_VALUE));
                    run.addOutput(name.getName(), url.getValueAsString());
                }
                bindings.setRun(run);

                // Workflow details
                KBObject workflowobj = kb.getPropertyValue(wbObj, DISKOnt.getProperty(DISK.HAS_WORKFLOW));
                if (workflowobj != null && methodAdapter != null) {
                    bindings.setWorkflow(workflowobj.getName());
                    String link = methodAdapter.getWorkflowLink(workflowobj.getName());
                    if (link != null)
                        bindings.setWorkflowLink(link);
                }

                // Variable binding details
                for (KBObject vbObj : kb.getPropertyValues(wbObj, DISKOnt.getProperty(DISK.HAS_VARIABLE_BINDING))) {
                    KBObject varobj = kb.getPropertyValue(vbObj, DISKOnt.getProperty(DISK.HAS_VARIABLE));
                    KBObject bindobj = kb.getPropertyValue(vbObj, DISKOnt.getProperty(DISK.HAS_BINDING_VALUE));
                    VariableBinding vBinding = new VariableBinding(varobj.getName(), bindobj.getValueAsString());
                    String cls = null;
                    try {
                        KBObject typeprop = kb.getProperty(KBConstants.RDF_NS + "type");
                        for (KBTriple tr : kb.genericTripleQuery(vbObj, typeprop, null)) {
                            KBObject o = tr.getObject();
                            if (o != null && !o.getValueAsString().startsWith("http://disk-project.org/ontology/disk#")) {
                                cls = o.getValueAsString();
                            }
                        }
                    } catch (Exception e) {
                        // TODO: handle exception
                        System.out.println("Something weird has happened");
                    }
                    if (cls != null) {
                        vBinding.setType(cls);
                    }
                    bindings.getBindings().add(vBinding);
                }

                KBObject hypobj = kb.getPropertyValue(wbObj, DISKOnt.getProperty(DISK.HAS_HYPOTHESIS_VARIABLE));
                if (hypobj != null)
                    bindings.getMeta().setHypothesis(hypobj.getName());
                KBObject revhypobj = kb.getPropertyValue(wbObj,
                        DISKOnt.getProperty(DISK.HAS_REVISED_HYPOTHESIS_VARIABLE));
                if (revhypobj != null)
                    bindings.getMeta().setRevisedHypothesis(revhypobj.getName());

                list.add(bindings);
            }
            this.end();
        }
        return list;
    }
}