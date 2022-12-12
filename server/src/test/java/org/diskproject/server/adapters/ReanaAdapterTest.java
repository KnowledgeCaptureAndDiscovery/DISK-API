package org.diskproject.server.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.diskproject.server.adapters.Reana.ApiSchema.ReanaSpecification;
import org.diskproject.server.adapters.Reana.ApiSchema.ResponseGetSpecification;
import org.diskproject.shared.classes.workflow.Variable;
import org.diskproject.shared.classes.workflow.VariableBinding;
import org.diskproject.shared.classes.workflow.WorkflowRun;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

public class ReanaAdapterTest {
    ReanaAdapter adapter = new ReanaAdapter("reana", "http://localhost:30080", "null", "qTgcgo9iXWIWHxt3IfLHig");
    private Map<String, String> convertReanaOutputs;

    @Test
    public void test() {
        System.out.println("Hello World!");
    }

    @Test
    // TODO: fix this test
    public void getDiskVariablesTest() throws Exception {
        List<VariableBinding> variableBinding = new ArrayList<VariableBinding>();
        VariableBinding vb1 = new VariableBinding();
        String vb1Binding = "[SHAW86598_bikes-2019-2020-ny.csv, SHAW266eb_bikes-2021-ny.csv]";
        String vb1VariableName = "databikes";
        vb1.setBinding(vb1Binding);
        vb1.setVariable(vb1VariableName);
        String vb2Binding = "Temperature";
        String vb2VariableName = "variables";
        VariableBinding vb2 = new VariableBinding();
        vb2.setBinding(vb2Binding);
        vb2.setVariable(vb2VariableName);
        variableBinding.add(vb1);
        variableBinding.add(vb2);
    }

    @Test
    public void getVariablesBindingDiskTest() {

    }

    @Test
    public void addDataTest() throws Exception {
        String path = this.adapter.addData(null, null, null);
        assertEquals(null, path);
    }

    @Test
    public void addDataWorkspaceTest() throws Exception {
        String url = "https://raw.githubusercontent.com/mosoriob/bikes_rent/master/data/bikes-2021-ny.csv";
        String name = "data/bikes-2021-ny.csv";
        String workspaceId = "85cbce19-0ce7-4a09-a80a-142a3fbbe301";
        String path = this.adapter.addData(url, workspaceId, name, null);
    }

    @Test
    public void downloadData() throws IOException {
        String helloUrl = "https://gist.githubusercontent.com/mosoriob/7aa54dab98fb18e4bf861191f0a50032/raw/12cbf08067c97a9dbff3009e7e94693dfa6de045/hello.txt";
        String path = this.adapter.downloadData(helloUrl, "hello.txt");
        byte[] actualFileBytes = Files.readAllBytes(Paths.get(path));
        String actualFileStrings = new String(actualFileBytes, StandardCharsets.UTF_8);
        assertEquals("hello", actualFileStrings);
    }

    @Test
    // TODO: fix this test
    public void runWorkflowTest() {
        return;
    }

    @Test
    public void pingTest() {
        assertEquals(true, this.adapter.ping());
    }

    @Test
    // TODO: mockup
    public void getWorkflowList() {
        return;
    }

    @Test
    public void getWorkflowVariablesBindingTest() throws IOException {
        Path testResources = Paths.get("src/test/resources/root/specification.json");
        Reader reader = Files.newBufferedReader(testResources);
        Gson gson = new Gson();
        ResponseGetSpecification specification = gson.fromJson(reader, ResponseGetSpecification.class);
        return;
    }

    @Test
    public void handleCollectionParameterTest() throws Exception {
        String parameter = "[SHAW86598_bikes-2019-2020-ny.csv, SHAW266eb_bikes-2021-ny.csv]";
        String expected = "SHAW86598_bikes-2019-2020-ny.csv SHAW266eb_bikes-2021-ny.csv";
        String path = this.adapter.handleCollectionParameter(parameter);
        assertEquals(expected, path);
    }

    @Test
    public void handleCollectionParameterTest2() throws Exception {
        String parameter = "SHAW86598_bikes-2019-2020-ny.csv";
        String expected = "SHAW86598_bikes-2019-2020-ny.csv";
        String path = this.adapter.handleCollectionParameter(parameter);
        assertEquals(expected, path);
    }

    @Test
    public void getWorkflowInputsTest() throws Exception {
        String workflowId = "85cbce19-0ce7-4a09-a80a-142a3fbbe301";
        Map<String, Variable> workflowInputs = adapter.getWorkflowInputs(workflowId);
        System.out.print("test");
    }

    @Test
    public void getRunStatusTest() throws Exception {
        WorkflowRun runStatus = this.adapter.getRunStatus("020d40b9-911e-4bf0-8b40-2e6df92d21aa");
        assertEquals(2, runStatus.getOutputs().size());
        assertEquals(1, runStatus.getFiles().size());
        return;
    }

    @Test
    public void getVariablesTest() throws Exception {
        Path testResources = Paths.get("src/test/resources/bikes/specification.json");
        Reader reader = Files.newBufferedReader(testResources);
        Gson gson = new Gson();
        ReanaSpecification specification = gson.fromJson(reader, ReanaSpecification.class);
        List<Variable> result;
    }

    @Test
    public void duplicateWorkflowTest() throws Exception {
        adapter.duplicateWorkflow("bikes-prod.1", null);
    }

    @Test
    public void convertReanaOutputsTest() throws Exception {
        Path testResources = Paths.get("src/test/resources/bikes/specification.json");
        Reader reader = Files.newBufferedReader(testResources);
        Gson gson = new Gson();
        ReanaSpecification specification = gson.fromJson(reader, ReanaSpecification.class);
        Map<String, String> actual = adapter.convertReanaOutputs(specification);
        Map<String, String> expected = new HashMap<String, String>();
        expected.put("r_squared.txt", "results/r_squared.txt");
        expected.put("summary.txt", "results/summary.txt");
        assertEquals(expected, actual);
    }
}
