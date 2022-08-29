package org.diskproject.server.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.diskproject.server.adapters.Reana.ApiSchema.ReanaSpecification;
import org.diskproject.shared.classes.workflow.Variable;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

public class ReanaAdapterTest {
    ReanaAdapter adapter = new ReanaAdapter("reana", "http://localhost:30080", "null", "qTgcgo9iXWIWHxt3IfLHig",
            "null");
    private Map<String, String> convertReanaOutputs;

    @Test
    public void test() {
        System.out.println("Hello World!");
    }

    @Test
    // TODO: fix this test
    public void createInputParametersTest() {
        this.adapter.createInputParameters(null, null);
    }

    @Test
    public void addDataTest() throws Exception {
        String path = this.adapter.addData(null, null, null);
        assertEquals(null, path);
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
    public void getWorkflowVariablesTest() {
        return;
    }

    @Test
    public void getRunStatusTest() {
        return;
    }

    @Test
    public void convertReanaVariable() throws Exception {
        Path testResources = Paths.get("src/test/resources/bikes/specification.json");
        Reader reader = Files.newBufferedReader(testResources);
        Gson gson = new Gson();
        ReanaSpecification specification = gson.fromJson(reader, ReanaSpecification.class);
        List<Variable> result;
        result = adapter.convertReanaVariable(specification);
        assertEquals(2, result.size());
    }

    @Test
    public void duplicateWorkflowTest() {
        return;
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
