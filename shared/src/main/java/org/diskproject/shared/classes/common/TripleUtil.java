package org.diskproject.shared.classes.common;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.diskproject.shared.classes.util.KBConstants;

public class TripleUtil {
  private Map<String, String> nsMap;
  Pattern uriPattern = Pattern.compile("^<(.+)>$");
  Pattern qNamePattern = Pattern.compile("^(\\w*):(.+)$");
  Pattern typedLiteralPattern = Pattern.compile("\"(.+)\"\\^\\^(.+)");
  Pattern stringLiteralPattern = Pattern.compile("^\"(.+)\"$");
  Pattern boolLiteralPattern = Pattern.compile("^(true|false)$");
  Pattern intLiteralPattern = Pattern.compile("^\\d+$");
  Pattern floatLiteralPattern = Pattern.compile("^[\\d\\.]+$");
  
  public TripleUtil() {
    nsMap = new HashMap<String, String>();
    nsMap.put("rdf", KBConstants.RDF_NS);
    nsMap.put("rdfs", KBConstants.RDFS_NS);
    nsMap.put("owl", KBConstants.OWL_NS);
    nsMap.put("xsd", KBConstants.XSD_NS);    

  }
  
  public void addNamespacePrefix(String prefix, String ns) {
    nsMap.put(prefix, ns);
  }
  
  public Triple fromString(String tripleString) {
    String[] tArr = tripleString.split("\\s+", 3);
    String subject = this.getURIValue(tArr[0]);
    String predicate = this.getURIValue(tArr[1]);
    Value object = this.getObjectValue(tArr[2]);

    if(subject != null && predicate != null && object != null) {
      Triple triple = new Triple();
      triple.setSubject(subject);
      triple.setPredicate(predicate);
      triple.setObject(object);      
      return triple;
    }
    return null;
  }
  
  private String getURIValue(String qName) {
    if(qName.equals("a"))
      return nsMap.get("rdf") + "type";
    
    Matcher m1 = uriPattern.matcher(qName);
    if(m1.find())
      return m1.group(1);
    Matcher m2 = qNamePattern.matcher(qName);
    if(m2.find()) {
      if(this.nsMap.containsKey(m2.group(1)))
        return this.nsMap.get(m2.group(1)) + m2.group(2);
    }
    
    return null;
  }
  
  private String getQName(String uri) {
    if(uri.equals(nsMap.get("rdf") + "type"))
      return "a";
    
    for(String prefix: nsMap.keySet()) {
      String nsUri = nsMap.get(prefix);
      if(uri.startsWith(nsUri)) {
        String lname = uri.substring(nsUri.length());
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
    String uriVal = this.getURIValue(literalstr);
    if(uriVal != null)
      return new Value(uriVal);
    
    // Check if the object is a typed literal example: "true"^^xsd:boolean
    Matcher m1 = typedLiteralPattern.matcher(literalstr);
    if(m1.find()) 
      return new Value(m1.group(1), this.getURIValue(m1.group(2)));

    // Check if the object is an untyped literal
    
    // Check for string
    Matcher m2 = stringLiteralPattern.matcher(literalstr);
    if(m2.find()) 
      return new Value(m2.group(1), 
          KBConstants.XSD_NS + "string");
    
    // Check for int
    Matcher m3 = intLiteralPattern.matcher(literalstr);
    if(m3.find())
      return new Value(Integer.parseInt(literalstr), 
          KBConstants.XSD_NS + "int");
    
    // Check for float
    Matcher m4 = floatLiteralPattern.matcher(literalstr);
    if(m4.find())
      return new Value(Float.parseFloat(literalstr), 
          KBConstants.XSD_NS + "float");
    
    // Check for boolean
    Matcher m5 = boolLiteralPattern.matcher(literalstr);
    if(m5.find())
      return new Value(Boolean.parseBoolean(literalstr), 
          KBConstants.XSD_NS + "boolean");
    
    return null;
  }
  
  public String toString(Triple t) {
	  
    return this.getQName(t.getSubject())+" "
        + this.getQName(t.getPredicate())+" "
        + this.getObjectString(t.getObject());
  }
  
  
}

