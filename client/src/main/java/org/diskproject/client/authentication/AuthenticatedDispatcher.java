package org.diskproject.client.authentication;

import org.fusesource.restygwt.client.dispatcher.DefaultFilterawareDispatcher;

public class AuthenticatedDispatcher extends DefaultFilterawareDispatcher {
  
  public AuthenticatedDispatcher() {
    this.addFilter(new SessionFilter());
  }
}
