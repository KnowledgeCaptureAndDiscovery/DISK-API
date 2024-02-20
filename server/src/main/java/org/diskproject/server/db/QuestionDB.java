package org.diskproject.server.db;

import org.diskproject.server.managers.DataAdapterManager;
import org.diskproject.server.repository.DiskRDF;
import org.diskproject.server.util.Config;
import org.diskproject.server.util.KBCache;
import org.diskproject.server.util.KBUtils;
import org.diskproject.shared.classes.adapters.DataAdapter;
import org.diskproject.shared.classes.adapters.DataResult;
import org.diskproject.shared.classes.common.Triple;
import org.diskproject.shared.classes.common.Value;
import org.diskproject.shared.classes.question.BoundingBoxQuestionVariable;
import org.diskproject.shared.classes.question.DynamicOptionsQuestionVariable;
import org.diskproject.shared.classes.question.Question;
import org.diskproject.shared.classes.question.QuestionCategory;
import org.diskproject.shared.classes.question.QuestionVariable;
import org.diskproject.shared.classes.question.QuestionVariable.QuestionSubtype;
import org.diskproject.shared.classes.question.StaticOptionsQuestionVariable;
import org.diskproject.shared.classes.question.TimeIntervalQuestionVariable;
import org.diskproject.shared.classes.question.UserInputQuestionVariable;
import org.diskproject.shared.classes.question.VariableOption;
import org.diskproject.shared.classes.util.KBConstants;
import org.diskproject.shared.classes.util.QuestionOptionsRequest;
import org.diskproject.shared.ontologies.SQO;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.isi.kcap.ontapi.KBAPI;
import edu.isi.kcap.ontapi.KBObject;
import edu.isi.kcap.ontapi.KBTriple;
import edu.isi.kcap.ontapi.OntSpec;

public class QuestionDB {
    private DiskRDF rdf;
    private KBCache SQOnt;
    private KBAPI questionKB;
    private Map<String, List<VariableOption>> optionsCache;
    private Map<String, Question> allQuestions;
    private Map<String, QuestionVariable> allVariables;
    private DataAdapterManager dataAdapters;

    public QuestionDB (DiskRDF rdf, DataAdapterManager data) {
        this.rdf = rdf;
        this.optionsCache = new WeakHashMap<String, List<VariableOption>>();
        this.dataAdapters = data;
        this.loadKB();
        this.loadQuestionTemplates();
    }

    public KBAPI getKB () {
        return this.questionKB;
    }

    private void loadKB () {
        try {
            this.questionKB = this.rdf.getFactory().getKB(KBConstants.QUESTION_URI, OntSpec.PLAIN, false, true);
        } catch (Exception e) {
            System.err.println("Could not load question ontology: " + KBConstants.QUESTION_URI);
            return;
        }
        this.rdf.startRead();
        this.SQOnt = new KBCache(this.questionKB);
        this.rdf.end();
    }

    public void reloadKB () {
        KBUtils.clearKB(this.questionKB, this.rdf);
        this.loadKB();
    }


    private void loadQuestionTemplates() {
        this.allQuestions = new HashMap<String, Question>();
        this.allVariables = new HashMap<String, QuestionVariable>();
        for (String url : Config.get().questions.values()) {
            // Clear cache first
            try {
                this.rdf.startWrite();
                KBAPI kb = this.rdf.getFactory().getKB(url, OntSpec.PLAIN, true, true);
                kb.removeAllTriples();
                kb.delete();
                this.rdf.save(kb);
                this.rdf.end();
            } catch (Exception e) {
                System.err.println("Error while loading " + url);
                e.printStackTrace();
                if (this.rdf.isInTransaction()) {
                    this.rdf.end();
                }
            }
            // Load questions and cache them
            for (Question q : loadQuestionsFromKB(url)) {
                this.allQuestions.put(q.getId(), q);
            }
        }
    }

    public List<Question> loadQuestionsFromKB(String url) {
        System.out.println("Loading Question Templates: " + url);
        List<Question> questions = new ArrayList<Question>();
        try {
            KBAPI kb = this.rdf.getFactory().getKB(url, OntSpec.PLAIN, true, true);
            this.rdf.startRead();
            KBObject typeprop = kb.getProperty(KBConstants.RDF_NS + "type");
            KBObject labelprop = kb.getProperty(KBConstants.RDFS_NS + "label");
            // Load question variables:
            for (KBTriple t : kb.genericTripleQuery(null, typeprop, SQOnt.getClass(SQO.QUESTION_VARIABLE))) {
                KBObject qv = t.getSubject();
                allVariables.put(qv.getID(), LoadQuestionVariableFromKB(qv, kb));
            }

            // Load questions and subproperties
            for (KBTriple t : kb.genericTripleQuery(null, typeprop, SQOnt.getClass(SQO.QUESTION))) {
                KBObject question = t.getSubject();
                KBObject name = kb.getPropertyValue(question, labelprop);
                KBObject template = kb.getPropertyValue(question, SQOnt.getProperty(SQO.HAS_TEMPLATE));
                KBObject constraint = kb.getPropertyValue(question, SQOnt.getProperty(SQO.HAS_QUESTION_CONSTRAINT_QUERY));
                KBObject category = kb.getPropertyValue(question, SQOnt.getProperty(SQO.HAS_QUESTION_CATEGORY));
                ArrayList<KBObject> variables = kb.getPropertyValues(question, SQOnt.getProperty(SQO.HAS_VARIABLE));
                KBObject pattern = kb.getPropertyValue(question, SQOnt.getProperty(SQO.HAS_PATTERN));
                if (name != null && template != null) {
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
                            pattern != null ? LoadStatements(pattern, kb) : null, vars);
                    if (constraint != null) q.setConstraint(constraint.getValueAsString());
                    if (category != null) {
                        KBObject catName = kb.getPropertyValue(category, labelprop);
                        q.setCategory( new QuestionCategory(category.getID(), catName.getValueAsString()) );
                    }
                    questions.add(q);
                }
            }
            this.rdf.end();
            return questions;
        } catch (Exception e) {
            e.printStackTrace();
            if (this.rdf.isInTransaction())
                this.rdf.end();
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
        boolean useOr = true;
        if (types != null) {
            for (KBObject typ : types) {
                String urlvalue = typ.getValueAsString();
                // Only use QUESTION_NS subclasses
                if (urlvalue.startsWith(KBConstants.QUESTION_NS)
                    && !urlvalue.equals(KBConstants.QUESTION_NS + SQO.QUESTION_VARIABLE)) {
                    // Check if multiple values will be used with AND or OR 
                    if (urlvalue.equals(KBConstants.QUESTION_NS + SQO.QUESTION_VARIABLE_OR)) {
                        useOr = true;
                    } else if (urlvalue.equals(KBConstants.QUESTION_NS + SQO.QUESTION_VARIABLE_AND)) {
                        useOr = false;
                    } else {
                        additionalType = urlvalue;
                    }
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
            q.setConjunction(!useOr);
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

    public List<Question> listQuestions() {
        return new ArrayList<Question>(this.allQuestions.values());
    }

    // OPTIONS: 
    private List<VariableOption> queryForOptions (String varName, String query) throws Exception {
        if (optionsCache.containsKey(varName + query))
            return optionsCache.get(varName + query);

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
        optionsCache.put(varName + query, options);
        return options;
    }

    public Question getQuestion (String id) {
        return this.allQuestions.get(id);
    }

    public QuestionVariable getVariable (String id) {
        return this.allVariables.get(id);
    }

    public String createQuestionOptionsQuery (Question q, List<QuestionVariable> includedVariables) {
        if (q != null) {
            String queryConstraint = q.getConstraint();
            String query = queryConstraint != null ? queryConstraint : "";
            for (QuestionVariable qv: includedVariables) {
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
        // Returns a map[var.name] -> [option, option2, ...]
        Map<String, String[]> bindings = cfg.getBindings();
        Question q = allQuestions.get(cfg.getId());
        if (q == null) {
            System.err.println("Question not found: " + cfg.getId());
            return null;
        }

        Map<String, String> queries = new HashMap<String, String>();
        Map<String, List<VariableOption>> options = new HashMap<String, List<VariableOption>>();
        if (bindings == null || bindings.size() == 0) {
            // If there are no bindings, we can just send the base query + the constraint for this variable
            for (QuestionVariable qv: q.getVariables()) {
                QuestionSubtype t = qv.getSubType();
                if (t == QuestionSubtype.DYNAMIC_OPTIONS || t == QuestionSubtype.BOUNDING_BOX || t == QuestionSubtype.TIME_INTERVAL ) { 
                    String curQuery = (q.getConstraint() != null ? q.getConstraint() : "") + ((DynamicOptionsQuestionVariable) qv).getOptionsQuery();
                    queries.put(qv.getId(), curQuery);
                }
            }
        } else {
            // If we have at leas one binding, we need to create filters for all queries:
            Map<String, String> filters = new HashMap<String, String>();
            for (String varId: bindings.keySet()) {
                QuestionVariable curVar = allVariables.get(varId); // Should be the same as question.getVariables.
                if (curVar != null) {
                    String value = bindings.get(varId)[0];
                    String sparqlValue = value.startsWith("http") ? "<" + value + ">" : "\"" + value + "\"";
                    String line = "VALUES " + curVar.getVariableName() + " { " + sparqlValue + " }";
                    filters.put(varId, line);
                } else {
                    System.err.println("Cannot find variable with ID: " + varId);
                }
            }

            String baseQuery = createQuestionOptionsQuery(q, q.getVariables());
            for (QuestionVariable qv: q.getVariables()) {
                QuestionSubtype t = qv.getSubType();
                if (t == QuestionSubtype.DYNAMIC_OPTIONS || t == QuestionSubtype.BOUNDING_BOX || t == QuestionSubtype.TIME_INTERVAL ) { 
                    String varId = qv.getId();
                    String curQuery = baseQuery;
                    // We need to add all filters that ARE NOT THIS ONE
                    for (String filterId: filters.keySet()) {
                        if (!filterId.equals(varId)) {
                            curQuery += "\n" + filters.get(filterId);
                        }
                    }
                    queries.put(varId, curQuery);
                }
            }
        }

        // Now run the queries and load results.
        for (QuestionVariable qv: q.getVariables()) {
            QuestionSubtype t = qv.getSubType();
            String varName = qv.getVariableName();
            if (t == QuestionSubtype.STATIC_OPTIONS) {
                options.put(varName, ((StaticOptionsQuestionVariable) qv).getOptions());
            } else if (t == QuestionSubtype.DYNAMIC_OPTIONS) {
                // We add all the filter except the value for the queried variable
                if (queries.containsKey(qv.getId())) {
                    String curQuery = queries.get(qv.getId());
                    options.put(varName, queryForOptions(varName, curQuery));
                    if (options.get(varName).size() == 0) {
                        System.out.println(qv.getId() + " got 0 results:");
                        System.out.println(curQuery);
                    } else {
                        System.out.println(qv.getId() + " got " + options.get(varName).size() + " results.");
                    }
                }
            }
        }
        return options;
    }

}
