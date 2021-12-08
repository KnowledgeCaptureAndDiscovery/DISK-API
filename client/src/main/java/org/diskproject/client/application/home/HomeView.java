package org.diskproject.client.application.home;

import org.diskproject.client.Config;
import org.diskproject.client.application.ApplicationSubviewImpl;

import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class HomeView extends ApplicationSubviewImpl implements HomePresenter.MyView {

  @UiField
  HTML html;
  
  interface Binder extends UiBinder<Widget, HomeView> {
  }

  @Inject
  public HomeView(Binder binder) {
    initWidget(binder.createAndBindUi(this));
    //title.setText(Config.getPortalTitle() + " Portal");
    html.setHTML(Config.getHomeHTML());
  }

  @Override
  public void initializeParameters(String userid, String domain, String[] params, boolean edit,
      SimplePanel sidebar, SimplePanel toolbar) {
  }
}
