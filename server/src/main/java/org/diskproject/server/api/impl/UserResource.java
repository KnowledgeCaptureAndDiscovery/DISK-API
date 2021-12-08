package org.diskproject.server.api.impl;

import java.util.List;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

import org.diskproject.server.users.User;
import org.diskproject.server.users.UserDatabase;
import org.diskproject.shared.api.UserService;
import org.diskproject.shared.classes.users.UserCredentials;
import org.diskproject.shared.classes.users.UserSession;

import com.fasterxml.jackson.annotation.JsonProperty;

@Path("")
@DeclareRoles({"user", "admin", "importer"})
public class UserResource implements UserService {
  @Context
  HttpServletResponse response;
  
  @Context
  HttpServletRequest request;
  
  @Context
  SecurityContext securityContext;
  
  /**
   * Authentication
   */
  @POST
  @Path("login")
  @Produces("application/json")
  @Consumes("application/json")
  @Override
  public UserSession login(
      @JsonProperty("credentials") UserCredentials credentials) {
    return UserDatabase.get().login(credentials);
  }
  
  @POST
  @Path("validate")
  @Produces("application/json")
  @Consumes("application/json")
  @Override
  public UserSession validateSession(
      @JsonProperty("session") UserSession session) {
    return UserDatabase.get().validateSession(session);
  }
  
  @POST
  @Path("logout")
  @Consumes("application/json")
  @Override
  public void logout(UserSession session) {
    UserDatabase.get().logout(session);
  }
  
  /**
   * Query users
   */
  
  @GET
  @Produces("application/json")
  @RolesAllowed("admin")
  public List<String> getUsers() {
    return UserDatabase.get().getUsers();
  }
  
  @GET
  @Path("users/{username}")
  @Produces("application/json")
  @RolesAllowed("user")
  @Override  
  public UserCredentials getUser(@PathParam("username") String username) {
    User loggedinuser = (User)securityContext.getUserPrincipal();
    if(loggedinuser != null) {
      if(loggedinuser.getName().equals(username) || loggedinuser.getRoles().contains("admin"))
        return UserDatabase.get().getUser(username);
    }
    return null;
  }
  
  @GET
  @Path("users/{username}/check")
  @Produces("application/json")
  @Override  
  public String userExists(@PathParam("username") String username) {
    return new Boolean(UserDatabase.get().userExists(username)).toString();
  }
  
  /**
   * Edit users
   */
  @DELETE
  @Path("users/{username}")
  @RolesAllowed("admin")
  @Override
  public void deleteUser(@PathParam("username") String username) {
    if(username != null) {
      // Cannot delete yourself
      User loggedinuser = (User) securityContext.getUserPrincipal();
      if(loggedinuser.getName().equals(username))
        return;
      
      UserDatabase.get().deleteUser(username);
    }
  }
  
  @POST
  @Path("users")
  @Consumes("application/json")
  @Produces("application/json")
  @Override  
  public UserCredentials addUser(@JsonProperty("user") UserCredentials user) {
    if(UserDatabase.get().userExists(user.getName()))
      return null;
    
    User loggedinuser = (User) securityContext.getUserPrincipal();
    user = this.sanitizeUserCredentials(user, loggedinuser);
    return UserDatabase.get().addUser(user);
  }
  
  @PUT
  @Path("users/{username}")
  @Consumes("application/json")
  @Produces("application/json")
  @RolesAllowed("user")
  @Override  
  public UserCredentials updateUser(@PathParam("username") String username,
      @JsonProperty("user") UserCredentials user) {
    User loggedinuser = (User) securityContext.getUserPrincipal();
    if (loggedinuser != null) {
      // Allow updating for same user or admins
      if (loggedinuser.getName().equals(username)
          || loggedinuser.getRoles().contains("admin")) {
        user = this.sanitizeUserCredentials(user, loggedinuser);
        return UserDatabase.get().updateUser(username, user);
      }
    }
    return null;
  }
  
  private UserCredentials sanitizeUserCredentials(UserCredentials user, User loggedinuser) {
    // If the user wants to set admin role, and the logged in user isn't admin
    // - then do not set the admin role
    if(user.getRoles().contains("admin") && 
        (loggedinuser == null || !loggedinuser.getRoles().contains("admin")))
      user.getRoles().remove("admin");
    return user;
  }
}
