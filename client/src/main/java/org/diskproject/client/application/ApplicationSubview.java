package org.diskproject.client.application;

import com.google.gwt.user.client.ui.SimplePanel;
import com.gwtplatform.mvp.client.View;

public interface ApplicationSubview extends View {
  public void initializeParameters(String userid, String domain, String[] params, boolean edit, 
      SimplePanel sidebar, SimplePanel toolbar);
}
