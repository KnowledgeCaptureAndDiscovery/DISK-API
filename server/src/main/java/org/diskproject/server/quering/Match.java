package org.diskproject.server.quering;

import org.diskproject.shared.classes.adapters.DataAdapter;
import org.diskproject.shared.classes.adapters.DataResult;
import org.diskproject.shared.classes.common.Endpoint;
import org.diskproject.shared.classes.hypothesis.Goal;
import org.diskproject.shared.classes.loi.DataQueryResult;
import org.diskproject.shared.classes.loi.DataQueryTemplate;
import org.diskproject.shared.classes.loi.LineOfInquiry;
import org.diskproject.shared.classes.loi.TriggeredLOI;
import org.diskproject.shared.classes.question.Question;
import org.diskproject.shared.classes.question.QuestionVariable;
import org.diskproject.shared.classes.util.KBConstants.SPECIAL;
import org.diskproject.shared.classes.workflow.VariableBinding;
import org.diskproject.shared.classes.workflow.WorkflowInstantiation;
import org.diskproject.shared.classes.workflow.WorkflowSeed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Match {
    public final Goal goal;
    public final LineOfInquiry loi;
    public final Question question;
    public final DataAdapter dataSource;
    public boolean fullCSV, valid, ready;
    public Set<String> selectVariables, seedVariables, arrayVariables; // These are the variables required from the seeds.
    public String csvURL, querySend;
    public List<DataResult> queryResult;
    public Set<String> queryVariables; //this ones do not have the `?`

    public Match (Goal goal, LineOfInquiry loi, Question question, DataAdapter dataSource) {
        this.goal = goal;
        this.loi = loi;
        this.question = question;
        this.dataSource = dataSource;
        // Check for necessary data for execution
        DataQueryTemplate qt = this.loi.getDataQueryTemplate();
        if (qt == null || question == null || dataSource == null
            || qt.getTemplate() == null || qt.getTemplate().equals("")
            || (loi.getWorkflowSeeds().size() == 0 && loi.getMetaWorkflowSeeds().size() == 0)) {
            System.out.println("LOI must define at least a template, endpoint and workflow");
            this.valid = false;
            return;
        }

        this.valid = this.analyzeWorkflows();
    }

    private boolean analyzeWorkflows () {
        boolean isCSV = false;
        List<WorkflowSeed> allSeeds = Stream.concat(loi.getWorkflowSeeds().stream(), loi.getMetaWorkflowSeeds().stream()).collect(Collectors.toList());
        Set<String> reqVariables = new HashSet<String>(), arrayVars = new HashSet<String>(), selectVars = new HashSet<String>();
        for (WorkflowSeed seed: allSeeds) {
            Endpoint source = seed.getSource();
            if (source == null || source.getId() == null || source.getId().equals("")) {
                System.out.println("Method source ID was not found for seed id: " + seed.getId());
                return false;
            }

            for (VariableBinding vb: Stream.concat(seed.getInputs().stream(), seed.getParameters().stream()).collect(Collectors.toList())) {
                //Workflow variable bindings should ALWAYS have only one value.
                String raw = null;
                if (vb.getIsArray()) {
                    if (vb.getBindings().size() == 1) {
                        raw = vb.getBindings().get(0);
                        arrayVars.add(raw);
                    } else {
                        //This should not happen.
                        System.out.println("Workflow seed configuration sets two variables for a single parameter/input");
                        return false;
                    }
                } else {
                    raw = vb.getSingleBinding();
                }
                if (raw.startsWith("?")) {
                    reqVariables.add(raw);
                    selectVars.add(raw);
                } else if (raw.equals(SPECIAL.CSV)) {
                    isCSV = true;
                }
            }
        }

        if (reqVariables.size() == 0 && !isCSV) {
            System.out.println("Workflow seeds must require at least one variable from the data query ");
            return false;
        }

        if (loi.getDataQueryTemplate() != null) {
            for (String v: loi.getDataQueryTemplate().getVariablesToShow()) {
                selectVars.add(v);
            }
        }
        this.fullCSV = isCSV;
        this.seedVariables = reqVariables;
        this.arrayVariables = arrayVars;
        this.selectVariables = selectVars;
        return true;
    }

    public boolean isValid () {
        return this.valid;
    }

    public static Set<String> getDataQueryVariables(String template) {
        Pattern varPattern = Pattern.compile("\\?(.+?)\\b");
        Set<String> l = new HashSet<String>();
        Matcher a = varPattern.matcher(template);
        while (a.find()) {
            String var = a.group();
            if (var.charAt(1) != '_')
                l.add(var);
        }
        return l;
    }

    private static Map<String,String> idToName (List<QuestionVariable> list) {
        Map<String, String> map = new HashMap<String,String>();
        for (QuestionVariable questionBinding: list) {
            map.put(questionBinding.getId(), questionBinding.getVariableName());
        }
        return map;
    }

    public String createQueryTemplate () {
        if (!valid)
            return null;
        String query = this.loi.getDataQueryTemplate().getTemplate();
        Set<String> varNames = getDataQueryVariables(query);
        Map<String, String> questionVariables = idToName(question.getVariables());
        
        for (VariableBinding vb: goal.getQuestionBindings()) {
            String varURI = vb.getVariable();
            String varName = questionVariables.get(varURI);
            if (varNames.contains(varName)) {
                boolean writtenVarName = false;
                List<String> allValues = vb.getBinding();
                for (String value: allValues) {
                    if (value != null && !value.equals("")) {
                        String datatype = vb.getDatatype();
                        if (!writtenVarName) {
                            query += "\n  VALUES " + varName + " {";
                            if (allValues.size() > 1) {
                                query += "\n";
                            }
                            writtenVarName = true;
                        }
                        if (datatype != null && datatype.endsWith("anyURI")) {
                            query += "  <" + value + ">";
                        } else {
                            query += "  \"" + value + "\"";
                        }
                        if (allValues.size() > 1) {
                            query += "\n";
                        }
                    }
                }
                if (writtenVarName) {
                    if (allValues.size() > 1) {
                        query += " ";
                    }
                    query += "}";
                }
            }
        }
        this.ready = !fullCSV || (csvURL != null && !csvURL.equals(""));
        return query;
    }

    public void setCSVURL (String url) {
        this.csvURL = url;
        this.ready = true;
    }

    public void setQueryResults(List<DataResult> solutions, String query) {
        this.queryResult = solutions;
        this.querySend = query;
        queryVariables = queryResult.get(0).getVariableNames();
    }

    public boolean createWorkflowInstances () {
        List<WorkflowSeed> allSeeds = Stream.concat(loi.getWorkflowSeeds().stream(), loi.getMetaWorkflowSeeds().stream()).collect(Collectors.toList());
        for (WorkflowSeed seed: allSeeds) {
            createWorkflowInstance(seed);
        }
        return false;
    }

    public String getResultsAsCSV () {
        String csv = String.join(",", queryVariables);
        for (DataResult line: queryResult) {
            List<String> arrLine = new ArrayList<String>();
            for (String varName: queryVariables) {
                arrLine.add(line.getValue(varName));
            }
            csv += "\n" + String.join(",", arrLine);
        }
        return csv;
    }

    public TriggeredLOI createTLOI () {
        List<WorkflowInstantiation> wf = new ArrayList<WorkflowInstantiation>(),
                mwf = new ArrayList<WorkflowInstantiation>();
        for (WorkflowSeed seed: loi.getWorkflowSeeds()) {
            wf.addAll( createWorkflowInstance(seed) );
        }
        for (WorkflowSeed seed: loi.getMetaWorkflowSeeds()) {
            mwf.addAll( createWorkflowInstance(seed) );
        }
        TriggeredLOI tloi = new TriggeredLOI(loi, goal);
        tloi.setWorkflows(wf);
        tloi.setMetaWorkflows(mwf);
        tloi.setQueryResults(new DataQueryResult(loi.getDataQueryTemplate(), querySend, getResultsAsCSV()));
        return tloi;
    }

    private List<WorkflowInstantiation> createWorkflowInstance (WorkflowSeed seed) {
        // This can be improved. some caches could be done before this moment.
        if (queryResult == null || queryVariables == null || !ready || queryResult.size() == 0 || queryVariables.size() == 0) {
            return null;
        }
        List<DataResult> filteredResults = filterQueryResults(selectVariables);
        // One seed can create multiple instances. As the results are a table, we need to aggregate the results.
        int runs = 0;
        Map<String,Integer> ticks = new HashMap<String,Integer>();
        for (String name: this.queryVariables) {
            ticks.put(name, 0);
            String lastValue = null;
            boolean isArray = arrayVariables.contains("?" + name);
            for (DataResult cell: filteredResults) {
                String currentValue = cell.getValue(name);
                if (currentValue != lastValue) {
                    int newMax = ticks.get(name) + 1;
                    ticks.put(name, newMax);
                    lastValue = currentValue;
                    if (!isArray && newMax > runs) {
                        runs = newMax;
                    }
                }
            }
        }

        //Separate the table depending of the runs.
        List<List<DataResult>> independentResults = new ArrayList<List<DataResult>>();
        List<DataResult> lastList = new ArrayList<DataResult>();
        int count = 0, splitSize = filteredResults.size()/runs;
        for (DataResult cell: filteredResults) {
            count += 1;
            lastList.add(cell);
            if (count >= splitSize) {
                count = 0;
                independentResults.add(lastList);
                lastList = new ArrayList<DataResult>();
            }
        }

        List<WorkflowInstantiation> inst = new ArrayList<WorkflowInstantiation>();
        for (List<DataResult> resultsToBind: independentResults) {
            WorkflowInstantiation current = new WorkflowInstantiation(seed);
            List<VariableBinding> dataBindings = new ArrayList<VariableBinding>();
            for (VariableBinding varB: Stream.concat(seed.getInputs().stream(), seed.getParameters().stream()).collect(Collectors.toList())) {
                // These only have one value
                String wfBiding = varB.getBinding().get(0);
                VariableBinding newDataBinding = new VariableBinding(varB);
                List<String> newBindingValues = new ArrayList<String>();
                if (wfBiding.equals(SPECIAL.CSV)) {
                    newBindingValues.add(csvURL);
                } else if (wfBiding.startsWith("?")) {
                    String name = wfBiding.substring(1);
                    if (queryVariables.contains(name)) {
                        if (arrayVariables.contains(wfBiding)) {
                            //Is array, send a list with all values.
                            for (DataResult cell: resultsToBind) {
                                newBindingValues.add(cell.getValue(name));
                            }
                        } else {
                            // Al values in the list should be the same, send one.
                            newBindingValues.add(resultsToBind.get(0).getValue(name));
                        }
                    } else {
                        // Required variable is not on the query results.
                        // Should break the outer loop?
                        continue;
                    }
                }
                if (newBindingValues.size() > 0) {
                    newDataBinding.setBinding(newBindingValues);
                    dataBindings.add(newDataBinding);
                }
            }
            current.setDataBindings(dataBindings);
            inst.add(current);
        }
        return inst;
    }

    private List<DataResult> filterQueryResults (Set<String> allowedVariables) {
        // If the query results includes variables not used on the workflow, these are removed if the contents are the same.
        List<DataResult> list = new ArrayList<DataResult>();
        String lastLine = "";
        for (DataResult cell: queryResult) {
            String currentLine = "";
            for (String v: allowedVariables) {
                String varName = v.startsWith("?") ? v.substring(1) : v;
                currentLine += cell.getValue(varName) + ",";
            }
            if (!currentLine.equals(lastLine)) {
                lastLine = currentLine;
                list.add(cell);
            }
        }
        return list;
    }
}