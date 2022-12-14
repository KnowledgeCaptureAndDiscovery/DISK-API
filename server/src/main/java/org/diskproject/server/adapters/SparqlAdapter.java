package org.diskproject.server.adapters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.sparql.engine.http.QueryExceptionHTTP;
import org.diskproject.shared.classes.adapters.DataAdapter;
import org.diskproject.shared.classes.adapters.DataResult;
import org.diskproject.shared.classes.util.KBConstants;

import edu.isi.kcap.ontapi.KBAPI;
import edu.isi.kcap.ontapi.KBObject;
import edu.isi.kcap.ontapi.OntSpec;
import edu.isi.kcap.ontapi.SparqlQuerySolution;
import edu.isi.kcap.ontapi.jena.KBAPIJena;

public class SparqlAdapter extends DataAdapter {
    private final KBAPI plainKb = new KBAPIJena(OntSpec.PLAIN);
    private final static Pattern varPattern = Pattern.compile("\\?(.+?)\\b");
    private CloseableHttpClient httpClient;
    private PoolingHttpClientConnectionManager connectionManager;

    public SparqlAdapter(String endpoint, String name, String username, String password) {
        super(endpoint, name, username, password);
        
        connectionManager = new PoolingHttpClientConnectionManager();
        httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();
    }

    public static Set<String> interceptVariables(final String queryA, final String queryB) {
        Set<String> A = new HashSet<String>();
        Matcher a = varPattern.matcher(queryA);
        while (a.find())
            A.add(a.group());

        Set<String> B = new HashSet<String>();
        Matcher b = varPattern.matcher(queryB);
        while (b.find()) {
            String v = b.group();
            for (String v2 : A) {
                if (v.equals(v2)) {
                    B.add(v);
                }
            }
        }
        return B;
    }

    @Override
    public List<DataResult> query(String queryString) throws Exception, QueryParseException, QueryExceptionHTTP {
        System.out.println("SparqlAdapter.query: " + queryString);
        ArrayList<ArrayList<SparqlQuerySolution>> solutions = null;
        try {
            String user = this.getUsername(), pass = this.getPassword();
            if (user != null && pass != null) {
                solutions = plainKb.sparqlQueryRemote(queryString, this.getEndpointUrl(), this.getUsername(),
                        this.getPassword());
            } else {
                solutions = plainKb.sparqlQueryRemote(queryString, this.getEndpointUrl());
            }
        } catch (Exception e) {
            System.err.println("Error querying SPARQL endpoint: " + e.getMessage());
            throw e;
        }
        List<DataResult> results = new ArrayList<DataResult>();

        if (solutions != null) {
            for (ArrayList<SparqlQuerySolution> row : solutions) {
                DataResult curResult = new DataResult();
                for (SparqlQuerySolution cell : row) {
                    String varName = cell.getVariable();
                    KBObject varValue = cell.getObject();
                    if (varValue != null) {
                        if (varValue.isLiteral()) {
                            curResult.addValue(varName, varValue.getValueAsString());
                        } else {
                            String name = varValue.getName();
                            name = name.replaceAll("-", "%"); // Semantic media wiki changes % to -
                            curResult.addValue(varName, varValue.getID(), name);
                        }
                    } else {
                        curResult.addValue(varName, null);
                    }
                }
                results.add(curResult);
            }
            }
        return results;
    }

    @Override
    public List<DataResult> queryOptions(String varname, String queryPart) throws Exception, QueryParseException {
        String name = varname.substring(1);
        String labelVar = varname + "Label";
        String query = "PREFIX xsd:  <" + KBConstants.XSD_NS + ">\n" +
                "PREFIX rdfs: <" + KBConstants.RDFS_NS + ">\n" +
                "PREFIX rdf:  <" + KBConstants.RDF_NS + ">\n" +
                "SELECT DISTINCT " + varname + " " + labelVar + " WHERE {\n" +
                queryPart;

        if (!queryPart.contains(labelVar))
            query += "\n  OPTIONAL { " + varname + " rdfs:label " + labelVar + " . }";
        query += "\n}";

        List<DataResult> solutions = this.query(query);
        List<DataResult> fixedSolutions = new ArrayList<DataResult>();

        for (DataResult solution : solutions) {
            DataResult cur = new DataResult();
            String valUrl = solution.getValue(name);
            String valName = solution.getName(name);
            String label = solution.getValue(name + "Label");
            if (label == null && valName != null) {
                label = valName;
            } else if (label == null) {
                label = valUrl.replaceAll("^.*\\/", "");
                // Try to remove mediawiki stuff
                label = label.replaceAll("Property-3A", "");
                label = label.replaceAll("-28E-29", "");
                label = label.replaceAll("_", " ");
            }
            cur.addValue(VARURI, valUrl);
            cur.addValue(VARLABEL, label);
            fixedSolutions.add(cur);
        }

        return fixedSolutions;
    }

    @Override
    public Map<String, String> getFileHashes(List<String> files) throws Exception, QueryParseException {
        String query = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
                + "SELECT DISTINCT ?file ?sha WHERE {\n"
                + "  ?page ?contentUrl ?file .\n"
                + "  ?page ?hashProp ?sha .\n"
                + "  ?contentUrl rdfs:label \"HasContentUrl (E)\" .\n"
                + "  ?hashProp rdfs:label \"Checksum (E)\"\n"
                + "  VALUES ?file {\n    <"
                + String.join(">\n    <", files) + ">\n"
                + "  }\n"
                + "}";

        Map<String, String> result = new HashMap<String, String>();

        List<DataResult> solutions = this.query(query);

        for (DataResult solution : solutions) {
            String filename = solution.getValue("file");
            String sha = solution.getValue("sha");
            if (filename != null && sha != null)
                result.put(filename, sha);
        }

        return result;

    }

    public Map<String, String> getFileHashesByETag(List<String> files) {
        Map<String, String> result = new HashMap<String, String>();
        for (String file : files) {
            String eTag = getFileETag(file);
            if (eTag != null)
                result.put(file, eTag);
        }
        return result;
    }

    public String getFileETag(String url){
        try {
            HttpHead request = new HttpHead(url); 
            HttpResponse response = httpClient.execute(request);
            if (response.containsHeader(HttpHeaders.ETAG)) {
                Header[] eHeaders = response.getHeaders(HttpHeaders.ETAG);
                if (eHeaders.length > 0) {
                    String eTag = eHeaders[0].getValue();
                    return eTag.replaceAll("\"", "").replaceAll("/", "").replaceAll("_", "").replaceAll("-", "");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean ping() {
        try {
            List<DataResult> results = this.query("SELECT * WHERE { ?a ?b ?c . } LIMIT 1");
            return results.size() > 0;
        } catch (Exception e) {
            return false;
        }
    }

}