package org.diskproject.client.authentication;

import org.diskproject.shared.classes.users.UserSession;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.dispatcher.DispatcherFilter;

import com.google.gwt.http.client.RequestBuilder;

public final class SessionFilter implements DispatcherFilter {
  @Override
  public boolean filter(Method method, RequestBuilder builder) {
    if(SessionStorage.getSession() != null)
      builder.setHeader(UserSession.SESSION_HEADER, 
          SessionStorage.getSession().getSessionString());
    return true;
  }

}
