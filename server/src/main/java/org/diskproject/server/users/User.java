package org.diskproject.server.users;

import java.security.Principal;
import java.util.ArrayList;

import org.diskproject.shared.classes.users.UserCredentials;

public class User extends UserCredentials implements Principal {

  public User(int id, String name, String password, ArrayList<String> roles) {
    this.setId(id);
    this.setName(name);
    this.setPassword(password);
    this.setRoles(roles);
  }

}
