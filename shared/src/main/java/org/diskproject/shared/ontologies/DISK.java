package org.diskproject.shared.ontologies;

public class DISK {
    public static final String _BASE = "http://disk-project.org/ontology/disk#";
    // Classes
    public static final String ENTITY = "Entity";
    public static final String ENDPOINT = "Endpoint";
    public static final String HYPOTHESIS = "Hypothesis";
    public static final String LINE_OF_INQUIRY = "LineOfInquiry";
    public static final String TRIGGERED_LINE_OF_INQUIRY = "TriggeredLineOfInquiry"; // Subclass of LOI
    public static final String GOAL = "Goal";
    public static final String GOAL_RESULT = "GoalResult";
    public static final String VARIABLE_BINDING = "VariableBinding";
    public static final String DATA_QUERY_TEMPLATE = "DataQueryTemplate";
    public static final String DATA_QUERY_RESULTS = "DataQueryResults";             // Subclass of DQT
    public static final String EXECUTION_RECORD = "ExecutionRecord";
    public static final String EXECUTION = "Execution";                             // Subclass of ExecutionRecord
    public static final String WORKFLOW = "Workflow";
    public static final String META_WORKFLOW = "MetaWorkflow";                      // Subclass of workflow?
    public static final String WORKFLOW_SEED = "WorkflowSeed";
    public static final String WORKFLOW_INSTANTIATION = "WorkflowInstantiation";    // Subclass of workflow instantiation

    // Object Properties
    public static final String HAS_HYPOTHESIS = "hasHypothesis";
    public static final String HAS_SOURCE = "hasSource";
    public static final String CREATED_FROM = "createdFrom";                    // Workflow instantiation -> Workflow seed
    public static final String HAS_AUTHOR = "hasAuthor";                        // Common -> Entity
    public static final String HAS_DATA_BINDINGS = "hasDataBindings";           // Workflow instantiation -> Variable binding
    public static final String HAS_DATA_QUERY = "hasDataQuery";                 // LOI -> Data Query Template
    public static final String HAS_DATA_QUERY_RESULT = "hasDataQueryResult";    // TLOI -> Data Query result
    public static final String HAS_DATA_SOURCE = "hasDataSource";               // Data query template -> Endpoint
    public static final String HAS_EXECUTION = "hasExecution";                  // Workflow instantiation -> Execution
    public static final String HAS_INPUT_FILE = "hasInputFile";                 // Execution -> Variable binding
    public static final String HAS_INPUT = "hasInput";                          // Workflow seed -> Variable binding
    public static final String HAS_OUTPUT = "hasOutput";                        // Workflow seed -> Variable binding
    public static final String HAS_LINE_OF_INQUIRY = "hasLineOfInquiry";        // TLOI -> LOI
    public static final String HAS_OUTPUT_FILE = "hasOutputFile";               // Execution -> Variable binding
    public static final String HAS_PARAMETER = "hasParameter";                  // Workflow seed -> Variable binding
    public static final String HAS_QUESTION = "hasQuestion";                    // Goal -> Question
    public static final String HAS_RESULT = "hasResult";                        // Execution -> Goal result
    public static final String HAS_STEP = "hasStep";                            // Execution -> Execution record
    public static final String HAS_WORKFLOW = "hasWorkflow";                    // Workflow seed -> Workflow
    public static final String HAS_META_WORKFLOW_SEED = "hasMetaWorkflowSeed";  // LOI -> Workflow seed
    public static final String HAS_WORKFLOW_SEED = "hasWorkflowSeed";           // LOI -> Workflow seed
    public static final String HAS_WORKFLOW_SOURCE = "hasWorkflowSource";       // Workflow seed -> Endpoint
    public static final String HAS_QUESTION_BINDINGS = "hasQuestionBindings";   // Goal -> Variable binding
    public static final String HAS_GOAL = "hasGoal";                            // TLOI -> Goal
    public static final String HAS_WORKFLOW_INST = "hasWorkflowInstantiation"; // TLOI -> Workflow inst
    public static final String HAS_META_WORKFLOW_INST = "hasMetaWorkflowInstantiation";  // TLOI -> Workflow ints

    // Data properties
    public static final String DATE_CREATED = "dateCreated";
    public static final String DATE_MODIFIED = "dateModified";
    public static final String HAS_USAGE_NOTES = "hasUsageNotes";
    public static final String HAS_BINDING_VALUE = "hasBindingValue";
    public static final String HAS_BINDING_VARIABLE = "hasBindingVariable";
    public static final String HAS_CONFIDENCE_TYPE = "hasConfidenceType";       // Goal result
    public static final String HAS_CONFIDENCE_VALUE = "hasConfidenceValue";     // Goal result
    public static final String HAS_DATATYPE = "hasDatatype";                    // Variable binding
    public static final String HAS_EMAIL = "hasEmail";                          // Entity
    public static final String HAS_GOAL_QUERY = "hasGoalQuery";                 // Goal
    public static final String HAS_ID = "hasId";                                // Execution, Workflow seed
    public static final String HAS_LOG = "hasLog";                              // Execution record
    public static final String HAS_NAME = "hasName";                            // Entity
    public static final String HAS_QUERY = "hasQuery";                          // Data query results
    public static final String HAS_QUERY_RESULTS = "hasQueryResults";           // Data query results
    public static final String HAS_QUERY_TEMPLATE = "hasQueryTemplate";         // Data query template 
    public static final String HAS_RUN_END_DATE = "hasRunEndDate";              // Execution record
    public static final String HAS_RUN_START_DATE = "hasRunStartDate";          // Execution record
    public static final String HAS_SOURCE_NAME = "hasSourceName";               // Endpoint
    public static final String HAS_SOURCE_URL = "hasSourceURL";                 // Endpoint
    public static final String HAS_STATUS = "hasStatus";                        // TLOI, Execution record, workflow instantiation
    public static final String HAS_TABLE_DESCRIPTION = "hasTableDescription";   // Data query template
    public static final String HAS_TABLE_VARIABLES = "hasTableVariables";       // Data query template
    public static final String HAS_TYPE = "hasType";                            // Variable binding
    public static final String HAS_UPDATE_CONDITION = "hasUpdateCondition";     // Line of inquiry 
    public static final String HAS_RUN_LINK = "hasRunLink";                     //
}