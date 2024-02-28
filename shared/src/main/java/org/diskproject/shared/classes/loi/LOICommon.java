package org.diskproject.shared.classes.loi;

import org.diskproject.shared.classes.DISKResource;
import org.diskproject.shared.classes.question.Question;

public class LOICommon extends DISKResource {
    public static class UpdateConditions {
        public static final int
                ON_DATA_UPDATE = 1,
                ON_METHOD_UPDATE = 2,
                MANUAL = 4;
    }
    int updateCondition = UpdateConditions.ON_DATA_UPDATE;
    String goalQuery; //Simple query to get the graph.
    Question question;

    public LOICommon () {}

    public LOICommon (DISKResource src) {
        super(src);
    }

    public LOICommon (String id){
        this.setId(id);
    }

    public LOICommon(String id, String name, LOICommon src){
        super(id,name,src.getDescription());
        this.updateCondition = src.getUpdateCondition();
        this.question = src.getQuestion();
        this.goalQuery = src.getGoalQuery();
    }

    public void setUpdateCondition (int b) {
        this.updateCondition = b;
    }

    public int getUpdateCondition () {
        return this.updateCondition;
    }

    public String getGoalQuery() {
        return goalQuery;
    }

    public void setGoalQuery(String goalQuery) {
        this.goalQuery = goalQuery;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }
}