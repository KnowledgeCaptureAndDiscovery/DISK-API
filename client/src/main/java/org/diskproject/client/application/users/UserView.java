package org.diskproject.client.application.users;

import org.diskproject.client.application.ApplicationSubviewImpl;
import org.diskproject.client.authentication.SessionStorage;
import org.diskproject.client.rest.UserREST;
import org.diskproject.shared.classes.users.UserCredentials;
import org.diskproject.shared.classes.users.UserSession;

import com.google.gwt.core.client.Callback;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.vaadin.polymer.paper.PaperInputElement;

public class UserView extends ApplicationSubviewImpl 
  implements UserPresenter.MyView {

  String username, loggedinuser;
  boolean isadmin, isupdate;
  
  boolean uservalid;
  //Validator<String> passvalid, emailvalid;
  
  @UiField
  PaperInputElement uname, password1, password2, fullname, email, affiliation;
  
  /*
  @UiField
  CheckBox adminrole, importrole;
  
  @UiField
  Form form;
  
  @UiField
  Button submitbutton, deletebutton;
  
  @UiField
  Heading heading;
  
  @UiField
  FormGroup namegroup, rolegroup;
  
  @UiField
  ListBox userlist;*/
  
  interface Binder extends UiBinder<Widget, UserView> {
  }

  @Inject
  public UserView(Binder binder) {
    initWidget(binder.createAndBindUi(this));
    //setValidators();
  }

  @Override
  public void initializeParameters(String userid, String domain, String[] params, boolean edit, 
      SimplePanel sidebar, SimplePanel toolbar) {
    clear();
    UserSession session = SessionStorage.getSession();
    if(session != null) {
      // User logged in
      this.loggedinuser = session.getUsername();
      this.username = this.loggedinuser;
      if(session.getRoles().contains("admin"))
        isadmin = true;
    }
    // Parse tokens in case admin wants to change an existing user
    if(params.length > 0 && this.isadmin) {
      this.username = params[0].replaceAll("[^a-zA-Z0-9_]", "_");
    }
    
    /*if(this.isadmin) {
      userlist.setVisible(true);
      setUserList();
    }*/
    
    String heading = "Register new user";
    if(this.username != null) {
      setUserData();
      heading = "Edit user details";
    }
    toolbar.clear();
    toolbar.add(new HTML("<h3>" + heading + "</h3>"));
  }
  
  private void clear() {
    isadmin = false;
    isupdate = false;
    username = null;
    uservalid = true;
    
    uname.setValue(null);
    password1.setValue(null);
    password2.setValue(null);
    fullname.setValue(null);
    email.setValue(null);
    affiliation.setValue(null);
    
    /*userlist.clear();
    heading.setText("");
    namegroup.setVisible(true);
    rolegroup.setVisible(false);
    userlist.setVisible(false);
    adminrole.setValue(false);
    importrole.setValue(false);
    deletebutton.setVisible(false);*/
  }
  
  /*
  private void setUserList() {
    UserREST.getUsers(new Callback<List<String>, Throwable>() {
      @Override
      public void onFailure(Throwable reason) {
        AppNotification.notifyFailure(reason.getMessage());
      }
      @Override
      public void onSuccess(List<String> list) {
        int i=0;
        for(String name : list) {
          userlist.addItem(name);
          if(name.equals(username))
            userlist.setItemSelected(i, true);
          i++;
        }
      }
    });
  }*/
  
  private void setUserData() {
    uname.setValue(this.username);

    /*if (!isadmin)
      namegroup.setVisible(false);
    else
      rolegroup.setVisible(true);*/

    UserREST.getUser(this.username, new Callback<UserCredentials, Throwable>() {
      @Override
      public void onFailure(Throwable reason) {}
      @Override
      public void onSuccess(UserCredentials user) {
        if (user == null) {
          //heading.setText("Register new user");
        } else {
          isupdate = true;
          /*if (isadmin && !loggedinuser.equals(username))
            deletebutton.setVisible(true);*/
          password1.setValue(user.getPassword());
          password2.setValue(user.getPassword());
          fullname.setValue(user.getFullname());
          email.setValue(user.getEmail());
          affiliation.setValue(user.getAffiliation());
          /*adminrole.setValue(user.getRoles().contains("admin"));
          importrole.setValue(user.getRoles().contains("importer"));*/
        }
      }
    });
  }
  
  /*
  private void setValidators() {
    final Validator<String> userexists_error = new Validator<String>() {
      @Override
      public int getPriority() {
        return Priority.LOW;
      }
      @Override
      public List<EditorError> validate(Editor<String> editor, String value) {
        List<EditorError> result = new ArrayList<EditorError>();
        if(!uservalid)
          result.add(new BasicEditorError(editor, value, "Username already exists"));
        return result;
      }
    };
    
    Validator<String> passvalidator = new Validator<String>() {
      @Override
      public int getPriority() {return Priority.LOW;}
      @Override
      public List<EditorError> validate(Editor<String> editor, String value) {
        List<EditorError> result = new ArrayList<EditorError>();
        String val1 = password1.getValue();
        String val2 = password2.getValue();
        if( (val1 != null && !val1.equals(val2)) ||
            (val2 != null && !val2.equals(val1)) ) {
          result.add(new BasicEditorError(editor, value, "Passwords don't match"));
        }
        else if(val1 != null && val1.length() < 5) {
          result.add(new BasicEditorError(editor, value, "Password too short (atleast 5 characters)"));
        }
        return result;
      }
    };
    
    Validator<String> emailvalidator = new Validator<String>() {
      @Override
      public int getPriority() {return Priority.LOW;}
      @Override
      public List<EditorError> validate(Editor<String> editor, String value) {
        List<EditorError> result = new ArrayList<EditorError>();
        String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.(?:[a-zA-Z]{2,6})$";
        if(value != null && !value.matches(emailPattern)) {
          result.add(new BasicEditorError(editor, value, "Not a valid email"));
        }
        return result;
      }
    };
    
    uname.addValidator(userexists_error);
    password1.addValidator(passvalidator);
    password2.addValidator(passvalidator);
    email.addValidator(emailvalidator);
  }
  
  @UiHandler("uname")
  public void onUsernameChange(ChangeEvent event) {
    // Check if such a user exists in backend
    UserREST.userExists(uname.getValue(), new Callback<Boolean, Throwable>() {
      @Override
      public void onSuccess(Boolean exists) {
        uservalid = !exists;
        //uname.validate(true);
      }
      @Override
      public void onFailure(Throwable reason) {
        GWT.log("Error", reason);
      }
    });
  }
  
  @UiHandler("userlist")
  public void onUserSelect(ChangeEvent event) {
    History.replaceItem(NameTokens.users + "/" + userlist.getSelectedValue());
  }
  
  @UiHandler("submitbutton")
  public void onSubmit(ClickEvent event) {
    if(form.validate(true) && uservalid) {
      UserCredentials user = new UserCredentials();
      user.setAffiliation(affiliation.getValue());
      user.setFullname(fullname.getValue());
      user.setPassword(password1.getValue());
      user.setEmail(email.getValue());
      user.setName(uname.getValue());
      
      List<String> roles = new ArrayList<String>();
      roles.add("user");
      if (adminrole.getValue())
        roles.add("admin");
      if (importrole.getValue())
        roles.add("importer");
      user.setRoles(roles);
      
      if(this.isupdate) {
        UserREST.updateUser(this.username, user, new Callback<UserCredentials, Throwable>() {
          @Override
          public void onFailure(Throwable reason) {
            AppNotification.notifyFailure(reason.getMessage());
          }
          @Override
          public void onSuccess(UserCredentials result) {
            AppNotification.notifySuccess("User updated", 1000);
            History.replaceItem(History.getToken());
          }
        });
      }
      else {
        UserREST.addUser(user, new Callback<UserCredentials, Throwable>() {
          @Override
          public void onFailure(Throwable reason) {
            AppNotification.notifyFailure(reason.getMessage());
          }
          @Override
          public void onSuccess(UserCredentials result) {
            AppNotification.notifySuccess("User added", 1000);
            History.replaceItem(History.getToken());
          }
        });
      }
    }
  }
  
  @UiHandler("deletebutton")
  public void onDelete(ClickEvent event) {
    final String name = this.username;
    if (Window.confirm("Are you sure you want to delete user " + name + "?")) {
      UserREST.deleteUser(name, new Callback<Void, Throwable>() {
        @Override
        public void onFailure(Throwable reason) {
          AppNotification.notifyFailure("Could not delete: "+reason.getMessage());
        }
        @Override
        public void onSuccess(Void result) {
          AppNotification.notifySuccess(name + " deleted", 1000);
          History.replaceItem(History.getToken());
        }
      });
    }
  }*/
}
