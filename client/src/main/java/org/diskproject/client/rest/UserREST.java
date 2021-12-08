package org.diskproject.client.rest;


import java.util.List;

import org.diskproject.client.Config;
import org.diskproject.client.authentication.AuthenticatedDispatcher;
import org.diskproject.client.authentication.SessionStorage;
import org.diskproject.shared.api.UserService;
import org.diskproject.shared.classes.users.UserCredentials;
import org.diskproject.shared.classes.users.UserSession;
import org.fusesource.restygwt.client.Defaults;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;
import org.fusesource.restygwt.client.REST;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.GWT;

public class UserREST {
  public static UserService userService;

  public static UserService getUserService() {
    if(userService == null) {
      Defaults.setServiceRoot(Config.getServerURL());
      Defaults.setDateFormat(null);
      Defaults.setDispatcher(new AuthenticatedDispatcher());
      userService = GWT.create(UserService.class);
    }
    return userService;
  }
  
  public static void login(UserCredentials credentials, 
      final Callback<UserSession, Throwable> callback) {
    if(SessionStorage.getSession() != null) {
      callback.onSuccess(SessionStorage.getSession());
    }
    else {
      REST.withCallback(new MethodCallback<UserSession>() {
        @Override
        public void onFailure(Method method, Throwable exception) {
          callback.onFailure(exception);
        }
        @Override
        public void onSuccess(Method method, UserSession session) {
          if(session != null) {
            SessionStorage.setSession(session);
            callback.onSuccess(session);
          }
          else
            callback.onFailure(new Throwable("Login incorrect"));
        }
      }).call(getUserService()).login(credentials);
    }
  }
  
  public static void validateSession(UserSession session, 
      final Callback<UserSession, Throwable> callback) {
    REST.withCallback(new MethodCallback<UserSession>() {
      @Override
      public void onFailure(Method method, Throwable exception) {
        callback.onFailure(exception);
      }
      @Override
      public void onSuccess(Method method, UserSession session) {
        SessionStorage.setSession(session);
        callback.onSuccess(session);
      }
    }).call(getUserService()).validateSession(session);
  }
  
  public static void logout(final Callback<Void, Throwable> callback) {
    if(SessionStorage.getSession() == null) {
      callback.onFailure(new Throwable("Not logged in"));
    }
    else {
      REST.withCallback(new MethodCallback<Void>() {
        @Override
        public void onFailure(Method method, Throwable exception) {
          callback.onFailure(exception);
        }
        @Override
        public void onSuccess(Method method, Void response) {
          SessionStorage.setSession(null);
          callback.onSuccess(response);
        }
      }).call(getUserService()).logout(SessionStorage.getSession());
    }
  }
  
  public static void addUser(UserCredentials user,
      final Callback<UserCredentials, Throwable> callback) {
    REST.withCallback(new MethodCallback<UserCredentials>() {
      @Override
      public void onFailure(Method method, Throwable exception) {
        callback.onFailure(exception);
      }
      @Override
      public void onSuccess(Method method, UserCredentials response) {
        if(response == null)
          callback.onFailure(new Throwable("Could not add user"));
        else
          callback.onSuccess(response);
      }
    }).call(getUserService()).addUser(user);
  }

  public static void deleteUser(String username,
      final Callback<Void, Throwable> callback) {
    REST.withCallback(new MethodCallback<Void>() {
      @Override
      public void onFailure(Method method, Throwable exception) {
        callback.onFailure(exception);
      }
      @Override
      public void onSuccess(Method method, Void response) {
        callback.onSuccess(response);
      }
    }).call(getUserService()).deleteUser(username);
  }
  
  public static void updateUser(String username, UserCredentials user,
      final Callback<UserCredentials, Throwable> callback) {
    REST.withCallback(new MethodCallback<UserCredentials>() {
      @Override
      public void onFailure(Method method, Throwable exception) {
        callback.onFailure(exception);
      }
      @Override
      public void onSuccess(Method method, UserCredentials response) {
        callback.onSuccess(response);
      }
    }).call(getUserService()).updateUser(username, user);
  }
  
  public static void getUsers(final Callback<List<String>, Throwable> callback) {
    REST.withCallback(new MethodCallback<List<String>>() {
      @Override
      public void onFailure(Method method, Throwable exception) {
        callback.onFailure(exception);
      }
      @Override
      public void onSuccess(Method method, List<String> response) {
        callback.onSuccess(response);
      }
    }).call(getUserService()).getUsers();
  }
  
  public static void getUser(String username,
      final Callback<UserCredentials, Throwable> callback) {
    REST.withCallback(new MethodCallback<UserCredentials>() {
      @Override
      public void onFailure(Method method, Throwable exception) {
        callback.onFailure(exception);
      }
      @Override
      public void onSuccess(Method method, UserCredentials response) {
        callback.onSuccess(response);
      }
    }).call(getUserService()).getUser(username);
  }
  
  public static void userExists(String username,
      final Callback<Boolean, Throwable> callback) {
    REST.withCallback(new MethodCallback<String>() {
      @Override
      public void onFailure(Method method, Throwable exception) {
        callback.onFailure(exception);
      }
      @Override
      public void onSuccess(Method method, String response) {
        callback.onSuccess((response != null &&  Boolean.parseBoolean(response)));
      }
    }).call(getUserService()).userExists(username);
  }
}
