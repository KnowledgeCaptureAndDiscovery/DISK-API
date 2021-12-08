package org.diskproject.shared.api;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.diskproject.shared.classes.users.UserCredentials;
import org.diskproject.shared.classes.users.UserSession;
import org.fusesource.restygwt.client.DirectRestService;

import com.fasterxml.jackson.annotation.JsonProperty;

@Path("")
public interface UserService extends DirectRestService {
  /**
   * Authentication
   */
  @POST
  @Path("login")
  @Produces("application/json")
  @Consumes("application/json")
  public UserSession login(
      @JsonProperty("credentials") UserCredentials credentials);

  @POST
  @Path("validate")
  @Produces("application/json")
  @Consumes("application/json")
  public UserSession validateSession(UserSession session);
  
  @POST
  @Path("logout")
  @Consumes("application/json")
  public void logout(UserSession session);

  /**
   * Query users
   */
  @GET
  @Produces("application/json")
  public List<String> getUsers();
  
  @GET
  @Path("users/{username}")
  @Produces("application/json")
  public UserCredentials getUser(@PathParam("username") String username);
  
  @GET
  @Path("users/{username}/check")
  public String userExists(@PathParam("username") String username);
  
  /**
   * Edit users
   */
  @DELETE
  @Path("users/{username}")
  public void deleteUser(@PathParam("username") String username);
  
  @POST
  @Path("users")
  @Produces("application/json")
  public UserCredentials addUser(@JsonProperty("user") UserCredentials user);
  
  @PUT
  @Path("users/{username}")
  @Consumes("application/json")
  @Produces("application/json")
  public UserCredentials updateUser(@PathParam("username") String username, 
      @JsonProperty("user") UserCredentials user);
}
