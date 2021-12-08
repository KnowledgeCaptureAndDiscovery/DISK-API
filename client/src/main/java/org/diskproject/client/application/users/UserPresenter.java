package org.diskproject.client.application.users;

import org.diskproject.client.application.ApplicationPresenter;
import org.diskproject.client.application.ApplicationSubview;
import org.diskproject.client.place.NameTokens;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.ProxyPlace;

public class UserPresenter extends
    Presenter<UserPresenter.MyView, UserPresenter.MyProxy> {

  @ProxyCodeSplit
  @NameToken(NameTokens.users)
  public interface MyProxy extends ProxyPlace<UserPresenter> {
  }

  public interface MyView extends ApplicationSubview {
  }

  @Inject
  public UserPresenter(EventBus eventBus, MyView view, MyProxy proxy) {
    super(eventBus, view, proxy, ApplicationPresenter.CONTENT_SLOT);
  }
  
}
