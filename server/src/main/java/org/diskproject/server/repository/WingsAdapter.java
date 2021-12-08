package org.diskproject.server.repository;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
import org.diskproject.server.adapters.MethodAdapter;
import org.diskproject.server.util.Config;
import org.diskproject.shared.classes.loi.LineOfInquiry;
import org.diskproject.shared.classes.util.KBConstants;
import org.diskproject.shared.classes.workflow.Variable;
import org.diskproject.shared.classes.workflow.VariableBinding;
import org.diskproject.shared.classes.workflow.Workflow;
import org.diskproject.shared.classes.workflow.WorkflowRun;

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
	static WingsAdapter singleton = null;

	private Gson json;
	private String server;
	private String internal_server;
	private CookieStore cookieStore;
	
	public String wflowns = "http://www.wings-workflows.org/ontology/workflow.owl#";
	public String execns = "http://www.wings-workflows.org/ontology/execution.owl#";
	public String datans = "http://www.wings-workflows.org/ontology/data.owl#";

	public static WingsAdapter get() {
		if (singleton == null)
			singleton = new WingsAdapter();
		return singleton;
	}

	public WingsAdapter() {
		this.server = Config.get().getProperties().getString("wings.server");
		this.internal_server = Config.get().getProperties().getString("wings.internal_server");
		this.cookieStore = new BasicCookieStore();
		this.json = new Gson();
	}

	public String DOMURI(String username, String domain) {
		return this.internal_server + "/export/users/" + username + "/" + domain;
	}

	public String WFLOWURI(String username, String domain) {
		return this.DOMURI(username, domain) + "/workflows";
	}

	public String WFLOWURI(String username, String domain, String id) {
		return this.DOMURI(username, domain) + "/workflows/" + id + ".owl";
	}

	public String WFLOWID(String username, String domain, String id) {
		return this.internal_server + "/export/users/" + username + "/" + domain + 
		    "/workflows/" + id + ".owl#" + id;
	}

	public String DATAID(String username, String domain, String id) {
		return this.DOMURI(username, domain) + "/data/library.owl#" + id;
	}

	public String RUNURI(String username, String domain, String id) {
		return this.DOMURI(username, domain) + "/executions/" + id + ".owl";
	}

	public String RUNID(String username, String domain, String id) {
		return this.RUNURI(username, domain, id) + "#" + id;
	}

	public List<Workflow> getWorkflowList(String username, String domain) {
		String liburi = this.WFLOWURI(username, domain) + "/library.owl";
		try {
			List<Workflow> list = new ArrayList<Workflow>();
			OntFactory fac = new OntFactory(OntFactory.JENA);
			KBAPI kb = fac.getKB(liburi, OntSpec.PLAIN);
			KBObject typeprop = kb.getProperty(KBConstants.RDFNS() + "type");
			KBObject templatecls = kb.getResource(this.wflowns
					+ "WorkflowTemplate");
			for (KBTriple triple : kb.genericTripleQuery(null, typeprop,
					templatecls)) {
				KBObject tobj = triple.getSubject();
				Workflow wflow = new Workflow();
				wflow.setName(tobj.getName());
				wflow.setLink(this.getWorkflowLink(username, domain,
						wflow.getName()));
				list.add(wflow);
			}
			return list;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public List<Variable> getWorkflowVariables(String username, String domain,
			String id) {
		String wflowuri = this.WFLOWURI(username, domain, id);
		try {
			List<Variable> list = new ArrayList<Variable>();
			Map<String, Boolean> varmap = new HashMap<String, Boolean>();
			OntFactory fac = new OntFactory(OntFactory.JENA);
			KBAPI kb = fac.getKB(wflowuri, OntSpec.PLAIN);
			KBObject linkprop = kb.getProperty(this.wflowns + "hasLink");
			KBObject origprop = kb.getProperty(this.wflowns + "hasOriginNode");
			KBObject destprop = kb.getProperty(this.wflowns
					+ "hasDestinationNode");
			KBObject varprop = kb.getProperty(this.wflowns + "hasVariable");
			KBObject paramcls = kb.getConcept(this.wflowns
					+ "ParameterVariable");

			for (KBTriple triple : kb.genericTripleQuery(null, linkprop, null)) {
				KBObject linkobj = triple.getObject();
				KBObject orignode = kb.getPropertyValue(linkobj, origprop);
				KBObject destnode = kb.getPropertyValue(linkobj, destprop);

				// Only return Input and Output variables
				if (orignode != null && destnode != null)
					continue;

				KBObject varobj = kb.getPropertyValue(linkobj, varprop);

				if (varmap.containsKey(varobj.getID()))
					continue;
				varmap.put(varobj.getID(), true);

				Variable var = new Variable();
				var.setName(varobj.getName());
				if (orignode == null)
					var.setInput(true);

				if (kb.isA(varobj, paramcls))
					var.setParam(true);

				list.add(var);
			}
			return list;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Map<String, String> getRunVariableBindings(String username,
			String domain, String runid) {
		String runuri = runid.replace("#.*", "");
		try {
			OntFactory fac = new OntFactory(OntFactory.JENA);
			KBAPI kb = fac.getKB(runuri, OntSpec.PLAIN);
			KBObject execobj = kb.getIndividual(runid);
			KBObject prop = kb.getProperty(execns + "hasExpandedTemplate");
			KBObject xtpl = kb.getPropertyValue(execobj, prop);
			String xtpluri = xtpl.getID().replaceAll("#.*", "");

			Map<String, String> varmap = new HashMap<String, String>();
			KBAPI xkb = fac.getKB(xtpluri, OntSpec.PLAIN);
			KBObject bindprop = xkb
					.getProperty(this.wflowns + "hasDataBinding");

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

	public Map<String, Variable> getWorkflowInputs(String username,
			String domain, String id) {
		String pageid = "users/" + username + "/" + domain
				+ "/workflows/getInputsJSON";

		String wflowid = this.WFLOWID(username, domain, id);
		List<NameValuePair> data = new ArrayList<NameValuePair>();
		data.add(new BasicNameValuePair("template_id", wflowid));

		String inputsjson = this.get(username, pageid, data);

		Type type = new TypeToken<List<Map<String, Object>>>() {
		}.getType();
		List<Map<String, Object>> list = json.fromJson(inputsjson, type);

		Map<String, Variable> inputs = new HashMap<String, Variable>();
		for (Map<String, Object> inputitem : list) {
			Variable var = new Variable();
			String varid = (String) inputitem.get("id");
			var.setName(varid.replaceAll("^.*#", ""));
			if (inputitem.containsKey("dim"))
				var.setDimensionality(((Double) inputitem.get("dim"))
						.intValue());
			if (inputitem.containsKey("dtype"))
				var.setType((String) inputitem.get("dtype"));
			String vartype = (String) inputitem.get("type");
			var.setParam(vartype.equals("param"));

			inputs.put(var.getName(), var);
		}
		return inputs;
	}

	private boolean login(String username) {
		String password = Config.get().getProperties()
				.getString("wings.passwords." + username);

		CloseableHttpClient client = HttpClientBuilder.create()
        .setDefaultCookieStore(this.cookieStore).build();
		HttpClientContext context = HttpClientContext.create();
		try {
		  // Get a default domains page
			HttpGet securedResource = new HttpGet(this.server + "/users/" + username + "/domains");
			HttpResponse httpResponse = client
					.execute(securedResource, context);
			HttpEntity responseEntity = httpResponse.getEntity();
			String strResponse = EntityUtils.toString(responseEntity);
			// If it doesn't ask for a username/password form, then we are already logged in
			if(!strResponse.contains("j_security_check")) {
			  return true;
			}

			// Login with the username/password
			HttpPost authpost = new HttpPost(this.server + "/j_security_check");
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			nameValuePairs.add(new BasicNameValuePair("j_username", username));
			nameValuePairs.add(new BasicNameValuePair("j_password", password));
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
				if (cookie.getName().equalsIgnoreCase("JSESSIONID"))
					return true;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return false;
	}

	public WorkflowRun getWorkflowRunStatus(String username, String domain, String runid) {
		try {
			// Get data
			String execid = RUNID(username, domain, runid);
			List<NameValuePair> formdata = new ArrayList<NameValuePair>();
			formdata.add(new BasicNameValuePair("run_id", execid));
			String pageid = "users/" + username + "/" + domain
					+ "/executions/getRunDetails";
			String runjson = this.post(username, pageid, formdata);
			if (runjson == null)
				return null;
			
			WorkflowRun wflowstatus = new WorkflowRun();
			wflowstatus.setId(execid);

			JsonParser jsonParser = new JsonParser();
			JsonObject runobj = jsonParser.parse(runjson).getAsJsonObject();
			
			// Try to get the execution information
			String status = null,
				   tsStart = null,
				   tsEnd = null;
			
			try {
				JsonObject expobj = runobj.get("execution").getAsJsonObject();
				JsonObject runInfo = expobj.get("runtimeInfo").getAsJsonObject();
				status = runInfo.get("status").getAsString();
				tsStart = runInfo.get("startTime").getAsString();
				tsEnd = runInfo.get("endTime").getAsString();
			} catch (Exception e) {
				System.out.println("Run ID " + runid + ": No execution information");
			}

			// Try to get output files
			Map<String, String> outputs = new HashMap<String, String>();
			try {
				JsonObject vars = runobj.get("variables").getAsJsonObject();
				try {
					JsonArray outs = vars.get("output").getAsJsonArray();
					for (JsonElement resp: outs) {
						JsonObject outputObj = resp.getAsJsonObject();
						JsonObject bindingObj = outputObj.get("binding").getAsJsonObject();
						String outid = outputObj.get("derivedFrom").getAsString();
						String binding = bindingObj.get("id").toString().replaceAll("\"", "");
						String sp[] = outid.split("#");
						outputs.put(sp[sp.length-1], binding);
					}
				} catch (Exception e) {
					System.out.println("Run ID " + runid + ": No output files");
				}
				try {
					JsonArray inputs = vars.get("input").getAsJsonArray();
					for (JsonElement input: inputs) {
						JsonObject binding = input.getAsJsonObject().get("binding").getAsJsonObject();
						if (binding.get("type").toString().equals("\"uri\"")) {
							String id = binding.get("id").toString().replaceAll("\"", "");
							String[] sp = id.split("#");
							if (sp.length > 0) {
								String name = sp[sp.length-1];
								wflowstatus.addFile(name, id);
							}
						}
					}
				} catch (Exception e) {
					System.out.println("Run ID " + runid + ": No input files");
				}
			} catch (Exception e) {
				System.out.println("Run ID " + runid + ": No variables attribute");
			}

			// Creating link
			String link = this.server + "/users/" + username + "/" + domain + "/executions";
			link += "?run_id=" + URLEncoder.encode(execid, "UTF-8");

			wflowstatus.setStatus(status);
			wflowstatus.setLink(link);
			wflowstatus.setOutputs(outputs);
			
			Format formatter = new SimpleDateFormat("HH:mm:ss yyyy-MM-dd");
			
			if (tsStart != null) {
				Date dateStart = new Date( Long.parseLong(tsStart) * 1000);
				wflowstatus.setStartDate( formatter.format(dateStart));
				System.out.println(" Start: " + tsStart + " " + dateStart.toString());
			}

			if (tsEnd != null) {
				Date dateEnd = new Date( Long.parseLong(tsEnd) * 1000);
				wflowstatus.setEndDate(formatter.format(dateEnd));
				System.out.println(" End: " + tsEnd + " " + dateEnd.toString());
			}
			
			if (status != null) {
				System.out.println(" Status: " + status);
			}
			
			if (link != null) {
				System.out.println(" Link: " + link);
			}

			if (outputs.size() > 0) {
				System.out.println(" Outputs:");
				for (String id: outputs.keySet()) {
					System.out.println(id + ": " + outputs.get(id));
				}
			}

			return wflowstatus;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	// TODO: Hackish function. Fix it !!!! *IMPORTANT*
	private String getWorkflowRunWithSameBindings(String username,
			String domain, String templateid, List<VariableBinding> vbindings) {

		// Get all successful runs for the template (and their variable
		// bindings)
		String query = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
				+ "PREFIX exec: <http://www.wings-workflows.org/ontology/execution.owl#>\n"
				+ "PREFIX wflow: <http://www.wings-workflows.org/ontology/workflow.owl#>\n"
				+ "\n"
				+ "SELECT ?run\n"
				+ "(group_concat(concat(strafter(str(?iv), \"#\"), \"=\", str(?b));separator=\"||\") as ?bindings)  \n"
				+ "WHERE {\n" + "  ?run a exec:Execution .\n"
				+ "  ?run exec:hasTemplate <" + templateid + "> .\n"
				+ "  ?run exec:hasExecutionStatus \"SUCCESS\"^^xsd:string .\n"
				+ "\n" + "  ?run exec:hasExpandedTemplate ?xt .\n"
				+ "  ?xt wflow:hasInputRole ?ir .\n"
				+ "  ?ir wflow:mapsToVariable ?iv .\n"
				+ "  OPTIONAL { ?iv wflow:hasDataBinding ?b } .\n"
				+ "  OPTIONAL { ?iv wflow:hasParameterValue ?b } .\n" + "}\n"
				+ "GROUP BY ?run";

		String pageid = "sparql";
		List<NameValuePair> formdata = new ArrayList<NameValuePair>();
		formdata.add(new BasicNameValuePair("query", query));
		formdata.add(new BasicNameValuePair("format", "json"));
		String resultjson = get(username, pageid, formdata);
		if (resultjson == null || resultjson.equals(""))
			return null;

		// Check the variable bindings to see if this matches the values that we
		// have
		JsonParser jsonParser = new JsonParser();
		JsonObject result = jsonParser.parse(resultjson).getAsJsonObject();
		JsonArray qbindings = result.get("results").getAsJsonObject()
				.get("bindings").getAsJsonArray();

		for (JsonElement qbinding : qbindings) {
			JsonObject qb = qbinding.getAsJsonObject();
			if (qb.get("run") == null)
				continue;
			String runid = qb.get("run").getAsJsonObject().get("value")
					.getAsString();
			String bindstrs = qb.get("bindings").getAsJsonObject().get("value")
					.getAsString();
			HashMap<String, String> keyvalues = new HashMap<String, String>();
			for (String bindstr : bindstrs.split("\\|\\|")) {
				String[] keyval = bindstr.split("=", 2);
				String varid = keyval[0];
				String value = keyval[1];
				keyvalues.put(varid, value);
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
			for (VariableBinding vbinding : vbindings) {
				String value = keyvalues.get(vbinding.getVariable());

				if (value == null) {
					match = false;
					break;
				}
				String[] tempValues = value.split(",");
				String[] vbindingValues = vbinding.getBinding().split(",");
				for (int i = 0; i < vbindingValues.length; i++) {
					boolean singleMatch = false;
					for (int j = 0; j < tempValues.length; j++) {
						if (vbindingValues[i].equals(tempValues[j]
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

	public String getWorkflowLink(String username, String domain, String id) {
		return this.server + "/users/" + username + "/" + domain
				+ "/workflows/" + id + ".owl";
	}

	public String runWorkflow(String username, String domain, String wflowname,
			List<VariableBinding> vbindings,
			Map<String, Variable> inputVariables) {
		try {
			wflowname = WFLOWID(username,domain,wflowname);
			String toPost = toPlanAcceptableFormat(username, domain, wflowname,
					vbindings, inputVariables);
			String getData = postWithSpecifiedMediaType(username, "users/"+username+"/"+domain+"/plan/getData",
					toPost, "application/json", "application/json");
			vbindings = addDataBindings(inputVariables, vbindings, getData, false);
			toPost = toPlanAcceptableFormat(username, domain, wflowname, vbindings,
					inputVariables);
			String getParams = postWithSpecifiedMediaType(username, "users/"+username+"/"+domain+"/plan/getParameters", toPost,
					"application/json", "application/json");
			vbindings = addDataBindings(inputVariables, vbindings, getParams, true);
			toPost = toPlanAcceptableFormat(username, domain, wflowname, vbindings,
					inputVariables);

		// TODO: This should be called after getting expanded workflow.
		// - Create mapping data from expanded workflow, and then check.
		// - *NEEDED* to handle collections properly

			String runid = getWorkflowRunWithSameBindings(username, domain,
					wflowname, vbindings);
		if (runid != null) {
			System.out.println("Found existing run : " + runid);
			return runid;
		}
		String s = postWithSpecifiedMediaType(username, "users/"+username+"/"+domain+"/plan/getExpansions",
				toPost, "application/json", "application/json");
		JsonParser jsonParser = new JsonParser();

		JsonObject expobj = (JsonObject) jsonParser.parse(s);
		JsonObject dataobj = expobj.get("data").getAsJsonObject();

		JsonArray templatesobj = dataobj.get("templates").getAsJsonArray();
		if (templatesobj.size() == 0)
			return null;
		JsonObject templateobj = templatesobj.get(0).getAsJsonObject();
		JsonObject seedobj = dataobj.get("seed").getAsJsonObject();

		// Run the first Expanded workflow
		List<NameValuePair> formdata = new ArrayList<NameValuePair>();
		formdata.add(new BasicNameValuePair("template_id", wflowname));
		formdata.add(new BasicNameValuePair("json", templateobj.get("template")
				.toString()));
		formdata.add(new BasicNameValuePair("constraints_json", templateobj
				.get("constraints").toString()));
		formdata.add(new BasicNameValuePair("seed_json", seedobj
				.get("template").toString()));
		formdata.add(new BasicNameValuePair("seed_constraints_json", seedobj
				.get("constraints").toString()));
		String pageid = "users/" + username + "/" + domain
				+ "/executions/runWorkflow";
		runid = post(username, pageid, formdata);
		return runid;
	} catch (Exception e) {
		e.printStackTrace();
	}
	return null;
}

	public String fetchDataFromWings(String username, String domain,
			String dataid) {
		String getpage = "users/" + username + "/" + domain + "/data/fetch";

		// Check for data already present on the server
		List<NameValuePair> formdata = new ArrayList<NameValuePair>();
		formdata.add(new BasicNameValuePair("data_id", dataid));
		return this.get(username, getpage, formdata);
	}

	public String addOrUpdateData(String username, String domain, String id,
			String type, String contents, boolean addServer) {
		if(addServer)
			type = this.server+type;
		String postpage = "users/" + username + "/" + domain
				+ "/data/addDataForType";
		String uploadpage = "users/" + username + "/" + domain + "/upload";
		String locationpage = "users/" + username + "/" + domain + "/data/setDataLocation";

		String dataid = this.DATAID(username, domain, id);

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
			this.upload(username, uploadpage, "data", f);
			f.delete();

			List<NameValuePair> data = new ArrayList<NameValuePair>();
			data.add(new BasicNameValuePair("data_id", dataid));
			data.add(new BasicNameValuePair("data_type", type));
			response = post(username, postpage, data);
			List<NameValuePair> location = new ArrayList<NameValuePair>();
			location.add(new BasicNameValuePair("data_id", dataid));
			location.add(new BasicNameValuePair("location", "/scratch/data/wings/storage/default/users/"+username+"/"+domain+"/data/"+dataid.substring(dataid.indexOf('#')+1)));
			response = post(username, locationpage, location);
			if(response.equals("OK"))
				System.out.println("Upload successful.");
			else 
				System.out.println("Upload failed.");
		} catch (Exception e) {
			System.out.println("Upload failed.");
			e.printStackTrace();
		}
		return response;
	}

	public String addDataToWings(String username, String domain, String id,
			String type, String contents) {

		String getpage = "users/" + username + "/" + domain
				+ "/data/getDataJSON";
		String postpage = "users/" + username + "/" + domain
				+ "/data/addDataForType";
		String uploadpage = "users/" + username + "/" + domain + "/upload";

		// Add unique md5 hash to id based on contents
		String md5 = DigestUtils.md5Hex(contents.getBytes());
		Pattern extensionPattern = Pattern.compile("^(.*)(\\..+)$");
		Matcher mat = extensionPattern.matcher(id);
		if (mat.find()) {
			id = mat.group(1) + "-" + md5 + mat.group(2);
		} else
			id += "-" + md5;

		// Check for data already present on the server
		String dataid = this.DATAID(username, domain, id);
		List<NameValuePair> formdata = new ArrayList<NameValuePair>();
		formdata.add(new BasicNameValuePair("data_id", dataid));
		String datajson = this.get(username, getpage, formdata);
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
			this.upload(username, uploadpage, "data", f);
			f.delete();

			List<NameValuePair> data = new ArrayList<NameValuePair>();
			data.add(new BasicNameValuePair("data_id", dataid));
			data.add(new BasicNameValuePair("data_type", type));
			String response = this.post(username, postpage, data);
			if (response != null && response.equals("OK"))
				return dataid;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public String addDataToWingsAsFile(String username, String domain, String id, String contents) {
	    String type = this.internal_server + "/export/users/" + username + "/" + domain + "/data/ontology.owl#File";

		String postpage = "users/" + username + "/" + domain + "/data/addDataForType";
		String uploadpage = "users/" + username + "/" + domain + "/upload";
		String dataid = this.DATAID(username, domain, id);
		
		
		String sha = DigestUtils.sha1Hex(contents.getBytes());
		String correctName = "SHA" + sha.substring(0,6);
		if (!id.contains(correctName))
		    System.out.println("File " + id + " does not have the same hash: " + sha);

		// Create temporary file and upload
		try {
			File dir = File.createTempFile("tmp", "");
			if (!dir.delete() || !dir.mkdirs()) {
				System.err.println("Could not create temporary directory "
						+ dir);
				return null;
			}
			File f = new File(dir.getAbsolutePath() + "/" + id);
			FileUtils.write(f, contents);
			this.upload(username, uploadpage, "data", f);
			f.delete();

			List<NameValuePair> data = new ArrayList<NameValuePair>();
			data.add(new BasicNameValuePair("data_id", dataid));
			data.add(new BasicNameValuePair("data_type", type));
			String response = this.post(username, postpage, data);
			if (response != null && response.equals("OK"))
				return dataid;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public String addRemoteDataToWings(String username, String domain, String url) {
	  String type = this.internal_server + "/export/users/" + username + "/" + domain + "/data/ontology.owl#File";
	  String opurl = "users/" + username + "/" + domain + "/data/addRemoteDataForType";
	  List<NameValuePair> keyvalues = new ArrayList<NameValuePair>();
	  keyvalues.add(new BasicNameValuePair("data_url", url));
	  keyvalues.add(new BasicNameValuePair("data_type", type));
	  return this.post(username, opurl, keyvalues);
	}

	public String addRemoteDataToWings(String username, String domain, String url, String name) {
	    /* FIXME: Wings rename does not rename the file, only the id 
	     * thus we cannot upload two files with the same name and then rename them.
	     */
	    String fileContents = null;
	    CloseableHttpClient client = HttpClientBuilder.create().build();
	    try {
	        HttpGet securedResource = new HttpGet(url);
	        CloseableHttpResponse httpResponse = client.execute(securedResource);

	        try {
	            HttpEntity responseEntity = httpResponse.getEntity();
	            fileContents = EntityUtils.toString(responseEntity);
	            EntityUtils.consume(responseEntity);
	            httpResponse.close();
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
	    
		String dataid = addDataToWingsAsFile(username, domain, name, fileContents);
		return dataid;
	    
		/*String dataid = addRemoteDataToWings(username, domain, url);
		String newid = this.internal_server + "/export/users/" + username + "/" + domain + "/data/library.owl#" + name;
		
		System.out.println("stored id: " + dataid);
		System.out.println("new id: " + newid);

		String opurl = "users/" + username + "/" + domain + "/data/renameData";
		List<NameValuePair> keyvalues = new ArrayList<NameValuePair>();
		keyvalues.add(new BasicNameValuePair("data_id", dataid));
		keyvalues.add(new BasicNameValuePair("newid", newid));
		String rpp = this.post(username, opurl, keyvalues);
		System.out.println(">>>> " + rpp);
		return rpp;*/
	}
	
	public List<String> isFileListOnWings (String username, String domain, Set<String> filelist) {
		List<String> returnValue = new ArrayList<String>();
		String filetype = this.internal_server + "/export/users/" + username + "/" + domain + "/data/ontology.owl#File";
		String fileprefix = "<" + this.internal_server + "/export/users/" + username + "/" + domain + "/data/library.owl#";
		
		
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
		    for (String file: filelist) {
		        cur.add(file);
		        i++;
		        if (i == 20) {
		            grouped.add(cur);
		            cur = new HashSet<String>();
		            i = 0;
		        }
		    }
		    if (i != 0) { // That means the last one wasnt added yet
		        grouped.add(cur);
		    }
		}
		
		for (Set<String> group: grouped) {
		    String query = queryStart;
		    for (String file: group) query += fileprefix + file + ">\n";
		    query += queryEnd;

		    //Doing the query
            String pageid = "sparql";
            List<NameValuePair> formdata = new ArrayList<NameValuePair>();
            formdata.add(new BasicNameValuePair("query", query));
            formdata.add(new BasicNameValuePair("format", "json"));
            String resultjson = get(username, pageid, formdata);
            if (resultjson == null || resultjson.equals(""))
                return returnValue;
            
            JsonParser jsonParser = new JsonParser();
            JsonObject result = jsonParser.parse(resultjson).getAsJsonObject();
            JsonArray qbindings = result.get("results").getAsJsonObject().get("bindings").getAsJsonArray();

            for (JsonElement qbinding : qbindings) {
                JsonObject qb = qbinding.getAsJsonObject();
                if (qb.get("value") == null)
                    continue;
                String fileurl = qb.get("value").getAsJsonObject().get("value").getAsString();
                String name = fileurl.replaceAll("^.*\\#", "");
                returnValue.add(name);
            }
		}

		return returnValue;

	}

	public boolean isFileOnWings (String username, String domain, String url) {
		String id = url.replaceAll("^.*\\/", "");
		String filetype = this.internal_server + "/export/users/" + username + "/" + domain + "/data/ontology.owl#File";
		String wingsid = this.internal_server + "/export/users/" + username + "/" + domain + "/data/library.owl#" + id;
		// Only checking that the filename is already on WINGs
		String query = "SELECT DISTINCT ?prop WHERE {\n"
					 + "  <" + wingsid + "> ?prop <" + filetype + "> .\n"
					 + "\n}";

		String pageid = "sparql";
		List<NameValuePair> formdata = new ArrayList<NameValuePair>();
		formdata.add(new BasicNameValuePair("query", query));
		formdata.add(new BasicNameValuePair("format", "json"));
		String resultjson = get(username, pageid, formdata);
		if (resultjson == null || resultjson.equals(""))
			return false;
		
		JsonParser jsonParser = new JsonParser();
		JsonObject result = jsonParser.parse(resultjson).getAsJsonObject();
		JsonArray qbindings = result.get("results").getAsJsonObject().get("bindings").getAsJsonArray();

		for (JsonElement qbinding : qbindings) {
			JsonObject qb = qbinding.getAsJsonObject();
			if (qb.get("prop") == null)
				continue;
			return true;
		}
		return false;
	}

private List<NameValuePair> getBindings(String json,
		List<VariableBinding> initbindings, List<NameValuePair> formdata) {

	Map<String, Boolean> isBound = new HashMap<String, Boolean>();
	for (VariableBinding vbinding : initbindings)
		isBound.put(vbinding.getVariable(), true);

	if (json == null)
		return null;

	JsonParser jsonParser = new JsonParser();
	JsonObject expobj = jsonParser.parse(json.trim()).getAsJsonObject();

	if (!expobj.get("success").getAsBoolean())
		return null;

	JsonObject dataobj = expobj.get("data").getAsJsonObject();
	JsonArray bindingsobj = dataobj.get("bindings").getAsJsonArray();
	if (bindingsobj.size() == 0)
		return formdata;

	JsonObject bindingobj = bindingsobj.get(0).getAsJsonObject();
	for (Entry<String, JsonElement> entry : bindingobj.entrySet()) {
		if (!isBound.containsKey(entry.getKey())) {
			if (entry.getValue().isJsonArray()) {
				for (JsonElement bindingEl : entry.getValue()
						.getAsJsonArray())
					formdata.add(new BasicNameValuePair(entry.getKey(),
							this.getJsonBindingValue(bindingEl)));
			} else {
				formdata.add(new BasicNameValuePair(entry.getKey(), this
						.getJsonBindingValue(entry.getValue())));
			}
		}
	}
	return formdata;
}

private String getJsonBindingValue(JsonElement el) {
	JsonObject obj = el.getAsJsonObject();
	if (obj.get("type").getAsString().equals("uri"))
		return obj.get("id").getAsString();
	else
		return obj.get("value").getAsString();
}

private List<NameValuePair> createFormData(String username, String domain,
		String templateid, List<VariableBinding> bindings,
		Map<String, Variable> inputs) {
	List<NameValuePair> data = new ArrayList<NameValuePair>();
	HashMap<String, String> paramDTypes = new HashMap<String, String>();


	data.add(new BasicNameValuePair("templateId", templateid));

	for (VariableBinding vbinding : bindings) {
		if (paramDTypes.containsKey(vbinding.getVariable())) {
			data.add(new BasicNameValuePair(vbinding.getVariable(),
					vbinding.getBinding()));
		} else {
			data.add(new BasicNameValuePair(vbinding.getVariable(), DATAID(
					username, domain, vbinding.getBinding())));
		}
	}
	data.add(new BasicNameValuePair("__paramdtypes", json
			.toJson(paramDTypes)));
	return data;
}

private String get(String username, String pageid, List<NameValuePair> data) {
	this.login(username);
	CloseableHttpClient client = HttpClientBuilder.create().setDefaultCookieStore(this.cookieStore).build();
	try {
		String url = this.server + "/" + pageid;
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

	JsonParser jsonParser = new JsonParser();
	JsonObject expobj = jsonParser.parse(data.trim()).getAsJsonObject();
	if (!expobj.get("success").getAsBoolean())
		return vbl;

	JsonObject dataobj = expobj.get("data").getAsJsonObject();
	JsonArray datalist = dataobj.get("bindings").getAsJsonArray();
	if (datalist.size() == 0)
		return vbl;

	JsonObject bindingobj = new JsonObject();
	for(Iterator<JsonElement> it = datalist.iterator(); it.hasNext(); ) {
	  JsonElement el = it.next();
	  JsonObject obj = null;
	  if(param) {
	    // If Parameter, the entries aren't arrays
	    obj = el.getAsJsonObject();
	  }
	  else {
	    JsonArray ellist = el.getAsJsonArray();
	    if(ellist.size() > 0) {
	      obj = ellist.get(0).getAsJsonObject();
	    }
	  }
    for(Entry<String, JsonElement> entry : obj.entrySet()) {
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
				vbl.add(new VariableBinding(key,value));
			}
		}
	}
	return vbl;
}

private String toPlanAcceptableFormat(String username, String domain,
		String wfname, List<VariableBinding> vbl, Map<String, Variable> ivm) {
	String output = "";

	// Set Template ID first
	output += "{\"templateId\":\"" + wfname + "\",";
	wfname = wfname.substring(0, wfname.lastIndexOf("#") + 1);

	// Set Component Bindings
	output += "\"componentBindings\": {},";

	// Set Parameter Types
	String paramTypes = "";
	for(String key: ivm.keySet()){
		if(ivm.get(key).isParam())
		paramTypes += "\"" + wfname + key + "\":\"" + ivm.get(key).getType()+"\",";
	}
	if(paramTypes.length() > 0)
		paramTypes = paramTypes.substring(0,paramTypes.length()-1);
	output += "\"parameterTypes\": {" + paramTypes +"},";
	
	// Set Inputs (Parameters and Data)
	String paramBindings = "\"parameterBindings\": {";
	boolean paramAdded = false;
	String dataBindings = "\"dataBindings\": {";
	boolean dataAdded = false;
	String dataID = this.internal_server + "/export/users/" + username + "/" + domain
			+ "/data/library.owl#";
	for (String key : ivm.keySet()) {
		Variable v = ivm.get(key);
		if (v.isParam()) {
			for (int i = 0; i < vbl.size(); i++) {
				VariableBinding vb = vbl.get(i);
				if (vb.getVariable().equals(v.getName())) {
					paramBindings += "\"" + wfname + v.getName() + "\":\""
							+ vb.getBinding() + "\",";
					paramAdded = true;
				}
			}
		} else {
			for (int i = 0; i < vbl.size(); i++) {
				VariableBinding vb = vbl.get(i);
				if (vb.getVariable().equals(v.getName())) {
					dataBindings += "\"" + wfname + v.getName() + "\":[";
					String[] dBs = vb.getBinding()
					    .replaceFirst("^\\[",  "")
					    .replaceFirst("\\]$", "").split("\\s*,\\s*");
					for (int j = 0; j < dBs.length; j++) {
						if (dBs[j].length() > 0)
							dataBindings += "\"" + dataID + dBs[j] + "\",";
					}
					dataBindings = dataBindings.substring(0,
							dataBindings.length() - 1);
					dataBindings += "],";
					dataAdded = true;
				}
			}
		}

	}
	if (paramAdded)
		paramBindings = paramBindings.substring(0,
				paramBindings.length() - 1);
	if (dataAdded)
		dataBindings = dataBindings.substring(0, dataBindings.length() - 1);

	output += paramBindings + "}," + dataBindings + "}}";

	return output;
}

private String postWithSpecifiedMediaType(String username, String pageid, String data,
		String type, String type2) {
  this.login(username);
	CloseableHttpClient client = HttpClientBuilder.create()
      .setDefaultCookieStore(this.cookieStore).build();
	try {
		HttpPost securedResource = new HttpPost(server + "/" + pageid);
		securedResource.setEntity(new StringEntity(data));
		securedResource.addHeader("Accept", type);
		securedResource.addHeader("Content-type", type2);
		CloseableHttpResponse httpResponse = client
				.execute(securedResource);
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
} finally {
	try {
		client.close();
	} catch (IOException e) {
	}
}
return null;
}

private String post(String username, String pageid, List<NameValuePair> data) {
  this.login(username);
	CloseableHttpClient client = HttpClientBuilder.create()
      .setDefaultCookieStore(this.cookieStore).build();

	try {
		HttpPost securedResource = new HttpPost(this.server + "/" + pageid);
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

private String upload(String username, String pageid, String type, File file) {
  this.login(username);
  
	CloseableHttpClient client = HttpClientBuilder.create()
      .setDefaultCookieStore(this.cookieStore).build();
	try {
		HttpPost post = new HttpPost(this.server + "/" + pageid);
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

    //TODO: Move this to other file when we have more adapters, and is related to SPARQL bc the variables...
    @Override
    public boolean validateLOI (LineOfInquiry loi, Map<String, String> values) {
        String loiDataQuery = loi.getDataQuery();
        Set<String> workflowVars = loi.getAllWorkflowVariables();
        if ((loi.getWorkflows().size() == 0 && loi.getMetaWorkflows().size() == 0) || workflowVars.size() == 0)
            return false;
        
        //All workflow variables must be on the data-query
        for (String wvar: workflowVars)
            if (!loiDataQuery.contains(wvar))
                return false;

        // All parameters must be set.
        for (String parameter: loi.getAllWorkflowParameters())
            if (!values.containsKey(parameter.substring(1)))
                return false;

        return true;
    }
}
