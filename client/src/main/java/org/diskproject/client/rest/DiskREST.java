package org.diskproject.client.rest;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.diskproject.client.Config;
import org.diskproject.client.authentication.KeycloakUser;
import org.diskproject.client.authentication.AuthenticatedDispatcher;
import org.diskproject.shared.api.DiskService;
import org.diskproject.shared.classes.common.Graph;
import org.diskproject.shared.classes.common.TreeItem;
import org.diskproject.shared.classes.hypothesis.Hypothesis;
import org.diskproject.shared.classes.loi.LineOfInquiry;
import org.diskproject.shared.classes.loi.TriggeredLOI;
import org.diskproject.shared.classes.question.Question;
import org.diskproject.shared.classes.vocabulary.Vocabulary;
import org.diskproject.shared.classes.workflow.Variable;
import org.diskproject.shared.classes.workflow.Workflow;
import org.diskproject.shared.classes.workflow.WorkflowRun;
import org.fusesource.restygwt.client.Defaults;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;
import org.fusesource.restygwt.client.REST;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.i18n.shared.DateTimeFormat;

public class DiskREST {
  public static DiskService diskService;
  
  //Vocabulary
  static class VocabularyCallbacks {
    List<Callback<Vocabulary, Throwable>> callbacks;
    public VocabularyCallbacks() {
      callbacks = new ArrayList<Callback<Vocabulary, Throwable>>();
    };
    public void add(Callback<Vocabulary, Throwable> callback) {
      this.callbacks.add(callback);
    }
    public boolean isEmpty() {
      return this.callbacks.isEmpty();
    }
    public void clear() {
      this.callbacks.clear();
    }
    public List<Callback<Vocabulary, Throwable>> getCallbacks() {
      return this.callbacks;
    }
  };

  //private static Vocabulary user_vocabulary;
  //private static VocabularyCallbacks user_vocabulary_callbacks = new VocabularyCallbacks();
  
  private static Map<String, Vocabulary> vocabularies = 
      new HashMap<String, Vocabulary>();
  private static Map<String, VocabularyCallbacks> vocabulary_callbacks =
      new HashMap<String, VocabularyCallbacks>(); 
  
  //Workflows
  private static List<Workflow> workflows = 
      new ArrayList<Workflow>();
  private static Map<String, List<Variable>> workflow_variables =
      new HashMap<String, List<Variable>>();
  
  private static String username, domain;

  private static String stackTraceToString(Throwable e) {
      StringBuilder sb = new StringBuilder();
      for (StackTraceElement element : e.getStackTrace()) {
          sb.append(element.toString());
          sb.append("\n");
      }
      return sb.toString();
  }

  public static DiskService getDiskService() {
    if(diskService == null) {
      Defaults.setServiceRoot(Config.getServerURL());
      Defaults.setDateFormat(null);
      Defaults.setDispatcher(new AuthenticatedDispatcher());
      diskService = GWT.create(DiskService.class);
    }
    return diskService;
  }

  public static void setUsername(String username) {
    DiskREST.username = username;
  }

  public static void setDomain(String domain) {
    DiskREST.domain = domain;
  }

  /*
   * Vocabulary
   */
  public static void getVocabulary(
      final Callback<Vocabulary, Throwable> callback,
      final String uri,
      boolean reload) {
    if(vocabularies.containsKey(uri) && !reload) {
      callback.onSuccess(vocabularies.get(uri));
    }
    else {
      if(!vocabulary_callbacks.containsKey(uri)) {  
        vocabulary_callbacks.put(uri, new VocabularyCallbacks());
        vocabulary_callbacks.get(uri).add(callback);
        
        REST.withCallback(new MethodCallback<Map<String, Vocabulary>>() {
          @Override
          public void onSuccess(Method method, Map<String, Vocabulary> vocabs) {
            vocabularies = vocabs;
            for(Callback<Vocabulary, Throwable> cb : 
                vocabulary_callbacks.get(uri).getCallbacks())
              cb.onSuccess(vocabularies.get(uri));
            vocabulary_callbacks.get(uri).clear();
          }
          @Override
          public void onFailure(Method method, Throwable exception) {
            AppNotification.notifyFailure("Could not load vocabularies");
            callback.onFailure(exception);
          }
        }).call(getDiskService()).getVocabularies();
      }
      else {
        vocabulary_callbacks.get(uri).add(callback);
      }
    }
  }
  
  /*public static void getUserVocabulary(
      final Callback<Vocabulary, Throwable> callback,
      String username, String domain,
      boolean reload) {
    if(user_vocabulary != null && !reload) {
      callback.onSuccess(user_vocabulary);
    }
    else {
      if(user_vocabulary_callbacks.isEmpty()) {
        user_vocabulary_callbacks.add(callback);        
        REST.withCallback(new MethodCallback<Vocabulary>() {
          @Override
          public void onSuccess(Method method, Vocabulary vocab) {
            user_vocabulary = vocab;
            for(Callback<Vocabulary, Throwable> vcb : 
                user_vocabulary_callbacks.getCallbacks())
              vcb.onSuccess(user_vocabulary);
            user_vocabulary_callbacks.clear();
          }
          @Override
          public void onFailure(Method method, Throwable exception) {
            AppNotification.notifyFailure("Could not load user vocabulary");
            callback.onFailure(exception);
          }
        }).call(getDiskService()).getUserVocabulary();        
      }
      else {
        user_vocabulary_callbacks.add(callback);
      }
    }
  }*/
  
  /*
   * Hypotheses
   */
  public static void listHypotheses(final Callback<List<TreeItem>, Throwable> callback) {
    REST.withCallback(new MethodCallback<List<TreeItem>>() {
      @Override
      public void onFailure(Method method, Throwable exception) {
        callback.onFailure(exception);
      }
      @Override
      public void onSuccess(Method method, List<TreeItem> response) {
        callback.onSuccess(response);
      }
    }).call(getDiskService()).listHypotheses();
  }
  
  public static void getHypothesis(String id, 
      final Callback<Hypothesis, Throwable> callback) {
    REST.withCallback(new MethodCallback<Hypothesis>() {
      @Override
      public void onSuccess(Method method, Hypothesis response) {
        callback.onSuccess(response);
      }
      @Override
      public void onFailure(Method method, Throwable exception) {
    	  GWT.log("ERROR HERE!");
        callback.onFailure(exception);
      }
    }).call(getDiskService()).getHypothesis(id);
  }

  public static void addHypothesis(Hypothesis hypothesis,
      final Callback<Void, Throwable> callback) {
	String user = KeycloakUser.getUsername();
	if (user.equals("guest")) {
		AppNotification.notifyFailure("Guest account can not create Hypothesis");
		return;
	}
	  
	DateTimeFormat fm = DateTimeFormat.getFormat("HH:mm:ss yyyy-MM-dd");
	String date = fm.format(new Date());

	if (hypothesis.getDateCreated() != null) {
		hypothesis.setDateModified(date);
	} else {
		hypothesis.setDateCreated(date);
	}
	hypothesis.setAuthor(user);

    REST.withCallback(new MethodCallback<Void>() {
      @Override
      public void onSuccess(Method method, Void response) {
        callback.onSuccess(response);
      }
      @Override
      public void onFailure(Method method, Throwable exception) {
        callback.onFailure(exception);
      }      
    }).call(getDiskService()).addHypothesis(hypothesis);
  }

  public static void updateHypothesis(Hypothesis hypothesis,
      final Callback<Void, Throwable> callback) {
	String user = KeycloakUser.getUsername();
	if (user.equals("guest")) {
		AppNotification.notifyFailure("Guest account can not update Hypothesis");
		return;
	}
    REST.withCallback(new MethodCallback<Void>() {
      @Override
      public void onSuccess(Method method, Void response) {
        callback.onSuccess(response);
      }
      @Override
      public void onFailure(Method method, Throwable exception) {
        callback.onFailure(exception);
      }      
    }).call(getDiskService()).updateHypothesis(hypothesis.getId(), hypothesis);
  }

  public static void deleteHypothesis(String id,
      final Callback<Void, Throwable> callback) {
	String user = KeycloakUser.getUsername();
	if (user.equals("guest")) {
		AppNotification.notifyFailure("Guest account can not delete Hypothesis");
		return;
	}
    REST.withCallback(new MethodCallback<Void>() {
      @Override
      public void onSuccess(Method method, Void response) {
        callback.onSuccess(response);
      }
      @Override
      public void onFailure(Method method, Throwable exception) {
        callback.onFailure(exception);
      }      
    }).call(getDiskService()).deleteHypothesis(id);
  }  

  public static void queryHypothesis(String id,
      final Callback<List<TriggeredLOI>, Throwable> callback) {
	String user = KeycloakUser.getUsername();
	if (user.equals("guest")) {
		AppNotification.notifyFailure("Guest account can not run hypotheses");
		return;
	}
    REST.withCallback(new MethodCallback<List<TriggeredLOI>>() {
      @Override
      public void onFailure(Method method, Throwable exception) {
        callback.onFailure(exception);
      }
      @Override
      public void onSuccess(Method method, List<TriggeredLOI> response) {
        callback.onSuccess(response);
      }
    }).call(getDiskService()).queryHypothesis(id);
  }

  /*
   * Lines of Inquiry
   */
  public static void listLOI(final Callback<List<TreeItem>, Throwable> callback) {
    REST.withCallback(new MethodCallback<List<TreeItem>>() {
      @Override
      public void onFailure(Method method, Throwable exception) {
        callback.onFailure(exception);
      }
      @Override
      public void onSuccess(Method method, List<TreeItem> response) {
        callback.onSuccess(response);
      }
    }).call(getDiskService()).listLOIs();
  }

  public static void getLOI(String id, 
      final Callback<LineOfInquiry, Throwable> callback) {
    REST.withCallback(new MethodCallback<LineOfInquiry>() {
      @Override
      public void onSuccess(Method method, LineOfInquiry response) {
        callback.onSuccess(response);
      }
      @Override
      public void onFailure(Method method, Throwable exception) {
        callback.onFailure(exception);
      }
    }).call(getDiskService()).getLOI( id);
  }

  public static void addLOI(LineOfInquiry loi,
      final Callback<Void, Throwable> callback) {
	String user = KeycloakUser.getUsername();
	if (user.equals("guest")) {
		AppNotification.notifyFailure("Guest account can not create lines of inquiry");
		return;
	}
	DateTimeFormat fm = DateTimeFormat.getFormat("HH:mm:ss yyyy-MM-dd");
	String date = fm.format(new Date());
	
	loi.setDateCreated(date);
	loi.setAuthor(user);

    REST.withCallback(new MethodCallback<Void>() {
      @Override
      public void onSuccess(Method method, Void response) {
        callback.onSuccess(response);
      }
      @Override
      public void onFailure(Method method, Throwable exception) {
        callback.onFailure(exception);
      }      
    }).call(getDiskService()).addLOI( loi);
  }

  public static void deleteLOI(String id,
      final Callback<Void, Throwable> callback) {
	String user = KeycloakUser.getUsername();
	if (user.equals("guest")) {
		AppNotification.notifyFailure("Guest account can not delete lines of inquiry");
		return;
	}
    REST.withCallback(new MethodCallback<Void>() {
      @Override
      public void onSuccess(Method method, Void response) {
        callback.onSuccess(response);
      }
      @Override
      public void onFailure(Method method, Throwable exception) {
        callback.onFailure(exception);
      }      
    }).call(getDiskService()).deleteLOI( id);
  }
  
  public static void updateLOI(LineOfInquiry loi,
      final Callback<Void, Throwable> callback) {
	String user = KeycloakUser.getUsername();
	if (user.equals("guest")) {
		AppNotification.notifyFailure("Guest account can not update lines of inquiry");
		return;
	}

	DateTimeFormat fm = DateTimeFormat.getFormat("HH:mm:ss yyyy-MM-dd");
	String date = fm.format(new Date());

	GWT.log(date);
	GWT.log(KeycloakUser.getUsername());
	
	if (loi.getDateCreated() == null) {
		loi.setDateCreated(date);
	}
	loi.setDateModified(date);
	loi.setAuthor(user);
	  
    REST.withCallback(new MethodCallback<Void>() {
      @Override
      public void onSuccess(Method method, Void response) {
        callback.onSuccess(response);
      }
      @Override
      public void onFailure(Method method, Throwable exception) {
        callback.onFailure(exception);
      }      
    }).call(getDiskService()).updateLOI( 
        loi.getId(), loi);
  }  
  
  /*
   * Triggered LOIs
   */
  public static void addTriggeredLOI(TriggeredLOI tloi, 
      final Callback<Void, Throwable> callback) {
	String user = KeycloakUser.getUsername();
	if (user.equals("guest")) {
		AppNotification.notifyFailure("Guest account can not create triggered lines of inquiry");
		return;
	}

	DateTimeFormat fm = DateTimeFormat.getFormat("HH:mm:ss yyyy-MM-dd");
	String date = fm.format(new Date());
	
	if (tloi.getDateCreated() != null) {
		tloi.setDateModified(date);
	} else {
		tloi.setDateCreated(date);
	}
	
	tloi.setAuthor(user);

    REST.withCallback(new MethodCallback<Void>() {
      @Override
      public void onSuccess(Method method, Void response) {
        callback.onSuccess(response);
      }
      @Override
      public void onFailure(Method method, Throwable exception) {
        callback.onFailure(exception);
      }      
    }).call(getDiskService()).addTriggeredLOI( tloi);
  }
  
  public static void listTriggeredLOIs(final Callback<List<TriggeredLOI>, Throwable> callback) {
    REST.withCallback(new MethodCallback<List<TriggeredLOI>>() {
      @Override
      public void onFailure(Method method, Throwable exception) {
        callback.onFailure(exception);
      }
      @Override
      public void onSuccess(Method method, List<TriggeredLOI> response) {
        callback.onSuccess(response);
      }
    }).call(getDiskService()).listTriggeredLOIs();
  }

  public static void getTriggeredLOI(String id, 
      final Callback<TriggeredLOI, Throwable> callback) {
    REST.withCallback(new MethodCallback<TriggeredLOI>() {
      @Override
      public void onSuccess(Method method, TriggeredLOI response) {
        callback.onSuccess(response);
      }
      @Override
      public void onFailure(Method method, Throwable exception) {
        callback.onFailure(exception);
      }
    }).call(getDiskService()).getTriggeredLOI( id);
  }

  public static void deleteTriggeredLOI(String id,
      final Callback<Void, Throwable> callback) {
	String user = KeycloakUser.getUsername();
	if (user.equals("guest")) {
		AppNotification.notifyFailure("Guest account can not delete triggered lines of inquiry");
		return;
	}
    REST.withCallback(new MethodCallback<Void>() {
      @Override
      public void onSuccess(Method method, Void response) {
        callback.onSuccess(response);
      }
      @Override
      public void onFailure(Method method, Throwable exception) {
        callback.onFailure(exception);
      }      
    }).call(getDiskService()).deleteTriggeredLOI( id);
  }  

  /*
   * Assertions
   */

  public static void updateAssertions(Graph graph,
      final Callback<Void, Throwable> callback) {
	String user = KeycloakUser.getUsername();
	if (user.equals("guest")) {
		AppNotification.notifyFailure("Guest account can not update assertions");
		return;
	}
    REST.withCallback(new MethodCallback<Void>() {
      @Override
      public void onSuccess(Method method, Void response) {
        callback.onSuccess(response);
      }
      @Override
      public void onFailure(Method method, Throwable exception) {
        callback.onFailure(exception);
      }      
    }).call(getDiskService()).updateAssertions( graph);
  }
  
  public static void listAssertions(
      final Callback<Graph, Throwable> callback) {
    REST.withCallback(new MethodCallback<Graph>() {
      @Override
      public void onSuccess(Method method, Graph response) {
        callback.onSuccess(response);
      }
      @Override
      public void onFailure(Method method, Throwable exception) {
        callback.onFailure(exception);
      }      
    }).call(getDiskService()).listAssertions();
  }
  
  /*
   * Workflows
   */
  
  public static void listWorkflows(
      final Callback<List<Workflow>, Throwable> callback) {
    if(workflows.size() > 0)
      callback.onSuccess(workflows);
    REST.withCallback(new MethodCallback<List<Workflow>>() {
      @Override
      public void onSuccess(Method method, List<Workflow> response) {
        if(response == null)
          callback.onFailure(new Throwable("No WINGS workflows found"));
        else {
          workflows = response;
          callback.onSuccess(response);
        }
      }
      @Override
      public void onFailure(Method method, Throwable exception) {
        GWT.log(stackTraceToString(exception));
        callback.onFailure(exception);
      }      
    }).call(getDiskService()).listWorkflows();
  }
  
  public static void getWorkflowVariables(final String id, 
      final Callback<List<Variable>, Throwable> callback) {
	GWT.log("getWorkflowVariables id:" + id);
    if(workflow_variables.containsKey(id)) {
      callback.onSuccess(workflow_variables.get(id));
      return;
    }
    
    REST.withCallback(new MethodCallback<List<Variable>>() {
      @Override
      public void onSuccess(Method method, List<Variable> response) {
        if(response == null) {
          callback.onFailure(new Throwable("No WINGS workflow variables found"));
        }
        else {
          workflow_variables.put(id, response);
          callback.onSuccess(response);
        }
      }
      @Override
      public void onFailure(Method method, Throwable exception) {
        callback.onFailure(exception);
      }      
    }).call(getDiskService()).getWorkflowVariables( id);
  }
  
  public static void monitorWorkflow(final String id,
	      final Callback<WorkflowRun, Throwable> callback) {
	  	/**/GWT.log("> monitorWorkflow");
	    REST.withCallback(new MethodCallback<WorkflowRun>() {
	      @Override
	      public void onSuccess(Method method, WorkflowRun response) {
	    	  GWT.log("Success: " + response.toString());
	    	  callback.onSuccess(response);
	      }
	      @Override
	      public void onFailure(Method method, Throwable exception) {
	    	GWT.log("ERROR");
	        GWT.log(stackTraceToString(exception));
	        callback.onFailure(exception);
	      }      
	    }).call(getDiskService()).monitorWorkflow( id);
	  } 

  /*
   * Endpoints
   */

  public static void getEndpoints(
    final Callback<Map<String, String>, Throwable> callback) {      
      if (Config.endpoints != null) {
        callback.onSuccess(Config.endpoints);
      } else {
        REST.withCallback(new MethodCallback<Map<String, String>>() {
          @Override
          public void onSuccess(Method method, Map<String, String> endp) {
            Config.endpoints = endp;
            callback.onSuccess(endp);
          }
          @Override
          public void onFailure(Method method, Throwable exception) {
            AppNotification.notifyFailure("Could not load endpoint list");
            callback.onFailure(exception);
          }
        }).call(getDiskService()).getEndpoints();
      }
  }

  public static void queryExternalStore(String endpoint, String query, String variables,
      final Callback<Map<String, List<String>>, Throwable> callback) {
	 GWT.log("variables: " + variables);
	 GWT.log("query: " + query);
    REST.withCallback(new MethodCallback<Map<String, List<String>>>() {
      @Override
      public void onSuccess(Method method, Map<String, List<String>> response) {
        callback.onSuccess(response);
      }
      @Override
      public void onFailure(Method method, Throwable exception) {
        callback.onFailure(exception);
      }      
    }).call(getDiskService()).queryExternalStore(endpoint, variables, query);
  }
  
  /*
   * Questions
   */
  public static void listHypothesesQuestions(final Callback<List<Question>, Throwable> callback) {
    REST.withCallback(new MethodCallback<List<Question>>() {
      @Override
      public void onFailure(Method method, Throwable exception) {
        callback.onFailure(exception);
      }
      @Override
      public void onSuccess(Method method, List<Question> response) {
        callback.onSuccess(response);
      }
    }).call(getDiskService()).listQuestions();
  }

  public static void listVariableOptions(String id, final Callback<List<List<String>>, Throwable> callback) {
	//Trim id
	String[] sp = id.split("/");
	String qid = sp[sp.length-1];
    REST.withCallback(new MethodCallback<List<List<String>>>() {
      @Override
      public void onFailure(Method method, Throwable exception) {
        callback.onFailure(exception);
      }
      @Override
      public void onSuccess(Method method, List<List<String>> response) {
        callback.onSuccess(response);
      }
    }).call(getDiskService()).listOptions( qid);
  }
  
  /* Narratives */

  public static void getTLOINarratives(final String tloiid,
		  final Callback<Map<String, String>, Throwable> callback) {
    REST.withCallback(new MethodCallback<Map<String, String>>() {
      @Override
      public void onFailure(Method method, Throwable exception) {
        callback.onFailure(exception);
      }
      @Override
      public void onSuccess(Method method, Map<String, String> response) {
        callback.onSuccess(response);
      }
    }).call(getDiskService()).getNarratives( tloiid);
  }

  public static void getDataFromWings(String dataid,
      final Callback<String, Throwable> callback) {
    REST.withCallback(new MethodCallback<String>() {
      @Override
      public void onSuccess(Method method, String response) {
        callback.onSuccess(response);
      }
      @Override
      public void onFailure(Method method, Throwable exception) {
        callback.onFailure(exception);
      }      
    }).call(getDiskService()).getDataFromWings( dataid);
  }

	public static void getDataFromWingsAsJS(String dataid, final Callback<JavaScriptObject, Throwable> callback) {
		String url = Config.getServerURL() + "/" + username + "/" + domain + "/wings-data/" + dataid;
		RequestBuilder builder =  new RequestBuilder(RequestBuilder.GET, url);

		builder.setHeader("Content-Type", "application/json");
		builder.setHeader("Accept", "application/json");

		try {
			builder.sendRequest(null, new RequestCallback() {
				@Override
				public void onResponseReceived(Request request, Response response) {
					if (response.getStatusCode() == 200) {
						JavaScriptObject json = JsonUtils.safeEval(response.getText());
						callback.onSuccess(json);
					} else {
						GWT.log("Status code error:" + response.getStatusCode());
					}
				}
				@Override
				public void onError(Request request, Throwable exception) {
					GWT.log("error2");
					// TODO Auto-generated method stub
				}
			});
		} catch (Exception e) {
			GWT.log("some error");
			// TODO: handle exception
		}
	}
	
    public static void getNewExecution(String hypid, String loiid,
        final Callback<List<TriggeredLOI>, Throwable> callback) {
      REST.withCallback(new MethodCallback<List<TriggeredLOI>>() {
        @Override
        public void onSuccess(Method method, List<TriggeredLOI> response) {
          callback.onSuccess(response);
        }
        @Override
        public void onFailure(Method method, Throwable exception) {
          callback.onFailure(exception);
        }
      }).call(getDiskService()).runHypothesisAndLOI( hypid, loiid);
    }
}