package org.diskproject.client.components.question;


import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;

import java.util.ArrayList;
import java.util.List;

import org.diskproject.client.components.loi.LOIEditor;
import org.diskproject.client.rest.AppNotification;
import org.diskproject.client.rest.DiskREST;
import org.diskproject.shared.classes.question.Question;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

public class QuestionSelector extends Composite {
  interface Binder extends UiBinder<Widget, QuestionSelector> {};
  private static Binder uiBinder = GWT.create(Binder.class);

  LOIEditor parent;
  @UiField DivElement questionTemplate;
  @UiField ListBox questionListBox;
  private List<Question> questions;
  private String selectedQuestionId;
  private static RegExp varPattern = RegExp.compile("\\?[a-zA-Z0-9]*", "g");

  public QuestionSelector() {
    initWidget(uiBinder.createAndBindUi(this)); 
    this.selectedQuestionId = null;
    getQuestions();
  }

  public void setParent (LOIEditor parent) {
    this.parent = parent;
  }

  public void getQuestions () {
	DiskREST.listHypothesesQuestions(new Callback<List<Question>, Throwable>() {
		@Override
		public void onSuccess(List<Question> result) {
			questions = result;
			setQuestions();
		}
		@Override
		public void onFailure(Throwable reason) {
			AppNotification.notifyFailure(reason.getMessage());
		}
	});
  }

  //Set the list of questions retrieved from the server.
  private void setQuestions () {
	  if (questions == null) return;
	  questionListBox.clear();
	  int len = questions.size();
	  int index = 0;
	  for (int i = 0; i < len; i++) {
		  Question q = questions.get(i);
		  questionListBox.addItem(q.getName(), q.getId());
		  if (selectedQuestionId != null && selectedQuestionId.equals(q.getId())) {
			  index = i;
		  }
	  }
	  questionListBox.setSelectedIndex(index);
	  onQuestionChange(null);
  }

  @UiHandler("questionListBox")
	void onQuestionChange(ChangeEvent event) {
	  String id = questionListBox.getSelectedValue();
	  if (id != null && !id.equals("")) {
		  selectedQuestionId = id;
		  updateQuestionPattern();
	  }
	}

  private void updateQuestionPattern () {
	  Question selectedQuestion = null;
	  for (Question q: questions) {
		  if (q.getId().equals(selectedQuestionId)) {
			  selectedQuestion = q;
			  break;
		  }
	  }
	  if (selectedQuestion != null) {
		  String template = selectedQuestion.getTemplate();
		  String parts[] = template.split("\\?[a-zA-Z0-9]*");
		  
		  List<String> vars = new ArrayList<String>();
		  for (MatchResult varMatcher = varPattern.exec(template); varMatcher != null; varMatcher = varPattern.exec(template)) {
			  vars.add(varMatcher.getGroup(0));
		  }
		  
		  int varlen = vars.size();
		  int len = varlen > parts.length ? varlen : parts.length;
		  
		  //Assuming each questions start with a text part.
		  questionTemplate.removeAllChildren();
		  for (int i = 0; i < len; i++) {
			 if (i < parts.length) {
				 Element spanEl = DOM.createSpan();
				 SpanElement mySpan = SpanElement.as(spanEl);
				 mySpan.setInnerText(parts[i]);
				 questionTemplate.appendChild(mySpan);
			 }
			 if (i < varlen) {
				 ListBox lb = new ListBox();
				 lb.addItem(vars.get(i).substring(1));
				 questionTemplate.appendChild(lb.getElement());
			 }
		  }
	  } 	  
  }

  public String getSelectedQuestion () {
    return questionListBox.getSelectedValue();
  }

  public void setQuestion (String id) {
    for (int i = questionListBox.getItemCount() - 1; i >= 0; i--) {
      String val = questionListBox.getValue(i);
      if (val.contains(id)) {
    	  questionListBox.setItemSelected(i, true);
    	  break;
      }
    }
    this.onQuestionChange(null);
  }

	@UiHandler("addPattern")
	void onAddTermButtonClicked(ClickEvent event) {
		String id = questionListBox.getSelectedValue();
		for (Question q: questions) {
			if (q.getId().equals(id)) {
				parent.setHypothesis(q.getPattern());
				break;
			}
		}
	}
}