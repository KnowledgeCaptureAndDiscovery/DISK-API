package org.diskproject.shared.classes.users;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UserSession {
  public static final String SESSION_HEADER = "X-Disk-Session";

  public String username;
  public String sessionid;
  public List<String> roles;

  @JsonCreator
  public UserSession(@JsonProperty("username") String username,
      @JsonProperty("sessionid") String sessionid,
      @JsonProperty("roles") List<String> roles) {
    this.username = username;
    this.sessionid = sessionid;
    this.roles = roles;
  }

  public static UserSession getSession(String sessionString) {
    if(sessionString != null) {
      String[] id_session = sessionString.split("\\|");
      if(id_session.length == 2)
        return new UserSession(id_session[0], id_session[1], new ArrayList<String>());
    }
    return null;
  }
  
  public String getSessionString() {
    return this.getUsername() + "|" + this.getSessionid();
  }
  
  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getSessionid() {
    return sessionid;
  }

  public void setSessionid(String sessionid) {
    this.sessionid = sessionid;
  }

  public List<String> getRoles() {
    return roles;
  }

  public void setRoles(List<String> roles) {
    this.roles = roles;
  }
}
