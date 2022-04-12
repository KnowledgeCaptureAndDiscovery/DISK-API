package org.diskproject.server.repository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

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

import edu.isi.kcap.ontapi.KBAPI;
import edu.isi.kcap.ontapi.KBObject;
import edu.isi.kcap.ontapi.KBTriple;
import edu.isi.kcap.ontapi.OntSpec;

public class WriteKBRepository extends KBRepository {
    protected String _domain;

    private static SimpleDateFormat dateformatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");

    public void setDomain (String url) {
        this._domain = url;
    }

    public String DOMURI (String username) {
        return this._domain + "/" + username;
    }

    public String HYPURI (String username) {
        return this.DOMURI(username) + "/hypotheses";
    }

    public String LOIURI (String username) {
        return this.DOMURI(username) + "/lois";
    }

    public String TLOIURI (String username) {
        return this.DOMURI(username) + "/tlois";
    }

    public String ASSERTIONSURI (String username) {
        return this.DOMURI(username) + "/assertions";
    }
    
    private KBAPI getOrCreateKB (String url) {
        KBAPI kb = null;
        try {
            kb = this.fac.getKB(url, OntSpec.PLAIN, true);
        } catch (Exception e) {
            System.err.print("Could not open KB: " + url);
        }
        return kb;
    }

    private KBAPI getKB (String url) {
        KBAPI kb = null;
        try {
            kb = this.fac.getKB(url, OntSpec.PLAIN, false);
        } catch (Exception e) {
            System.err.print("Could not open KB: " + url);
        }
        return kb;
    }

    private KBObject getKBValue (Value v, KBAPI kb) {
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

    private void setKBStatement(Triple triple, KBAPI kb, KBObject st) {
        KBObject subj = kb.getResource(triple.getSubject());
        KBObject pred = kb.getResource(triple.getPredicate());
        KBObject obj = getKBValue(triple.getObject(), kb);
        KBObject subprop = kb.getProperty(KBConstants.RDFNS() + "subject");
        KBObject predprop = kb.getProperty(KBConstants.RDFNS() + "predicate");
        KBObject objprop = kb.getProperty(KBConstants.RDFNS() + "object");
        kb.addTriple(st, subprop, subj);
        kb.addTriple(st, predprop, pred);
        kb.addTriple(st, objprop, obj);
    }

    //Deprecate this!
    private void storeTripleDetails(Triple triple, String provid, KBAPI provkb) {
        TripleDetails details = triple.getDetails();
        if (details != null) {
            KBObject stobj = provkb.getResource(provid + "#" + GUID.randomId("Statement"));
            this.setKBStatement(triple, provkb, stobj);

            if (details.getConfidenceValue() > 0)
                provkb.setPropertyValue(stobj, pmap.get("hasConfidenceValue"),
                        provkb.createLiteral(triple.getDetails().getConfidenceValue()));
            if (details.getTriggeredLOI() != null)
                provkb.setPropertyValue(stobj, pmap.get("hasTriggeredLineOfInquiry"),
                        provkb.getResource(triple.getDetails().getTriggeredLOI()));
        }
    }

    private Graph updateTripleDetails(Graph graph, KBAPI provkb) {
        HashMap<String, Triple> tripleMap = new HashMap<String, Triple>();
        for (Triple t : graph.getTriples())
            tripleMap.put(t.toString(), t);

        KBObject subprop = provkb.getProperty(KBConstants.RDFNS() + "subject");
        KBObject predprop = provkb.getProperty(KBConstants.RDFNS() + "predicate");
        KBObject objprop = provkb.getProperty(KBConstants.RDFNS() + "object");

        for (KBTriple kbt : provkb.genericTripleQuery(null, subprop, null)) {
            KBObject stobj = kbt.getSubject();
            KBObject subjobj = kbt.getObject();
            KBObject predobj = provkb.getPropertyValue(stobj, predprop);
            KBObject objobj = provkb.getPropertyValue(stobj, objprop);

            Value value = this.getObjectValue(objobj);
            Triple triple = new Triple();
            triple.setSubject(subjobj.getID());
            triple.setPredicate(predobj.getID());
            triple.setObject(value);

            String triplestr = triple.toString();
            if (tripleMap.containsKey(triplestr)) {
                Triple t = tripleMap.get(triplestr);

                KBObject conf = provkb.getPropertyValue(stobj, pmap.get("hasConfidenceValue"));
                KBObject tloi = provkb.getPropertyValue(stobj, pmap.get("hasTriggeredLineOfInquiry"));

                TripleDetails details = new TripleDetails();
                if (conf != null && conf.getValue() != null)
                    details.setConfidenceValue((Double) conf.getValue());
                if (tloi != null)
                    details.setTriggeredLOI(tloi.getID());

                t.setDetails(details);
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
    
    //--- Hypothesis
    protected boolean writeHypothesis (String username, Hypothesis hypothesis) {
        if (hypothesis.getId() == null)
            return false;
        
        String userDomain = this.HYPURI(username);
        String hypothesisId = userDomain + "/" + hypothesis.getId();

        KBAPI userKB = getOrCreateKB(userDomain);
        
        if (userKB == null) return false;
        this.start_write();

        // Insert hypothesis info on user's graph
        KBObject hypitem = userKB.createObjectOfClass(hypothesisId, this.cmap.get("Hypothesis"));
        
        if (hypothesis.getName() != null)
            userKB.setLabel(hypitem, hypothesis.getName());
        if (hypothesis.getDescription() != null)
            userKB.setComment(hypitem, hypothesis.getDescription());
        if (hypothesis.getDateCreated() != null)
            userKB.setPropertyValue(hypitem, pmap.get("dateCreated"), userKB.createLiteral(hypothesis.getDateCreated()));
        if (hypothesis.getDateModified() != null)
            userKB.setPropertyValue(hypitem, pmap.get("dateModified"), userKB.createLiteral(hypothesis.getDateModified()));
        if (hypothesis.getAuthor() != null)
            userKB.setPropertyValue(hypitem, pmap.get("hasAuthor"), userKB.createLiteral(hypothesis.getAuthor()));
        if (hypothesis.getNotes() != null)
            userKB.setPropertyValue(hypitem, pmap.get("hasUsageNotes"), userKB.createLiteral(hypothesis.getNotes()));
        
        // Adding paring hypotheis ID 
        if (hypothesis.getParentId() != null) {
            String fullparentid = userDomain + "/" + hypothesis.getParentId();
            userKB.setPropertyValue(hypitem, pmap.get("hasParentHypothesis"), userKB.getResource(fullparentid));
        }
        
        // Adding question template details
        if (hypothesis.getQuestion() != null)
            userKB.setPropertyValue(hypitem, pmap.get("hasQuestion"), userKB.createLiteral(hypothesis.getQuestion()));
        List<VariableBinding> questionBindings = hypothesis.getQuestionBindings();
        if (questionBindings != null) {
            for (VariableBinding vb: questionBindings) {
                String ID = hypothesisId + "/bindings/";
                String[] sp = vb.getVariable().split("/");
                ID += sp[sp.length-1];
                System.out.println("varBindingId: " + ID);
                KBObject binding = userKB.createObjectOfClass(ID, this.cmap.get("VariableBinding"));
                userKB.setPropertyValue(binding, pmap.get("hasVariable"), userKB.createLiteral(vb.getVariable()));
                userKB.setPropertyValue(binding, pmap.get("hasBindingValue"), userKB.createLiteral(vb.getBinding()));
                userKB.addPropertyValue(hypitem, pmap.get("hasVariableBinding"), binding);
            }
        }

        this.save(userKB);
        this.end();
        
        // Store hypothesis graph
        KBAPI hypkb = getOrCreateKB(hypothesisId);
        if (hypkb == null) return false;

        this.start_write();
        for (Triple triple : hypothesis.getGraph().getTriples()) {
                KBTriple t = this.getKBTriple(triple, hypkb);
                if (t != null)
                    hypkb.addTriple(t);
        }
        this.save(hypkb);
        this.end();

        //FIXME: remove this way of getting the p-value
        String hypotheisProv = hypothesisId + "/provenance";
        KBAPI provkb = getOrCreateKB(hypotheisProv);
        if (provkb == null) return false;

        this.start_write();
        for (Triple triple : hypothesis.getGraph().getTriples()) {
            // Add triple details (confidence value, provenance, etc)
            this.storeTripleDetails(triple, hypotheisProv, provkb);
        }
        this.save(provkb);
        this.end();

        return true;
    }
    
    protected Hypothesis loadHypothesis (String username, String id) {
        String userDomain = this.HYPURI(username);
        String hypothesisId = userDomain + "/" + id;

        KBAPI userKB = getKB(userDomain);
        if (userKB == null) return null;

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

        KBObject dateobj = userKB.getPropertyValue(hypitem, pmap.get("dateCreated"));
        if (dateobj != null)
            hypothesis.setDateCreated(dateobj.getValueAsString());

        KBObject dateModifiedObj = userKB.getPropertyValue(hypitem, pmap.get("dateModified"));
        if (dateModifiedObj != null)
            hypothesis.setDateModified(dateModifiedObj.getValueAsString());

        KBObject authorobj = userKB.getPropertyValue(hypitem, pmap.get("hasAuthor"));
        if (authorobj != null)
            hypothesis.setAuthor(authorobj.getValueAsString());

        KBObject notesobj = userKB.getPropertyValue(hypitem, pmap.get("hasUsageNotes"));
        if (notesobj != null)
            hypothesis.setNotes(notesobj.getValueAsString());

        // Parent hypothesis ID
        KBObject parentobj = userKB.getPropertyValue(hypitem, pmap.get("hasParentHypothesis"));
        if (parentobj != null)
            hypothesis.setParentId(parentobj.getName());
        
        // Question template info
        KBObject questionobj = userKB.getPropertyValue(hypitem, pmap.get("hasQuestion"));
        if (questionobj != null)
            hypothesis.setQuestion(questionobj.getValueAsString());

        ArrayList<KBObject> questionBindings = userKB.getPropertyValues(hypitem, pmap.get("hasVariableBinding"));
        if (questionBindings != null) {
            List<VariableBinding> variableBindings = new ArrayList<VariableBinding>();
            for (KBObject binding: questionBindings) {
                KBObject kbvar = userKB.getPropertyValue(binding, pmap.get("hasVariable"));
                KBObject kbval = userKB.getPropertyValue(binding, pmap.get("hasBindingValue"));
                if (kbvar != null && kbval != null) {
                    String var = kbvar.getValueAsString();
                    String val = kbval.getValueAsString();
                    variableBindings.add( new VariableBinding(var, val));
                }
            }
            if (variableBindings.size() > 0) hypothesis.setQuestionBindings(variableBindings);
        }

        //FIXME: dont remember what this does.
        String provid = hypothesisId + "/provenance";
        KBAPI provkb = getKB(provid);
        if (provkb != null)
            this.updateTripleDetails(graph, provkb);
        
        this.end();
        return hypothesis;
    }
    
    protected boolean deleteHypothesis (String username, String id) {
        if (id == null)
            return false;

        String userDomain = this.HYPURI(username);
        String hypothesisId = userDomain + "/" + id;
        String provid = hypothesisId + "/provenance";

        KBAPI userKB = getKB(userDomain);
        KBAPI hypkb = getKB(hypothesisId);
        KBAPI provkb = getKB(provid);

        if (userKB != null && hypkb != null && provkb != null) {
            this.start_read();
            KBObject hypitem = userKB.getIndividual(hypothesisId);
            if (hypitem != null) {
                ArrayList<KBTriple> parentHypotheses = userKB.genericTripleQuery(null, pmap.get("hasParentHypothesis"), hypitem);
                ArrayList<KBObject> questionBindings = userKB.getPropertyValues(hypitem, pmap.get("hasVariableBinding"));
                this.end();
                
                // Remove question template bindings
                this.start_write();
                if (questionBindings != null) for (KBObject binding: questionBindings) {
                    userKB.deleteObject(binding, true, true);
                }
                userKB.deleteObject(hypitem, true, true);
                this.save(userKB);
                this.end();

                // Remove parent hypothesis //FIXME
                for (KBTriple t : parentHypotheses) {
                    this.deleteHypothesis(username, t.getSubject().getName());
                }
            } else {
                this.end();
            }

            return this.start_write() && hypkb.delete() && this.save(hypkb) && this.end() && 
                this.start_write() && provkb.delete() && this.save(provkb) && this.end();
        }
        return false;
    }
    
    protected List<Hypothesis> listHypothesesPreviews (String username) {
        List<Hypothesis> list = new ArrayList<Hypothesis>();
        String userDomain = this.HYPURI(username);

        KBAPI userKB = getKB(userDomain);
        if (userKB != null) {
            this.start_read();
            KBObject hypcls = this.cmap.get("Hypothesis");
            KBObject typeprop = userKB.getProperty(KBConstants.RDFNS() + "type");
            for (KBTriple t : userKB.genericTripleQuery(null, typeprop, hypcls)) {
                KBObject hypobj = t.getSubject();
                String name = userKB.getLabel(hypobj);
                String description = userKB.getComment(hypobj);

                String parentid = null;
                KBObject parentobj = userKB.getPropertyValue(hypobj, pmap.get("hasParentHypothesis"));
                if (parentobj != null)
                    parentid = parentobj.getName();
                
                String dateCreated = null;
                KBObject dateobj = userKB.getPropertyValue(hypobj, pmap.get("dateCreated"));
                if (dateobj != null)
                    dateCreated = dateobj.getValueAsString();

                String dateModified = null;
                KBObject dateModifiedObj = userKB.getPropertyValue(hypobj, pmap.get("dateModified"));
                if (dateModifiedObj != null)
                    dateModified = dateModifiedObj.getValueAsString();

                String author = null;
                KBObject authorobj = userKB.getPropertyValue(hypobj, pmap.get("hasAuthor"));
                if (authorobj != null)
                    author = authorobj.getValueAsString();
                
                Hypothesis item = new Hypothesis(hypobj.getName(), name, description, parentid, null);
                if (dateCreated != null) item.setDateCreated(dateCreated);
                if (dateModified != null) item.setDateModified(dateModified);
                if (author != null) item.setAuthor(author);
        
                list.add(item);
            }
            this.end();
        }
        return list;
    }
    
    // -- Line of inquiry
    protected boolean writeLOI (String username, LineOfInquiry loi) {
        if (loi.getId() == null)
            return false;

        String userDomain = this.LOIURI(username);
        String loiId = userDomain + "/" + loi.getId();
        KBAPI userKB = getOrCreateKB(userDomain);
        
        if (userKB == null)
            return false;

        this.start_write();

        KBObject loiitem = userKB.createObjectOfClass(loiId, this.cmap.get("LineOfInquiry"));
        if (loi.getName() != null)
            userKB.setLabel(loiitem, loi.getName());
        if (loi.getDescription() != null)
            userKB.setComment(loiitem, loi.getDescription());
        if (loi.getDateCreated() != null)
            userKB.setPropertyValue(loiitem, pmap.get("dateCreated"), userKB.createLiteral(loi.getDateCreated()));
        if (loi.getDateModified() != null)
            userKB.setPropertyValue(loiitem, pmap.get("dateModified"), userKB.createLiteral(loi.getDateModified()));
        if (loi.getAuthor() != null)
            userKB.setPropertyValue(loiitem, pmap.get("hasAuthor"), userKB.createLiteral(loi.getAuthor()));

        if (loi.getHypothesisQuery() != null)
            userKB.setPropertyValue(loiitem, pmap.get("hasHypothesisQuery"), userKB.createLiteral(loi.getHypothesisQuery()));
        if (loi.getDataQuery() != null)
            userKB.setPropertyValue(loiitem, pmap.get("hasDataQuery"), userKB.createLiteral(loi.getDataQuery()));
        if (loi.getDataSource() != null)
            userKB.setPropertyValue(loiitem, pmap.get("hasDataSource"), userKB.createLiteral(loi.getDataSource()));
        if (loi.getNotes() != null)
            userKB.setPropertyValue(loiitem, pmap.get("hasUsageNotes"), userKB.createLiteral(loi.getNotes()));
        if (loi.getRelevantVariables() != null)
            userKB.setPropertyValue(loiitem, pmap.get("hasRelevantVariables"), userKB.createLiteral(loi.getRelevantVariables()));
        if (loi.getQuestion() != null)
            userKB.setPropertyValue(loiitem, pmap.get("hasQuestion"), userKB.createLiteral(loi.getQuestion()));
        if (loi.getExplanation() != null)
            userKB.setPropertyValue(loiitem, pmap.get("dataQueryDescription"), userKB.createLiteral(loi.getExplanation()));
        
        this.save(userKB);
        this.end();
        
        writeWorkflowsBindings(username, loi);
        writeMetaWorkflowsBindings(username, loi);
        
        return true;
    }
    
    protected LineOfInquiry loadLOI (String username, String id) {
        String userDomain = this.LOIURI(username);
        String loiId = userDomain + "/" + id;
        
        KBAPI userKB = getKB(userDomain);
        if (userKB == null)
            return null;
        
        this.start_read();
        KBObject loiitem = userKB.getIndividual(loiId);
        if (loiitem == null) {
            this.end();
            return null;
        }
        
        LineOfInquiry loi = new LineOfInquiry();
        loi.setId(id);
        loi.setName(userKB.getLabel(loiitem));
        loi.setDescription(userKB.getComment(loiitem));

        KBObject dateobj = userKB.getPropertyValue(loiitem, pmap.get("dateCreated"));
        if (dateobj != null)
            loi.setDateCreated(dateobj.getValueAsString());

        KBObject datasourceobj = userKB.getPropertyValue(loiitem, pmap.get("hasDataSource"));
        if (datasourceobj != null)
            loi.setDataSource(datasourceobj.getValueAsString());

        KBObject dateModifiedObj = userKB.getPropertyValue(loiitem, pmap.get("dateModified"));
        if (dateModifiedObj != null)
            loi.setDateModified(dateModifiedObj.getValueAsString());

        KBObject authorobj = userKB.getPropertyValue(loiitem, pmap.get("hasAuthor"));
        if (authorobj != null)
            loi.setAuthor(authorobj.getValueAsString());

        KBObject notesobj = userKB.getPropertyValue(loiitem, pmap.get("hasUsageNotes"));
        if (notesobj != null) {
            loi.setNotes(notesobj.getValueAsString());
        }

        KBObject rvarobj = userKB.getPropertyValue(loiitem, pmap.get("hasRelevantVariables"));
        if (rvarobj != null)
            loi.setRelevantVariables(rvarobj.getValueAsString());

        KBObject hqueryobj = userKB.getPropertyValue(loiitem, pmap.get("hasHypothesisQuery"));
        if (hqueryobj != null)
            loi.setHypothesisQuery(hqueryobj.getValueAsString());
        
        KBObject dqueryobj = userKB.getPropertyValue(loiitem, pmap.get("hasDataQuery"));
        if (dqueryobj != null)
            loi.setDataQuery(dqueryobj.getValueAsString());

        KBObject questionobj = userKB.getPropertyValue(loiitem, pmap.get("hasQuestion"));
        if (questionobj != null)
            loi.setQuestion(questionobj.getValueAsString());

        KBObject explobj = userKB.getPropertyValue(loiitem, pmap.get("dataQueryDescription"));
        if (explobj != null) {
            loi.setExplanation(explobj.getValueAsString());
        }
        
        this.end();

        loi.setWorkflows(loadWorkflowsBindings(userDomain, loi.getId()));
        loi.setMetaWorkflows(loadMetaWorkflowsBindings(userDomain, loi.getId()));

        return loi;
    }
    
    protected boolean deleteLOI (String username, String id) {
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
    
    protected List<LineOfInquiry> listLOIPreviews (String username) {
        List<LineOfInquiry> list = new ArrayList<LineOfInquiry>();

        String userDomain = this.LOIURI(username);
      
        KBAPI userKB = getKB(userDomain);
        
        if (userKB != null) {
            this.start_read();
            KBObject loicls = this.cmap.get("LineOfInquiry");
            KBObject typeprop = userKB.getProperty(KBConstants.RDFNS() + "type");
            for (KBTriple t : userKB.genericTripleQuery(null, typeprop, loicls)) {
                KBObject loiobj = t.getSubject();
                String name = userKB.getLabel(loiobj);
                String description = userKB.getComment(loiobj);

                KBObject dateobj = userKB.getPropertyValue(loiobj, pmap.get("dateCreated"));
                String dateCreated = (dateobj != null) ? dateobj.getValueAsString() : null;
                        
                KBObject dateModifiedObj = userKB.getPropertyValue(loiobj, pmap.get("dateModified"));
                String dateModified = (dateModifiedObj != null) ? dateModifiedObj.getValueAsString() : null;

                KBObject authorobj = userKB.getPropertyValue(loiobj, pmap.get("hasAuthor"));
                String author = (authorobj != null) ? authorobj.getValueAsString() : null;

                KBObject hqueryobj = userKB.getPropertyValue(loiobj, pmap.get("hasHypothesisQuery"));
                String hypothesisQuery = (hqueryobj != null) ? hqueryobj.getValueAsString() : null;
            
                LineOfInquiry item = new LineOfInquiry(loiobj.getName(), name, description);
                if (dateCreated != null) item.setDateCreated(dateCreated);
                if (dateModified != null) item.setDateModified(dateModified);
                if (author != null) item.setAuthor(author);
                if (hypothesisQuery != null) item.setHypothesisQuery(hypothesisQuery);

                list.add(item);
            }
            this.end();
        }
        return list;
    }
    
    // -- Triggered Lines of Inquiry
    protected boolean writeTLOI (String username, TriggeredLOI tloi) {
        if (tloi.getId() == null)
            return false;
        
        String userDomain = this.TLOIURI(username);
        String tloiId = userDomain + "/" + tloi.getId();

        String hypns = this.HYPURI(username) + "/";
        String loins = this.LOIURI(username) + "/";

        KBAPI userKB = getOrCreateKB(userDomain);
        if (userKB == null)
            return false;
        
        this.start_write();
        KBObject tloiitem = userKB.createObjectOfClass(tloiId, this.cmap.get("TriggeredLineOfInquiry"));

        if (tloi.getName() != null)
            userKB.setLabel(tloiitem, tloi.getName());
        if (tloi.getDescription() != null)
            userKB.setComment(tloiitem, tloi.getDescription());
        if (tloi.getDataSource() != null)
            userKB.setPropertyValue(tloiitem, pmap.get("hasDataSource"), userKB.createLiteral(tloi.getDataSource()));
        if (tloi.getDateCreated() != null)
            userKB.setPropertyValue(tloiitem, pmap.get("dateCreated"), userKB.createLiteral(tloi.getDateCreated()));
        if (tloi.getDateModified() != null)
            userKB.setPropertyValue(tloiitem, pmap.get("dateModified"), userKB.createLiteral(tloi.getDateModified()));
        if (tloi.getAuthor() != null)
            userKB.setPropertyValue(tloiitem, pmap.get("hasAuthor"), userKB.createLiteral(tloi.getAuthor()));
        if (tloi.getDataQuery() != null)
            userKB.setPropertyValue(tloiitem, pmap.get("hasDataQuery"), userKB.createLiteral(tloi.getDataQuery()));
        if (tloi.getRelevantVariables() != null)
            userKB.setPropertyValue(tloiitem, pmap.get("hasRelevantVariables"), userKB.createLiteral(tloi.getRelevantVariables()));
        if (tloi.getExplanation() != null)
            userKB.setPropertyValue(tloiitem, pmap.get("dataQueryDescription"), userKB.createLiteral(tloi.getExplanation()));
        if (tloi.getConfidenceValue() > 0)
            userKB.setPropertyValue(tloiitem, pmap.get("hasConfidenceValue"), userKB.createLiteral(Double.toString(tloi.getConfidenceValue())));
        if (tloi.getStatus() != null)
            userKB.setPropertyValue(tloiitem, pmap.get("hasTriggeredLineOfInquiryStatus"), userKB.createLiteral(tloi.getStatus().toString()));
        
        List<String> inputList = tloi.getInputFiles();
        if (inputList != null && inputList.size() > 0) {
            for (String inputurl: inputList) {
                userKB.addPropertyValue(tloiitem, pmap.get("hasInputFile"), userKB.createLiteral(inputurl));
            }
        }

        List<String> outputList = tloi.getOutputFiles();
        if (outputList != null && outputList.size() > 0) {
            for (String outputurl: outputList) {
                userKB.addPropertyValue(tloiitem, pmap.get("hasOutputFile"), userKB.createLiteral(outputurl));
            }
        }
            
        if (tloi.getLoiId() != null) {
            KBObject lobj = userKB.getResource(loins + tloi.getLoiId());
            userKB.setPropertyValue(tloiitem, pmap.get("hasLineOfInquiry"), lobj);
        }
        if (tloi.getParentHypothesisId() != null) {
            KBObject hobj = userKB.getResource(hypns + tloi.getParentHypothesisId());
            userKB.setPropertyValue(tloiitem, pmap.get("hasParentHypothesis"), hobj);
        }
        for (String hypid : tloi.getResultingHypothesisIds()) {
            KBObject hobj = userKB.getResource(hypns + hypid);
            userKB.addPropertyValue(tloiitem, pmap.get("hasResultingHypothesis"), hobj);
        }

        this.save(userKB);
        this.end();
        
        writeWorkflowsBindings(username, tloi);
        writeMetaWorkflowsBindings(username, tloi);

        return true;
    }
    
    protected TriggeredLOI loadTLOI (String username, String id) {
        String userDomain = this.TLOIURI(username);
        String tloiId = userDomain + "/" + id;

        KBAPI userKB = getKB(userDomain);
        if (userKB == null)
            return null;

        this.start_read();

        KBObject obj = userKB.getIndividual(id);
        if (obj.getName() != null) {
            TriggeredLOI tloi = new TriggeredLOI();
            tloi.setId(obj.getName());
            tloi.setName(userKB.getLabel(obj));
            tloi.setDescription(userKB.getComment(obj));
            
            KBObject hasLOI = userKB.getPropertyValue(obj, pmap.get("hasLineOfInquiry"));
            if (hasLOI != null)
                tloi.setLoiId(hasLOI.getName());

            KBObject pobj = userKB.getPropertyValue(obj, pmap.get("hasParentHypothesis"));
            if (pobj != null)
                tloi.setParentHypothesisId(pobj.getName());

            KBObject stobj = userKB.getPropertyValue(obj, pmap.get("hasTriggeredLineOfInquiryStatus"));
            if (stobj != null)
                tloi.setStatus(Status.valueOf(stobj.getValue().toString()));

            KBObject dateobj = userKB.getPropertyValue(obj, pmap.get("dateCreated"));
            if (dateobj != null)
                tloi.setDateCreated(dateobj.getValueAsString());
            
            KBObject dateModifiedObj = userKB.getPropertyValue(obj, pmap.get("dateModified"));
            if (dateModifiedObj != null)
                tloi.setDateModified(dateModifiedObj.getValueAsString());
            
            KBObject authorobj = userKB.getPropertyValue(obj, pmap.get("hasAuthor"));
            if (authorobj != null)
                tloi.setAuthor(authorobj.getValueAsString());

            KBObject dqobj = userKB.getPropertyValue(obj, pmap.get("hasDataQuery"));
            if (dqobj != null)
                tloi.setDataQuery(dqobj.getValueAsString());
            
            KBObject dataSourceObj = userKB.getPropertyValue(obj, pmap.get("hasDataSource"));
            if (dataSourceObj != null)
                tloi.setDataSource(dataSourceObj.getValueAsString());

            KBObject rvobj = userKB.getPropertyValue(obj, pmap.get("hasRelevantVariables"));
            if (rvobj != null)
                tloi.setRelevantVariables(rvobj.getValueAsString());

            KBObject explobj = userKB.getPropertyValue(obj, pmap.get("dataQueryDescription"));
            if (explobj != null)
                tloi.setExplanation(explobj.getValueAsString());
            
            KBObject confidenceObj = userKB.getPropertyValue(obj, pmap.get("hasConfidenceValue"));
            if (confidenceObj != null)
                tloi.setConfidenceValue(Double.valueOf(confidenceObj.getValueAsString()));

            for (KBObject robj : userKB.getPropertyValues(obj, pmap.get("hasResultingHypothesis"))) {
                String resHypId = robj.getName();
                tloi.addResultingHypothesisId(resHypId);
            }

            ArrayList<KBObject> inputFilesObj = userKB.getPropertyValues(obj, pmap.get("hasInputFile"));
            if (inputFilesObj != null && inputFilesObj.size() > 0) {
                for (KBObject inputf: inputFilesObj) {
                    tloi.addInputFile(inputf.getValueAsString());
                }
            }

            ArrayList<KBObject> outputFilesObj = userKB.getPropertyValues(obj, pmap.get("hasOutputFile"));
            if (outputFilesObj != null && outputFilesObj.size() > 0) {
                for (KBObject outputf: outputFilesObj) {
                    tloi.addOutputFile(outputf.getValueAsString());
                }
            }
            this.end();
            
            tloi.setWorkflows(loadWorkflowsBindings(userDomain, tloiId));
            tloi.setMetaWorkflows(loadWorkflowsBindings(userDomain, tloiId));
            
            return tloi;
        } else {
            this.end();
            return null;
        }
    }
    
    protected boolean deleteTLOI (String username, String id) {
        if (id == null)
            return false;

        String userDomain = this.TLOIURI(username);
        String tloiId = userDomain + "/" + id;

        KBAPI userKB = getKB(userDomain);
        if (userKB == null)
            return false;

        // Remove resulting hypothesis is is not being used
        this.start_read();
        KBObject item = userKB.getIndividual(tloiId);
        KBObject hypobj = userKB.getPropertyValue(item, pmap.get("hasResultingHypothesis"));

        if (item != null && hypobj != null) {
            List<KBTriple> alltlois = userKB.genericTripleQuery(null, pmap.get("hasParentHypothesis"), hypobj);
            this.end();
            if (alltlois != null && alltlois.size() == 1) {
                this.deleteHypothesis(username, hypobj.getName());
            } else {
                System.out.println("Resulting hypotesis cannot be deleted as is being used for other tloi.");
            }
        } else {
          this.end();
        }

        // Remove this TLOI
        if (item != null) {
            this.start_write();
            userKB.deleteObject(item, true, true);
            return this.save(userKB) && this.end();
        } else {
            return false;
        }
    }
    
    protected List<TriggeredLOI> listTLOIs (String username) {
        List<TriggeredLOI> list = new ArrayList<TriggeredLOI>();
        String userDomain = this.TLOIURI(username);
        KBAPI userKB = getKB(userDomain);
        
        if (userKB != null) {
            List<String> tloiIds = new ArrayList<String>();

            this.start_read();
            KBObject cls = this.cmap.get("TriggeredLineOfInquiry");
            KBObject typeprop = userKB.getProperty(KBConstants.RDFNS() + "type");

            for (KBTriple t :  userKB.genericTripleQuery(null, typeprop, cls))
                tloiIds.add(t.getSubject().getID());
            this.end();
            
            for (String tloiId: tloiIds) {
                TriggeredLOI tloi = loadTLOI(username, tloiId);
                if (tloi != null)
                    list.add(tloi);
            }
        }
        return list;
    }
    
    // -- Workflows... or methods.
    private void writeWorkflowsBindings (String username, LineOfInquiry loi) {
        writeBindings(LOIURI(username), loi.getId(), pmap.get("hasWorkflowBinding"), loi.getWorkflows());
    }

    private void writeWorkflowsBindings (String username, TriggeredLOI tloi) {
        writeBindings(TLOIURI(username), tloi.getId(), pmap.get("hasWorkflowBinding"), tloi.getWorkflows());
    }

    private void writeMetaWorkflowsBindings (String username, LineOfInquiry loi) {
        writeBindings(LOIURI(username), loi.getId(), pmap.get("hasMetaWorkflowBinding"), loi.getMetaWorkflows());
    }

    private void writeMetaWorkflowsBindings (String username, TriggeredLOI tloi) {
        writeBindings(TLOIURI(username), tloi.getId(), pmap.get("hasMetaWorkflowBinding"), tloi.getMetaWorkflows());
    }
    
    private void writeBindings (String userDomain, String id, KBObject bindingprop, List<WorkflowBindings> bindingsList) {
        if (bindingsList == null || bindingsList.size() == 0)
            return;
        
        System.out.println(">> Writing workflows");
        
        String fullId = userDomain + "/" + id;
        KBAPI userKB = getOrCreateKB(userDomain);
        
        if (userKB != null) {
            this.start_write();
            KBObject item = userKB.getIndividual(fullId);
            
            //FIXME: This part refers to WINGS workflows, we need to change to methods
            KBAPI kb = userKB;
            for (WorkflowBindings bindings : bindingsList) {
                String workflowid = WingsAdapter.get().WFLOWID(bindings.getWorkflow());
                String workflowuri = WingsAdapter.get().WFLOWURI(bindings.getWorkflow());
                KBObject bindingobj = kb.createObjectOfClass(null, cmap.get("WorkflowBinding"));
                kb.addPropertyValue(item, bindingprop, bindingobj);

                kb.setPropertyValue(bindingobj, pmap.get("hasWorkflow"), kb.getResource(workflowid));

                // Get Run details
                if (bindings.getRun() != null) {
                    if (bindings.getRun().getId() != null)
                        kb.setPropertyValue(bindingobj, pmap.get("hasId"), kb.createLiteral(bindings.getRun().getId()));
                    if (bindings.getRun().getStatus() != null)
                        kb.setPropertyValue(bindingobj, pmap.get("hasStatus"), kb.createLiteral(bindings.getRun().getStatus()));
                    if (bindings.getRun().getLink() != null)
                        kb.setPropertyValue(bindingobj, pmap.get("hasRunLink"), kb.createLiteral(bindings.getRun().getLink()));
                }

                // Creating workflow data bindings
                for (VariableBinding vbinding : bindings.getBindings()) {
                    String varid = vbinding.getVariable();
                    String binding = vbinding.getBinding();
                    Value bindingValue = new Value(binding, KBConstants.XSDNS() + "string");
                    KBObject varbindingobj = kb.createObjectOfClass(null, cmap.get("VariableBinding"));
                    kb.setPropertyValue(varbindingobj, pmap.get("hasVariable"), kb.getResource(workflowuri + "#" + varid));
                    kb.setPropertyValue(varbindingobj, pmap.get("hasBindingValue"), this.getKBValue(bindingValue, kb));
                    kb.addPropertyValue(bindingobj, pmap.get("hasVariableBinding"), varbindingobj);
                }
                
                // Creating parameters
                for (VariableBinding param : bindings.getParameters()) {
                    String varid = param.getVariable();
                    String binding = param.getBinding();
                    Value bindingValue = new Value(binding, KBConstants.XSDNS() + "string");
                    KBObject paramobj = kb.createObjectOfClass(null, cmap.get("VariableBinding"));
                    kb.setPropertyValue(paramobj, pmap.get("hasVariable"), kb.getResource(workflowuri + "#" + varid));
                    kb.setPropertyValue(paramobj, pmap.get("hasBindingValue"), this.getKBValue(bindingValue, kb));
                    kb.addPropertyValue(bindingobj, pmap.get("hasParameter"), paramobj);
                }

                // Creating optional parameters
                for (VariableBinding param : bindings.getOptionalParameters()) {
                    String varid = param.getVariable();
                    String binding = param.getBinding();
                    Value bindingValue = new Value(binding, KBConstants.XSDNS() + "string");
                    KBObject paramobj = kb.createObjectOfClass(null, cmap.get("VariableBinding"));
                    kb.setPropertyValue(paramobj, pmap.get("hasVariable"), kb.getResource(workflowuri + "#" + varid));
                    kb.setPropertyValue(paramobj, pmap.get("hasBindingValue"), this.getKBValue(bindingValue, kb));

                    kb.addPropertyValue(bindingobj, pmap.get("hasOptionalParameter"), paramobj);
                }

                String hypid = bindings.getMeta().getHypothesis();
                String revhypid = bindings.getMeta().getRevisedHypothesis();
                System.out.println(hypid + " -- " + revhypid);
                if (hypid != null)
                    kb.setPropertyValue(bindingobj, pmap.get("hasHypothesisVariable"),
                            kb.getResource(workflowuri + "#" + hypid));
                if (revhypid != null)
                    kb.setPropertyValue(bindingobj, pmap.get("hasRevisedHypothesisVariable"),
                            kb.getResource(workflowuri + "#" + revhypid));
            }
            
            this.save(kb);
            this.end();
        }
    }

    
    private List<WorkflowBindings> loadWorkflowsBindings (String userDomain, String id) {
        return loadBindings(userDomain, id, pmap.get("hasWorkflowBinding"));
    }

    private List<WorkflowBindings> loadMetaWorkflowsBindings (String userDomain, String id) {
        return loadBindings(userDomain, id, pmap.get("hasMetaWorkflowBinding"));
    }
    
    private List<WorkflowBindings> loadBindings (String userDomain, String id, KBObject bindingprop) {
        List<WorkflowBindings> list = new ArrayList<WorkflowBindings>();
        String loiId = userDomain + "/" + id;
        KBAPI kb = getOrCreateKB(userDomain);

        
        if (kb != null) {
            this.start_write();
            KBObject loiitem = kb.getIndividual(loiId);

            for (KBTriple t : kb.genericTripleQuery(loiitem, bindingprop, null)) {
                KBObject wbobj = t.getObject();
                WorkflowBindings bindings = new WorkflowBindings();

                // Workflow Run details
                WorkflowRun run = new WorkflowRun();
                KBObject robj = kb.getPropertyValue(wbobj, pmap.get("hasId"));
                if (robj != null)
                    run.setId(robj.getValue().toString());
                KBObject statusobj = kb.getPropertyValue(wbobj, pmap.get("hasStatus"));
                if (statusobj != null)
                    run.setStatus(statusobj.getValue().toString());
                KBObject linkobj = kb.getPropertyValue(wbobj, pmap.get("hasRunLink"));
                if (linkobj != null)
                    run.setLink(linkobj.getValue().toString());
                bindings.setRun(run);

                // Workflow details
                KBObject workflowobj = kb.getPropertyValue(wbobj, pmap.get("hasWorkflow"));
                if (workflowobj != null) {
                  bindings.setWorkflow(workflowobj.getName());
                  String link = WingsAdapter.get().getWorkflowLink(workflowobj.getName());
                  if (link != null)
                      bindings.setWorkflowLink(link);
                }

                // Variable binding details
                for (KBObject vbobj : kb.getPropertyValues(wbobj, pmap.get("hasVariableBinding"))) {
                    KBObject varobj = kb.getPropertyValue(vbobj, pmap.get("hasVariable"));
                    KBObject bindobj = kb.getPropertyValue(vbobj, pmap.get("hasBindingValue"));
                    VariableBinding vbinding = new VariableBinding();
                    vbinding.setVariable(varobj.getName());
                    vbinding.setBinding(bindobj.getValueAsString());
                    bindings.getBindings().add(vbinding);
                }

                // Parameters details
                for (KBObject vbobj : kb.getPropertyValues(wbobj, pmap.get("hasParameter"))) {
                    KBObject varobj = kb.getPropertyValue(vbobj, pmap.get("hasVariable"));
                    KBObject bindobj = kb.getPropertyValue(vbobj, pmap.get("hasBindingValue"));
                    VariableBinding param = new VariableBinding(varobj.getName(),bindobj.getValueAsString());
                    bindings.addParameter(param);
                }

                // Optional parameters details
                for (KBObject vbobj : kb.getPropertyValues(wbobj, pmap.get("hasOptionalParameter"))) {
                    KBObject varobj = kb.getPropertyValue(vbobj, pmap.get("hasVariable"));
                    KBObject bindobj = kb.getPropertyValue(vbobj, pmap.get("hasBindingValue"));
                    VariableBinding optionalParam = new VariableBinding(varobj.getName(),bindobj.getValueAsString());
                    bindings.addOptionalParameter(optionalParam);
                }

                KBObject hypobj = kb.getPropertyValue(wbobj, pmap.get("hasHypothesisVariable"));
                if (hypobj != null)
                    bindings.getMeta().setHypothesis(hypobj.getName());
                KBObject revhypobj = kb.getPropertyValue(wbobj, pmap.get("hasRevisedHypothesisVariable"));
                if (revhypobj != null)
                    bindings.getMeta().setRevisedHypothesis(revhypobj.getName());

                list.add(bindings);
            }
            this.end();
        }
        return list;
    }
    
}