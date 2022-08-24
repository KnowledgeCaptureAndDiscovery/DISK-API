package org.diskproject.server.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.diskproject.server.adapters.Reana.ApiSchema.ReanaSpecification;
import org.diskproject.shared.classes.workflow.Variable;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

public class ReanaAdapterTest {
    ReanaAdapter adapter = new ReanaAdapter("reana", "http://localhost:30080", "null", "qTgcgo9iXWIWHxt3IfLHig",
            "null");

    @Test
    public void test() {
        System.out.println("Hello World!");
    }

    @Test
    public void convertReanaVariable() throws Exception {
        String variable = "{\n  \"parameters\": {},\n  \"specification\": {\n    \"inputs\": {\n      \"files\": [\n        \"data/bikes-2019-2020-ny.csv\"\n      ],\n      \"parameters\": {\n        \"databikes\": \"data/bikes-2019-2020-ny.csv\",\n        \"variables\": \"Temperature Humidity\"\n      }\n    },\n    \"outputs\": {\n      \"files\": [\n        \"results/r_squared.txt\",\n        \"results/summary.txt\"\n      ]\n    },\n    \"version\": \"0.6.0\",\n    \"workflow\": {\n      \"specification\": {\n        \"steps\": [\n          {\n            \"commands\": [\n              \"mkdir -p results\",\n              \"python /usr/src/app/main.py --inputs ${databikes} --variables ${variables} --r_squared results/r_squared.txt --summary results/summary.txt\"\n            ],\n            \"environment\": \"ikcap/bikes_rent:bcd48921c6e2ed502b84c768b7408e9466a9d576\",\n            \"kubernetes_memory_limit\": \"256Mi\",\n            \"name\": \"predict\"\n          }\n        ]\n      },\n      \"type\": \"serial\"\n    }\n  }\n}";
        Gson gson = new Gson();
        ReanaSpecification specification = gson.fromJson(variable, ReanaSpecification.class);
        List<Variable> result;
        result = adapter.convertReanaVariable(specification);
        assertEquals(2, result.size());
    }
}
