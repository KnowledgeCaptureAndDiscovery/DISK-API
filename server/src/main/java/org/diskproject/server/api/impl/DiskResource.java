package org.diskproject.server.api.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

import org.apache.commons.configuration.plist.PropertyListConfiguration;
import org.diskproject.server.repository.DiskRepository;
import org.diskproject.server.repository.WingsAdapter;
import org.diskproject.shared.api.DiskService;
import org.diskproject.shared.classes.common.Graph;
import org.diskproject.shared.classes.common.TreeItem;
import org.diskproject.shared.classes.hypothesis.Hypothesis;
import org.diskproject.shared.classes.loi.LineOfInquiry;
import org.diskproject.shared.classes.loi.TriggeredLOI;
import org.diskproject.shared.classes.question.Question;
import org.diskproject.shared.classes.vocabulary.Vocabulary;
import org.diskproject.shared.classes.workflow.Variable;
import org.diskproject.shared.classes.workflow.Workflow;
import org.diskproject.shared.classes.workflow.WorkflowRun;

import com.fasterxml.jackson.annotation.JsonProperty;


@Path("")
@Produces("application/json")
@Consumes("application/json")
public class DiskResource implements DiskService {

  @Context
  HttpServletResponse response;
  @Context
  HttpServletRequest request;
  @Context
  SecurityContext securityContext;
  
  DiskRepository repo;
  
  public DiskResource() {
    this.repo = DiskRepository.get(); 
  }
  
  @GET
  @Path("server/config")
  @Override
  public Map<String, String> getConfig() {
    try {
      PropertyListConfiguration config = this.repo.getConfig();
      Map<String, String> vals = new HashMap<String, String>();
      vals.put("username", config.getProperty("username").toString());
      vals.put("domain", config.getProperty("domain").toString());
      vals.put("wings.server", config.getProperty("wings.server").toString());
      return vals;
    } catch (Exception e) {
      // e.printStackTrace();
      throw new RuntimeException("Exception: " + e.getMessage());
    }
  }

  @GET
  @Path("server/endpoints")
  @Override
  public Map<String, String> getEndpoints () {
      return this.repo.getEndpoints();
  }
  
  /*
   * Vocabulary
   */
  @GET
  @Path("vocabulary")
  @Override
  public Map<String, Vocabulary> getVocabularies() {
    try {
      return this.repo.getVocabularies();
    } catch (Exception e) {
      // e.printStackTrace();
      throw new RuntimeException("Exception: " + e.getMessage());
    }
  }

  @GET
  @Path("{username}/{domain}/vocabulary")
  @Override
  public Vocabulary getUserVocabulary(
      @PathParam("username") String username, 
      @PathParam("domain") String domain) {
    try {
      return this.repo.getUserVocabulary(username, domain);
    } catch (Exception e) {
      // e.printStackTrace();
      throw new RuntimeException("Exception: " + e.getMessage());
    }
  }

  @GET
  @Path("vocabulary/reload")
  @Produces("text/html")
  @Override
  public String reloadVocabularies() {
    try {
      this.repo.reloadKBCaches();
      //this.repo.initializeKB();
      response.sendRedirect("");
      return "";
    } catch (Exception e) {
      // e.printStackTrace();
      throw new RuntimeException("Exception: " + e.getMessage());
    }
  }
  
  @GET
  @Path("vocabulary/reload")
  public String APIReloadVocabularies() {
    try {
      this.repo.reloadKBCaches();
      //this.repo.initializeKB();
      return "OK";
    } catch (Exception e) {
      // e.printStackTrace();
      throw new RuntimeException("Exception: " + e.getMessage());
    }
  }
    
    
  /**
   * Hypothesis
   */
  @POST
  @Path("{username}/{domain}/hypotheses")
  @Override
  public void addHypothesis(
      @PathParam("username") String username, 
      @PathParam("domain") String domain,
      @JsonProperty("hypothesis") Hypothesis hypothesis) {
    this.repo.addHypothesis(username, domain, hypothesis);
  }
      
  @GET
  @Path("{username}/{domain}/hypotheses")
  @Override
  public List<TreeItem> listHypotheses(
      @PathParam("username") String username, 
      @PathParam("domain") String domain) {
    return this.repo.listHypotheses(username, domain);
  }
  
  @GET
  @Path("{username}/{domain}/hypotheses/{id}")
  @Override
  public Hypothesis getHypothesis(
      @PathParam("username") String username, 
      @PathParam("domain") String domain,
      @PathParam("id") String id) {
    return this.repo.getHypothesis(username, domain, id);
  }
  
  @PUT
  @Path("{username}/{domain}/hypotheses/{id}")
  @Override
  public void updateHypothesis(
      @PathParam("username") String username, 
      @PathParam("domain") String domain,
      @PathParam("id") String id,
      @JsonProperty("hypothesis") Hypothesis hypothesis) {
    this.repo.updateHypothesis(username, domain, id, hypothesis);
  }
  
  @DELETE
  @Path("{username}/{domain}/hypotheses/{id}")
  @Override
  public void deleteHypothesis(
      @PathParam("username") String username, 
      @PathParam("domain") String domain,
      @PathParam("id") String id) {
    this.repo.deleteHypothesis(username, domain, id);
  }
  
  @GET
  @Path("{username}/{domain}/hypotheses/{id}/query")
  @Override
  public List<TriggeredLOI> queryHypothesis(
      @PathParam("username") String username, 
      @PathParam("domain") String domain,
      @PathParam("id") String id) {
    return this.repo.queryHypothesis(username, domain, id);
  }

  @GET
  @Path("{username}/{domain}/hypotheses/{id}/tlois")
  @Override
  public Map<String, List<TriggeredLOI>> getHypothesisTLOIs(
      @PathParam("username") String username, 
      @PathParam("domain") String domain,
      @PathParam("id") String id) {
    return this.repo.getHypothesisTLOIs(username, domain, id);
  }

  /**
   * Assertions
   */
  @POST
  @Path("{username}/{domain}/assertions")
  @Override
  public void addAssertion(
      @PathParam("username") String username, 
      @PathParam("domain") String domain, 
      @JsonProperty("assertions") Graph assertions) {
    this.repo.addAssertion(username, domain, assertions);
  }
  
  @GET
  @Path("{username}/{domain}/assertions")
  @Override
  public Graph listAssertions(
      @PathParam("username") String username, 
      @PathParam("domain") String domain) {
    return this.repo.listAssertions(username, domain);
  }
  
  @DELETE
  @Path("{username}/{domain}/assertions")
  @Override
  public void deleteAssertion(
      @PathParam("username") String username, 
      @PathParam("domain") String domain,
      @JsonProperty("assertions") Graph assertions) {
    this.repo.deleteAssertion(username, domain, assertions);
  }
  
  @PUT
  @Path("{username}/{domain}/assertions")
  @Override
  public void updateAssertions (
      @PathParam("username") String username, 
      @PathParam("domain") String domain, 
      @JsonProperty("assertions") Graph assertions) {
    this.repo.updateAssertions(username, domain, assertions);
  }
  
  /**
   * Lines of Inquiry
   */
  @POST
  @Path("{username}/{domain}/lois")
  @Override
  public void addLOI(
      @PathParam("username") String username, 
      @PathParam("domain") String domain, 
      @JsonProperty("loi") LineOfInquiry loi) {
    this.repo.addLOI(username, domain, loi);
  }

  @GET
  @Path("{username}/{domain}/lois")
  @Override
  public List<TreeItem> listLOIs(
      @PathParam("username") String username, 
      @PathParam("domain") String domain) {
    return this.repo.listLOIs(username, domain);
  }

  @GET
  @Path("{username}/{domain}/lois/{id}")
  @Override
  public LineOfInquiry getLOI(
      @PathParam("username") String username, 
      @PathParam("domain") String domain, 
      @PathParam("id") String id) {
    return this.repo.getLOI(username, domain, id);
  }

  @PUT
  @Path("{username}/{domain}/lois/{id}")
  @Override
  public void updateLOI(
      @PathParam("username") String username, 
      @PathParam("domain") String domain,
      @PathParam("id") String id,
      @JsonProperty("loi") LineOfInquiry loi) {
    this.repo.updateLOI(username, domain, id, loi);
  }
  
  @DELETE
  @Path("{username}/{domain}/lois/{id}")
  @Override
  public void deleteLOI(
      @PathParam("username") String username, 
      @PathParam("domain") String domain,
      @PathParam("id") String id) {
    this.repo.deleteLOI(username, domain, id);
  }
  
  /*
   * Triggered LOIs
   */
  @POST
  @Path("{username}/{domain}/tlois")
  @Override
  public void addTriggeredLOI(@PathParam("username") String username, 
      @PathParam("domain") String domain,
      @JsonProperty("tloi") TriggeredLOI tloi) {
    this.repo.addTriggeredLOI(username, domain, tloi);
  }
  
  @GET
  @Path("{username}/{domain}/tlois")
  @Override
  public List<TriggeredLOI> listTriggeredLOIs(@PathParam("username") String username, 
      @PathParam("domain") String domain) {
    return this.repo.listTriggeredLOIs(username, domain);
  }
  
  @GET
  @Path("{username}/{domain}/tlois/{id}")
  @Override
  public TriggeredLOI getTriggeredLOI(@PathParam("username") String username, 
      @PathParam("domain") String domain,
      @PathParam("id") String id) {
    return this.repo.getTriggeredLOI(username, domain, id);
  }
  
  @DELETE
  @Path("{username}/{domain}/tlois/{id}")
  @Override
  public void deleteTriggeredLOI(@PathParam("username") String username, 
      @PathParam("domain") String domain,
      @PathParam("id") String id) {
    this.repo.deleteTriggeredLOI(username, domain, id);
  }
 
  /*
   * Workflows
   */
  @GET
  @Override
  @Path("{username}/{domain}/workflows")
  public List<Workflow> listWorkflows(
      @PathParam("username") String username, 
      @PathParam("domain") String domain) {
    return WingsAdapter.get().getWorkflowList(username, domain);
  }

  @GET
  @Override
  @Path("{username}/{domain}/workflows/{id}")
  public List<Variable> getWorkflowVariables(
      @PathParam("username") String username, 
      @PathParam("domain") String domain,
      @PathParam("id") String id) {
    return WingsAdapter.get().getWorkflowVariables(username, domain, id);    
  }
  
  @GET
  @Override
  @Path("{username}/{domain}/runs/{id}")
  public WorkflowRun monitorWorkflow(
      @PathParam("username") String username, 
      @PathParam("domain") String domain,
      @PathParam("id") String id) {
    // Check execution status
    return WingsAdapter.get().getWorkflowRunStatus(username, domain, id);
  }  

  @GET
  @Path("{username}/{domain}/externalQuery")
  @Override
  public Map<String, List<String>> queryExternalStore(
      @PathParam("username") String username, 
      @PathParam("domain") String domain, 
      @QueryParam("endpoint") String endpoint,
      @QueryParam("variables") String variables,
      @QueryParam("query") String query) {
	return repo.queryExternalStore(username, domain, endpoint, variables, query);
  }
  
  
  /*
   * Question
   */
  @GET
  @Path("{username}/{domain}/questions")
  @Override
  public List<Question> listQuestions(
      @PathParam("username") String username, 
      @PathParam("domain") String domain) {
    return this.repo.listHypothesesQuestions();
  }

  @GET
  @Path("{username}/{domain}/question/{id}/options")
  @Override
  public List<List<String>> listOptions(
      @PathParam("username") String username, 
      @PathParam("domain") String domain,
	  @PathParam("id") String id) {
    return this.repo.listVariableOptions(id);
  };

  /*
   * CUSTOM
   */
  @GET
  @Path("{username}/{domain}/executions/{hid}/{lid}")
  @Override
  public List<TriggeredLOI> getExecutions(
      @PathParam("username") String username, 
      @PathParam("domain") String domain,
      @PathParam("hid") String hid, 
      @PathParam("lid") String lid) {
	return this.repo.getTLOIsForHypothesisAndLOI(username, domain, hid, lid);
  }

  @GET
  @Path("{username}/{domain}/execute/{hid}/{lid}")
  @Override
  public List<TriggeredLOI> runHypothesisAndLOI(
      @PathParam("username") String username, 
      @PathParam("domain") String domain,
      @PathParam("hid") String hid, 
      @PathParam("lid") String lid) {
	return this.repo.runHypothesisAndLOI(username, domain, hid, lid);
  }
  
  
  @GET
  @Path("{username}/{domain}/runhypotheses")
  @Override
  public Boolean runHypotheses(
      @PathParam("username") String username, 
      @PathParam("domain") String domain) {
	return this.repo.runAllHypotheses(username, domain);
  }

  @GET
  @Path("{username}/{domain}/tloi/{tloiid}/narratives")
  @Override
  public Map<String, String> getNarratives(
      @PathParam("username") String username, 
      @PathParam("domain") String domain,
      @PathParam("tloiid") String tloiid) {
	return this.repo.getNarratives(username, domain, tloiid);
  }
  
  @GET
  @Path("{username}/{domain}/wings-data/{dataid}")
  @Produces("application/json")
  @Override
  public String getDataFromWings(
      @PathParam("username") String username, 
      @PathParam("domain") String domain,
      @PathParam("dataid") String dataid) {
	String result = this.repo.getDataFromWings(username, domain, dataid);
	if (result == null) {
		System.out.println("ERROR: " + dataid + " not available on WINGS.");
		result = "";
	} 
	return result;
  }
  
}