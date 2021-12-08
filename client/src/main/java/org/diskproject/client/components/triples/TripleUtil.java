package org.diskproject.client.components.triples;

import java.util.HashMap;
import java.util.Map;

import org.diskproject.shared.classes.common.Triple;
import org.diskproject.shared.classes.common.Value;
import org.diskproject.shared.classes.util.KBConstants;

import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

public class TripleUtil {
  private Map<String, String> nsmap;
  RegExp uriPattern = RegExp.compile("^<(.+)>$");
  RegExp qnamePattern = RegExp.compile("^(\\w*):(.+)$");
  RegExp typedLiteralPattern = RegExp.compile("\"(.+)\"\\^\\^(.+)");
  RegExp stringLiteralPattern = RegExp.compile("^\"(.+)\"$");
  RegExp boolLiteralPattern = RegExp.compile("^(true|false)$");
  RegExp intLiteralPattern = RegExp.compile("^\\d+$");
  RegExp floatLiteralPattern = RegExp.compile("^[\\d\\.]+$");
  
  public TripleUtil() {
    nsmap = new HashMap<String, String>();
    nsmap.put("rdf", KBConstants.RDFNS());
    nsmap.put("rdfs", KBConstants.RDFSNS());
    nsmap.put("owl", KBConstants.OWLNS());
    nsmap.put("xsd", KBConstants.XSDNS());    
  }
  
  public void addNamespacePrefix(String prefix, String ns) {
    nsmap.put(prefix, ns);
  }
  
  public Triple fromString(String tripleString) {
    String[] tarr = tripleString.split("\\s+", 3);
    String subject = this.getURIValue(tarr[0]);
    String predicate = this.getURIValue(tarr[1]);
    Value object = this.getObjectValue(tarr[2]);
    if(subject != null && predicate != null && object != null) {
      Triple triple = new Triple();
      triple.setSubject(subject);
      triple.setPredicate(predicate);
      triple.setObject(object);      
      return triple;
    }
    return null;
  }
  
  private String getURIValue(String qname) {
    if(qname.equals("a"))
      return nsmap.get("rdf") + "type";
    
    MatchResult m1 = uriPattern.exec(qname);
    if(m1 != null)
      return m1.getGroup(1);
    
    MatchResult m2 = qnamePattern.exec(qname);
    if(m2 != null) {
      if(this.nsmap.containsKey(m2.getGroup(1)))
        return this.nsmap.get(m2.getGroup(1)) + m2.getGroup(2);
    }
    
    return null;
  }
  
  private String getQName(String uri) {
    if(uri.equals(nsmap.get("rdf") + "type"))
      return "a";
    
    for(String prefix: nsmap.keySet()) {
      String nsuri = nsmap.get(prefix);
      if(uri.startsWith(nsuri)) {
        String lname = uri.substring(nsuri.length());
        return prefix + ":" + lname;
      }
    }
    return "<"+uri+">";
  }
  
  private String getObjectString(Value value) {
    if(value.getType() == Value.Type.LITERAL) {
      if(value.getDatatype() != null)
        return "\""+value.getValue()+"\"^^"+this.getQName(value.getDatatype());
      else
        return "\""+value.getValue()+"\"";
    }
    else
      return this.getQName(value.getValue().toString());
  }
  
  private Value getObjectValue(String literalstr) {
    // Check if the object is a URI
    String urival = this.getURIValue(literalstr);
    if(urival != null)
      return new Value(urival);
    
    // Check if the object is a typed literal example: "true"^^xsd:boolean
    MatchResult m1 = typedLiteralPattern.exec(literalstr);
    if(m1 != null) 
      return new Value(m1.getGroup(1), this.getURIValue(m1.getGroup(2)));

    // Check if the object is an untyped literal
    
    // Check for string
    MatchResult m2 = stringLiteralPattern.exec(literalstr);
    if(m2 != null) 
      return new Value(m2.getGroup(1), 
          KBConstants.XSDNS() + "string");
    
    // Check for int
    MatchResult m3 = intLiteralPattern.exec(literalstr);
    if(m3 != null)
      return new Value(Integer.parseInt(literalstr), 
          KBConstants.XSDNS() + "int");
    
    // Check for float
    MatchResult m4 = floatLiteralPattern.exec(literalstr);
    if(m4 != null)
      return new Value(Float.parseFloat(literalstr), 
          KBConstants.XSDNS() + "float");
    
    // Check for boolean
    MatchResult m5 = boolLiteralPattern.exec(literalstr);
    if(m5 != null)
      return new Value(Boolean.parseBoolean(literalstr), 
          KBConstants.XSDNS() + "boolean");
    
    return null;
  }
  
  public String toString(Triple t) {
    return this.getQName(t.getSubject())+" "
        + this.getQName(t.getPredicate())+" "
        + this.getObjectString(t.getObject());
  }
  
}
