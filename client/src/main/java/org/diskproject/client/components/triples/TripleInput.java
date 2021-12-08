package org.diskproject.client.components.triples;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.diskproject.client.components.customise.GWTCodeMirror;
import org.diskproject.client.place.NameTokens;
import org.diskproject.client.rest.AppNotification;
import org.diskproject.client.rest.DiskREST;
import org.diskproject.shared.classes.common.Triple;
import org.diskproject.shared.classes.util.KBConstants;
import org.diskproject.shared.classes.vocabulary.Individual;
import org.diskproject.shared.classes.vocabulary.Property;
import org.diskproject.shared.classes.vocabulary.Type;
import org.diskproject.shared.classes.vocabulary.Vocabulary;

import com.google.gwt.core.client.Callback;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.HTML;
import com.vaadin.polymer.Polymer;
import com.vaadin.polymer.elemental.Event;
import com.vaadin.polymer.elemental.EventListener;
import com.vaadin.polymer.paper.PaperIconButtonElement;
import com.vaadin.polymer.paper.widget.PaperDialog;

import edu.stanford.bmir.gwtcodemirror.client.AutoCompletionCallback;
import edu.stanford.bmir.gwtcodemirror.client.AutoCompletionChoice;
import edu.stanford.bmir.gwtcodemirror.client.AutoCompletionHandler;
import edu.stanford.bmir.gwtcodemirror.client.AutoCompletionResult;
import edu.stanford.bmir.gwtcodemirror.client.EditorPosition;

public class TripleInput extends GWTCodeMirror {
  Map<String, Vocabulary> vocabularies;
  Map<String, Property> allprops;
  Map<String, Individual> allinds;
  Map<String, Type> alltypes;
  
  String username;
  String domain;
  
  List<Triple> triples;
  
  TripleUtil util;
  boolean showInfoGutter;
  
  PaperDialog dialog;
  
  public TripleInput() {
    super();
    this.initialize();
  }
  
  public TripleInput(boolean showInfoGutter) {
    super(showInfoGutter);
    this.showInfoGutter = showInfoGutter;
    this.initialize();
  }

  public TripleInput(String mode) {
    super(mode);
    this.initialize();
  }

  protected void initialize() {
    this.vocabularies = new HashMap<String, Vocabulary>();
    this.allprops = new HashMap<String, Property>();
    this.allinds = new HashMap<String, Individual>();
    this.alltypes = new HashMap<String, Type>();
    
    this.util = new TripleUtil();
    this.setAutoCompletionHandler(this.completionHandler);
    this.addValueChangeHandler(this.changeHandler);
    
    this.dialog = new PaperDialog();
    this.getElement().appendChild(this.dialog.getElement());
  }
  
  private List<Integer[]> getWordLocations(String triple) {
    List<Integer[]> locations = new ArrayList<Integer[]>();
    int start=0, end=0, i=0;
    boolean inword = false;
    for(i=0; i<triple.length(); i++) {
      char c = triple.charAt(i);
      if(c == ' ' && inword) {
        end = i;
        inword = false;
        locations.add(new Integer[]{start, end});
      }
      else if(c != ' ' && !inword) {
        start = i;
        inword = true;
      }
    }
    if(inword)
      locations.add(new Integer[]{start, i});
    
    return locations;
  }
  
  public void setDomainInformation(String username, String domain) {
    this.username = username;
    this.domain = domain;
  }
  
  ValueChangeHandler<String> changeHandler = new ValueChangeHandler<String>() {
    @Override
    public void onValueChange(ValueChangeEvent<String> event) {
      validate();
      // Reset Triple infoboxes ?
    }
  };
  
  @SuppressWarnings("rawtypes")
  private void showInfoGutters(List<Triple> triples) {
    int i=0; 
    for(final Triple t : triples) {
      if(t.getDetails() != null) {
        PaperIconButtonElement icon = Polymer.createElement(PaperIconButtonElement.TAG);        
        icon.setIcon("icons:info");
        setInfoGutter(i, (Element)icon);
        icon.addEventListener("click", new EventListener() {
          @Override
          public void handleEvent(Event event) {
            dialog.clear();
            dialog.add(new HTML(util.toString(t)));
            dialog.add(new HTML("<b>Confidence: </b>" +
                t.getDetails().getConfidenceValue()+ "<br/> " +
                "<a href='#" + NameTokens.tlois + "/" + username + "/" + domain +
                "/" + t.getDetails().getTriggeredLOI() +"'>Click for Provenance details</a>"));
            dialog.open();
          }
        });
      }
      else {
        setInfoGutter(i, null);
      }
      i++;
    }
  }

  AutoCompletionHandler completionHandler = new AutoCompletionHandler() {
    @Override
    public void getCompletions(String text, EditorPosition caretPos,
        int caretIndex, AutoCompletionCallback callback) {

      String line = text.split("\n")[caretPos.getLineNumber()];
      String subline = line.substring(0, caretPos.getColumnNumber());
      
      List<Integer[]> locations = getWordLocations(line);
      int wordstart = 0, wordend = 0;
      int col = caretPos.getColumnNumber();
      int positionIndex = 0;
      for(Integer[] loc : locations) {
        positionIndex++;        
        if(col >= loc[0] && col <= loc[1]) {
          wordstart = loc[0];
          wordend = loc[1];
          break;
        }
      }
      String keyword = subline.substring(wordstart, col);
      
      EditorPosition fromPos = new EditorPosition(caretPos.getLineNumber(), wordstart);
      EditorPosition toPos = new EditorPosition(caretPos.getLineNumber(), wordend);

      List<String> suggestions = new ArrayList<String>();
      
      if(positionIndex == 1) {
        // Subject suggestions : All individuals       
        suggestions.addAll(allinds.keySet());
      }
      else if(positionIndex == 2) {
        // Property suggestions        
        suggestions.addAll(allprops.keySet());
        suggestions.add("a");
      }
      else if(positionIndex == 3) {
        // Object suggestions
        // -- Get all classes for property "a"
        String propstr = line.substring(locations.get(1)[0], locations.get(1)[1]);
        if(propstr.equals("a")) {
          suggestions.addAll(alltypes.keySet());
        }
        else {
          // -- TODO: Get predicate, find range, and get appropriate values        
          // -- For now just adding all individuals
          suggestions.addAll(allinds.keySet());
        }
      }
      Collections.sort(suggestions);
      
      List<AutoCompletionChoice> choices = new ArrayList<AutoCompletionChoice>();
      for(String suggestion : suggestions) {
        if(suggestion.startsWith(keyword))
          choices.add(new AutoCompletionChoice(suggestion, suggestion, 
              "cssName", fromPos, toPos));
      }
      AutoCompletionResult result = new AutoCompletionResult(choices, caretPos);
      callback.completionsReady(result);
    }
  };  
  
  public boolean validate() {
    this.clearErrorRange();
    boolean ok = true;
    
    // Validate triple items
    String[] lines = this.getValue().split("\\n");
    for(int i=0; i<lines.length; i++) {
      String line = lines[i];
      List<Integer[]> locations = this.getWordLocations(line);
      
      int start=0, end=0;
      if(locations.size() > 0) {
        String subject = line.substring(locations.get(0)[0], locations.get(0)[1]);
        start = 0;
        end = subject.length();
        if(subject.matches(".+:.+")) {
          if(!this.allinds.containsKey(subject)) {
            this.setErrorRange(new EditorPosition(i, start), 
                new EditorPosition(i, end));
            ok = false;
          }
        }
      }
        
      if(locations.size() > 1) {
        String predicate = line.substring(locations.get(1)[0], locations.get(1)[1]);
        start = end+1;
        end = start + predicate.length();
        if(predicate.matches(".+:.+")) {
          if(predicate.equals("a") || !this.allprops.containsKey(predicate)) {
            this.setErrorRange(new EditorPosition(i, start), 
                new EditorPosition(i, end));
            ok = false;
          }
        }
      }
      
      if(locations.size() > 2) {
        String object = line.substring(locations.get(2)[0], locations.get(2)[1]);
        start = end+1;
        end = start + object.length();
        if(object.matches("^\\w+:.+")) {
          if(!this.allinds.containsKey(object) && 
              !this.alltypes.containsKey(object)) {
            this.setErrorRange(new EditorPosition(i, start), 
                new EditorPosition(i, end));
            ok = false;
          }
        }
      }
    }
    return ok;
  }
  
  public List<Triple> getTriples() {
    List<Triple> triples = new ArrayList<Triple>();
    for(String tstr : this.getValue().split("\\n")) {
      Triple t = util.fromString(tstr);
      if(t != null)
        triples.add(t);
    }
    return triples;
  }
  
  public String getTripleString(List<Triple> triples) {
    String triplestr = "";
    if(triples == null)
      return triplestr;
    
    boolean done = false;
    for(Triple t : triples) {
      if(done) 
        triplestr += "\n";
      triplestr += this.util.toString(t);
      done = true;
    }
    return triplestr;
  }
  
//  @Override
  public void setStringValue(String value) {
    super.setValue(value);
    this.validate();
  }
  
  public void setValue(List<Triple> triples) {
    this.triples = triples;
    if(this.triples != null) {
      this.setValue(this.getTripleString(triples));
      if(this.showInfoGutter)
        showInfoGutters(triples);
    }
  }
  
  public void setDefaultNamespace(String ns) {
    this.util.addNamespacePrefix("", ns);
  }
  
  public Vocabulary getVocabulary(String prefix) {
    return vocabularies.get(prefix);
  }
  
  public void loadVocabulary(final String prefix, final String uri, 
      final Callback<String, Throwable> callback) {
    DiskREST.getVocabulary(new Callback<Vocabulary, Throwable>() {
      @Override
      public void onSuccess(Vocabulary result) {
        setVocabulary(prefix, result);
        if(callback != null)
          callback.onSuccess(prefix);
      }      
      @Override
      public void onFailure(Throwable reason) {
        AppNotification.notifyFailure("Could not load vocabulary for "+uri
            +" : " + reason.getMessage());
        if(callback != null)
          callback.onFailure(reason);
      }
    }, uri, false);
  }
  
  public void setVocabulary (String prefix, Vocabulary result) {
      vocabularies.put(prefix, result);
      util.addNamespacePrefix(prefix, result.getNamespace());
      loadTerms(prefix, result);
  }
  
  public void loadUserVocabulary(final String prefix, String userid, String domain,
      final Callback<String, Throwable> callback) {
    this.loadUserVocabulary(prefix, userid, domain, false, callback);
  }
  
  public void loadUserVocabulary(final String prefix, String userid, String domain,
      boolean reload,
      final Callback<String, Throwable> callback) {
    DiskREST.getUserVocabulary(new Callback<Vocabulary, Throwable>() {
      @Override
      public void onSuccess(Vocabulary result) {
        vocabularies.put(prefix, result);
        util.addNamespacePrefix(prefix, result.getNamespace());
        loadTerms(prefix, result);
        /*GWT.log("user vocabulary loaded:");
        Map<String, Individual> indvs = result.getIndividuals();
        for (String key: indvs.keySet()) {
          GWT.log(key + ": " + indvs.get(key).getName());
        }*/
          
        if(callback != null)
          callback.onSuccess(prefix);
      }      
      @Override
      public void onFailure(Throwable reason) {
        AppNotification.notifyFailure("Could not load user vocabulary"
            +" : " + reason.getMessage());
        if(callback != null)
          callback.onFailure(reason);
      }      
    }, userid, domain, reload);
  }
  
  //TODO: Can read prefixes here.
  void loadTerms(String prefix, Vocabulary vocab) {
    for(Type type : vocab.getTypes().values())
      alltypes.put(prefix+":"+type.getName(), type);
    for(Individual ind : vocab.getIndividuals().values())
      allinds.put(prefix+":"+ind.getName(), ind);
    for(Property prop : vocab.getProperties().values())
      allprops.put(prefix+":"+prop.getName(), prop);
    
    Property subcProp = new Property();
    subcProp.setId(KBConstants.RDFSNS() + "subClassOf");
    allprops.put("rdfs:subClassOf", subcProp);
    
    Property labelProp = new Property();
    labelProp.setId(KBConstants.RDFSNS() + "label");
    allprops.put("rdfs:label", labelProp);
    
    Type integerType = new Type();
    integerType.setId("http://www.w3.org/2001/XMLSchema#integer");
    alltypes.put("xsd:integer", integerType);
  }
}
