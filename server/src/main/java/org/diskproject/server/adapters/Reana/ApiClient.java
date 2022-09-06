package org.diskproject.server.adapters.Reana;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.diskproject.server.adapters.Reana.ApiSchema.ReanaSpecification;
import org.diskproject.server.adapters.Reana.ApiSchema.ReanaWorkflow;
import org.diskproject.server.adapters.Reana.ApiSchema.ResponseGetSpecification;
import org.diskproject.server.adapters.Reana.ApiSchema.ResponseRunStatus;

import com.google.gson.Gson;

public class ApiClient {
    public String url;
    String token;
    CloseableHttpClient httpClient;
    String API_WORKFLOW_URL = "%s/api/workflows";
    String API_WORKFLOW_STATUS_URL = "%s/api/workflows/%s/status";
    String API_WORKFLOW_START = "%s/api/workflows/%s/start";
    String API_WORKFLOW_ADD_FILE = "%s/api/workflows/%s/workspace";
    String API_WORKFLOW_SPECIFICATION = "%s/api/workflows/%s/specification";
    public String API_WORKFLOW_DOWNLOAD_FILE = "%s/api/workflows/%s/workspace/%s";

    String API_LAUNCH_URL = "/api/launch";

    public ApiClient(String url, String token, String username) {
        this.url = url;
        this.token = token;
        httpClient = HttpClients.custom()
                .build();

    }

    public String createWorkflow(ReanaSpecification specification, String name) throws Exception {
        String workflowSpecification = new Gson().toJson(specification);
        String workflowName = name;
        String workflowId = null;
        String requestUrl = String.format(API_WORKFLOW_URL, this.url);
        List<NameValuePair> requestParameters = new ArrayList<NameValuePair>();
        requestParameters.add(new BasicNameValuePair("workflow_name", workflowName));
        try {
            String responseString = this.performHTTPPost(requestUrl, workflowSpecification, requestParameters);
            Gson gson = new Gson();
            ResponseCreateWorkflow responseCreateWorkflow = gson.fromJson(responseString, ResponseCreateWorkflow.class);
            workflowId = responseCreateWorkflow.workflow_id;
            return workflowId;
        } catch (Exception e) {
            System.out.println("Error creating workflow: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Get the workflow specification for a workflow
     * 
     * @param id The id of the workflow (not url)
     * @return The workflow specification
     * @throws Exception
     */
    public ResponseGetSpecification getSpecification(String id) throws Exception {
        String requestUrl = String.format(API_WORKFLOW_SPECIFICATION, this.url, id);
        String requestAccept = "application/json";
        try {
            String response = this.performHTTPGet(requestUrl, requestAccept);
            Gson gson = new Gson();
            return gson.fromJson(response, ResponseGetSpecification.class);
        } catch (Exception e) {
            System.err.println("Could not get the workflow specification for workflow " + id);
            System.err.println(e.getMessage());
            throw e;
        }
    }

    /**
     * List all workflows filter by status (created)
     * 
     * @return List of workflows
     * @throws Exception
     */
    public ResponseListWorkflow getWorkflows() throws Exception {
        String requestUrl = String.format(API_WORKFLOW_URL, this.url);
        String requestAccept = "application/json";
        List<NameValuePair> requestParameters = new ArrayList<NameValuePair>();
        requestParameters.add(new BasicNameValuePair("status", "created"));
        try {
            String response = this.performHTTPGet(requestUrl, requestAccept, requestParameters);
            Gson gson = new Gson();
            return gson.fromJson(response, ResponseListWorkflow.class);
        } catch (Exception e) {
            System.err.println("Could not list methods");
            System.err.println(e.getMessage());
            throw e;
        }
    }

    public ResponseRunStatus getRunStatus(String id) throws Exception {
        String requestUrl = String.format(API_WORKFLOW_STATUS_URL, this.url, id);
        String requestAccept = "application/json";
        try {
            String response = this.performHTTPGet(requestUrl, requestAccept);
            Gson gson = new Gson();
            return gson.fromJson(response, ResponseRunStatus.class);
        } catch (Exception e) {
            System.err.println("Unable to get status");
            System.err.println(e.getMessage());
            throw e;
        }
    }

    public String downloadData(String url) throws Exception {
        String requestAccept = "application/octet-stream";
        try {
            return this.performHTTPGet(url, requestAccept);
        } catch (Exception e) {
            System.err.println("Unable to fetch file");
            System.err.println(e.getMessage());
            throw e;
        }
    }
    public void fetchFile(String workflowId, String filename) throws Exception {
        String requestUrl = String.format(API_WORKFLOW_DOWNLOAD_FILE, this.url, workflowId, filename);
        String requestAccept = "application/octet-stream";
        try {
            this.performHTTPGet(requestUrl, requestAccept);
        } catch (Exception e) {
            System.err.println("Unable to fetch file");
            System.err.println(e.getMessage());
            throw e;
        }
    }
    public Object startWorkflow(String workflowId, Map<String, String> inputParameters,
            Map<String, String> operationParameters) throws Exception {
        String requestUrl = String.format(API_WORKFLOW_START, this.url, workflowId);
        String requestAccept = "application/json";
        String requestContentType = "application/json";
        RequestApiStart requestApiStart = new RequestApiStart(inputParameters,
                operationParameters);
        String requestBody = new Gson().toJson(requestApiStart);
        try {
            return this.performHTTPPost(requestUrl, requestBody, requestAccept, requestContentType);
        } catch (Exception e) {
            System.err.println("Could start the workflow");
            System.err.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public String addFileWorkspace(String workflowId, String localFilePath, String remoteFilePath) throws Exception {
        File file = new File(localFilePath);
        String fileRemoteUrl = String.format(API_WORKFLOW_ADD_FILE, this.url, workflowId);
        String requestUrl = fileRemoteUrl + "?file_name=" + remoteFilePath;
        try {
            this.performHTTPPostFile(requestUrl, file);
        } catch (Exception e) {
            System.err.println("Could add file to workspace");
            System.err.println(e.getMessage());
            throw e;
        }
        return fileRemoteUrl;
    }

    public String performHTTPGet(String url, String accept, List<NameValuePair> parameters) throws Exception {
        /**
         * Perform an HTTP GET request
         */
        HttpGet httpGet = new HttpGet(url);
        parameters.add(new BasicNameValuePair("access_token", this.token));
        httpGet.setHeader(HttpHeaders.ACCEPT, accept);
        URI uri = new URIBuilder(httpGet.getURI())
                .addParameters(parameters)
                .build();
        ((HttpRequestBase) httpGet).setURI(uri);
        CloseableHttpResponse response = httpClient.execute(httpGet);
        if (response.getStatusLine().getStatusCode() != 200) {
            throw new Exception("HTTP GET request failed: " + response.getStatusLine().getStatusCode());
        }
        HttpEntity entity = response.getEntity();
        String responseString = EntityUtils.toString(entity);
        return responseString;
    }

    public String performHTTPGet(String url, String accept) throws Exception {
        /**
         * Perform an HTTP GET request
         */
        HttpGet httpGet = new HttpGet(url);
        ArrayList<NameValuePair> parameters = new ArrayList<NameValuePair>();
        parameters.add(new BasicNameValuePair("access_token", this.token));
        httpGet.setHeader(HttpHeaders.ACCEPT, accept);
        URI uri = new URIBuilder(httpGet.getURI())
                .addParameters(parameters)
                .build();
        ((HttpRequestBase) httpGet).setURI(uri);
        CloseableHttpResponse response = httpClient.execute(httpGet);
        if (response.getStatusLine().getStatusCode() != 200) {
            throw new Exception("HTTP GET request failed: " + response.getStatusLine().getStatusCode());
        }
        HttpEntity entity = response.getEntity();
        String responseString = EntityUtils.toString(entity);
        return responseString;
    }

    /**
     * Perform an HTTP POST request
     * 
     * @param url
     * @param data
     * @return
     */
    public String performHTTPPost(String url, String data, String contentType, String acceptContent) throws Exception {
        HttpPost httpPost = new HttpPost(url);
        ArrayList<NameValuePair> parameters = new ArrayList<NameValuePair>();
        parameters.add(new BasicNameValuePair("access_token", this.token));
        URI uri = new URIBuilder(httpPost.getURI())
                .addParameters(parameters)
                .build();
        ((HttpRequestBase) httpPost).setURI(uri);
        httpPost.setEntity(new StringEntity(data));
        httpPost.addHeader("Content-type", contentType);
        httpPost.addHeader("Accept", acceptContent);
        CloseableHttpResponse httpResponse = httpClient.execute(httpPost);
        try {
            HttpEntity responseEntity = httpResponse.getEntity();
            String strResponse = EntityUtils.toString(responseEntity);
            EntityUtils.consume(responseEntity);
            httpResponse.close();
            return strResponse;
        } finally {
            httpResponse.close();
        }
    }

    private String performHTTPPost(String url, String data, List<NameValuePair> parameters) throws Exception {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(new StringEntity(data));
        httpPost.addHeader("Content-type", "application/json");
        httpPost.addHeader("Accept", "application/json");
        parameters.add(new BasicNameValuePair("access_token", this.token));
        URI uri = new URIBuilder(httpPost.getURI())
                .addParameters(parameters)
                .build();
        ((HttpRequestBase) httpPost).setURI(uri);
        CloseableHttpResponse httpResponse = this.httpClient.execute(httpPost);
        if (httpResponse.getStatusLine().getStatusCode() != 201
                && httpResponse.getStatusLine().getStatusCode() != 200) {
            throw new Exception("HTTP GET request failed: " + httpResponse.getStatusLine().getStatusCode());
        }
        try {
            HttpEntity responseEntity = httpResponse.getEntity();
            String strResponse = EntityUtils.toString(responseEntity);
            EntityUtils.consume(responseEntity);
            httpResponse.close();

            return strResponse;
        } finally {
            httpResponse.close();
        }
    }

    private String performHTTPPostFile(String url, File file) throws Exception {
        HttpPost httpPost = new HttpPost(url);
        List<NameValuePair> parameters = new ArrayList<>();
        parameters.add(new BasicNameValuePair("access_token", this.token));
        URI uri = new URIBuilder(httpPost.getURI())
                .addParameters(parameters)
                .build();
        ((HttpRequestBase) httpPost).setURI(uri);
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/octet-stream");
        EntityBuilder builder = EntityBuilder.create();
        builder.setFile(file);
        HttpEntity entity = builder.build();
        httpPost.setEntity(entity);
        CloseableHttpResponse httpResponse = this.httpClient.execute(httpPost);

        HttpEntity responseEntity = httpResponse.getEntity();
        String strResponse = EntityUtils.toString(responseEntity);
        if (httpResponse.getStatusLine().getStatusCode() != 201
                && httpResponse.getStatusLine().getStatusCode() != 200) {
            throw new Exception("HTTP GET request failed: " + httpResponse.getStatusLine().getStatusCode());
        }
        try {
            EntityUtils.consume(responseEntity);
            httpResponse.close();
            return strResponse;
        } finally {
            httpResponse.close();
        }
    }

    public class RequestApiStart {
        public Map<String, String> input_parameters;
        public Map<String, String> operational_parameters;

        public RequestApiStart(Map<String, String> input_parameters,
                Map<String, String> operational_parameters) {
            this.input_parameters = input_parameters;
            this.operational_parameters = operational_parameters;
        }
    }

    public class RequestApiState {
        public String name;
        public String workflowSpecification;

        public RequestApiState(String name, String workflowSpecification) {
            this.name = name;
            this.workflowSpecification = workflowSpecification;
        }
    }

    public class ResponseBasic {
        public String message;
    }

    public class ResponseCreateWorkflow {
        public String message;
        public String workflow_id;
        public String workflow_name;
    }

    public class ResponseListWorkflow {
        public Boolean has_next;
        public String has_prev;
        public Integer page;
        public Integer total;
        public ReanaWorkflow[] items;
    }

    public class ResponseGetWorkspace {
        public Boolean has_next;
        public String has_prev;
        public Integer page;
        public Integer total;
        public Workspace[] items;
    }

    public class Workspace {
        String last_modified;
        String name;
        Object size;
    }

    public class ResponseStartWorkflow {
        public String message;
        public String workflow_id;
        public String workflow_name;
        public Integer run_number;
        public String status;
        public String user;
    }

}
