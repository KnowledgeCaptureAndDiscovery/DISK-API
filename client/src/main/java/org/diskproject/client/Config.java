package org.diskproject.client;

import java.util.Map;

public class Config {
  public static Map<String, String> serverConfig = null;
  public static Map<String, String> endpoints = null;

  public native static String getServerURL() /*-{
    return $wnd.CONFIG.SERVER;
  }-*/;

  public native static String getPortalTitle() /*-{
    return $wnd.CONFIG.TITLE;
  }-*/;
  
  public native static String getOKColor() /*-{
    return $wnd.CONFIG.COLORS.ok;
  }-*/;
  
  public native static String getErrorColor() /*-{
    return $wnd.CONFIG.COLORS.error;
  }-*/;
  
  public native static String getHomeHTML() /*-{
    return $wnd.CONFIG.HOME;
  }-*/;
  
  public static String getWingsUserid() {
    if(serverConfig != null) 
      return serverConfig.get("username");
    return null;
  };
  
  public static String getWingsDomain() {
    if(serverConfig != null) 
      return serverConfig.get("domain");
    return null;
  }

}
