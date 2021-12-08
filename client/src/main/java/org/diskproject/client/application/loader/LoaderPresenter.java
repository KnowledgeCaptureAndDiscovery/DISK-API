package org.diskproject.client.application.loader;

import org.diskproject.client.application.ApplicationPresenter;
import org.diskproject.client.application.ApplicationSubview;
import org.diskproject.client.place.NameTokens;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyStandard;
import com.gwtplatform.mvp.client.proxy.ProxyPlace;

public class LoaderPresenter extends
    Presenter<LoaderPresenter.MyView, LoaderPresenter.MyProxy> {

  @ProxyStandard
  @NameToken(NameTokens.loader)
  public interface MyProxy extends ProxyPlace<LoaderPresenter> {
  }

  public interface MyView extends ApplicationSubview {
  }

  @Inject
  public LoaderPresenter(EventBus eventBus, MyView view, MyProxy proxy) {
    super(eventBus, view, proxy, ApplicationPresenter.CONTENT_SLOT);
  }
  
}
