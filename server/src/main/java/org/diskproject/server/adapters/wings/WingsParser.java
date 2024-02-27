package org.diskproject.server.adapters.wings;

import org.diskproject.shared.classes.common.Status;
import org.diskproject.shared.classes.util.KBConstants;
import org.diskproject.shared.classes.workflow.Execution;
import org.diskproject.shared.classes.workflow.ExecutionRecord;
import org.diskproject.shared.classes.workflow.VariableBinding;
import org.diskproject.shared.classes.workflow.VariableBinding.BindingTypes;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class WingsParser {
	private  static JsonParser jsonParser = new JsonParser();
    private static SimpleDateFormat dateformatter = new SimpleDateFormat(KBConstants.DATE_FORMAT);

    public static String getLocalId (String id) {
		return id.replaceAll("^.*\\#", "");
    }

    public static ExecutionRecord parseExecutionRecord (JsonObject runInfoJson) {
		JsonElement status = runInfoJson.get("status");
		JsonElement log = runInfoJson.get("log");
		JsonElement tsStart = runInfoJson.get("startTime");
		JsonElement tsEnd = runInfoJson.get("endTime");

        ExecutionRecord exec = new ExecutionRecord();
        if (status != null)
            exec.setStatus(Status.fromString(status.getAsString()));
        if (log != null) {
            String rawLog = log.getAsString();
            if (rawLog.endsWith("\n"))
                rawLog = rawLog.substring(0, rawLog.length()-1);
            exec.setLog(rawLog);
        } 
        if (tsStart != null) {
            Timestamp stamp = new Timestamp(tsStart.getAsInt());
            exec.setStartDate(dateformatter.format(new Date(stamp.getTime())));
        }
        if (tsEnd != null) {
            Timestamp stamp = new Timestamp(tsEnd.getAsInt());
            exec.setEndDate(dateformatter.format(new Date(stamp.getTime())));
        }
        return exec;
    }

    public static Execution parseExecution (String details) {
		JsonObject runobj = jsonParser.parse(details).getAsJsonObject();
		JsonObject execution = null, variables = null;//, constraints = null;
		try {
			JsonElement xw = runobj.get("execution");
			execution = xw.getAsJsonObject();
		} catch (Exception e) {
			System.err.println("Error parsing execution details in:");
			System.err.println(details);
		}
		if (execution == null)
			return null;

		// Get run info.
		JsonObject runInfoJson = execution.get("runtimeInfo").getAsJsonObject();
		JsonElement id = execution.get("id");
        Execution exec = new Execution(parseExecutionRecord(runInfoJson));
        if (exec.getStatus() == null || id == null) {
			System.err.println("Error parsing execution details: Cannot get run status.");
			return null;
        }
        exec.setExternalId(id.getAsString());

		// Get steps info.
		List<ExecutionRecord> stepList = new ArrayList<ExecutionRecord>();
		if (execution != null) {
			JsonObject queueJson = execution.get("queue").getAsJsonObject();
			JsonArray stepsArray = queueJson.get("steps").getAsJsonArray();
			for (JsonElement step: stepsArray) {
				JsonObject rInfo = step.getAsJsonObject().get("runtimeInfo").getAsJsonObject();
				ExecutionRecord newStep = parseExecutionRecord(rInfo);
                stepList.add(newStep);
			}
		}
        exec.setSteps(stepList);

        // Inputs and outputs
		try {
			variables = runobj.get("variables").getAsJsonObject();
		} catch (Exception e) {
			System.err.println("Error parsing variable details in:");
			System.out.println(details);
		}

		if (variables != null) {
			JsonArray inputArray = variables.get("input").getAsJsonArray();
            exec.setInputs(reduceFilesToVariableBindings(inputArray));
			JsonArray outputArray = variables.get("output").getAsJsonArray();
            exec.setOutputs(reduceFilesToVariableBindings(outputArray));
        }
		return exec;
    }

    private static List<VariableBinding> reduceFilesToVariableBindings(JsonArray fileArray) {
        List<VariableBinding> list = new ArrayList<VariableBinding>();
        Map<String, Map<Integer, String>> acc = new HashMap<String, Map<Integer, String>>();
        Map<String, VariableBinding> groups = new HashMap<String, VariableBinding>();

        for (JsonElement file : fileArray) {
            JsonObject current = file.getAsJsonObject();
            JsonElement binding = current.get("binding"); // .getAsJsonObject();
            JsonElement type = current.get("type");// .getAsInt();
            JsonElement idObj = current.get("id");
            JsonElement fromObj = current.get("derivedFrom");
            if (idObj == null || type == null || binding == null || fromObj == null)
                continue;
            
            String groupId = getLocalId(fromObj.getAsString());
            String fileId  = getLocalId(idObj.getAsString());
            BindingTypes bindingType = type.getAsInt() == 1 ? BindingTypes.DISK_DATA : BindingTypes.DEFAULT; // Data is a URI
            VariableBinding newBinding = new VariableBinding();
            newBinding.setVariable(groupId);
            newBinding.setType(bindingType.name());

            JsonObject bindingsJson = binding.getAsJsonObject();
            JsonElement valueUri = bindingsJson.get("id");
            JsonElement valueLiteral = bindingsJson.get("value");
            JsonElement datatype = bindingsJson.get("datatype");
            if (datatype != null)
                newBinding.setDatatype(datatype.getAsString());

            if (groupId.equals(fileId)) {   //If both names are equal, this is a single binding
                if (valueUri != null && bindingType == BindingTypes.DISK_DATA) {
                    newBinding.setSingleBinding(valueUri.getAsString());
                } else if (valueLiteral != null && bindingType == BindingTypes.DEFAULT) {
                    String rawValue = valueLiteral.getAsString();
                    if (rawValue.startsWith("[") && rawValue.endsWith("]")) {
                        String[] sp = rawValue.substring(1, rawValue.length()-1).split(", *");
                        newBinding.setBindings(Arrays.asList(sp));
                    } else {
                        newBinding.setSingleBinding(rawValue);
                    }
                }
                list.add(newBinding);
            } else if (fileId.startsWith(groupId + "_")) { // This mean this is part of an array.
                if (!acc.containsKey(groupId)) {
                    acc.put(groupId, new HashMap<Integer, String>());
                    newBinding.setIsArray(true);
                    groups.put(groupId, newBinding);
                }
                Map<Integer, String> values = acc.get(groupId);
                int index = Integer.parseInt(fileId.replace(groupId + "_", ""));
                if (valueUri != null && bindingType == BindingTypes.DISK_DATA) {
                    values.put(index, valueUri.getAsString());
                    
                } else if (valueLiteral != null && bindingType == BindingTypes.DEFAULT) {
                    values.put(index, valueLiteral.getAsString());
                }

            }
        }

        for (String name: acc.keySet()) {
            VariableBinding cur = groups.get(name);
            Map<Integer, String> values = acc.get(name);
            List<String> valueList = new ArrayList<String>();
            // We assume the list is complete, we get the values in order too.
            for (int i = 1; i <= values.size(); i++) {
                if (values.containsKey(i)) {
                    valueList.add(values.get(i));
                } else {
                    System.out.println("Error parsing file list.");
                    System.out.println(i);
                }
            }
            cur.setBindings(valueList);
            list.add(cur);
        }
        return list;
    }
    
}
