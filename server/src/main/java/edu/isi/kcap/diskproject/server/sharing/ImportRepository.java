package edu.isi.kcap.diskproject.server.sharing;

public class ImportRepository {
  public String serverURL;

  public ImportRepository(String serverURL) {
    this.serverURL = serverURL;
  }

  public String getServerURL() {
    return serverURL;
  }

  public void setServerURL(String serverURL) {
    this.serverURL = serverURL;
  }
}
