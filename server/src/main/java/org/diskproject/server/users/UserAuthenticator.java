package org.diskproject.server.users;

import java.security.Principal;

import javax.ws.rs.core.SecurityContext;

import org.diskproject.shared.classes.users.UserSession;

public class UserAuthenticator implements SecurityContext {  
  UserSession session;
  User user;
  
  public UserAuthenticator(UserSession session) {
    this.session = session;
    this.user = UserDatabase.get().getUser(session);
  }

  @Override
  public String getAuthenticationScheme() {
    return BASIC_AUTH;
  }

  @Override
  public Principal getUserPrincipal() {
    return this.user;
  }

  @Override
  public boolean isSecure() {
    return true;
  }

  @Override
  public boolean isUserInRole(String role) {
    if(this.user != null && this.user.getRoles() != null) {
      for(String urole : this.user.getRoles()) {
        if(urole.equals(role))
          return true;
      }
    }
    return false;
  }
}
