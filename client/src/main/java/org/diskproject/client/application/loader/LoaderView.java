package org.diskproject.client.application.loader;

import org.diskproject.client.application.ApplicationSubviewImpl;

import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class LoaderView extends ApplicationSubviewImpl 
  implements LoaderPresenter.MyView {

  interface Binder extends UiBinder<Widget, LoaderView> {
  }

  @Inject
  public LoaderView(Binder binder) {
    initWidget(binder.createAndBindUi(this));
  }

  @Override
  public void initializeParameters(String userid, String domain, String[] params, boolean edit,
      SimplePanel sidebar, SimplePanel toolbar) {
  }
}
