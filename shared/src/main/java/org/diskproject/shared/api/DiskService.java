package org.diskproject.shared.api;

import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Produces;

import org.diskproject.shared.classes.common.TreeItem;
import org.diskproject.shared.classes.hypothesis.Hypothesis;
import org.diskproject.shared.classes.loi.LineOfInquiry;
import org.diskproject.shared.classes.loi.TriggeredLOI;
import org.diskproject.shared.classes.question.Question;
import org.diskproject.shared.classes.vocabulary.Vocabulary;
import org.diskproject.shared.classes.workflow.Variable;
import org.diskproject.shared.classes.workflow.Workflow;
import org.diskproject.shared.classes.workflow.WorkflowRun;
import org.fusesource.restygwt.client.DirectRestService;

import com.fasterxml.jackson.annotation.JsonProperty;

@Path("")
@Produces("application/json")
@Consumes("application/json")
public interface DiskService extends DirectRestService {
  @GET
  @Path("server/endpoints")
  public Map<String, String> getEndpoints();
  
   /*
   * Vocabulary
   */
  @GET
  @Path("vocabulary")
  public Map<String, Vocabulary> getVocabularies();
  
  @GET
  @Path("vocabulary/reload")
  @Produces("text/html")
  public String reloadVocabularies();
  
  /*
   * Hypothesis
   */
  @POST
  @Path("hypotheses")
  @Produces("application/json")
  @Consumes("application/json")
  public Hypothesis addHypothesis(
      @JsonProperty("hypothesis") Hypothesis hypothesis);
  
  @GET
  @Path("hypotheses")
  public List<TreeItem> listHypotheses();
  
  @GET
  @Path("hypotheses/{id}")
  public Hypothesis getHypothesis(
      @PathParam("id") String id);
  
  @PUT
  @Path("hypotheses/{id}")
  @Produces("application/json")
  @Consumes("application/json")
  public Hypothesis updateHypothesis(
      @PathParam("id") String id,
      @JsonProperty("hypothesis") Hypothesis hypothesis);
  
  @DELETE
  @Path("hypotheses/{id}")
  public void deleteHypothesis(
      @PathParam("id") String id);
  
  @GET
  @Path("hypotheses/{id}/query")
  public List<TriggeredLOI> queryHypothesis(
      @PathParam("id") String id);
  
  /*
   * Lines of Inquiry
   */

  @POST
  @Path("lois")  
  @Produces("application/json")
  @Consumes("application/json")
  public LineOfInquiry addLOI(
      @JsonProperty("loi") LineOfInquiry loi);

  @GET
  @Path("lois")  
  public List<TreeItem> listLOIs();

  @GET
  @Path("lois/{id}")  
  public LineOfInquiry getLOI(
      @PathParam("id") String id);

  @PUT
  @Produces("application/json")
  @Consumes("application/json")
  @Path("lois/{id}")
  public LineOfInquiry updateLOI(
      @PathParam("id") String id,
      @JsonProperty("loi") LineOfInquiry loi);

  @DELETE
  @Path("lois/{id}")  
  public void deleteLOI(
      @PathParam("id") String id);

  /*
   * Triggered LOIs
   */
  @POST
  @Produces("application/json")
  @Consumes("application/json")
  @Path("tlois")
  public TriggeredLOI addTriggeredLOI(
      @JsonProperty("tloi") TriggeredLOI tloi);
  
  @GET
  @Path("tlois")
  public List<TriggeredLOI> listTriggeredLOIs();

  @GET
  @Path("tlois/{id}")
  public TriggeredLOI getTriggeredLOI(
      @PathParam("id") String id);

  @DELETE
  @Path("tlois/{id}")
  public void deleteTriggeredLOI(
      @PathParam("id") String id);

  
  /*
   * Workflows
   */
  @GET
  @Path("workflows")
  public List<Workflow> listWorkflows();

  @GET
  @Path("workflows/{source}/{id}")
  public List<Variable> getWorkflowVariables(
      @PathParam("source") String source,
      @PathParam("id") String id);

  @GET
  @Path("runs/{source}/{id}")
  public WorkflowRun monitorWorkflow(
      @PathParam("source") String source,
      @PathParam("id") String id);


  /*
   * Hypothesis questions
   */

  @GET
  @Path("questions")
  public List<Question> listQuestions();
  
  @GET
  @Path("question/{id}/options")
  public List<List<String>> listOptions(
      @PathParam("id") String id);
  
  /*
   * CUSTOM
   */
  /* Re execute all hypotheses */
  @GET
  @Path("runhypotheses")
  public Boolean runHypotheses();

  /* Get TLOIs for Hypothesis ID and LOI ID */
  @GET
  @Path("executions/{hid}/{lid}")
  public List<TriggeredLOI> getExecutions(
      @PathParam("hid") String hid, 
      @PathParam("lid") String lid);
  
  
  @GET
  @Path("execute/{hid}/{lid}")
  public List<TriggeredLOI> runHypothesisAndLOI(
      @PathParam("hid") String hid, 
      @PathParam("lid") String lid);

  
  /* Get narratives for tloi id*/
  @GET
  @Path("tloi/{tloiid}/narratives")
  public Map<String, String> getNarratives(
      @PathParam("tloiid") String tloiid);


  @GET
  @Path("externalQuery")  
  public Map<String, List<String>> queryExternalStore(
      @QueryParam("endpoint") String endpoint,
      @QueryParam("variables") String variables,
      @QueryParam("query") String query);
  
  @GET
  @Produces("application/json")
  @Path("outputs/{source}/{dataid}")
  public String getOutputData(
      @PathParam("source") String source,
      @PathParam("dataid") String dataid);
}