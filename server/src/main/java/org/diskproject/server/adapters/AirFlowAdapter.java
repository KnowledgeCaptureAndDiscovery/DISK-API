package org.diskproject.server.adapters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.diskproject.shared.classes.adapters.MethodAdapter;
import org.diskproject.shared.classes.workflow.WorkflowVariable;
import org.diskproject.shared.classes.workflow.Execution;
import org.diskproject.shared.classes.workflow.VariableBinding;
import org.diskproject.shared.classes.workflow.WorkflowTemplate;

public class AirFlowAdapter extends MethodAdapter {
    private PoolingHttpClientConnectionManager connectionManager;
    private CloseableHttpClient httpClient;

    public AirFlowAdapter(String adapterName, String url, String username, String password) {
        super(adapterName, url, username, password);
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(username, "X};83^adj4cC`Z&>"));

        connectionManager = new PoolingHttpClientConnectionManager();
        httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultCredentialsProvider(credentialsProvider)
                .build();
    }

    public void getMethodList() {
        HttpGet userInfo = new HttpGet(this.getEndpointUrl() + "/dags?limit=100&only_active=true");
        userInfo.addHeader(HttpHeaders.ACCEPT, "application/json");

        try (CloseableHttpResponse httpResponse = httpClient.execute(userInfo)) {
            HttpEntity responseEntity = httpResponse.getEntity();
            String strResponse = EntityUtils.toString(responseEntity);
            System.out.println(strResponse);
        } catch (Exception e) {
            System.err.println("Could not list methods");
        }
    }

    @Override
    public boolean ping() {
        return false;
    }

    @Override
    public List<WorkflowTemplate> getWorkflowList() {
        List<WorkflowTemplate> list = new ArrayList<WorkflowTemplate>();
        return list;
    }

    public List<WorkflowVariable> getWorkflowVariables(String id) {
        return null;
    }

    @Override
    public String getWorkflowId(String id) {
        // Auto-generated method stub
        return null;
    }

    @Override
    public String getWorkflowUri(String id) {
        // Auto-generated method stub
        return null;
    }

    @Override
    public String getWorkflowLink(String id) {
        // Auto-generated method stub
        return null;
    }

    @Override
    public List<String> areFilesAvailable(Set<String> filelist, String dType) {
        // Auto-generated method stub
        return null;
    }

    @Override
    public String addData(String url, String name, String type) {
        // Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, WorkflowVariable> getWorkflowInputs(String id) {
        // Auto-generated method stub
        return null;
    }

    @Override
    public List<String> runWorkflow(String wfId, List<VariableBinding> vBindings, Map<String, WorkflowVariable> inputVariables) {
        // Auto-generated method stub
        return null;
    }

    @Override
    public Execution getRunStatus(String runId) {
        // Auto-generated method stub
        return null;
    }

    @Override
    public FileAndMeta fetchData(String dataId) {
        // Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, String> getRunVariableBindings(String runId) {
        // Auto-generated method stub
        return null;
    }

    @Override
    public String getDataUri(String id) {
        // Auto-generated method stub
        return null;
    }

	@Override
	public boolean registerData (String id, String type) {
        // Auto-generated method stub
        return false;
    }
}
