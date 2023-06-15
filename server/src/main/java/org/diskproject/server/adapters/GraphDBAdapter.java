package org.diskproject.server.adapters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.diskproject.shared.classes.adapters.DataAdapter;
import org.diskproject.shared.classes.adapters.DataResult;
import org.diskproject.shared.classes.util.KBConstants;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class GraphDBAdapter extends DataAdapter {
    private CloseableHttpClient httpClient;
    private PoolingHttpClientConnectionManager connectionManager;
    private String token, repository;

	private JsonParser jsonParser;

    public GraphDBAdapter(String URI, String name, String username, String password) {
        super(URI, name, username, password);
        token = null;
        connectionManager = new PoolingHttpClientConnectionManager();
		jsonParser = new JsonParser();
        httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();
    }

    public void setRepository (String id) {
        this.repository = id;
    }

    private boolean login () {
        String loginUrl = getEndpointUrl() + "rest/login/" + getUsername();
        HttpPost request = new HttpPost(loginUrl);
        request.addHeader("X-GraphDB-Password", getPassword());
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            if (response.containsHeader("Authorization")) {
                for (Header h : response.getHeaders("Authorization")) {
                    if (h.getValue() != null) {
                        token = h.getValue();
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<DataResult> getDataResultFromJsonString (String jsonString) {
        JsonArray variables = null;
        JsonArray bindings = null;
        try {
            JsonObject json = (JsonObject) jsonParser.parse(jsonString);
            variables = json.get("head").getAsJsonObject().get("vars").getAsJsonArray();
            bindings = json.get("results").getAsJsonObject().get("bindings").getAsJsonArray();
        } catch (Exception e) {
            System.out.println("Error decoding data results:");
            System.out.println(jsonString);
        }
        if (variables == null || bindings == null) {
            return new ArrayList<DataResult>();
        }

        List<DataResult> results = new ArrayList<DataResult>();

        for (JsonElement binding : bindings) {
            DataResult curResult = new DataResult();
            for (JsonElement variable : variables) {
                String varName = variable.getAsString();
                JsonElement valueJson = binding.getAsJsonObject().get(varName);
                String value = valueJson == null ? null : valueJson.getAsJsonObject().get("value").getAsString();
                curResult.addValue(varName, value);
            }
            results.add(curResult);
        }
        return results;
    }

    @Override
    public List<DataResult> query(String queryString) throws Exception {
        // queryString is not being checked
        String queryUrl = getEndpointUrl() + "repositories/" + this.repository;
        URIBuilder builder = new URIBuilder(queryUrl);
        builder.setParameter("query", queryString);

        HttpGet request = new HttpGet(builder.build());
        request.addHeader("Accept", "application/json");
        if (token != null)
            request.addHeader("Authorization", token);

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            HttpEntity entity = response.getEntity();
			String strResponse = EntityUtils.toString(entity);
		    EntityUtils.consume(entity);

            if (strResponse.startsWith("MALFORMED QUERY")) {
                System.out.println(strResponse);
                System.out.println(queryString);
                throw new RuntimeException("MALFORMED QUERY");
            } else {
                return getDataResultFromJsonString(strResponse);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<DataResult> queryOptions(String varname, String constraintQuery) throws Exception {
        String name = varname.substring(1);
        String labelVar = varname + "Label";
        String query = "PREFIX xsd:  <" + KBConstants.XSD_NS + ">\n" +
                "PREFIX rdfs: <" + KBConstants.RDFS_NS + ">\n" +
                "PREFIX rdf:  <" + KBConstants.RDF_NS + ">\n" +
                "SELECT DISTINCT " + varname + " " + labelVar + " WHERE {\n" +
                constraintQuery;

        if (!constraintQuery.contains(labelVar))
            query += "\n  OPTIONAL { " + varname + " rdfs:label " + labelVar + " . }";
        query += "\n}";

        List<DataResult> solutions = this.query(query);
        List<DataResult> fixedSolutions = new ArrayList<DataResult>();

        for (DataResult solution : solutions) {
            DataResult cur = new DataResult();
            String uri = solution.getValue(name);
            String label = solution.getValue(name + "Label");
            if (label == null) { //Theres no label, create one with the def value or extract it from the URI
                String defName = solution.getName(name);
                label = defName != null ? defName : uri.replaceAll("^.*\\/", "").replaceAll("_", " ");
            }
            cur.addValue(VARURI, uri);
            cur.addValue(VARLABEL, label);
            fixedSolutions.add(cur);
        }

        return fixedSolutions;
    }

    @Override
    public Map<String, String> getFileHashes(List<String> dsUrls) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean ping() {
        this.login();
        try {
            List<DataResult> results = this.query("SELECT * WHERE { ?a ?b ?c . } LIMIT 1");
            return results.size() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
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
    public void queryCSV(String queryString) throws Exception {
        String url = getEndpointUrl() + "repositories/" + this.repository;
        URIBuilder builder = new URIBuilder(url);
        builder.setParameter("query", queryString);

        HttpGet get = new HttpGet(builder.build());
        get.setHeader("Content-Type", "text/csv");
        if (token != null)
            get.addHeader("Authorization", token);

        try (CloseableHttpResponse response = httpClient.execute(get)) {
            HttpEntity entity = response.getEntity();
            String strResponse = EntityUtils.toString(entity);
            System.out.println(get.getURI());
            System.out.println(entity.getContentType());
            System.out.println(entity.getContentLength());
            EntityUtils.consume(entity);
            System.out.println(strResponse);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
