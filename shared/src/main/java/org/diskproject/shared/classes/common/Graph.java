package org.diskproject.shared.classes.common;

import java.util.ArrayList;
import java.util.List;

public class Graph {
  List<Triple> triples;

  public Graph() {
    this.triples = new ArrayList<Triple>();
  }
  public Graph(List<Triple> triples){
	  this.triples = triples;
  }
  
  public List<Triple> getTriples() {
    return triples;
  }

  public void setTriples(List<Triple> triples) {
    this.triples = triples;
  }
  
  public void addTriple(Triple triple) {
    this.triples.add(triple);
  }
  
  public List<Triple> getTriplesForSubject(String subjectid) {
    List<Triple> striples = new ArrayList<Triple>();
    for(Triple triple : this.triples) {
      if(triple.getSubject() != null && triple.getSubject().equals(subjectid))
        striples.add(triple);
    }
    return striples;
  }
  
  public String toString() {
	  String g = "";
	  for (Triple triple: this.triples) {
		  g += triple.toString() + "\n";
	  }
	  return g;
  }
  
}
