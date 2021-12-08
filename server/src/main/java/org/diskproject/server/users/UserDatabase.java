package org.diskproject.server.users;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.diskproject.server.util.Config;
import org.diskproject.shared.classes.users.UserCredentials;
import org.diskproject.shared.classes.users.UserSession;
import org.mindrot.jbcrypt.BCrypt;

public class UserDatabase {
  private static int expire_in_days = 1; // Session expiry
  private String dbdir;
  private Connection dbc;
  
  private static UserDatabase singleton = null;
  public static UserDatabase get() {
    if(singleton == null)
      singleton = new UserDatabase();
    return singleton;
  }
  
  public UserDatabase() {
    initializeDB();
  }
 
  /**
   * DB Initialization
   */
  private void initializeDB() {
    if(Config.get() != null) {
      this.dbdir = Config.get().getProperties().getString("storage.db");
      this.dbc = connectDB();
      if(!this.isDatabasePopulated()) {
        this.createSchema();
        this.createInitialData();
      }
      this.removeExpiredSessions();
    }
  }
  
  public Connection connectDB() {
    try {
      String driver = "org.apache.derby.jdbc.EmbeddedDriver";
      Class.forName(driver);
      String url = "jdbc:derby:"+this.dbdir+";create=true";
      return DriverManager.getConnection(url);
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  
  public static void shutdownDB() {
    if(singleton != null && singleton.dbc != null) {
      try {
        singleton.dbc.close();
        String url = "jdbc:derby:"+singleton.dbdir+";shutdown=true";
        singleton.dbc = DriverManager.getConnection(url);
        singleton.dbc.close();
      } catch (SQLException e) {
        //e.printStackTrace();
      }
    }
  }
  
  private boolean isDatabasePopulated() {
    try {
      DatabaseMetaData meta = this.dbc.getMetaData();
      
      // Check if a users table exists in DB
      ResultSet res = meta.getTables(null,  null,  "USERS", null);
      if(res.next()) {
        res.close();
        return true;
      }
      res.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return false;
  }
  
  private void createSchema() {
    try {
      String sql = "CREATE TABLE users "
          + "("
          + "id INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) , "
          + "username VARCHAR(36) UNIQUE NOT NULL, "
          + "password VARCHAR(255) NOT NULL, "
          + "fullname VARCHAR(255) NOT NULL, "
          + "email VARCHAR(255), "
          + "affiliation VARCHAR(255), "
          + "PRIMARY KEY (id)"
          + ")";
      dbc.createStatement().executeUpdate(sql);
      System.out.println("Created Users Table");

      sql = "CREATE TABLE roles "
          + "("
          + "id INTEGER NOT NULL, "
          + "role VARCHAR(255) NOT NULL, "
          + "PRIMARY KEY (id)"
          + ")";
      dbc.createStatement().executeUpdate(sql);
      System.out.println("Created Roles Table");

      sql = "CREATE TABLE user_roles "
          + "("
          + "userid INTEGER NOT NULL, "
          + "roleid INTEGER NOT NULL "
          + ")";
      dbc.createStatement().executeUpdate(sql); 
      System.out.println("Created User Roles Table");

      sql = "CREATE TABLE sessions "
          + "("
          + "sessionid CHAR(36) NOT NULL, "            
          + "userid INTEGER NOT NULL, "
          + "timestamp TIMESTAMP NOT NULL"
          + ")";
      dbc.createStatement().executeUpdate(sql);
      System.out.println("Created Sessions Table");   
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  private void createInitialData() {
    try {
      // Create roles
      String isql = "INSERT INTO roles VALUES "
          + "(1, 'admin'), "
          + "(2, 'user'), "
          + "(3, 'importer')";
      dbc.createStatement().executeUpdate(isql);
      
      // Create admin user
      UserCredentials adminUser = new UserCredentials();
      adminUser.setName("admin");
      adminUser.setEmail("varunr@isi.edu");
      adminUser.setFullname("Administrator");
      adminUser.setPassword("changeme!");
      adminUser.setAffiliation("USC/ISI");
      adminUser.setRoles(Arrays.asList("admin", "user"));
      adminUser = this.addUser(adminUser);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  private void removeExpiredSessions() {
    try {
      /* Remove any old sessions */
      String sql = "DELETE FROM sessions WHERE "
          + "{fn TIMESTAMPDIFF( SQL_TSI_DAY, TIMESTAMP, CURRENT_TIMESTAMP)} > "
          + expire_in_days;
      dbc.createStatement().executeUpdate(sql);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  public UserSession login(UserCredentials credentials) {
    if(credentials == null)
      return null;
    
    try {
      String sql = "SELECT id, password FROM users WHERE username = ?";
      PreparedStatement ps = dbc.prepareStatement(sql);
      ps.setString(1, credentials.getName());
      ResultSet rs = ps.executeQuery();
      int userid = 0;
      if(rs.next()) {
        String password = rs.getString(2);
        // Check stored hashed password against plaintext password sent over 
        if(BCrypt.checkpw(credentials.getPassword(), password))
          userid = rs.getInt(1);
      }
      rs.close();
      ps.close();
      
      if(userid > 0) {
        UUID uuid = UUID.randomUUID();
        String isql = "INSERT INTO sessions VALUES (?, ?, CURRENT_TIMESTAMP)";
        PreparedStatement ips = dbc.prepareStatement(isql);
        ips.setString(1, uuid.toString());
        ips.setInt(2, userid);
        ips.executeUpdate();
        ips.close();
        return validateSession(
            new UserSession(credentials.getName(), uuid.toString(), new ArrayList<String>()));
      }
      
      return null;
    }
    catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
  
  public UserSession validateSession(UserSession session) {
    User user = UserDatabase.get().getUser(session);
    if(user != null) {
      session.setRoles(user.getRoles());
      return session;
    }
    return null;
  }
  
  public boolean logout(UserSession session) {
    if(session == null)
      return true;
    
    try {
      String isql = "DELETE FROM sessions WHERE sessionid = ?";
      PreparedStatement ips = dbc.prepareStatement(isql);
      ips.setString(1, session.getSessionid());
      ips.executeUpdate();
      ips.close();
      return true;
    }
    catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
  
  public User getUser(UserSession session) {
    if(session == null)
      return null;

    User user = null;
    try {
      // Get user id, name, password
      String sql = "SELECT b.id, b.username, b.password FROM "
          + "sessions a "
          + "LEFT JOIN users b ON a.userid=b.id "
          + "WHERE sessionid = ? AND b.username = ?";
      PreparedStatement ps = dbc.prepareStatement(sql);
      ps.setString(1, session.getSessionid());
      ps.setString(2, session.getUsername());
      ResultSet rs = ps.executeQuery();
      if(rs.next()) {
        int id = rs.getInt(1);
        String name = rs.getString(2);
        String password = rs.getString(3);
        user = new User(id, name, password, new ArrayList<String>());
      }
      rs.close();
      ps.close();
      
      // Get user roles
      if(user != null) {
        sql = "SELECT b.role FROM user_roles a "
            + "LEFT JOIN roles b ON a.roleid=b.id "
            + "WHERE a.userid = ?";
        
        ps = dbc.prepareStatement(sql);
        ps.setInt(1, user.getId());
        rs = ps.executeQuery();
        ArrayList<String> roles = new ArrayList<String>();
        while(rs.next()) {
          String role = rs.getString(1);
          roles.add(role);
        }
        user.setRoles(roles);
        rs.close();
        ps.close();
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    
    return user;
  }
  
  public UserCredentials getUser(String username) {
    UserCredentials user = null;
    try {
      String sql = "SELECT id, username, password, fullname, email, affiliation FROM "
          + "users WHERE username = ?";
      PreparedStatement ps = dbc.prepareStatement(sql);
      ps.setString(1, username);
      ResultSet rs = ps.executeQuery();
      if(rs.next()) {
        user = new UserCredentials();
        user.setId(rs.getInt(1));
        user.setName(rs.getString(2));
        user.setPassword(rs.getString(3));
        user.setFullname(rs.getString(4));
        user.setEmail(rs.getString(5));
        user.setAffiliation(rs.getString(6));
      }
      rs.close();
      ps.close();
      
      // Get user roles
      if(user != null) {
        sql = "SELECT b.role FROM user_roles a "
            + "LEFT JOIN roles b ON a.roleid=b.id "
            + "WHERE a.userid = ?";
        
        ps = dbc.prepareStatement(sql);
        ps.setInt(1, user.getId());
        rs = ps.executeQuery();
        ArrayList<String> roles = new ArrayList<String>();
        while(rs.next()) {
          String role = rs.getString(1);
          roles.add(role);
        }
        user.setRoles(roles);
        rs.close();
        ps.close();
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return user;    
  }
  
  public boolean userExists(String username) {
    try {
      String sql = "SELECT id FROM users WHERE username = ?";
      PreparedStatement ps = dbc.prepareStatement(sql);
      ps.setString(1, username);
      ResultSet rs = ps.executeQuery();
      boolean exists = false;
      while(rs.next())
        exists = true;
      rs.close();
      ps.close();
      return exists;
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }
  
  public Map<String, Integer> getRoles() {
    Map<String, Integer> roles = new HashMap<String, Integer>();
    try {
      String sql = "SELECT id, role FROM roles";
      PreparedStatement ps = dbc.prepareStatement(sql);
      ResultSet rs = ps.executeQuery();
      while(rs.next())
        roles.put(rs.getString(2), rs.getInt(1));
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return roles;
  }
  
  public List<String> getUsers() {
    List<String> usernames = new ArrayList<String>();
    try {
      String sql = "SELECT username FROM users";
      PreparedStatement ps = dbc.prepareStatement(sql);
      ResultSet rs = ps.executeQuery();
      while(rs.next())
        usernames.add(rs.getString(1));
      rs.close();
      ps.close();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return usernames;
  }
  
  public int getUserId(String username) {
    try {
      String sql = "SELECT id FROM users WHERE username = ?";
      PreparedStatement ps = dbc.prepareStatement(sql);
      ps.setString(1, username);
      ResultSet rs = ps.executeQuery();
      int uid = -1;
      if(rs.next())
        uid = rs.getInt(1);
      rs.close();
      ps.close();
      return uid;
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return -1;
  }
  
  public void deleteUser(String username) {
    try {
      int uid = getUserId(username);
      if(uid > 0) {
        String sql = "DELETE FROM users WHERE username = ?";
        PreparedStatement ps = dbc.prepareStatement(sql);
        ps.setString(1, username);
        ps.executeUpdate();
        ps.close();
        
        sql = "DELETE FROM sessions WHERE userid = ?";
        ps = dbc.prepareStatement(sql);
        ps.setInt(1, uid);
        ps.executeUpdate();
        ps.close(); 
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  public UserCredentials addUser(UserCredentials user) {
    try {
      String sql = "INSERT INTO users (username, password, fullname, email, affiliation)"
          + "VALUES (?, ?, ?, ?, ?)";
      PreparedStatement ps = dbc.prepareStatement(sql, new int[]{1});
      ps.setString(1, user.getName());
      String cryptedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt(12));
      ps.setString(2, cryptedPassword);
      ps.setString(3, user.getFullname());
      ps.setString(4, user.getEmail());
      ps.setString(5, user.getAffiliation());
      ps.executeUpdate();
      
      ResultSet rs = ps.getGeneratedKeys();
      while(rs.next())
        user.setId(rs.getInt(1));
      rs.close();
      ps.close();
      
      Map<String, Integer> roles = getRoles();
      sql = "INSERT INTO user_roles VALUES ";
      int i=0;
      for(String rolename : user.getRoles()) {
        if(roles.containsKey(rolename)) {
          if(i > 0)
            sql += ",";
          sql += "(" + user.getId() + "," + roles.get(rolename) + ")";
          i++;
        }
      }
      ps = dbc.prepareStatement(sql);
      ps.executeUpdate();
      ps.close();
      
      return user;
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public UserCredentials updateUser(String username, UserCredentials user) {
    try {
      UserCredentials curuser = this.getUser(username);
      String password = curuser.getPassword();
      // If the password has changed (i.e. user entered a new password, then encrypt it)
      if(!password.equals(user.getPassword()))
        password = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt(12));
      
      String sql = "UPDATE users SET "
          + "password = ?, fullname = ?, email = ?, affiliation = ? "
          + "WHERE username = ?";
      PreparedStatement ps = dbc.prepareStatement(sql);
      ps.setString(1, password);
      ps.setString(2, user.getFullname());
      ps.setString(3, user.getEmail());
      ps.setString(4, user.getAffiliation());
      ps.setString(5, username);
      ps.executeUpdate();
      ps.close();
      
      int uid = this.getUserId(username);
      if(uid > 0) {
        sql = "DELETE FROM user_roles WHERE userid = ?";
        ps = dbc.prepareStatement(sql);
        ps.setInt(1, uid);
        ps.executeUpdate();
        ps.close();
        
        Map<String, Integer> roles = getRoles();
        sql = "INSERT INTO user_roles VALUES ";
        int i=0;
        for(String rolename : user.getRoles()) {
          if(roles.containsKey(rolename)) {
            if(i > 0)
              sql += ",";
            sql += "(" + uid + "," + roles.get(rolename) + ")";
            i++;
          }
        }
        ps = dbc.prepareStatement(sql);
        ps.executeUpdate();
        ps.close();
      }
      return user;
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }
}
