package org.diskproject.client.authentication;

import org.diskproject.shared.classes.users.UserSession;

import com.google.gwt.core.client.Callback;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.History;

public class SessionStorage {
  private static UserSession session;
  
  public static void setSession(UserSession session) {
    SessionStorage.session = session;
    setSessionCookie();
    History.replaceItem(History.getToken());
  }
  
  public static UserSession getSession() {
    return SessionStorage.session;
  }
  
  public static void loadSession() {
    String session_string = Cookies.getCookie(UserSession.SESSION_HEADER);
    if(session_string != null) {
      UserSession cachedSession = UserSession.getSession(session_string);
      // Check session validity
      //UserREST.validateSession(cachedSession, new Callback<UserSession, Throwable>() {
      //  @Override
      //  public void onSuccess(UserSession session) {}
      //  @Override
      //  public void onFailure(Throwable reason) {}
      //});
    }
  }
  
  private static void setSessionCookie() {
    if(session != null)
      Cookies.setCookie(UserSession.SESSION_HEADER, session.getSessionString());
    else
      Cookies.removeCookie(UserSession.SESSION_HEADER);
  }
}
