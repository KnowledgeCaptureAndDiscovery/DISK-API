package org.diskproject.server.adapters.Reana;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.diskproject.server.adapters.Reana.ApiSchema.ReanaSpecification;
import org.diskproject.server.adapters.Reana.ApiSchema.ReanaWorkflow;
import org.diskproject.server.adapters.Reana.ApiSchema.ResponseRunStatus;

import com.google.gson.Gson;

public class ApiClient {
    String url;
    String token;
    CloseableHttpClient httpClient;

    public class RequestApiStartParameters {
        public Map<String, String> input_parameters;
        public Map<String, String> operational_parameters;

        public RequestApiStartParameters(Map<String, String> input_parameters,
                Map<String, String> operational_parameters) {
            this.input_parameters = input_parameters;
            this.operational_parameters = operational_parameters;
        }
    }

    public class RequestApiStart {
        public RequestApiStartParameters parameters;

        public RequestApiStart(RequestApiStartParameters parameters) {
            this.parameters = parameters;
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

    public ApiClient(String url, String token, String username) {
        this.url = url;
        this.token = token;
        httpClient = HttpClients.custom()
                .build();

    }

    public String createWorkflow(ReanaSpecification specification, String name) {
        String workflowSpecification = new Gson().toJson(specification);
        String workflowName = name;
        String workflowId = null;
        String url = this.url + "/workflows";
        List<NameValuePair> requestParameters = new ArrayList<NameValuePair>();
        requestParameters.add(new BasicNameValuePair("workflow_name", workflowName));
        String responseString = this.performHTTPPost(url, workflowSpecification, requestParameters);
        Gson gson = new Gson();
        ResponseCreateWorkflow responseCreateWorkflow = gson.fromJson(responseString, ResponseCreateWorkflow.class);
        workflowId = responseCreateWorkflow.workflow_id;
        return workflowId;
    }

    public ReanaSpecification getSpecification(String id) throws Exception {
        String requestUrl = this.url + "/api/workflows/" + id + "/specification";
        String requestAccept = "application/json";
        try {
            String response = this.performHTTPGet(requestUrl, requestAccept);
            Gson gson = new Gson();
            return gson.fromJson(response, ReanaSpecification.class);
        } catch (Exception e) {
            System.err.println("Could not list methods");
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
        String requestUrl = this.url + "/api/workflows";
        String requestAccept = "application/json";
        List<NameValuePair> requestParameters = new ArrayList<NameValuePair>();
        requestParameters.add(new BasicNameValuePair("status", "created"));
        try {
            String response = this.performHTTPGet(requestUrl, requestAccept, requestParameters);
            Gson gson = new Gson();
            return gson.fromJson(response, ResponseListWorkflow.class);
        } catch (Exception e) {
            System.err.println("Could not list methods");
            throw e;
        }
    }

    public ResponseRunStatus getRunStatus(String id) throws Exception {
        String requestUrl = this.url + "/api/workflows/" + id + "/status";
        String requestAccept = "application/json";
        try {
            String response = this.performHTTPGet(requestUrl, requestAccept);
            Gson gson = new Gson();
            return gson.fromJson(response, ResponseRunStatus.class);
        } catch (Exception e) {
            System.err.println("Could not list methods");
            throw e;
        }
    }

    public Object startWorkflow(String workflowId, Map<String, String> inputParameters,
            Map<String, String> operationParameters) {
        String requestUrl = this.url + "/api/workflows" + workflowId + "/start";
        String requestAccept = "application/json";
        String requestContentType = "application/json";
        RequestApiStartParameters requestApiStartParameters = new RequestApiStartParameters(inputParameters,
                operationParameters);
        RequestApiStart requestApiStart = new RequestApiStart(requestApiStartParameters);
        String requestBody = new Gson().toJson(requestApiStart);
        try {
            return this.performHTTPPost(requestUrl, requestBody, requestAccept, requestContentType);
        } catch (Exception e) {
            System.err.println("Could not list methods");
            throw new RuntimeException(e);
        }
    }

    public String addFileWorkspace(String workflowId, String filePath) throws Exception {
        File file = new File(filePath);
        String fileName = file.getName();
        String requestUrl = this.url + "/api/workflows/" + workflowId + "/workspace?file_name=" + fileName;
        String fileRemoteUrl = this.url + "/api/workflows/" + workflowId + "/workspace" + fileName;
        try {
            this.performHTTPPostFile(requestUrl, file);
        } catch (Exception e) {
            System.err.println("Could not list methods");
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
    public String performHTTPPost(String url, String data, String contentType, String acceptContent) {
        try {
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String performHTTPPost(String url, String data, List<NameValuePair> parameters) {
        try {
            HttpPost securedResource = new HttpPost(url);
            securedResource.setEntity(new StringEntity(data));
            securedResource.addHeader("Content-type", "application/json");
            securedResource.addHeader("Accept", "application/json");
            CloseableHttpResponse httpResponse = this.httpClient.execute(securedResource);
            try {
                HttpEntity responseEntity = httpResponse.getEntity();
                String strResponse = EntityUtils.toString(responseEntity);
                EntityUtils.consume(responseEntity);
                httpResponse.close();

                return strResponse;
            } finally {
                httpResponse.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String performHTTPPostFile(String url, File file) {
        try {
            HttpPost httpPost = new HttpPost(url);
            String fileName = file.getName();
            List<NameValuePair> parameters = new ArrayList<>();
            parameters.add(new BasicNameValuePair("access_token", this.token));
            URI uri = new URIBuilder(httpPost.getURI())
                    .addParameters(parameters)
                    .build();
            ((HttpRequestBase) httpPost).setURI(uri);
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            builder.addBinaryBody("upfile", file, ContentType.DEFAULT_BINARY, fileName);
            HttpEntity entity = builder.build();
            httpPost.setEntity(entity);
            CloseableHttpResponse httpResponse = this.httpClient.execute(httpPost);
            try {
                HttpEntity responseEntity = httpResponse.getEntity();
                String strResponse = EntityUtils.toString(responseEntity);
                EntityUtils.consume(responseEntity);
                httpResponse.close();
                return strResponse;
            } finally {
                httpResponse.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
