package org.diskproject.client.authentication;

import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.dispatcher.DispatcherFilter;
import com.google.gwt.http.client.RequestBuilder;

public final class SessionFilter implements DispatcherFilter {
  @Override
  public boolean filter(Method method, RequestBuilder builder) {
    if (KeycloakUser.getToken() != null) {
        builder.setHeader("Authorization", "Bearer " + KeycloakUser.getToken());
    }
    return true;
  }

}
