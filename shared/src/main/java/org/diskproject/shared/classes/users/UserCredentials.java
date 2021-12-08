package org.diskproject.shared.classes.users;

import java.util.ArrayList;
import java.util.List;

public class UserCredentials {
  int id;
  String name;
  String fullname;
  String password;
  String email;
  String affiliation;
  
  List<String> roles;

  public UserCredentials(
  int id,
  String name,
  String fullname,
  String password,
  String email,
  String affiliation,  
  List<String> roles) {
	  this.id = id;
	  this.name = name;
	  this.fullname = fullname;
	  this.password = password;
	  this.email = email;
	  this.affiliation = affiliation;
  }  
  public UserCredentials() {
    roles = new ArrayList<String>();
  }
  
  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public List<String> getRoles() {
    return roles;
  }

  public void setRoles(List<String> roles) {
    this.roles = roles;
  }

  public String getFullname() {
    return fullname;
  }

  public void setFullname(String fullname) {
    this.fullname = fullname;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getAffiliation() {
    return affiliation;
  }

  public void setAffiliation(String affiliation) {
    this.affiliation = affiliation;
  }
}
