package org.diskproject.client.authentication;

import org.realityforge.gwt.keycloak.Keycloak;

import com.google.gwt.core.client.JavaScriptObject;

public class KeycloakUser {
  public static Keycloak kc;

  private static String username = null;
  private static String email = null;
  private static String token = null;
  private static String refreshToken = null;
  
  public static void init (Keycloak keycloak) {
      // Get user information from keycloak instance.
	  KeycloakUser.kc = keycloak;
	  
	  JavaScriptObject userObj = keycloak.getParsedToken();

	  KeycloakUser.username = KeycloakUser.getUserFromParsedToken(userObj);
	  KeycloakUser.email = KeycloakUser.getEmailFromParsedToken(userObj);
	  KeycloakUser.token = keycloak.getToken();
	  KeycloakUser.refreshToken = keycloak.getRefreshToken();
  }
  
  private static native String getUserFromParsedToken (JavaScriptObject userObj) /*-{
  	return userObj["preferred_username"];
  }-*/;

  private static native String getEmailFromParsedToken (JavaScriptObject userObj) /*-{
  	return userObj["email"];
  }-*/;

  public static String getUsername () {
	  return KeycloakUser.username;
  }
  
  public static String getEmail () {
	  return KeycloakUser.email;
  }
  
  public static String getToken () {
      return KeycloakUser.token;
  }
}