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
import javax.ws.rs.core.Response;
import javax.ws.rs.Produces;

import org.diskproject.shared.classes.hypothesis.Goal;
import org.diskproject.shared.classes.loi.LineOfInquiry;
import org.diskproject.shared.classes.loi.TriggeredLOI;
import org.diskproject.shared.classes.question.Question;
import org.diskproject.shared.classes.question.VariableOption;
import org.diskproject.shared.classes.util.DataAdapterResponse;
import org.diskproject.shared.classes.util.ExternalDataRequest;
import org.diskproject.shared.classes.util.QuestionOptionsRequest;
import org.diskproject.shared.classes.util.WorkflowTemplateResponse;
import org.diskproject.shared.classes.vocabulary.Vocabulary;
import org.diskproject.shared.classes.workflow.WorkflowVariable;
import org.diskproject.shared.classes.workflow.WorkflowRun;
import com.fasterxml.jackson.annotation.JsonProperty;

@Path("")
@Produces("application/json")
@Consumes("application/json")
public interface DiskService {
  @GET
  @Path("server/endpoints")
  public List<DataAdapterResponse> getEndpoints();
  
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
   * Goals
   */
  @POST
  @Path("goals")
  @Produces("application/json")
  @Consumes("application/json")
  public Goal addGoal(
      @JsonProperty("goal") Goal goal);
  
  @GET
  @Path("goals")
  public List<Goal> listGoals();
  
  @GET
  @Path("goals/{id}")
  public Goal getGoal(
      @PathParam("id") String id);
  
  @PUT
  @Path("goals/{id}")
  @Produces("application/json")
  @Consumes("application/json")
  public Goal updateGoal(
      @PathParam("id") String id,
      @JsonProperty("goal") Goal goal);
  
  @DELETE
  @Path("goals/{id}")
  public void deleteGoal(
      @PathParam("id") String id);
  
  @GET
  @Path("goals/{id}/query")
  public List<TriggeredLOI> queryGoal(
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
  public List<LineOfInquiry> listLOIs();

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

  @PUT
  @Produces("application/json")
  @Consumes("application/json")
  @Path("tlois/{id}")
  public TriggeredLOI updateTLOI(
      @PathParam("id") String id,
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
  public List<WorkflowTemplateResponse> listWorkflows();

  @GET
  @Path("workflows/{source}/{id}")
  public List<WorkflowVariable> getWorkflowVariables(
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

  @POST
  @Path("question/options")
  public Map<String, List<VariableOption>> listDynamicOptions(
      @JsonProperty("config") QuestionOptionsRequest opts);
  
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

  
  @GET
  @Path("externalQuery")  
  public Map<String, List<String>> queryExternalStore(
      @QueryParam("endpoint") String endpoint,
      @QueryParam("variables") String variables,
      @QueryParam("query") String query);
  
  @POST
  @Path("getData")
  public Response getOutputData(
      @JsonProperty("request") ExternalDataRequest r);
}