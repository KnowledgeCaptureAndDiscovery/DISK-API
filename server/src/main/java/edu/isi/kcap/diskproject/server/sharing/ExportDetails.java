package edu.isi.kcap.diskproject.server.sharing;

import java.util.ArrayList;

public class ExportDetails {
  public ArrayList<String> allowedIPs;
  public String featureQuery;

  public ExportDetails() {
    allowedIPs = new ArrayList<String>();
  }

  public ArrayList<String> getAllowedIPs() {
    return allowedIPs;
  }

  public void setAllowedIPs(ArrayList<String> allowedIPs) {
    this.allowedIPs = allowedIPs;
  }

  public String getFeatureQuery() {
    return featureQuery;
  }

  public void setFeatureQuery(String featureQuery) {
    this.featureQuery = featureQuery;
  }
}
