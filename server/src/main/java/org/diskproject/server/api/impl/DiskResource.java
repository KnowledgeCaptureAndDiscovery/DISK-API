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
import org.diskproject.shared.classes.DISKResource;
import org.diskproject.shared.classes.adapters.MethodAdapter.FileAndMeta;
import org.diskproject.shared.classes.common.Entity;
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
import org.diskproject.shared.classes.workflow.Execution;

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
  @Context HttpServletResponse response;
  @Context HttpServletRequest request;
  @Context SecurityContext securityContext;

  @GET
  @Path("server/endpoints")
  @Override
  public List<DataAdapterResponse> getEndpoints() {
    return DiskRepository.get().getDataAdapters();
  }

  /*
   * Vocabulary
   */
  @GET
  @Path("vocabulary")
  @Override
  public Map<String, Vocabulary> getVocabularies() {
    return DiskRepository.get().getVocabularies();
  }

  @GET
  @Path("vocabulary/reload")
  @Produces("text/html")
  @Override
  public String reloadVocabularies() {
    try {
      DiskRepository.get().reloadKBCaches();
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

  //--
  private void addAuthorFromRequest (DISKResource obj, HttpServletRequest request) {
    //Enumeration attr = request.getAttributeNames();
    //while (attr.hasMoreElements()) {
    //  String value =  attr.nextElement().toString();
    //  System.out.println(value + " = " + request.getAttribute(value));
    //}

    String username = (String) request.getAttribute("username"); //username is an email.
    if (username != null) {
      Entity author = DiskRepository.get().getOrCreateEntity(username);
      obj.setAuthor(author);
    }
  }

  /**
   * Goals
   */
  @POST
  @Path("goals")
  @Override
  public Goal addGoal(
      @JsonProperty("goal") Goal goal) {
    this.addAuthorFromRequest(goal, request);
    return DiskRepository.get().addGoal(goal);
  }

  @GET
  @Path("goals")
  @Override
  public List<Goal> listGoals() {
    return DiskRepository.get().listGoals();
  }

  @GET
  @Path("goals/{id}")
  @Override
  public Goal getGoal(
      @PathParam("id") String id) {
    return DiskRepository.get().getGoal(id);
  }

  @PUT
  @Path("goals/{id}")
  @Override
  public Goal updateGoal(
      @PathParam("id") String id,
      @JsonProperty("goal") Goal goal) {
    this.addAuthorFromRequest(goal, request);
    return DiskRepository.get().updateGoal(id, goal);
  }

  @DELETE
  @Path("goals/{id}")
  @Override
  public void deleteGoal(
      @PathParam("id") String id) {
    DiskRepository.get().removeGoal(id);
  }

  @GET
  @Path("goals/{id}/query")
  @Override
  public List<TriggeredLOI> queryGoal(
      @PathParam("id") String id) {
    Gson response_body = new Gson();
    response.setContentType("application/json");
    response.setCharacterEncoding("utf-8");
    try {
      return DiskRepository.get().queryGoal(id);
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
   * Lines of Inquiry
   */
  @POST
  @Path("lois")
  @Override
  public LineOfInquiry addLOI(
      @JsonProperty("loi") LineOfInquiry loi) {
    this.addAuthorFromRequest(loi, request);
    return DiskRepository.get().addLOI(loi);
  }

  @GET
  @Path("lois")
  @Override
  public List<LineOfInquiry> listLOIs() {
    return DiskRepository.get().listLOIs();
  }

  @GET
  @Path("lois/{id}")
  @Override
  public LineOfInquiry getLOI(
      @PathParam("id") String id) {
    return DiskRepository.get().getLOI(id);
  }

  @PUT
  @Path("lois/{id}")
  @Override
  public LineOfInquiry updateLOI(
      @PathParam("id") String id,
      @JsonProperty("loi") LineOfInquiry loi) {
    this.addAuthorFromRequest(loi, request);
    return DiskRepository.get().updateLOI(id, loi);
  }

  @DELETE
  @Path("lois/{id}")
  @Override
  public void deleteLOI(
      @PathParam("id") String id) {
    DiskRepository.get().removeLOI(id);
  }

  /*
   * Triggered LOIs
   */
  @POST
  @Path("tlois")
  @Override
  public TriggeredLOI addTriggeredLOI(
      @JsonProperty("tloi") TriggeredLOI tloi) {
    return DiskRepository.get().addTriggeredLOI(tloi);
  }

  @PUT
  @Path("tlois/{id}")
  @Override
  public TriggeredLOI updateTLOI(
      @PathParam("id") String id,
      @JsonProperty("tloi") TriggeredLOI tloi) {
    this.addAuthorFromRequest(tloi, request);
    return DiskRepository.get().updateTriggeredLOI(id, tloi);
  }

  @GET
  @Path("tlois")
  @Override
  public List<TriggeredLOI> listTriggeredLOIs() {
    return DiskRepository.get().listTriggeredLOIs();
  }

  @GET
  @Path("tlois/{id}")
  @Override
  public TriggeredLOI getTriggeredLOI(
      @PathParam("id") String id) {
    return DiskRepository.get().getTriggeredLOI(id);
  }

  @DELETE
  @Path("tlois/{id}")
  @Override
  public void deleteTriggeredLOI(
      @PathParam("id") String id) {
    DiskRepository.get().removeTriggeredLOI(id);
  }

  /*
   * Workflows
   */
  @GET
  @Override
  @Path("workflows")
  public List<WorkflowTemplateResponse> listWorkflows() {
    Gson response_error = new Gson();
    try {
      return DiskRepository.get().methodAdapters.getWorkflowList();
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
    return DiskRepository.get().methodAdapters.getWorkflowVariablesByName(source, id);
  }

  @GET
  @Override
  @Path("runs/{source}/{id}")
  public Execution monitorWorkflow(
      @PathParam("source") String source,
      @PathParam("id") String id) {
    return DiskRepository.get().getWorkflowRunStatus(source, id);
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
      return DiskRepository.get().queryExternalStore(endpoint, query, variables);
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
    return DiskRepository.get().listHypothesesQuestions();
  }

  @POST
  @Path("question/options")
  public Map<String, List<VariableOption>> listDynamicOptions(
      @JsonProperty("config") QuestionOptionsRequest opts) {
    try {
      return DiskRepository.get().listDynamicOptions(opts);
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
    return DiskRepository.get().getTLOIsForHypothesisAndLOI(hid, lid);
  }

  @GET
  @Path("execute/{hid}/{lid}")
  @Override
  public List<TriggeredLOI> runHypothesisAndLOI(
      @PathParam("hid") String hid,
      @PathParam("lid") String lid) {
    try {
      return DiskRepository.get().runHypothesisAndLOI(hid, lid);
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
      return DiskRepository.get().runAllHypotheses();
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

  @POST
  @Path("getData")
  @Override
  public Response getOutputData(@JsonProperty("request") ExternalDataRequest r) {
    FileAndMeta result = DiskRepository.get().getOutputData(r.getSource(), r.getDataId());
    if (result == null) {
      ResponseBuilder rBuilder = Response.status(Response.Status.NOT_FOUND);
      return rBuilder.type(MediaType.TEXT_PLAIN)
          .entity("Could not find file")
          .build();
    } 

    ResponseBuilder rBuild = Response.ok(result.data, result.contentType);
    return rBuild.build();
  }

  @GET
  @Path("ontology.nq")
  @Override
  public Response getOntologyAll() {
    try {
      FileAndMeta all = DiskRepository.get().getOntologyAll();
      ResponseBuilder rBuild = Response.ok(all.data, all.contentType);
      return rBuild.build();
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

}