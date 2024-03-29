package org.diskproject.server.api.impl;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.http.HttpStatus;
import org.apache.jena.query.QueryException;
import org.diskproject.server.repository.DiskRepository;
import org.diskproject.shared.api.DiskService;
import org.diskproject.shared.classes.adapters.MethodAdapter.FileAndMeta;
import org.diskproject.shared.classes.common.TreeItem;
import org.diskproject.shared.classes.hypothesis.Hypothesis;
import org.diskproject.shared.classes.loi.LineOfInquiry;
import org.diskproject.shared.classes.loi.TriggeredLOI;
import org.diskproject.shared.classes.question.Question;
import org.diskproject.shared.classes.question.VariableOption;
import org.diskproject.shared.classes.util.DataAdapterResponse;
import org.diskproject.shared.classes.util.ExternalDataRequest;
import org.diskproject.shared.classes.util.QuestionOptionsRequest;
import org.diskproject.shared.classes.vocabulary.Vocabulary;
import org.diskproject.shared.classes.workflow.WorkflowVariable;
import org.diskproject.shared.classes.workflow.Workflow;
import org.diskproject.shared.classes.workflow.WorkflowRun;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.Gson;

class ErrorMessage {
  // constructor
  public ErrorMessage(String message) {
    this.message = message;
  }

  public String message;
}

@Path("")
@Produces("application/json")
@Consumes("application/json")
public class DiskResource implements DiskService {

  private static String USERNAME = "admin";

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
  @Path("server/endpoints")
  @Override
  public List<DataAdapterResponse> getEndpoints() {
    return this.repo.getDataAdapters();
  }

  /*
   * Vocabulary
   */
  @GET
  @Path("vocabulary")
  @Override
  public Map<String, Vocabulary> getVocabularies() {
    return this.repo.getVocabularies();
  }

  @GET
  @Path("vocabulary/reload")
  @Produces("text/html")
  @Override
  public String reloadVocabularies() {
    try {
      this.repo.reloadKBCaches();
      return "OK";
    } catch (Exception e) {
      e.printStackTrace();
      try {
        response.sendError(500);
      } catch (IOException e1) {
        e1.printStackTrace();
      }
    }
    return "";
  }

  /**
   * Hypothesis
   */
  @POST
  @Path("hypotheses")
  @Override
  public Hypothesis addHypothesis(
      @JsonProperty("hypothesis") Hypothesis hypothesis) {

    String username = (String) request.getAttribute("username");
    if (username != null) {
      hypothesis.setAuthor(username);
    }
    return this.repo.addHypothesis(USERNAME, hypothesis);
  }

  @GET
  @Path("hypotheses")
  @Override
  public List<Hypothesis> listHypotheses() {
    return this.repo.listHypotheses(USERNAME);
  }

  @GET
  @Path("hypotheses/{id}")
  @Override
  public Hypothesis getHypothesis(
      @PathParam("id") String id) {
    return this.repo.getHypothesis(USERNAME, id);
  }

  @PUT
  @Path("hypotheses/{id}")
  @Override
  public Hypothesis updateHypothesis(
      @PathParam("id") String id,
      @JsonProperty("hypothesis") Hypothesis hypothesis) {
    String username = (String) request.getAttribute("username");
    if (username != null) {
      hypothesis.setAuthor(username);
    }
    return this.repo.updateHypothesis(USERNAME, id, hypothesis);
  }

  @DELETE
  @Path("hypotheses/{id}")
  @Override
  public void deleteHypothesis(
      @PathParam("id") String id) {
    this.repo.removeHypothesis(USERNAME, id);
  }

  @GET
  @Path("hypotheses/{id}/query")
  @Override
  public List<TriggeredLOI> queryHypothesis(
      @PathParam("id") String id) {
    Gson response_body = new Gson();
    response.setContentType("application/json");
    response.setCharacterEncoding("utf-8");
    try {
      return this.repo.queryHypothesis(USERNAME, id);
    } catch (NotFoundException e) {
      try {
        ErrorMessage error = new ErrorMessage(e.getMessage());
        String jsonData = response_body.toJson(error);
        response.setStatus(404);
        response.getWriter().print(jsonData.toString());
        response.getWriter().flush();
        System.err.println(e.getMessage());
      } catch (IOException e1) {
        e1.printStackTrace();
      }
    } catch (QueryException e) {
      try {
        ErrorMessage error = new ErrorMessage(e.getMessage());
        String jsonData = response_body.toJson(error);
        response.setStatus(HttpStatus.SC_BAD_REQUEST);
        response.getWriter().print(jsonData.toString());
        response.getWriter().flush();
        System.err.println(e.getMessage());
      } catch (IOException e1) {
        e1.printStackTrace();
      }
    } catch (Exception e) {
      try {
        ErrorMessage error = new ErrorMessage(e.getMessage());
        String jsonData = response_body.toJson(error);
        response.setContentType("application/json");
        response.setCharacterEncoding("utf-8");
        response.setStatus(500);
        response.getWriter().print(jsonData.toString());
        response.getWriter().flush();
        e.printStackTrace();
      } catch (IOException e1) {
        e1.printStackTrace();
      }
    }
    return null;
  }

  /**
   * Assertions LATER!
   * 
   * @POST
   *       @Path("assertions")
   * @Override
   *           public void addAssertion(
   *           @JsonProperty("assertions") Graph assertions) {
   *           this.repo.addAssertion(USERNAME, assertions);
   *           }
   * 
   * @GET
   *      @Path("assertions")
   * @Override
   *           public Graph listAssertions() {
   *           return this.repo.listAssertions(USERNAME, DOMAIN);
   *           }
   * 
   * @DELETE
   *         @Path("assertions")
   * @Override
   *           public void deleteAssertion(
   *           @JsonProperty("assertions") Graph assertions) {
   *           this.repo.deleteAssertion(USERNAME, assertions);
   *           }
   * 
   * @PUT
   *      @Path("assertions")
   * @Override
   *           public void updateAssertions (
   *           @JsonProperty("assertions") Graph assertions) {
   *           this.repo.updateAssertions(USERNAME, assertions);
   *           }
   */

  /**
   * Lines of Inquiry
   */
  @POST
  @Path("lois")
  @Override
  public LineOfInquiry addLOI(
      @JsonProperty("loi") LineOfInquiry loi) {

    String username = (String) request.getAttribute("username");
    if (username != null) {
      loi.setAuthor(username);
    }
    return this.repo.addLOI(USERNAME, loi);
  }

  @GET
  @Path("lois")
  @Override
  public List<TreeItem> listLOIs() {
    return this.repo.listLOIs(USERNAME);
  }

  @GET
  @Path("lois/{id}")
  @Override
  public LineOfInquiry getLOI(
      @PathParam("id") String id) {
    return this.repo.getLOI(USERNAME, id);
  }

  @PUT
  @Path("lois/{id}")
  @Override
  public LineOfInquiry updateLOI(
      @PathParam("id") String id,
      @JsonProperty("loi") LineOfInquiry loi) {
    String username = (String) request.getAttribute("username");
    if (username != null) {
      loi.setAuthor(username);
    }
    return this.repo.updateLOI(USERNAME, id, loi);
  }

  @DELETE
  @Path("lois/{id}")
  @Override
  public void deleteLOI(
      @PathParam("id") String id) {
    this.repo.removeLOI(USERNAME, id);
  }

  /*
   * Triggered LOIs
   */
  @POST
  @Path("tlois")
  @Override
  public TriggeredLOI addTriggeredLOI(
      @JsonProperty("tloi") TriggeredLOI tloi) {
    return this.repo.addTriggeredLOI(USERNAME, tloi);
  }

  @PUT
  @Path("tlois/{id}")
  @Override
  public TriggeredLOI updateTLOI(
      @PathParam("id") String id,
      @JsonProperty("tloi") TriggeredLOI tloi) {
    String username = (String) request.getAttribute("username");
    if (username != null) {
      tloi.setAuthor(username);
    }
    return this.repo.updateTLOINotes(USERNAME, id, tloi);
  }

  @GET
  @Path("tlois")
  @Override
  public List<TriggeredLOI> listTriggeredLOIs() {
    return this.repo.listTriggeredLOIs(USERNAME);
  }

  @GET
  @Path("tlois/{id}")
  @Override
  public TriggeredLOI getTriggeredLOI(
      @PathParam("id") String id) {
    return this.repo.getTriggeredLOI(USERNAME, id);
  }

  @DELETE
  @Path("tlois/{id}")
  @Override
  public void deleteTriggeredLOI(
      @PathParam("id") String id) {
    this.repo.removeTriggeredLOI(USERNAME, id);
  }

  /*
   * Workflows
   */
  @GET
  @Override
  @Path("workflows")
  public List<Workflow> listWorkflows() {
    Gson response_error = new Gson();
    try {
      return this.repo.methodAdapters.getWorkflowList();
    } catch (Exception e) {
      try {
        // Create Json error response
        ErrorMessage error = new ErrorMessage(e.getMessage());
        String jsonData = response_error.toJson(error);

        // Prepare the response
        response.setContentType("application/json");
        response.setCharacterEncoding("utf-8");
        response.setStatus(500);

        // Send the response
        response.getWriter().print(jsonData.toString());
        response.getWriter().flush();
      } catch (IOException e1) {
        e1.printStackTrace();
      }
    }
    return null;
  }

  @GET
  @Override
  @Path("workflows/{source}/{id}")
  public List<WorkflowVariable> getWorkflowVariables(
      @PathParam("source") String source,
      @PathParam("id") String id) {
    return this.repo.methodAdapters.getWorkflowVariablesByName(source, id);
  }

  @GET
  @Override
  @Path("runs/{source}/{id}")
  public WorkflowRun monitorWorkflow(
      @PathParam("source") String source,
      @PathParam("id") String id) {
    return this.repo.getWorkflowRunStatus(source, id);
  }

  @GET
  @Path("externalQuery")
  @Override
  public Map<String, List<String>> queryExternalStore(
      @QueryParam("endpoint") String endpoint,
      @QueryParam("variables") String variables,
      @QueryParam("query") String query) {
    try {
      // return WingsAdapter.get().getWorkflowList();
      return repo.queryExternalStore(endpoint, query, variables);
    } catch (Exception e) {
      try {
        // Create Json error response
        Gson gson = new Gson();
        ErrorMessage error = new ErrorMessage(e.getMessage());
        String jsonData = gson.toJson(error);

        // Prepare the response
        response.setContentType("application/json");
        response.setCharacterEncoding("utf-8");
        response.setStatus(500);

        // Send the response
        response.getWriter().print(jsonData.toString());
        response.getWriter().flush();
      } catch (IOException e1) {
        e1.printStackTrace();
      }
    }
    return null;
  }

  /*
   * Question
   */
  @GET
  @Path("questions")
  @Override
  public List<Question> listQuestions() {
    return this.repo.listHypothesesQuestions();
  }

  @POST
  @Path("question/options")
  public Map<String, List<VariableOption>> listDynamicOptions(
      @JsonProperty("config") QuestionOptionsRequest opts) {
    try {
      return this.repo.listDynamicOptions(opts);
    } catch (Exception e) {
      try {
        e.printStackTrace();
        // Create Json error response
        Gson gson = new Gson();
        ErrorMessage error = new ErrorMessage(e.getMessage());
        String jsonData = gson.toJson(error);

        // Prepare the response
        response.setContentType("application/json");
        response.setCharacterEncoding("utf-8");
        response.setStatus(500);

        // Send the response
        response.getWriter().print(jsonData.toString());
        response.getWriter().flush();
      } catch (IOException e1) {
        e1.printStackTrace();
      }
    }
    return null;
      };
  
  /*
   * CUSTOM
   */
  @GET
  @Path("executions/{hid}/{lid}")
  @Override
  public List<TriggeredLOI> getExecutions(
      @PathParam("hid") String hid,
      @PathParam("lid") String lid) {
    return this.repo.getTLOIsForHypothesisAndLOI(USERNAME, hid, lid);
  }

  @GET
  @Path("execute/{hid}/{lid}")
  @Override
  public List<TriggeredLOI> runHypothesisAndLOI(
      @PathParam("hid") String hid,
      @PathParam("lid") String lid) {
    try {
      return this.repo.runHypothesisAndLOI(USERNAME, hid, lid);
    } catch (Exception e) {
      try {
        // Create Json error response
        Gson gson = new Gson();
        ErrorMessage error = new ErrorMessage(e.getMessage());
        String jsonData = gson.toJson(error);

        // Prepare the response
        response.setContentType("application/json");
        response.setCharacterEncoding("utf-8");
        response.setStatus(500);

        // Send the response
        response.getWriter().print(jsonData.toString());
        response.getWriter().flush();
      } catch (IOException e1) {
        e1.printStackTrace();
      }
    }
    return null;
  }

  @GET
  @Path("runhypotheses")
  @Override
  public Boolean runHypotheses() {
    try {
      return this.repo.runAllHypotheses(USERNAME);
    } catch (Exception e) {
      try {
        // Create Json error response
        Gson gson = new Gson();
        ErrorMessage error = new ErrorMessage(e.getMessage());
        String jsonData = gson.toJson(error);

        // Prepare the response
        response.setContentType("application/json");
        response.setCharacterEncoding("utf-8");
        response.setStatus(500);

        // Send the response
        response.getWriter().print(jsonData.toString());
        response.getWriter().flush();
      } catch (IOException e1) {
        e1.printStackTrace();
      }
    }
    return null;
  }

  @GET
  @Path("tloi/{tloiId}/narratives")
  @Override
  public Map<String, String> getNarratives(
      @PathParam("tloiId") String tloiId) {
    return this.repo.getNarratives(USERNAME, tloiId);
  }

  @POST
  @Path("getData")
  @Override
  public Response getOutputData(@JsonProperty("request") ExternalDataRequest r) {
    FileAndMeta result = this.repo.getOutputData(r.getSource(), r.getDataId());
    if (result == null) {
      ResponseBuilder rBuilder = Response.status(Response.Status.NOT_FOUND);
      return rBuilder.type(MediaType.TEXT_PLAIN)
          .entity("Could not find file")
          .build();
    } 

    ResponseBuilder rBuild = Response.ok(result.data, result.contentType);
    return rBuild.build();
  }
}