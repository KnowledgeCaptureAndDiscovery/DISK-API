package org.diskproject;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.diskproject.server.adapters.Reana.ApiClient;
import org.diskproject.server.adapters.Reana.ApiClient.ResponseListWorkflow;
import org.diskproject.server.adapters.Reana.ApiSchema.ReanaSpecification;
import org.diskproject.server.adapters.Reana.ApiSchema.ResponseGetSpecification;
import org.diskproject.server.adapters.Reana.ApiSchema.ResponseRunStatus;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

public class ApiClientTest {
    String apiUrl = "http://localhost:30080";
    String apiKey = "qTgcgo9iXWIWHxt3IfLHig";
    ApiClient apiClient = new ApiClient(apiUrl, apiKey, null);

    private ReanaSpecification getSpecificationFromFile() {
        Gson gson = new Gson();
        Path path = Paths.get("src/test/resources/bikes/specification.json");
        try (Reader reader = Files.newBufferedReader(path)) {
            return gson.fromJson(reader, ReanaSpecification.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Test
    public void createWorkflowTest() throws Exception {
        ReanaSpecification specification = getSpecificationFromFile();
        String workflowId = apiClient.createWorkflow(specification, "bikes-rental.1");
        System.out.print(workflowId);
    }

    @Test
    public void getSpecificationTest() throws Exception {
        ResponseGetSpecification responseGetSpecification = apiClient.getSpecification("dev.1");
        assertEquals(responseGetSpecification.specification.inputs.getParameters().size(), 2);
    }

    @Test
    public void getWorkflowsTest() throws Exception {
        ResponseListWorkflow workflows = apiClient.getWorkflows();
        System.out.println(workflows.total);
    }

    @Test
    public void getRunStatusTest() throws Exception {
        ResponseRunStatus runStatus = apiClient.getRunStatus("020d40b9-911e-4bf0-8b40-2e6df92d21aa");
        assertEquals(runStatus.status, "finished");
    }

    @Test
    public void startWorkflowTest() throws Exception {
        ReanaSpecification specification = getSpecificationFromFile();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("databikes", "data/bikes-2019-2020-ny.csv");
        parameters.put("variables", "Temperature Humidity");
        String workflowId = apiClient.createWorkflow(specification, "new workflow");
        apiClient.startWorkflow(workflowId, parameters, null);
        return;
    }

    @Test
    public void addFileWorkspaceTest() throws Exception {
        ReanaSpecification specification = getSpecificationFromFile();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("databikes", "data/bikes-2019-2020-ny.csv");
        parameters.put("variables", "Temperature Humidity");
        String workflowId = apiClient.createWorkflow(specification, "new workflow");
        apiClient.addFileWorkspace(workflowId, "src/test/resources/bikes/data/bikes-2019-2020-ny.csv", "data/bikes-2019-2020-ny.csv");
        apiClient.startWorkflow(workflowId, parameters, null);
        return;
    }
}
