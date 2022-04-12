package org.diskproject.client.components.triples;

import java.util.List;

import org.diskproject.client.Config;
import org.diskproject.shared.classes.common.Triple;
import org.diskproject.shared.classes.util.KBConstants;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.vaadin.polymer.paper.widget.PaperDialog;
import com.vaadin.polymer.paper.widget.PaperIconButton;

public class TripleViewer extends HTMLPanel {

  PaperDialog dialog;
  TripleUtil tripleUtil;
  
  String username, domain;
  
  public TripleViewer(String html) {
    super(html);
    
    tripleUtil = new TripleUtil();
    this.dialog = new PaperDialog();
    this.getElement().appendChild(this.dialog.getElement());
  }
  
  public void initialize(String username, String domain) {
    this.username = username;
    this.domain = domain;
    //tripleUtil.addNamespacePrefix("bio", KBConstants.OMICSNS());
    //tripleUtil.addNamespacePrefix("neuro", KBConstants.NEURONS());
    tripleUtil.addNamespacePrefix("hyp", KBConstants.HYPNS());
    tripleUtil.addNamespacePrefix("user", Config.getServerURL() + "/" + 
        username + "/" + domain + "/assertions#");
  }
  
  public void setDefaultNamespace(String ns) {
    tripleUtil.addNamespacePrefix("", ns);
  }
  
  public void load(List<Triple> triples) {
    this.clear();
    
    int num = 1;
    for(final Triple t : triples) {
      HTMLPanel line = new HTMLPanel("");
      line.setStyleName("triple");
      
      HTMLPanel gutter = new HTMLPanel("");
      gutter.setStyleName("gutter");
      
      final String tripleString = tripleUtil.toString(t);
      
      if(t.getDetails() != null) {
        PaperIconButton icon = new PaperIconButton();
        icon.setIcon("icons:info");
        icon.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            dialog.clear();
            dialog.add(new HTML(tripleString));
            dialog.add(new HTML("<b>Confidence: </b>" +
                t.getDetails().getConfidenceValue()));
            dialog.open();
          }
        });
        gutter.add(icon);
      }
      gutter.add(new HTML(num+""));
      HTMLPanel triple = new HTMLPanel(SafeHtmlUtils.fromString(tripleString));
      triple.setStyleName("triple-text");
      
      line.add(gutter);
      line.add(triple);
      
      this.add(line);
      num++;
    }
  }

}
