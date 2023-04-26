package edu.isi.kcap.diskproject.shared.api;

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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.Produces;

import com.fasterxml.jackson.annotation.JsonProperty;

import edu.isi.kcap.diskproject.shared.classes.common.TreeItem;
import edu.isi.kcap.diskproject.shared.classes.hypothesis.Hypothesis;
import edu.isi.kcap.diskproject.shared.classes.loi.LineOfInquiry;
import edu.isi.kcap.diskproject.shared.classes.loi.TriggeredLOI;
import edu.isi.kcap.diskproject.shared.classes.question.Question;
import edu.isi.kcap.diskproject.shared.classes.question.VariableOption;
import edu.isi.kcap.diskproject.shared.classes.util.DataAdapterResponse;
import edu.isi.kcap.diskproject.shared.classes.util.ExternalDataRequest;
import edu.isi.kcap.diskproject.shared.classes.util.ProvenanceRequest;
import edu.isi.kcap.diskproject.shared.classes.util.QuestionOptionsRequest;
import edu.isi.kcap.diskproject.shared.classes.vocabulary.Vocabulary;
import edu.isi.kcap.diskproject.shared.classes.workflow.Variable;
import edu.isi.kcap.diskproject.shared.classes.workflow.Workflow;
import edu.isi.kcap.diskproject.shared.classes.workflow.WorkflowRun;

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
        public List<Hypothesis> listHypotheses();

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
        public List<VariableOption> listOptions(
                        @PathParam("id") String id);

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

        /* Get narratives for tloi id */
        @GET
        @Path("tloi/{tloiid}/narratives")
        public Map<String, String> getNarratives(
                        @PathParam("tloiid") String tloiid);

        @GET
        @Path("tloi/{tloiid}/provenance/{format}")
        @Produces(MediaType.TEXT_PLAIN)
        public String getProvenance(
                        @PathParam("tloiid") String tloiid,
                        @PathParam("format") String format);

        @GET
        @Path("externalQuery")
        public Map<String, List<String>> queryExternalStore(
                        @QueryParam("endpoint") String endpoint,
                        @QueryParam("variables") String variables,
                        @QueryParam("query") String query);

        @POST
        @Path("getData")
        @Produces("text/html")
        public String getOutputData(
                        @JsonProperty("request") ExternalDataRequest r);
}