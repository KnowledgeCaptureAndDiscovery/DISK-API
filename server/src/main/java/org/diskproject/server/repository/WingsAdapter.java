package org.diskproject.server.repository;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.diskproject.shared.classes.adapters.MethodAdapter;
import org.diskproject.shared.classes.util.KBConstants;
import org.diskproject.shared.classes.workflow.Variable;
import org.diskproject.shared.classes.workflow.VariableBinding;
import org.diskproject.shared.classes.workflow.Workflow;
import org.diskproject.shared.classes.workflow.WorkflowRun;
import org.diskproject.shared.classes.workflow.WorkflowRun.RunBinding;
import org.diskproject.shared.classes.workflow.WorkflowRun.RuntimeInfo;
import org.diskproject.shared.classes.workflow.WorkflowRun.Status;
import org.diskproject.shared.classes.common.Value;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import edu.isi.kcap.ontapi.KBAPI;
import edu.isi.kcap.ontapi.KBObject;
import edu.isi.kcap.ontapi.KBTriple;
import edu.isi.kcap.ontapi.OntFactory;
import edu.isi.kcap.ontapi.OntSpec;

public class WingsAdapter extends MethodAdapter {
	private Gson json;
	private JsonParser jsonParser;
	private String server;
	private String domain;
	private String internal_server;

	private CookieStore cookieStore;

	public String workflowNS = "http://www.wings-workflows.org/ontology/workflow.owl#";
	public String executionNS = "http://www.wings-workflows.org/ontology/execution.owl#";
	public String dataNS = "http://www.wings-workflows.org/ontology/data.owl#";

	public WingsAdapter(String name, String url, String username, String password, String domain,
			String internalServer) {
		super(name, url, username, password);
		this.server = url;
		this.internal_server = internalServer;
		this.domain = domain;
		this.cookieStore = new BasicCookieStore();
		this.json = new Gson();
		this.jsonParser = new JsonParser();
	}

	public String DOMURI() {
		return this.internal_server + "/export/users/" + this.getUsername() + "/" + this.domain;
	}

	public String eDOMURI() {
		return this.server + "/export/users/" + this.getUsername() + "/" + this.domain;
	}

	public String WFLOWURI() {
		return this.eDOMURI() + "/workflows";
	}

	public String WFLOWURI(String id) {
		return this.eDOMURI() + "/workflows/" + id + ".owl";
	}

	public String WFLOWID(String id) {
		return this.internal_server + "/export/users/" + this.getUsername() + "/" + this.domain +
				"/workflows/" + id + ".owl#" + id;
	}

	public String DATAID(String id) {
		return this.DOMURI() + "/data/library.owl#" + id;
	}

	public String RUNURI(String id) {
		return this.DOMURI() + "/executions/" + id + ".owl";
	}

	public String RUNID(String id) {
		return this.RUNURI(id) + "#" + id;
	}

	@Override
	public String getWorkflowId(String id) {
		return this.WFLOWID(id);
	}

	@Override
	public String getWorkflowUri(String id) {
		return this.WFLOWURI(id);
	}

	@Override
	public String getDataUri(String id) {
		return this.DATAID(id);
	}

	public String getWorkflowLink(String id) {
		return this.server + "/users/" + this.getUsername() + "/" + this.domain
				+ "/workflows/" + id + ".owl";
	}

	public List<Workflow> getWorkflowList() {
		String getTemplatesUrl = "users/" + this.getUsername() + "/" + this.domain +"/workflows/getTemplatesListJSON"; 
		String resp = this.get(getTemplatesUrl, null);
		List<Workflow> wList = new ArrayList<Workflow>();

		try {
			JsonArray arr = (JsonArray) jsonParser.parse(resp);
			int len = arr.size();
			for (int i  = 0; i < len; i++) {
				String fullId = arr.get(i).getAsString();
				String localId = fullId.replaceAll("^.*#", "");
				wList.add(new Workflow(fullId, localId, getWorkflowLink(localId), this.getName()));
			}
		} catch (Exception e) {
			System.err.println("Error decoding " + resp);
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return wList;
	}

	public List<Variable> getWorkflowVariables(String id) {
		List<Variable> vList = new ArrayList<Variable>();
		String fullId = this.DOMURI() + "/workflows/" + id + ".owl#" + id;
		String getVariablesUrl = "users/" + this.getUsername() + "/" + this.domain +"/workflows/getViewerJSON"; 
		List<NameValuePair> formdata = new ArrayList<NameValuePair>();
		formdata.add(new BasicNameValuePair("template_id", fullId));
		String resp = this.get(getVariablesUrl, formdata);
		try {
			JsonObject obj = (JsonObject) jsonParser.parse(resp);
			JsonArray variables = obj.get("inputs").getAsJsonArray();
			int len = variables.size();
			for (int i = 0; i < len; i++) {
				JsonObject varObj = variables.get(i).getAsJsonObject();
				String name = varObj.get("name").getAsString();
				String dType = (varObj.has("dtype")) ? varObj.get("dtype").getAsString() : null;
				String param = varObj.get("type").getAsString();
				JsonElement dObj = varObj.get("dim");
				int dim = dObj != null ? dObj.getAsInt() : 0;

				List<String> type = getSubClasses(dType);
				vList.add(new Variable(name, type, dim, param.equals("param"), true));
			}

		} catch (Exception e) {
			System.err.println("Error decoding " + resp);
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return vList;
	}

	private List<String> getSubClasses(String superClass) {
		List<String> subClasses = new ArrayList<String>();
		if (superClass == null)
			return subClasses;
		
		subClasses.add(superClass);
		if (superClass.startsWith(KBConstants.XSD_NS))
			return subClasses;

		String query = "SELECT ?sc WHERE {\n  ?sc <http://www.w3.org/2000/01/rdf-schema#subClassOf> <" + superClass + ">\n}";
		String pageId = "sparql";
		List<NameValuePair> formdata = new ArrayList<NameValuePair>();
		formdata.add(new BasicNameValuePair("query", query));
		formdata.add(new BasicNameValuePair("format", "json"));
		String resultjson = get(pageId, formdata);
		if (resultjson != null && !resultjson.equals("")) {
			JsonObject result = null;
			try {
				result = jsonParser.parse(resultjson).getAsJsonObject();
			} catch (Exception e) {
				System.out.println("Could not decode: " + resultjson);
				return subClasses;
			}			 

			JsonArray bindings = result.get("results").getAsJsonObject().get("bindings").getAsJsonArray();

			for (JsonElement binding : bindings) {
				JsonObject bindingJson = binding.getAsJsonObject();
				if (bindingJson.get("sc") == null)
					continue;
				String subClass = bindingJson.get("sc").getAsJsonObject().get("value").getAsString();
				subClasses.add(subClass);
			}
		}

		return subClasses;
	}

	@Override
	public Map<String, String> getRunVariableBindings(String runid) {
		String runuri = runid.replace("#.*", "");
		try {
			OntFactory fac = new OntFactory(OntFactory.JENA);
			KBAPI kb = fac.getKB(runuri, OntSpec.PLAIN);
			KBObject execobj = kb.getIndividual(runid);
			KBObject prop = kb.getProperty(executionNS + "hasExpandedTemplate");
			KBObject xtpl = kb.getPropertyValue(execobj, prop);
			String xtplUri = xtpl.getID().replaceAll("#.*", "");

			Map<String, String> varmap = new HashMap<String, String>();
			KBAPI xkb = fac.getKB(xtplUri, OntSpec.PLAIN);
			KBObject bindprop = xkb.getProperty(this.workflowNS + "hasDataBinding");

			for (KBTriple triple : xkb.genericTripleQuery(null, bindprop, null)) {
				KBObject varobj = triple.getSubject();
				KBObject bindobj = triple.getObject();
				varmap.put(varobj.getName(), bindobj.getID());
			}
			return varmap;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Map<String, Variable> getWorkflowInputs(String id) {
		String pageId = "users/" + getUsername() + "/" + this.domain + "/workflows/getInputsJSON";

		String wflowid = this.WFLOWID(id);
		List<NameValuePair> data = new ArrayList<NameValuePair>();
		data.add(new BasicNameValuePair("template_id", wflowid));

		String inputsjson = this.get(pageId, data);

		Type type = new TypeToken<List<Map<String, Object>>>() {
		}.getType();
		List<Map<String, Object>> list = json.fromJson(inputsjson, type);

		Map<String, Variable> inputs = new HashMap<String, Variable>();
		for (Map<String, Object> inputitem : list) {
			Variable var = new Variable();
			String varId = (String) inputitem.get("id");
			var.setName(varId.replaceAll("^.*#", ""));
			if (inputitem.containsKey("dim"))
				var.setDimensionality(((Double) inputitem.get("dim"))
						.intValue());
			if (inputitem.containsKey("dtype"))
				var.setType(getSubClasses((String) inputitem.get("dtype")));

			String vartype = (String) inputitem.get("type");
			var.setParam(vartype.equals("param"));

			inputs.put(var.getName(), var);
		}
		return inputs;
	}

	private boolean login() {
		CloseableHttpClient client = HttpClientBuilder.create().setDefaultCookieStore(this.cookieStore).build();
		HttpClientContext context = HttpClientContext.create();
		try {
			// Get a default domains page
			HttpGet securedResource = new HttpGet(this.server + "/users/" + getUsername() + "/domains");
			HttpResponse httpResponse = client
					.execute(securedResource, context);
			HttpEntity responseEntity = httpResponse.getEntity();
			String strResponse = EntityUtils.toString(responseEntity);
			// If it doesn't ask for a username/password form, then we are already logged in
			if (!strResponse.contains("j_security_check")) {
				return true;
			}

			// Login with the username/password
			HttpPost authpost = new HttpPost(this.server + "/j_security_check");
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			nameValuePairs.add(new BasicNameValuePair("j_username", getUsername()));
			nameValuePairs.add(new BasicNameValuePair("j_password", getPassword()));
			authpost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			try {
				httpResponse = client.execute(authpost);
				responseEntity = httpResponse.getEntity();
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
			httpResponse = client.execute(securedResource);
			responseEntity = httpResponse.getEntity();
			EntityUtils.consume(responseEntity);

			// Check for Session ID to make sure we've logged in
			for (Cookie cookie : context.getCookieStore().getCookies()) {
				if (cookie.getName().equalsIgnoreCase("JSESSIONID")) {
					return true;
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return false;
	}

	private WorkflowRun parseExecutionDetails (String details) {
		JsonObject runobj = jsonParser.parse(details).getAsJsonObject();
		JsonObject execution = null, variables = null;//, constraints = null;
		WorkflowRun curRun = new WorkflowRun();

		try {
			JsonElement xw = runobj.get("execution");
			execution = xw.getAsJsonObject();
		} catch (Exception e) {
			System.err.println("Error parsing execution details: Cannot parse execution.");
		}
		if (execution == null)
			return null;

		// Get run info.
		RuntimeInfo runInfo = null;
		JsonObject runInfoJson = execution.get("runtimeInfo").getAsJsonObject();
		JsonElement id = execution.get("id");
		JsonElement status = runInfoJson.get("status");
		JsonElement log = runInfoJson.get("log");
		JsonElement tsStart = runInfoJson.get("startTime");
		JsonElement tsEnd = runInfoJson.get("endTime");
		if (status != null && id != null) {
			curRun.setId(id.getAsString());
			runInfo = new RuntimeInfo();
			runInfo.status = getStatusFromString(status.getAsString());
			runInfo.log = log == null ? "" : log.getAsString();
			if (tsStart != null) runInfo.startTime = tsStart.getAsInt();
			if (tsEnd != null) runInfo.endTime = tsEnd.getAsInt();
		} else {
			System.err.println("Error parsing execution details: Cannot get run status.");
		}
		if (runInfo == null) 
			return null;
		curRun.setExecutionInfo(runInfo);

		// Get steps info.
		List<RuntimeInfo> stepInfo = null;
		if (execution != null) {
			JsonObject queueJson = execution.get("queue").getAsJsonObject();
			JsonArray stepsArray = queueJson.get("steps").getAsJsonArray();
			stepInfo = new ArrayList<RuntimeInfo>();
			for (JsonElement step: stepsArray) {
				RuntimeInfo info = new RuntimeInfo();
				JsonObject rInfo = step.getAsJsonObject().get("runtimeInfo").getAsJsonObject();
				JsonElement stepStatus = rInfo.get("status");
				JsonElement stepLog = rInfo.get("log");
				JsonElement stepStart = rInfo.get("startTime");
				JsonElement stepEnd = rInfo.get("endTime");
				if (stepStatus != null) {
					info.status = getStatusFromString(stepStatus.getAsString());
					info.log = stepLog == null ? "" : stepLog.getAsString();
					if (stepStart != null) info.startTime = stepStart.getAsInt();
					if (stepEnd != null) info.endTime = stepEnd.getAsInt();
				} else {
					System.err.println("Error parsing execution details: Cannot get step status.");
				}
			}
		}

		if (stepInfo != null)
			curRun.setStepsInfo(stepInfo);

		try {
			variables = runobj.get("variables").getAsJsonObject();
		} catch (Exception e) {
			System.out.println(details);
		}

		Map<String, RunBinding> input = null, output = null;
		if (variables != null) {
			input = new HashMap<String, RunBinding>();
			JsonArray inputArray = variables.get("input").getAsJsonArray();
			for (JsonElement cur : inputArray) {
				JsonObject curObj = cur.getAsJsonObject();
				JsonElement bindingObj = curObj.get("binding"); //.getAsJsonObject();
				JsonElement inputTypeObj = curObj.get("type");//.getAsInt();
				JsonElement nameObj = curObj.get("id");
				if (nameObj != null && inputTypeObj != null && bindingObj != null) {
					RunBinding curInput = new RunBinding();
					JsonObject bindingsJson = bindingObj.getAsJsonObject();
					String name = nameObj.getAsString().replaceAll("^.*\\#", "");
					curInput.type = inputTypeObj.getAsInt() == 1 ? Value.Type.URI : Value.Type.LITERAL;
					// --
					JsonElement idObj = bindingsJson.get("id");
					if (idObj != null) curInput.id = idObj.getAsString();
					JsonElement valueJson = bindingsJson.get("value");
					if (valueJson != null) curInput.value = valueJson.getAsString();
					JsonElement datatypeJson = bindingsJson.get("datatype");
					if (datatypeJson != null) curInput.datatype = datatypeJson.getAsString();
					input.put(name, curInput);
				}
			}

			output = new HashMap<String, RunBinding>();
			JsonArray outputArray = variables.get("output").getAsJsonArray();
			for (JsonElement cur : outputArray) {
				JsonObject curObj = cur.getAsJsonObject();
				JsonElement bindingObj = curObj.get("binding"); //.getAsJsonObject();
				JsonElement inputTypeObj = curObj.get("type");//.getAsInt();
				JsonElement nameObj = curObj.get("id");
				if (nameObj != null && inputTypeObj != null && bindingObj != null) {
					RunBinding curOutput = new RunBinding();
					JsonObject bindingsJson = bindingObj.getAsJsonObject();
					String name = nameObj.getAsString().replaceAll("^.*\\#", "");
					curOutput.type = inputTypeObj.getAsInt() == 1 ? Value.Type.URI : Value.Type.LITERAL;
					// --
					JsonElement idObj = bindingsJson.get("id");
					if (idObj != null) curOutput.id = idObj.getAsString();
					JsonElement valueJson = bindingsJson.get("value");
					if (valueJson != null) curOutput.value = valueJson.getAsString();
					JsonElement datatypeJson = bindingsJson.get("datatype");
					if (datatypeJson != null) curOutput.datatype = datatypeJson.getAsString();
					output.put(name, curOutput);
				}
			}
		}
		if (input != null) curRun.setInputs(input);
		if (output != null) curRun.setOutputs(output);
		return curRun;
	}

	public WorkflowRun getWorkflowRunStatus(String runid) {
		String execId = RUNID(runid);
		String runjson = null;
		try {
			// Get run details.
			List<NameValuePair> formdata = new ArrayList<NameValuePair>();
			formdata.add(new BasicNameValuePair("run_id", execId));
			String pageId = "users/" + getUsername() + "/" + this.domain + "/executions/getRunDetails";
			runjson = this.post(pageId, formdata);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (runjson == null)
			return null;

		WorkflowRun wfRunStatus = parseExecutionDetails(runjson);
		if (wfRunStatus == null) {
			System.out.println(runjson);
			return null;
		}
		String link = this.server + "/users/" + getUsername() + "/" + domain + "/executions";
		try {
			link += "?run_id=" + URLEncoder.encode(execId, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		wfRunStatus.setLink(link);
		return wfRunStatus;
	}
	
    public Status getStatusFromString (String statusStr) {
		if (statusStr == null || statusStr.equals(""))
			return Status.PENDING;
        return statusStr.equals("SUCCESS") ? Status.SUCCESSFUL
                : (statusStr.equals("FAILED") || statusStr.equals("FAILURE")   ? Status.FAILED
                        : (statusStr.equals("RUNNING") ? Status.RUNNING
                                : (statusStr.equals("QUEUED") ? Status.QUEUED : Status.PENDING)));

    }

	// TODO: Hackish function. Fix it !!!! *IMPORTANT*
	private String getWorkflowRunWithSameBindings(String templateId, List<VariableBinding> vBindings) {
		// Get all successful runs for the template (and their variable
		// bindings)
		String query = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
				+ "PREFIX exec: <http://www.wings-workflows.org/ontology/execution.owl#>\n"
				+ "PREFIX wflow: <http://www.wings-workflows.org/ontology/workflow.owl#>\n"
				+ "\n"
				+ "SELECT ?run\n"
				+ "(group_concat(concat(strafter(str(?iv), \"#\"), \"=\", str(?b));separator=\"||\") as ?bindings)  \n"
				+ "WHERE {\n" + "  ?run a exec:Execution .\n"
				+ "  ?run exec:hasTemplate <" + templateId + "> .\n"
				+ "  ?run exec:hasExecutionStatus \"SUCCESS\"^^xsd:string .\n"
				+ "\n" + "  ?run exec:hasExpandedTemplate ?xt .\n"
				+ "  ?xt wflow:hasInputRole ?ir .\n"
				+ "  ?ir wflow:mapsToVariable ?iv .\n"
				+ "  OPTIONAL { ?iv wflow:hasDataBinding ?b } .\n"
				+ "  OPTIONAL { ?iv wflow:hasParameterValue ?b } .\n" + "}\n"
				+ "GROUP BY ?run";

		String pageId = "sparql";
		List<NameValuePair> formdata = new ArrayList<NameValuePair>();
		formdata.add(new BasicNameValuePair("query", query));
		formdata.add(new BasicNameValuePair("format", "json"));
		String resultjson = get(pageId, formdata);
		if (resultjson == null || resultjson.equals(""))
			return null;

		// Check the variable bindings to see if this matches the values that we
		// have
		JsonObject result = jsonParser.parse(resultjson).getAsJsonObject();
		JsonArray qBindings = result.get("results").getAsJsonObject()
				.get("bindings").getAsJsonArray();

		for (JsonElement qBinding : qBindings) {
			JsonObject qb = qBinding.getAsJsonObject();
			if (qb.get("run") == null)
				continue;
			String runid = qb.get("run").getAsJsonObject().get("value")
					.getAsString();
			String bindStrList = qb.get("bindings").getAsJsonObject().get("value")
					.getAsString();
			HashMap<String, String> keyvalues = new HashMap<String, String>();
			for (String bindstr : bindStrList.split("\\|\\|")) {
				String[] keyval = bindstr.split("=", 2);
				String varId = keyval[0];
				String value = keyval[1];
				keyvalues.put(varId, value);
			}
			String[] array = keyvalues.keySet().toArray(new String[keyvalues.keySet().size()]);
			for (String key : array) {
				if (isPartOfCollection(key)) {
					String newKey = key.substring(0, key.lastIndexOf("_"));
					if (keyvalues.get(newKey) == null)
						keyvalues.put(newKey, keyvalues.get(key));
					else {
						keyvalues.put(newKey, keyvalues.get(newKey) + ","
								+ keyvalues.get(key));
					}
					keyvalues.remove(key);
				}
			}
			boolean match = true;
			for (VariableBinding vBinding : vBindings) {
				String value = keyvalues.get(vBinding.getVariable());

				if (value == null) {
					match = false;
					break;
				}
				String[] tempValues = value.split(",");
				String[] vBindingValues = vBinding.getBinding().split(",");
				for (int i = 0; i < vBindingValues.length; i++) {
					boolean singleMatch = false;
					for (int j = 0; j < tempValues.length; j++) {
						if (vBindingValues[i].equals(tempValues[j]
								.substring(tempValues[j].indexOf("#") + 1))) {
							singleMatch = true;
							break;
						}
					}
					if (!singleMatch) {
						match = false;
						break;
					}
				}
			}

			if (match)
				return runid;
		}

		return null;
	}

	private boolean isPartOfCollection(String key) {
		if (key.lastIndexOf("_") != key.length() - 5)
			return false;
		for (int i = key.length() - 4; i < key.length(); i++) {
			if (!Character.isDigit(key.charAt(i)))
				return false;
		}
		return true;
	}

	@Override
	public List<String> runWorkflow(String workflowName, List<VariableBinding> vBindings, Map<String, Variable> inputVariables) {
		workflowName = WFLOWID(workflowName);
		String toPost = null, getData = null, getParams = null, getExpansions = null;
		JsonObject response = null;
		try {
			// GET DATA is suggest data on WINGS, this can add inputs, I'm not sure is necessary.
			toPost = toPlanAcceptableFormat(workflowName, vBindings, inputVariables);
			getData = postWithSpecifiedMediaType("users/" + getUsername() + "/" + domain + "/plan/getData",
					toPost, "application/json", "application/json");

			response = (JsonObject) jsonParser.parse(getData);
			boolean ok = response.get("success").getAsBoolean();
			if (!ok) {
				JsonObject dataObj = response.get("data").getAsJsonObject();
				JsonArray explanations = dataObj.get("explanations").getAsJsonArray();
				int l = explanations.size();
				System.err.println("Error planning data for workflow run:");
				for (int i = 0; i < l; i++)
					System.err.println(explanations.get(i));
				System.err.println("REQUEST: " + toPost);
				return null;
			}
		} catch (Exception e) {
			System.err.println("Error planning data for workflow run. " + e.getMessage());
			System.err.println("REQUEST: " + toPost);
			System.err.println("RESPONSE: " + getData);
			return null;
		}
		
		try {
			// GET PARAMETERS is suggest parameters in WINGS, this adds the default parameters.
			vBindings = addDataBindings(inputVariables, vBindings, getData, false);
			toPost = toPlanAcceptableFormat(workflowName, vBindings, inputVariables);
			getParams = postWithSpecifiedMediaType(
					"users/" + getUsername() + "/" + domain + "/plan/getParameters",
					toPost, "application/json", "application/json");

			response = (JsonObject) jsonParser.parse(getParams);
			boolean ok = response.get("success").getAsBoolean();
			if (!ok) {
				System.err.println("Error planning parameters for workflow run:");
				System.err.println(response);
				return null;
			}
		} catch (Exception e) {
			System.err.println("Error planning parameters for workflow run. " + e.getMessage());
			System.err.println("REQUEST: " + toPost);
			System.err.println("RESPONSE: " + getParams);
			return null;
		}

		//At this point we could use /expandAndRunWorkflow
		try {
			vBindings = addDataBindings(inputVariables, vBindings, getParams, true);
			toPost = toPlanAcceptableFormat(workflowName, vBindings, inputVariables);

			String expandAndRun = postWithSpecifiedMediaType("users/" + getUsername() + "/" + domain + "/executions/expandAndRunWorkflow",
					toPost, "application/json", "application/json");

			if (expandAndRun != null && expandAndRun.length() > 0) {
				List<String> l = new ArrayList<String>();
				String[] uris = expandAndRun.substring(2, expandAndRun.length()-2).split("\",\"");
				for (String uri: uris) {
					l.add(uri);
				}
				return l;
			}
		} catch (Exception e) {
			System.err.println("Error expanding and running " + e.getMessage());
			System.err.println("REQUEST: " + toPost);
		}

		try {
			vBindings = addDataBindings(inputVariables, vBindings, getParams, true);
			toPost = toPlanAcceptableFormat(workflowName, vBindings, inputVariables);

			// TODO: This should be called after getting expanded workflow.
			// - Create mapping data from expanded workflow, and then check.
			// - *NEEDED* to handle collections properly

			String runid = getWorkflowRunWithSameBindings(workflowName, vBindings);
			if (runid != null) {
				System.out.println("Found existing run : " + runid);
				List<String> l = new ArrayList<String>(); 
				l.add(runid);
				return l;
			}
			getExpansions = postWithSpecifiedMediaType("users/" + getUsername() + "/" + domain + "/plan/getExpansions",
					toPost, "application/json", "application/json");

			response = (JsonObject) jsonParser.parse(getExpansions);
			boolean ok = response.get("success").getAsBoolean();
			if (ok) {
			} else {
				System.err.println("Error planning expansions for workflow run:");
				System.err.println(response);
				return null;
			}
		} catch (Exception e) {
			System.err.println("Error planning expansions for workflow run. " + e.getMessage());
			System.err.println("REQUEST: " + toPost);
			System.err.println("RESPONSE: " + getExpansions);
			return null;
		}

		// We can run the workflow now:
		String jsonTemplate = null, jsonConstraints = null, jsonSeed = null, jsonSeedConstraints = null;

		// Decode response
		try {
			JsonObject dataobj = response.get("data").getAsJsonObject();
			JsonArray templatesobj = dataobj.get("templates").getAsJsonArray();
			if (templatesobj.size() == 0) {
				System.err.println("No templates found");
				return null;
			}
			//System.out.println(templatesobj.size());
			JsonObject templateobj = templatesobj.get(0).getAsJsonObject();
			jsonTemplate = templateobj.get("template") .toString();
			jsonConstraints = templateobj.get("constraints").toString();

			JsonObject seedobj = dataobj.get("seed").getAsJsonObject();
			jsonSeed = seedobj.get("template").toString();
			jsonSeedConstraints = seedobj.get("constraints").toString();
		} catch (Exception e) {
			System.err.println("Error decoding parameter for workflow run. " + e.getMessage());
			System.err.println(response);
			return null;
		}

		// Run the first Expanded workflow
		List<NameValuePair> formdata = new ArrayList<NameValuePair>();
		formdata.add(new BasicNameValuePair("template_id", workflowName));
		formdata.add(new BasicNameValuePair("json", jsonTemplate));
		formdata.add(new BasicNameValuePair("constraints_json", jsonConstraints));
		formdata.add(new BasicNameValuePair("seed_json", jsonSeed));
		formdata.add(new BasicNameValuePair("seed_constraints_json", jsonSeedConstraints));
		String pageId = "users/" + getUsername() + "/" + domain + "/executions/runWorkflow";
		List<String> l = new ArrayList<String>(); 
		String runid = post(pageId, formdata);
		l.add(runid);
		return l;
	}

	public FileAndMeta fetchDataFromWings(String dataid) {
		String url = this.server + "/users/" + getUsername() + "/" + domain + "/data/fetch";
		// Download data already present on the server
		List<NameValuePair> formdata = new ArrayList<NameValuePair>();
		formdata.add(new BasicNameValuePair("data_id", dataid));
		url += "?" + URLEncodedUtils.format(formdata, "UTF-8");

		if (!this.login()) {
			return null;
		}
		CloseableHttpClient client = HttpClientBuilder.create().setDefaultCookieStore(this.cookieStore).build();
		FileAndMeta fileData = null;
		try {
			HttpGet securedResource = new HttpGet(url);
			CloseableHttpResponse httpResponse = client.execute(securedResource);
			try {
				HttpEntity responseEntity = httpResponse.getEntity();
				Header x = responseEntity.getContentType();
				String ct = x == null ? null : x.getValue();
				ByteArrayOutputStream rawBytes = new ByteArrayOutputStream(); 
    			responseEntity.writeTo(rawBytes);
				byte[] bytes = rawBytes.toByteArray();
				fileData = new FileAndMeta(bytes, ct);
				EntityUtils.consume(responseEntity);
				httpResponse.close();
			} catch (Exception e) {
				throw e;
			} finally {
				httpResponse.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				client.close();
			} catch (IOException e) {
			}
		}

		return fileData;
	}

	public String addOrUpdateData(String id, String type, String contents, boolean addServer) {
		if (addServer)
			type = this.server + type;
		String postpage = "users/" + getUsername() + "/" + domain + "/data/addDataForType";
		String uploadpage = "users/" + getUsername() + "/" + domain + "/upload";
		String locationpage = "users/" + getUsername() + "/" + domain + "/data/setDataLocation";

		String dataid = this.DATAID(id);

		String response = null;
		try {
			File dir = File.createTempFile("tmp", "");
			if (!dir.delete() || !dir.mkdirs()) {
				System.err.println("Could not create temporary directory "
						+ dir);
				return null;
			}
			File f = new File(dir.getAbsolutePath() + "/" + id);
			FileUtils.write(f, contents);
			this.upload(uploadpage, "data", f);
			f.delete();

			List<NameValuePair> data = new ArrayList<NameValuePair>();
			data.add(new BasicNameValuePair("data_id", dataid));
			data.add(new BasicNameValuePair("data_type", type));
			response = post(postpage, data);
			List<NameValuePair> location = new ArrayList<NameValuePair>();
			location.add(new BasicNameValuePair("data_id", dataid));
			location.add(new BasicNameValuePair("location",
					"/scratch/data/wings/storage/default/users/" + getUsername() + "/" + domain + "/data/"
							+ dataid.substring(dataid.indexOf('#') + 1)));
			response = post(locationpage, location);
			if (response.equals("OK"))
				System.out.println("Upload successful.");
			else
				System.out.println("Upload failed.");
		} catch (Exception e) {
			System.out.println("Upload failed.");
			e.printStackTrace();
		}
		return response;
	}

	public String addDataToWings(String id, String type, String contents) {
		String getpage = "users/" + getUsername() + "/" + domain + "/data/getDataJSON";
		String postpage = "users/" + getUsername() + "/" + domain + "/data/addDataForType";
		String uploadpage = "users/" + getUsername() + "/" + domain + "/upload";

		// Add unique md5 hash to id based on contents
		String md5 = DigestUtils.md5Hex(contents.getBytes());
		Pattern extensionPattern = Pattern.compile("^(.*)(\\..+)$");
		Matcher mat = extensionPattern.matcher(id);
		if (mat.find()) {
			id = mat.group(1) + "-" + md5 + mat.group(2);
		} else
			id += "-" + md5;

		// Check for data already present on the server
		String dataid = this.DATAID(id);
		List<NameValuePair> formdata = new ArrayList<NameValuePair>();
		formdata.add(new BasicNameValuePair("data_id", dataid));
		String datajson = this.get(getpage, formdata);
		if (datajson != null && !datajson.trim().equals("null")) {
			// Already there
			return dataid;
		}

		System.out.println("Not found, upload " + id);
		// If not present, Create temporary file and upload
		try {
			File dir = File.createTempFile("tmp", "");
			if (!dir.delete() || !dir.mkdirs()) {
				System.err.println("Could not create temporary directory "
						+ dir);
				return null;
			}
			File f = new File(dir.getAbsolutePath() + "/" + id);
			FileUtils.write(f, contents);
			this.upload(uploadpage, "data", f);
			f.delete();

			List<NameValuePair> data = new ArrayList<NameValuePair>();
			data.add(new BasicNameValuePair("data_id", dataid));
			data.add(new BasicNameValuePair("data_type", type));
			String response = this.post(postpage, data);
			if (response != null && response.equals("OK"))
				return dataid;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}


	public String addDataToWingsAsFile(String id, byte[] contents, String type) {
		// Compute the hash of the contents and check it agains filename.
		String sha = DigestUtils.sha1Hex(contents);
		String correctName = "SHA" + sha.substring(0, 6);
		if (!id.contains(correctName))
			System.out.println("File " + id + " does not have the same hash: " + sha);

		// Create temporary file and upload to WINGS
		String uploadPage = "users/" + getUsername() + "/" + domain + "/upload";
		String location = null;
		try {
			File dir = File.createTempFile("tmp", "");
			if (!dir.delete() || !dir.mkdirs()) {
				System.err.println("Could not create temporary directory " + dir);
				return null;
			}
			File f = new File(dir.getAbsolutePath() + "/" + id);
			FileUtils.writeByteArrayToFile(f, contents);
			String uploadResponse = this.upload(uploadPage, "data", f);

			// Decode response
			JsonObject uploadR = (JsonObject) jsonParser.parse(uploadResponse);
			boolean success =  uploadR.get("success").getAsBoolean();
			if (success)
				location = uploadR.get("location").getAsString();
			f.delete();
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (location == null) return null;

		// Add registry on WINGS
		String dataid = this.DATAID(id);
		String postAddData = "users/" + getUsername() + "/" + domain + "/data/addDataForType";
		try {
			List<NameValuePair> data = new ArrayList<NameValuePair>();
			data.add(new BasicNameValuePair("data_id", dataid));
			data.add(new BasicNameValuePair("data_type", type));
			String response = this.post(postAddData, data);
			if (response == null || !response.equals("OK"))
				return null;
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Set location of the file
		String postDataLocation = "users/" + getUsername() + "/" + domain + "/data/setDataLocation";
		try {
			List<NameValuePair> data = new ArrayList<NameValuePair>();
			data.add(new BasicNameValuePair("data_id", dataid));
			data.add(new BasicNameValuePair("location", location));
			String response = this.post(postDataLocation, data);
			System.out.println("RESP: " + response);
			if (response == null || !response.equals("OK"))
				return null;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return dataid;
	}

	public String addRemoteDataToWings(String url, String name, String dType) throws Exception {
		/* FIXME: Wings rename does not rename the file, only the id
		 * thus we cannot upload two files with the same name and then rename them. */
		// Get the file.
		CloseableHttpClient client = HttpClientBuilder.create().setDefaultCookieStore(this.cookieStore).build();
		byte[] bytes = null;
		try {
			HttpGet securedResource = new HttpGet(url);
			CloseableHttpResponse httpResponse = client.execute(securedResource);
			try {
				HttpEntity responseEntity = httpResponse.getEntity();
				ByteArrayOutputStream rawBits = new ByteArrayOutputStream(); 
    			responseEntity.writeTo(rawBits);
				bytes = rawBits.toByteArray();
				EntityUtils.consume(responseEntity);
				httpResponse.close();
			} catch (Exception e) {
				throw e;
			} finally {
				httpResponse.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				client.close();
			} catch (IOException e) {
			}
		}
		if (bytes == null) {
			return null;
		}

		System.out.println("Content downloaded [" + bytes.length + "] ");
		String dataid = addDataToWingsAsFile(name, bytes, dType);
		System.out.println("Data ID generated: " + dataid);
		return dataid;
	}

	public List<String> isFileListOnWings(Set<String> filelist, String filetype) {
		List<String> returnValue = new ArrayList<String>();
		//String filetype = this.internal_server + "/export/users/" + getUsername() + "/" + domain + "/data/ontology.owl#File";
		String fileprefix = "<" + this.internal_server + "/export/users/" + getUsername() + "/" + domain
				+ "/data/library.owl#";

		// Only checking that the filename is already on WINGs
		// This will fail if the query is too long, only ask 20 at one time.
		String queryStart = "SELECT DISTINCT ?value WHERE {\n"
				+ "  ?value a <" + filetype + "> .\n"
				+ "  VALUES ?value {\n";
		String queryEnd = "  }\n}";

		List<Set<String>> grouped = new ArrayList<Set<String>>();
		int size = filelist.size();
		if (size <= 20) {
			grouped.add(filelist);
		} else {
			int i = 0;
			Set<String> cur = new HashSet<String>();
			for (String file : filelist) {
				cur.add(file);
				i++;
				if (i == 20) {
					grouped.add(cur);
					cur = new HashSet<String>();
					i = 0;
				}
			}
			if (i != 0) { // That means the last one was not added yet
				grouped.add(cur);
			}
		}

		for (Set<String> group : grouped) {
			String query = queryStart;
			for (String file : group)
				query += fileprefix + file + ">\n";
			query += queryEnd;

			// Doing the query
			String pageId = "sparql";
			List<NameValuePair> formdata = new ArrayList<NameValuePair>();
			formdata.add(new BasicNameValuePair("query", query));
			formdata.add(new BasicNameValuePair("format", "json"));
			String resultjson = get(pageId, formdata);
			if (resultjson == null || resultjson.equals(""))
				return returnValue;

			JsonObject result = null;
			try {
				result = jsonParser.parse(resultjson).getAsJsonObject();
			} catch (Exception e) {
				System.out.println("ERROR: Return value is not a json object.");
				//TODO: this generates some errors
			}

			if (result != null) {
				JsonArray qBindings = result.get("results").getAsJsonObject().get("bindings").getAsJsonArray();

				for (JsonElement qBinding : qBindings) {
					JsonObject qb = qBinding.getAsJsonObject();
					if (qb.get("value") == null)
						continue;
					String fileurl = qb.get("value").getAsJsonObject().get("value").getAsString();
					String name = fileurl.replaceAll("^.*\\#", "");
					returnValue.add(name);
				}
			}
		}

		return returnValue;

	}

	public boolean isFileOnWings(String url) {
		String id = url.replaceAll("^.*\\/", "");
		//todo: this is hardcoded
		String filetype = this.internal_server + "/export/users/" + getUsername() + "/" + domain
				+ "/data/ontology.owl#File";
		String wingsid = this.internal_server + "/export/users/" + getUsername() + "/" + domain + "/data/library.owl#"
				+ id;
		// Only checking that the filename is already on WINGs
		String query = "SELECT DISTINCT ?prop WHERE {\n"
				+ "  <" + wingsid + "> ?prop <" + filetype + "> .\n"
				+ "\n}";

		String pageId = "sparql";
		List<NameValuePair> formdata = new ArrayList<NameValuePair>();
		formdata.add(new BasicNameValuePair("query", query));
		formdata.add(new BasicNameValuePair("format", "json"));
		String resultjson = get(pageId, formdata);
		if (resultjson == null || resultjson.equals(""))
			return false;

		JsonObject result = jsonParser.parse(resultjson).getAsJsonObject();
		JsonArray qBindings = result.get("results").getAsJsonObject().get("bindings").getAsJsonArray();

		for (JsonElement qBinding : qBindings) {
			JsonObject qb = qBinding.getAsJsonObject();
			if (qb.get("prop") == null)
				continue;
			return true;
		}
		return false;
	}

	private String get(String pageId, List<NameValuePair> data) {
		this.login();
		CloseableHttpClient client = HttpClientBuilder.create().setDefaultCookieStore(this.cookieStore).build();
		try {
			String url = this.server + "/" + pageId;
			if (data != null && data.size() > 0) {
				url += "?" + URLEncodedUtils.format(data, "UTF-8");
			}
			HttpGet securedResource = new HttpGet(url);
			CloseableHttpResponse httpResponse = client
					.execute(securedResource);

			try {
				HttpEntity responseEntity = httpResponse.getEntity();
				String strResponse = EntityUtils.toString(responseEntity);
				EntityUtils.consume(responseEntity);
				httpResponse.close();
				return strResponse;
			} catch (Exception e) {
				// TODO: handle exception
			} finally {
				httpResponse.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				client.close();
			} catch (IOException e) {
			}
		}
		return null;
	}

	private List<VariableBinding> addDataBindings(
			Map<String, Variable> inputVariables, List<VariableBinding> vbl,
			String data, boolean param) {

		JsonObject expobj = null;
		try {
			expobj = jsonParser.parse(data.trim()).getAsJsonObject();
		} catch (Exception e) {
			System.out.println("Error parsing: \n" + data);
			return vbl;
		}

		if (!expobj.get("success").getAsBoolean())
			return vbl;

		JsonObject dataobj = expobj.get("data").getAsJsonObject();
		JsonArray datalist = dataobj.get("bindings").getAsJsonArray();
		if (datalist.size() == 0)
			return vbl;

		JsonObject bindingobj = new JsonObject();
		for (Iterator<JsonElement> it = datalist.iterator(); it.hasNext();) {
			JsonElement el = it.next();

			JsonObject obj = null;
			if (param) {
				// If Parameter, the entries aren't arrays
				obj = el.getAsJsonObject();
			} else {
				JsonArray elList = el.getAsJsonArray();
				if (elList.size() > 0) {
					obj = elList.get(0).getAsJsonObject();
				}
			}
			if (obj != null)
				for (Entry<String, JsonElement> entry : obj.entrySet()) {
					bindingobj.add(entry.getKey(), entry.getValue());
				}
		}

		for (String key : inputVariables.keySet()) {
			Variable v = inputVariables.get(key);
			boolean existing = false;
			if (!v.isParam() && !param) {
				for (int i = 0; i < vbl.size(); i++) {
					if (vbl.get(i).getVariable().equals(key)) {
						existing = true;
						break;
					}
				}
				if (!existing) {
					if (bindingobj.get(key).isJsonArray()) {// is a collection
						JsonArray wingsData = bindingobj.get(key).getAsJsonArray();
						String id = "";
						String temp;
						for (int i = 0; i < wingsData.size(); i++) {
							temp = wingsData.get(i).getAsJsonObject().get("id").toString();
							temp = temp.substring(temp.indexOf("#") + 1, temp.length() - 1);
							id += temp + ",";
						}
						id = id.substring(0, id.length() - 1);
						vbl.add(new VariableBinding(key, id));
					} else {
						JsonObject wingsData = bindingobj.get(key).getAsJsonObject();
						String id = wingsData.get("id").toString();
						id = id.substring(id.indexOf("#") + 1, id.length() - 1);
						vbl.add(new VariableBinding(key, id));
					}
				}
			} else if (v.isParam() && param) {
				for (int i = 0; i < vbl.size(); i++) {
					if (vbl.get(i).getVariable().equals(key)) {
						existing = true;
						break;
					}
				}
				if (!existing) {
					JsonObject wingsParam = bindingobj.get(key).getAsJsonObject();
					String value = wingsParam.get("value").toString();
					value = value.substring(1, value.length() - 1);
					vbl.add(new VariableBinding(key, value));
				}
			}
		}
		return vbl;
	}

	private String toPlanAcceptableFormat(String wfName, List<VariableBinding> vbl, Map<String, Variable> ivm) {
		// We are creating a json here, should be a better way to do it.
		String output = "";

		// Set Template ID first
		output += "{\"templateId\":\"" + wfName + "\",";
		wfName = wfName.substring(0, wfName.lastIndexOf("#") + 1);

		// Set Component Bindings
		output += "\"componentBindings\": {},";

		// Set Parameter Types
		String paramTypes = "";
		for (String key : ivm.keySet()) {
			Variable var = ivm.get(key);
			if (var.isParam()) {
				List<String> types = var.getType();
				String type = types != null && types.size() > 0 ? types.get(0) : KBConstants.XSD_NS + "string";
				paramTypes += "\"" + wfName + key + "\":\"" + type + "\",";
			}
		}
		if (paramTypes.length() > 0)
			paramTypes = paramTypes.substring(0, paramTypes.length() - 1); // Removing comma
		output += "\"parameterTypes\": {" + paramTypes + "},";

		// Set Inputs (Parameters and Data)
		String paramBindings = "\"parameterBindings\": {";
		boolean paramAdded = false;
		String dataBindings = "\"dataBindings\": {";
		boolean dataAdded = false;
		String dataID = this.internal_server + "/export/users/" + getUsername() + "/" + domain + "/data/library.owl#";

		for (String key : ivm.keySet()) {
			Variable v = ivm.get(key);
			for (int i = 0; i < vbl.size(); i++) {
				VariableBinding vb = vbl.get(i);
				if (vb.getVariable().equals(v.getName())) {
					String curBinding = "\"" + wfName + v.getName() + "\":[";
					String bindingValue = vb.getBinding();

					if (v.getDimensionality() ==  0) { // && !bindingValue.startsWith("[")) {
						curBinding += "\"" + bindingValue + "\"";
					} else {
						if (v.getDimensionality() == 0) {
							System.err.println("WARNING: Variable " + v.getName() + " has dimensionality 0 but the binding is an array");
						}
						String[] dBs = bindingValue
								.replaceFirst("^\\[", "")
								.replaceFirst("\\]$", "")
								.split("\\s*,\\s*");
						for (int j = 0; j < dBs.length; j++) {
							if (dBs[j].length() > 0) {
								curBinding += "\"" + (v.isParam() ? "" : dataID) + dBs[j] + "\",";
							}
						}
						curBinding = curBinding.substring(0, curBinding.length() - 1); //rm extra comma
					}


					if (v.isParam()) {
						paramBindings += curBinding + "],";
						paramAdded = true;
					} else {
						dataBindings += curBinding + "],";
						dataAdded = true;
					}
				}
			}
		}
		//Removing commas again
		if (paramAdded) paramBindings = paramBindings.substring(0, paramBindings.length() - 1);
		if (dataAdded)  dataBindings = dataBindings.substring(0, dataBindings.length() - 1);

		output += paramBindings + "}," + dataBindings + "}}";
		return output;
	}

	private String postWithSpecifiedMediaType(String pageId, String data, String type, String type2) {
		this.login();
		CloseableHttpClient client = HttpClientBuilder.create()
				.setDefaultCookieStore(this.cookieStore).build();
		try {
			HttpPost securedResource = new HttpPost(server + "/" + pageId);
			securedResource.setEntity(new StringEntity(data));
			securedResource.addHeader("Accept", type);
			securedResource.addHeader("Content-type", type2);
			CloseableHttpResponse httpResponse = client.execute(securedResource);
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
			System.out.println("POST: " + pageId);
			System.out.println("DATA: " + data);
			e.printStackTrace();
		} finally {
			try {
				client.close();
			} catch (IOException e) {
			}
		}
		return null;
	}

	private String post(String pageId, List<NameValuePair> data) {
		this.login();
		CloseableHttpClient client = HttpClientBuilder.create()
				.setDefaultCookieStore(this.cookieStore).build();

		try {
			HttpPost securedResource = new HttpPost(this.server + "/" + pageId);
			securedResource.setEntity(new UrlEncodedFormEntity(data));
			CloseableHttpResponse httpResponse = client
					.execute(securedResource);
			try {
				HttpEntity responseEntity = httpResponse.getEntity();
				String strResponse = EntityUtils.toString(responseEntity);
				EntityUtils.consume(responseEntity);
				return strResponse;
			} finally {
				httpResponse.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				client.close();
			} catch (IOException e) {
			}
		}
		return null;
	}

	private String upload(String pageId, String type, File file) {
		this.login();
		CloseableHttpClient client = HttpClientBuilder.create()
				.setDefaultCookieStore(this.cookieStore).build();
		try {
			HttpPost post = new HttpPost(this.server + "/" + pageId);
			MultipartEntityBuilder builder = MultipartEntityBuilder.create();
			builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
			builder.addTextBody("name", file.getName());
			builder.addTextBody("type", type);
			builder.addBinaryBody("file", file);
			//
			HttpEntity entity = builder.build();
			post.setEntity(entity);
			CloseableHttpResponse response = client.execute(post);
			try {
				HttpEntity responseEntity = response.getEntity();
				String strResponse = EntityUtils.toString(responseEntity);
				EntityUtils.consume(responseEntity);
				return strResponse;
			} finally {
				response.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				client.close();
			} catch (IOException e) {
			}
		}
		return null;
	}

	@Override
	public boolean ping() {
		if (this.login()) {
			System.out.print("AUTHENTICATED ON WINGS: " + this.server + "... ");
			OntFactory fac = new OntFactory(OntFactory.JENA);

			String libUri = this.WFLOWURI() + "/library.owl";
			try {
				fac.getKB(libUri, OntSpec.PLAIN);
				System.out.print("OK\n");
				return true;
			} catch (Exception e) {
				System.err.print("\nERROR: WINGS adapter could not open KB: " + libUri);
				return false;
			}
		} else {
			return false;
		}
	}

	@Override
	public List<String> areFilesAvailable(Set<String> filelist, String dType) {
		return this.isFileListOnWings(filelist, dType);
	}

	@Override
	public String addData(String url, String name, String dType) throws Exception {
		try {
			return this.addRemoteDataToWings(url, name, dType);
		} catch (Exception e) {
			throw e;
		}
	}

	@Override
	public WorkflowRun getRunStatus(String runId) {
		return this.getWorkflowRunStatus(runId);
	}

	@Override
	public FileAndMeta fetchData(String dataId) {
		return this.fetchDataFromWings(dataId);
	}
}