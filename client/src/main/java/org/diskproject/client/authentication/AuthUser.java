package org.diskproject.client.authentication;

import org.realityforge.gwt.keycloak.Keycloak;

import com.google.gwt.core.client.JavaScriptObject;

public class AuthUser {
  public static Keycloak kc;
  private static String username = null;
  private static String email = null;
  
  public static void init (Keycloak keycloak) {
	  AuthUser.kc = keycloak;
	  JavaScriptObject userObj = AuthUser.kc.getParsedToken();
	  AuthUser.username = AuthUser.getUserJs(userObj);
	  AuthUser.email = AuthUser.getEmailJs(userObj);
  }
  
  private static native String getUserJs (JavaScriptObject userObj) /*-{
  	return userObj["preferred_username"];
  }-*/;

  private static native String getEmailJs (JavaScriptObject userObj) /*-{
  	return userObj["email"];
  }-*/;
  
  public static String getUsername () {
	  return AuthUser.username;
  }
  
  public static String getEmail () {
	  return AuthUser.email;
  }
}
