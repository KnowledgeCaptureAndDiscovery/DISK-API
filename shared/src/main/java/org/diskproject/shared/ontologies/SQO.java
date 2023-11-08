package org.diskproject.shared.ontologies;

//V1.3.0
public class SQO {
    // Classes
    public static final String QUESTION = "Question";
    public static final String QUESTION_CATEGORY = "QuestionCategory";
    public static final String VARIABLE_OPTION = "VariableOption";
    public static final String QUESTION_VARIABLE = "QuestionVariable";
    public static final String DYNAMIC_OPTIONS_QUESTION_VARIABLE = "DynamicOptionsQuestionVariable";
    public static final String STATIC_OPTIONS_QUESTION_VARIABLE = "StaticOptionsQuestionVariable";
    public static final String BOUNDING_BOX_QUESTION_VARIABLE = "BoundingBoxQuestionVariable";
    public static final String TIME_INTERVAL_QUESTION_VARIABLE = "TimeIntervalQuestionVariable";
    public static final String USER_INPUT_QUESTION_VARIABLE = "UserInputQuestionVariable";
    public static final String QUESTION_VARIABLE_OR = "Disjunction";
    public static final String QUESTION_VARIABLE_AND = "Conjunction";
    // Object Properties
    public static final String HAS_VARIABLE = "hasQuestionVariable";
    public static final String HAS_QUESTION_CATEGORY = "hasQuestionCategory";
    public static final String HAS_OPTION = "hasOption";
    public static final String HAS_MIN_LAT = "hasMinLat";
    public static final String HAS_MAX_LAT = "hasMaxLat";
    public static final String HAS_MIN_LNG = "hasMinLng";
    public static final String HAS_MAX_LNG = "hasMaxLng";
    public static final String HAS_START_TIME = "hasStartTime";
    public static final String HAS_END_TIME = "hasEndTime";
    public static final String HAS_TIME_TYPE = "hasTimeType";
    public static final String HAS_PATTERN = "hasQuestionPattern";
    public static final String HAS_PATTERN_FRAGMENT = "hasPatternFragment";
    // Datatype Properties
    public static final String HAS_COMMENT = "hasComment";
    public static final String HAS_EXPLANATION = "hasExplanation";
    public static final String HAS_EXPLANATION_QUERY = "hasExplanationQuery";
    public static final String HAS_INPUT_DATATYPE = "hasInputDatatype";
    public static final String HAS_LABEL = "hasLabel";
    public static final String HAS_VALUE = "hasValue";
    public static final String HAS_MIN_CARDINALITY = "minCardinality";
    public static final String HAS_MAX_CARDINALITY = "maxCardinality";
    public static final String HAS_OPTIONS_QUERY = "hasOptionsQuery";
    public static final String HAS_QUESTION_CONSTRAINT_QUERY = "hasConstraintQuery";
    public static final String HAS_TEMPLATE = "hasQuestionTemplate";
    public static final String HAS_VARIABLE_NAME = "hasVariableName";
    public static final String HAS_REPRESENTATION = "hasRepresentation";
}