package org.diskproject.server.filters;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.commons.configuration.plist.PropertyListConfiguration;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.diskproject.server.util.Config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@PreMatching
public class KeycloakAuthenticationFilter implements ContainerRequestFilter {
  @Context 
  HttpServletRequest request;
  
  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    String token = requestContext.getHeaderString("authorization");
    if (token != null) {
      KeycloakUser user = KeycloakSessions.getKeycloakUser(token);
      if (user != null && user.username != null) {
          requestContext.setProperty("username", user.username);
      }
      else
          requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity("Access denied").build());
    }
  }
  
  public static class KeycloakUser {
    public String username, email;
    public KeycloakUser(String user, String email) {
      this.username = user;
      this.email = email;
    }
  }
  
  public static class KeycloakSessions {
    // Token to User map
    private static Map<String, KeycloakUser> tokenCache = new HashMap<String, KeycloakUser>();
    // Username to Token map
    private static Map<String, String> userTokens = new HashMap<String, String>();
    // Token loading
    private static Map<String, Boolean> loadingToken = new HashMap<String, Boolean>();

    private static CloseableHttpClient httpClient;
    private static PoolingHttpClientConnectionManager connectionManager;
    private static String userInfoUrl;
    
    private static CloseableHttpClient getHttpClient () {
      if (KeycloakSessions.httpClient == null) {
        connectionManager = new PoolingHttpClientConnectionManager();
        KeycloakSessions.httpClient = HttpClients.custom()
            .setConnectionManager(connectionManager)
            .build();
        
        PropertyListConfiguration props = Config.get().getProperties();
        String server = props.getString("keycloak.url");
        String realm = props.getString("keycloak.realm");
        
        //Create API urls
	      KeycloakSessions.userInfoUrl = server + "/auth/realms/" + realm + "/protocol/openid-connect/userinfo";
      } 
      return KeycloakSessions.httpClient;
    }
    
    public static KeycloakUser getKeycloakUser (String token) {
      if (KeycloakSessions.tokenCache.containsKey(token)) {
        return KeycloakSessions.tokenCache.get(token);
      } else if (KeycloakSessions.loadingToken.containsKey(token)) {
        try {
          //This is a hack to not make the query multiple times if we get multiple request at the same time.
          System.err.println("WAITING TOKEN");
          Thread.sleep(500);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        return KeycloakSessions.getKeycloakUser(token);
      } else {
        return KeycloakSessions.fetchKeycloakUser(token);
      }
    }
    
    private static void updateUserToken (KeycloakUser user, String token) {
      String username = user.username;
	    if (KeycloakSessions.userTokens.containsKey(username)) {
	      String usedToken = KeycloakSessions.userTokens.get(username);
	      if (KeycloakSessions.tokenCache.containsKey(usedToken))
	        KeycloakSessions.tokenCache.remove(usedToken);
	    }
	    KeycloakSessions.userTokens.put(username, token);
	    KeycloakSessions.tokenCache.put(token, user);
    }
    
    private static KeycloakUser fetchKeycloakUser (String token) {
      KeycloakSessions.loadingToken.put(token, true);
      CloseableHttpClient client = KeycloakSessions.getHttpClient();
      if (KeycloakSessions.userInfoUrl == null)
        return null;
      HttpGet userInfo = new HttpGet(KeycloakSessions.userInfoUrl);
      userInfo.addHeader("Authorization", token);

	    try (CloseableHttpResponse httpResponse = client.execute(userInfo)) {
        HttpEntity responseEntity = httpResponse.getEntity();
        String strResponse = EntityUtils.toString(responseEntity);

        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObj = jsonParser.parse(strResponse.trim()).getAsJsonObject();
        KeycloakUser user = null;
        try {
          String username = jsonObj.get("preferred_username").getAsString();
          String email = jsonObj.get("email").getAsString();
          user = new KeycloakUser(username, email);
        } catch (Exception e) {
          System.out.println("Malformed JSON string: " + strResponse);
        }
        if (user != null) {
          updateUserToken(user, token);
          KeycloakSessions.loadingToken.remove(token);
          return user;
        }
      } catch (Exception e) {
        System.err.println("Could not verify Keycloak token");
        e.printStackTrace();
      }
      return null;
    }
  }
}
