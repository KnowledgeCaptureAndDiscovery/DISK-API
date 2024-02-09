package org.diskproject.shared.classes.hypothesis;

import org.diskproject.shared.classes.DISKResource;
import org.diskproject.shared.classes.common.Graph;
import org.diskproject.shared.classes.question.Question;
import org.diskproject.shared.classes.workflow.VariableBinding;

import java.util.List;

public class Goal extends DISKResource {
    Question question;
    List<VariableBinding> questionBindings;
    Graph graph;

    public Goal () {}; //TODO: remove me

    public Goal (String id) {
        this.setId(id);
    };

    public Goal (DISKResource src) {
        super(src);
    };

    public Goal (Question question, List<VariableBinding> bindings) {
        this.question = question;
        this.questionBindings = bindings;
    }

    public Question getQuestion() {
        return question;
    }
    public void setQuestion(Question question) {
        this.question = question;
    }
    public List<VariableBinding> getQuestionBindings() {
        return questionBindings;
    }
    public void setQuestionBindings(List<VariableBinding> questionBindings) {
        this.questionBindings = questionBindings;
    }
    public Graph getGraph() {
        return graph;
    }
    public void setGraph(Graph graph) {
        this.graph = graph;
    }
}
