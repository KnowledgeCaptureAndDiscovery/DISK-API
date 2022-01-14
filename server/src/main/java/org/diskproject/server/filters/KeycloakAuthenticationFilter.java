package org.diskproject.server.filters;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

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
        requestContext.setProperty("username", user.username);
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
      private static Map<String, KeycloakUser> tokenCache = new HashMap<String, KeycloakUser>();
      private static Map<String, String> userTokens = new HashMap<String, String>();
      
      public static KeycloakUser getKeycloakUser (String token) {
          if (KeycloakSessions.tokenCache.containsKey(token)) {
              return KeycloakSessions.tokenCache.get(token);
          } else {
              return fetchKeycloakUser(token);
          }
      }
      
      private static KeycloakUser fetchKeycloakUser (String token) {
		CloseableHttpClient client = HttpClientBuilder.create().build();
		HttpGet userInfo = new HttpGet("https://auth.mint.isi.edu/auth/realms/production/protocol/openid-connect/userinfo");
		userInfo.addHeader("Authorization", token);
		
		String strResponse = null;
		try {
			HttpResponse httpResponse = client.execute(userInfo, HttpClientContext.create());
			HttpEntity responseEntity = httpResponse.getEntity();
			
			strResponse = EntityUtils.toString(responseEntity);
		} catch (Exception e) {
            System.err.println("Could not verify token on Keycloak");
            return null;
        }
		
		JsonParser jsonParser = new JsonParser();
		JsonObject jsonObj = jsonParser.parse(strResponse.trim()).getAsJsonObject();
		
		String username = jsonObj.get("preferred_username").getAsString();
		String email = jsonObj.get("email").getAsString();
		KeycloakUser user = new KeycloakUser(username, email);
		
		if (KeycloakSessions.userTokens.containsKey(username)) {
		    String usedToken = KeycloakSessions.userTokens.get(username);
		    if (KeycloakSessions.tokenCache.containsKey(usedToken)) {
		        System.out.println("Cleaning token for " + username);
		        KeycloakSessions.tokenCache.remove(usedToken);
		    }
		}
		KeycloakSessions.userTokens.put(username, token);
		KeycloakSessions.tokenCache.put(token, user);
		System.out.println("User fetched: " + username);
		
		return user;
      }
  }
}
